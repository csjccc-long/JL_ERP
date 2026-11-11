<template>
  <div>
    <el-popover ref="noticePopover" placement="bottom-end" :width="320" trigger="click" :offset="12" v-model:visible="noticeVisible" popper-class="notice-popover">
      <!-- 弹出内容 -->
      <div class="notice-header">
        <span class="notice-title">通知公告</span>
        <span class="notice-mark-all" @click="markAllRead">全部已读</span>
      </div>
      <div v-if="noticeLoading" class="notice-loading">
        <el-icon class="is-loading"><Loading /></el-icon> 加载中...
      </div>
      <div v-else-if="noticeList.length === 0" class="notice-empty">
        <el-icon style="font-size:24px;display:block;margin-bottom:6px;"><Postcard /></el-icon>
        暂无公告
      </div>
      <div v-else>
        <div v-for="item in noticeList" :key="item.noticeId" class="notice-item" :class="{ 'is-read': item.isRead }" @click="previewNotice(item)">
          <el-tag size="small" :type="item.noticeType === '1' ? 'warning' : 'success'" class="notice-tag">
            {{ item.noticeType === '1' ? '通知' : '公告' }}
          </el-tag>
          <span class="notice-item-title">{{ item.noticeTitle }}</span>
          <span class="notice-item-date">{{ item.createTime }}</span>
        </div>
      </div>

      <!-- 触发器 -->
      <template #reference>
        <span class="notice-trigger">
          <bell-toggle
            ref="bellRef"
            :pressed="noticeVisible"
            :off-label="'系统通知'"
            :on-label="noticeLabel"
            :label="triggerLabel"
            :count="unreadCount"
            badge
            badge-always
            size="sm"
            :radius="18"
            background="#f4f5f6"
            color="#3b3d40"
            on-background="#00beb4"
            on-color="#ffffff"
            badge-color="#d8494b"
            :ring-amplitude="17"
            :ring-passes="5"
            :ring-duration="820"
            :ring-pivot="16"
            :crossfade-ms="180"
          >
            <template #icon>
              <svg-icon icon-class="bell" />
            </template>
          </bell-toggle>
        </span>
      </template>
    </el-popover>

    <!-- 预览弹窗 -->
    <notice-detail-view ref="noticeViewRef" />
  </div>
</template>

<script setup>
import NoticeDetailView from './DetailView'
import BellToggle from '@/components/BellToggle/index.vue'
import { listNoticeTop, markNoticeRead, markNoticeReadAll } from '@/api/system/notice'

const noticePopover = ref(null)
const bellRef = ref(null)
const noticeList = ref([])
const unreadCount = ref(0)
const noticeLoading = ref(false)
const noticeVisible = ref(false)
let pollTimer = null
const { proxy } = getCurrentInstance()

// 展开态在铃铛上显示的文案（宽度变化会触发胶囊伸缩动画）
const noticeLabel = computed(() => (unreadCount.value > 0 ? `共 ${unreadCount.value} 条未读` : '通知中心'))
const triggerLabel = computed(() =>
  unreadCount.value > 0 ? `系统通知，${unreadCount.value} 条未读` : '系统通知，暂无未读'
)

function applyList(data, unread) {
  noticeList.value = data
  unreadCount.value = unread
}

// 加载顶部公告列表
function loadNoticeTop() {
  noticeLoading.value = true
  listNoticeTop().then(res => {
    const list = res.data || []
    applyList(list, res.unreadCount !== undefined ? res.unreadCount : list.filter(n => !n.isRead).length)
  }).finally(() => {
    noticeLoading.value = false
  })
}

// 静默轮询：有新通知时摇铃，不打扰已展开的面板
function refreshNoticeTop() {
  if (noticeVisible.value) return
  listNoticeTop().then(res => {
    const list = res.data || []
    applyList(list, res.unreadCount !== undefined ? res.unreadCount : list.filter(n => !n.isRead).length)
  }).catch(() => {})
}

onMounted(() => {
  loadNoticeTop()
  pollTimer = setInterval(refreshNoticeTop, 60000)
})

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer)
})

// 预览公告详情
function previewNotice(item) {
  if (!item.isRead) {
    markNoticeRead(item.noticeId).catch(() => {})
    const idx = noticeList.value.indexOf(item)
    if (idx !== -1) noticeList.value[idx] = { ...item, isRead: true }
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }
  proxy.$refs["noticeViewRef"].open(item.noticeId)
}

// 全部已读
function markAllRead() {
  const ids = noticeList.value.map(n => n.noticeId).join(',')
  if (!ids) return
  markNoticeReadAll(ids).catch(() => {})
  noticeList.value = noticeList.value.map(n => ({ ...n, isRead: true }))
  unreadCount.value = 0
}
</script>

<style lang="scss" scoped>
.notice-trigger {
  display: inline-flex;
  align-items: center;
  height: 36px;
  margin: 0 6px;
  vertical-align: middle;
  .svg-icon { fill: currentColor; }
}
.notice-popover { padding: 0 !important; }
.notice-popover .notice-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: #f7f9fb;
  border-bottom: 1px solid #eee;
  font-size: 13px;
  font-weight: 600;
  color: #333;
}
.notice-popover .notice-mark-all {
  font-size: 12px;
  color: var(--el-color-primary);
  font-weight: normal;
  cursor: pointer;
}
.notice-popover .notice-mark-all:hover { color: #2b7cc1; }
.notice-popover .notice-loading,
.notice-popover .notice-empty {
  padding: 24px;
  text-align: center;
  color: #bbb;
  font-size: 12px;
  line-height: 1.8;
}
.notice-popover .notice-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: background 0.15s;
}
.notice-popover .notice-item:last-child { border-bottom: none; }
.notice-popover .notice-item:hover { background: #f7f9fb; }
.notice-popover .notice-item.is-read .notice-tag,
.notice-popover .notice-item.is-read .notice-item-title,
.notice-popover .notice-item.is-read .notice-item-date { opacity: 0.45; filter: grayscale(1); color: #999; }
.notice-popover .notice-tag { flex-shrink: 0; }
.notice-popover .notice-item-title {
  flex: 1;
  font-size: 12px;
  color: #333;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.notice-popover .notice-item-date {
  flex-shrink: 0;
  font-size: 11px;
  color: #bbb;
}
</style>
