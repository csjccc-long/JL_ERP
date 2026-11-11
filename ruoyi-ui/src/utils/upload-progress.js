import { reactive } from 'vue'

/**
 * 全局上传进度：所有上传入口（axios FormData 请求 + el-upload 组件）
 * 都写进同一个响应式队列，由 <upload-progress> 统一用波浪动效回显。
 */
const DONE_HOLD = 1200 // 完成后停留展示 100%
const ERROR_HOLD = 2400 // 失败后停留展示错误态
const MIN_VISIBLE = 800 // 小文件瞬时完成时也保证动效能被看到
const STALE_MS = 5 * 60 * 1000 // 兜底：没有任何回调的“僵尸”上传 5 分钟后自动收起

export const uploadProgress = reactive({
  items: []
})

let seq = 0

/**
 * 从 FormData 里找出文件字段（不同接口的字段名不一样：file / avatarfile / uploadFile…）
 */
export function pickUploadFile(formData) {
  if (!formData || typeof formData.get !== 'function') return null
  const preferred = ['file', 'avatarfile', 'uploadFile', 'multipartFile']
  for (const key of preferred) {
    const value = formData.get(key)
    if (value && typeof value === 'object' && typeof value.name === 'string') return value
  }
  if (typeof formData.entries === 'function') {
    for (const [, value] of formData.entries()) {
      if (value && typeof value === 'object' && typeof value.name === 'string') return value
    }
  }
  return null
}

function findItem(id) {
  return uploadProgress.items.find((item) => item.id === id)
}

function removeItem(id) {
  const index = uploadProgress.items.findIndex((item) => item.id === id)
  if (index > -1) uploadProgress.items.splice(index, 1)
}

/**
 * 开始一次上传，返回用于后续更新/结束的 id
 * @param {string} name 文件名
 * @param {number} size 文件大小（字节，可选）
 */
export function beginUpload(name = '文件', size = 0) {
  const id = 'upload-' + ++seq
  const item = {
    id,
    name: name || '文件',
    size: Number(size) || 0,
    percent: 0,
    status: 'uploading',
    startedAt: Date.now(),
    timer: null,
    staleTimer: null
  }
  // 上传被拦截（重复提交）、组件提前卸载等情况下不会有任何回调，兜底收起
  item.staleTimer = setTimeout(() => {
    const current = findItem(id)
    if (current && current.status === 'uploading') endUpload(id, false)
  }, STALE_MS)
  uploadProgress.items.push(item)
  return id
}

/** 0~100 的进度；上传中最多显示 99%，避免“停在 100% 还在等响应” */
export function updateUpload(id, percent) {
  const item = findItem(id)
  if (!item || item.status !== 'uploading') return
  const value = Number(percent)
  if (!Number.isFinite(value)) return
  const next = Math.max(0, Math.min(99, Math.round(value)))
  if (next > item.percent) item.percent = next
}

/** 字节级进度（axios onUploadProgress / el-upload 的 loaded、total） */
export function updateUploadBytes(id, loaded, total) {
  const item = findItem(id)
  if (!item) return
  const totalBytes = Number(total) || item.size || 0
  const loadedBytes = Number(loaded) || 0
  if (totalBytes > 0) {
    item.size = totalBytes
    updateUpload(id, (loadedBytes / totalBytes) * 100)
  }
}

/** 结束一次上传：ok=false 表示失败 */
export function endUpload(id, ok = true) {
  const item = findItem(id)
  if (!item || item.status !== 'uploading') return
  if (item.timer) clearTimeout(item.timer)
  if (item.staleTimer) clearTimeout(item.staleTimer)
  const elapsed = Date.now() - item.startedAt
  const wait = Math.max(0, MIN_VISIBLE - elapsed)
  item.status = ok ? 'done' : 'error'
  if (ok) item.percent = 100
  item.timer = setTimeout(() => {
    removeItem(id)
  }, wait + (ok ? DONE_HOLD : ERROR_HOLD))
}

/** 只读快照，便于需要时做自定义展示 */
export function uploadSnapshot() {
  return uploadProgress.items.map((item) => ({ ...item }))
}
