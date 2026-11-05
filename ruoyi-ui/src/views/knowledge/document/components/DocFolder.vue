<template>
  <div
    ref="rootRef"
    class="doc-folder"
    :class="{ 'is-peek': hovered && !opened, 'is-open': opened, 'is-shredding': shredding, 'is-disabled': doc.parseStatus === 'DISABLED' }"
    tabindex="0"
    role="group"
    :aria-label="'文档 ' + doc.documentName"
    @mouseenter="hovered = true"
    @mouseleave="onLeave"
    @focus="hovered = true"
    @blur="hovered = false"
    @pointermove="onPointerMove"
    @click="onFolderClick"
    @keydown.enter.self.prevent="onFolderClick"
  >
    <div ref="stageRef" class="folder-stage">
      <div class="folder-body" :style="bodyStyle">
        <span class="folder-ground" aria-hidden="true"></span>

        <svg class="folder-back" :width="W" :height="TAB_H + H" :viewBox="`0 0 ${W} ${TAB_H + H}`" aria-hidden="true">
          <defs>
            <linearGradient id="kbFolderBack" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#2ba49d" />
              <stop offset="100%" stop-color="#25827d" />
            </linearGradient>
          </defs>
          <path :d="BACK_PATH" fill="url(#kbFolderBack)" />
          <text class="folder-tab-label" :x="TAB_X + TAB_W / 2" :y="TAB_H / 2 + 4" text-anchor="middle">{{ ext }}</text>
        </svg>

        <button
          v-for="(action, actionIndex) in ACTIONS"
          :key="action.key"
          type="button"
          class="folder-paper"
              :class="['paper--' + action.key, { 'is-flying': flyFlags[actionIndex], 'is-eaten': eatenFlags[actionIndex] }]"
          :style="{
            '--eat-x': eatOffsets[actionIndex].x,
            '--eat-y': eatOffsets[actionIndex].y,
            '--eat-s': eatOffsets[actionIndex].s,
          }"
          :aria-label="action.label"
          :tabindex="opened ? 0 : -1"
          @click.stop="onAction(action.key)"
        >
          <el-icon :size="22"><component :is="action.icon" /></el-icon>
          <span class="paper-label">{{ action.label }}</span>
          <span class="paper-lines" aria-hidden="true"><i></i><i></i></span>
        </button>

        <div class="folder-front-wrap" aria-hidden="true">
          <div class="folder-front">
            <span class="folder-shine"></span>
          </div>
        </div>

        <div v-if="canDelete" class="shred-wrapper" :class="{ 'is-busy': busy }">
          <button
            ref="deleteBtnRef"
            v-hasPermi="['knowledge:document:remove']"
            type="button"
            class="btn-delete"
            title="停用并删除"
            aria-label="停用并删除文档"
            @click.stop="handleDelete"
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" class="btn-svg" aria-hidden="true">
              <path fill-rule="evenodd" clip-rule="evenodd" d="M16.5 4.478v.227a48.816 48.816 0 0 1 3.878.512.75.75 0 1 1-.256 1.478l-.209-.035-1.005 13.07a3 3 0 0 1-2.991 2.77H8.084a3 3 0 0 1-2.991-2.77L4.087 6.66l-.209.035a.75.75 0 0 1-.256-1.478A48.567 48.567 0 0 1 7.5 4.705v-.227c0-1.564 1.213-2.9 2.816-2.951a52.662 52.662 0 0 1 3.369 0c1.603.051 2.815 1.387 2.815 2.951Zm-6.136-1.452a51.196 51.196 0 0 1 3.273 0C14.39 3.05 15 3.684 15 4.478v.113a49.488 49.488 0 0 0-6 0v-.113c0-.794.609-1.428 1.364-1.452Zm-.355 5.945a.75.75 0 1 0-1.5.058l.347 9a.75.75 0 1 0 1.499-.058l-.346-9Zm5.48.058a.75.75 0 1 0-1.498-.058l-.347 9a.75.75 0 0 0 1.5.058l.345-9Z" />
            </svg>
            <span>删除</span>
          </button>
          <svg
            :key="shredTick"
            class="shredded"
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 154.57 151.52"
            aria-hidden="true"
          >
            <path v-for="(path, index) in SHRED_PATHS" :key="index" class="shred" :d="path"></path>
          </svg>
        </div>

        <button
          v-if="!canDelete"
          v-hasPermi="['knowledge:document:upload', 'knowledge:document:remove']"
          type="button"
          class="enable-btn"
          title="启用文档"
          aria-label="启用该文档"
          @click.stop="emit('enable', doc)"
        >
          <svg viewBox="0 0 24 24" width="12" height="12" aria-hidden="true">
            <path fill="currentColor" d="M12 5V2L7 6l5 4V7a5 5 0 1 1-5 5H5a7 7 0 1 0 7-7Z" />
          </svg>
          <span>启用</span>
        </button>
      </div>
    </div>

    <div class="folder-meta">
      <div class="folder-name" :title="doc.documentName" @click.stop="emit('open', doc)">{{ doc.documentName }}</div>
      <div class="folder-line">
        <span class="status-chip" :class="'tone-' + tone">{{ statusLabel }}</span>
        <span class="folder-dim">v{{ doc.documentVersion }}</span>
        <span class="folder-dim">{{ formatSize(doc.fileSize) }}</span>
      </div>
      <div class="folder-line folder-line--muted">
        <span class="folder-space" :title="spaceName">{{ spaceName }}</span>
        <span class="folder-dot"></span>
        <span>{{ doc.chunkCount || 0 }} 分片</span>
        <template v-if="doc.links && doc.links.length">
          <span class="folder-dot"></span>
          <span>{{ doc.links.length }} 关联</span>
        </template>
      </div>
      <div v-if="doc.errorMessage" class="folder-error" :title="doc.errorMessage">{{ doc.errorMessage }}</div>
      <div v-else class="folder-time">{{ doc.createTime }}</div>
    </div>
  </div>
</template>

<script setup name="DocFolder">
import { Download, Link, View } from "@element-plus/icons-vue"

const props = defineProps({
  doc: { type: Object, required: true },
  spaceName: { type: String, default: "未归属空间" },
  statusLabel: { type: String, default: "" },
  statusType: { type: String, default: "info" }
})

const emit = defineEmits(["open", "preview", "download", "link", "disable", "enable"])

const { proxy } = getCurrentInstance()
const rootRef = ref(null)
const stageRef = ref(null)
const hovered = ref(false)
const opened = ref(false)
const shredding = ref(false)
const deleteBtnRef = ref(null)
const eatOffsets = ref([{ x: "0px", y: "0px" }, { x: "0px", y: "0px" }, { x: "0px", y: "0px" }])
const flyFlags = ref([false, false, false])
const eatenFlags = ref([false, false, false])
const busy = ref(false)
const shredTick = ref(0)

/* Uiverse Shred 按钮的 11 条碎纸条 path（viewBox 0 0 154.57 151.52） */
const SHRED_PATHS = [
  "M22.74,0v59.49c0,3.37-.21,7.86-.53,12.79-1.34,20.7-4.18,32.27-8.35,52.58l-6.96,24.13",
  "M33.65,0v59.49c0,3.37-.17,7.86-.42,12.79-1.07,20.7-3.34,32.27-6.68,52.58l-5.57,24.13",
  "M44.56,0v59.49c0,3.37-.13,7.86-.32,12.79-.8,20.7-2.51,32.27-5.01,52.58l-4.18,24.13",
  "M55.47,0v59.49c0,3.37-.08,7.86-.21,12.79-.54,20.7-1.67,32.27-3.34,52.58l-2.78,24.13",
  "M66.37,0v59.49c0,3.37-.04,7.86-.11,12.79-.27,20.7-.84,32.27-1.67,52.58l-1.39,24.13",
  "M77.28,0v59.49c0,3.37,0,7.86,0,12.79,0,20.7,0,32.27,0,52.58v24.13",
  "M88.19,0v59.49c0,3.37.04,7.86.11,12.79.27,20.7.84,32.27,1.67,52.58l1.39,24.13",
  "M99.1,0v59.49c0,3.37.08,7.86.21,12.79.54,20.7,1.67,32.27,3.34,52.58l2.78,24.13",
  "M110.01,0v59.49c0,3.37.13,7.86.32,12.79.8,20.7,2.51,32.27,5.01,52.58l4.18,24.13",
  "M120.92,0v59.49c0,3.37.17,7.86.42,12.79,1.07,20.7,3.34,32.27,6.68,52.58l5.57,24.13",
  "M131.83,0v59.49c0,3.37.21,7.86.53,12.79,1.34,20.7,4.18,32.27,8.35,52.58l6.96,24.13"
]
const tiltX = ref(0)
const tiltY = ref(0)

/* 几何参数取自 react-bits Folder（flap / solid / tabPosition=right / openOn=click） */
const W = 224
const H = 176
const TAB_H = 20
const R = 12
const TAB_W = 78
const SHOULDER = 15
const TAB_X = W - R - 5 - SHOULDER - TAB_W

const BACK_PATH = [
  `M 0 ${TAB_H + R}`,
  `Q 0 ${TAB_H} ${R} ${TAB_H}`,
  `L ${TAB_X - SHOULDER} ${TAB_H}`,
  `C ${TAB_X} ${TAB_H} ${TAB_X} 0 ${TAB_X + SHOULDER} 0`,
  `L ${TAB_X + TAB_W - SHOULDER} 0`,
  `C ${TAB_X + TAB_W} 0 ${TAB_X + TAB_W} ${TAB_H} ${TAB_X + TAB_W + SHOULDER} ${TAB_H}`,
  `L ${W - R} ${TAB_H}`,
  `Q ${W} ${TAB_H} ${W} ${TAB_H + R}`,
  `L ${W} ${TAB_H + H - R}`,
  `Q ${W} ${TAB_H + H} ${W - R} ${TAB_H + H}`,
  `L ${R} ${TAB_H + H}`,
  `Q 0 ${TAB_H + H} 0 ${TAB_H + H - R}`,
  "Z"
].join(" ")

const ACTIONS = [
  { key: "preview", label: "预览", icon: View },
  { key: "download", label: "下载", icon: Download },
  { key: "link", label: "关联", icon: Link }
]

const bodyStyle = computed(() => ({
  transform: `rotateX(${tiltY.value.toFixed(2)}deg) rotateY(${tiltX.value.toFixed(2)}deg) scale(${hovered.value ? 1.008 : 1})`
}))

const ext = computed(() => {
  const name = props.doc.documentName || ""
  const raw = name.includes(".") ? name.split(".").pop() : ""
  const value = (raw || "").toUpperCase()
  return value && value.length <= 5 ? value : "FILE"
})

const canDelete = computed(() => props.doc.parseStatus !== "DISABLED")
const tone = computed(() => {
  const map = { READY: "ready", FAILED: "failed", INDEXING: "indexing", UPLOADED: "uploaded", DISABLED: "disabled" }
  return map[props.doc.parseStatus] || "uploaded"
})

function formatSize(size) {
  if (!size && size !== 0) return "-"
  if (size < 1024) return size + " B"
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + " KB"
  return (size / 1024 / 1024).toFixed(2) + " MB"
}

function onPointerMove(event) {
  if (!stageRef.value) return
  const rect = stageRef.value.getBoundingClientRect()
  const px = ((event.clientX - rect.left) / rect.width) * 2 - 1
  const py = ((event.clientY - rect.top) / rect.height) * 2 - 1
  tiltX.value = Math.max(-8, Math.min(8, px * 5))
  tiltY.value = Math.max(-8, Math.min(8, -py * 4))
}

function onLeave() {
  hovered.value = false
  tiltX.value = 0
  tiltY.value = 0
}

/* 与源码 openOn="click" 一致：hover 只是 peek，点击才完全打开 */
function onFolderClick() {
  if (shredding.value) return
  opened.value = !opened.value
}

function close() {
  opened.value = false
}

function onDocPointerDown(event) {
  if (!opened.value) return
  if (rootRef.value && rootRef.value.contains(event.target)) return
  close()
}

function onKeydown(event) {
  if (event.key === "Escape" && opened.value) close()
}

onMounted(() => {
  document.addEventListener("pointerdown", onDocPointerDown)
  document.addEventListener("keydown", onKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener("pointerdown", onDocPointerDown)
  document.removeEventListener("keydown", onKeydown)
})

function onAction(key) {
  if (key === "preview") emit("preview", props.doc)
  else if (key === "download") emit("download", props.doc)
  else emit("link", props.doc)
}

function handleDelete() {
  if (shredding.value) return
  proxy.$modal.confirm("停用后该文档不再参与检索，是否确认停用？").then(() => {
    opened.value = true
    nextTick(() => {
      window.setTimeout(startShredSequence, 380)
    })
  }).catch(() => {})
}

/* 三张卡片依次飞进删除按钮，被按钮吞下并粉碎，最后整个文件夹淡出 */
function startShredSequence() {
  if (!rootRef.value || !deleteBtnRef.value) return
  const cards = Array.from(rootRef.value.querySelectorAll(".folder-paper"))
  const button = deleteBtnRef.value.getBoundingClientRect()
  const buttonX = button.left + button.width / 2
  const buttonY = button.top + button.height / 2

  // 卡片缩到和碎纸屑同宽（都窄于删除按钮），飞进按钮后从下沿吐出碎屑
  const STRIP_W = 84
  cards.forEach((card, index) => {
    const rect = card.getBoundingClientRect()
    const dx = Math.round(buttonX - (rect.left + rect.width / 2))
    // 飞到按钮上方居中，卡片缩小后底边正好落在按钮入口处
    const scale = Math.min(1, STRIP_W / rect.width)
    const dy = Math.round(button.top - (rect.height * scale) / 2 - (rect.top + rect.height / 2))
    eatOffsets.value[index] = { x: dx + "px", y: dy + "px", s: scale }
  })

  // 一张飞过去、当场粉碎完，再飞下一张（粉碎动画 1.5s，与 Uiverse 源码一致）
  const FLY_MS = 360
  const FEED_MS = 1500
  const CYCLE = FEED_MS

  cards.forEach((card, index) => {
    const start = index * CYCLE
    window.setTimeout(() => { flyFlags.value[index] = true }, start)
    window.setTimeout(() => {
      eatenFlags.value[index] = true
      shredTick.value += 1
      busy.value = true
    }, start + FLY_MS)
    window.setTimeout(() => { busy.value = false }, start + FLY_MS + FEED_MS)
  })

  const fadeAt = (cards.length - 1) * CYCLE + FLY_MS + FEED_MS + 120
  const doneAt = fadeAt + 900

  window.setTimeout(() => { shredding.value = true }, fadeAt)
  window.setTimeout(() => emit("disable", props.doc), doneAt)
}
</script>

<style scoped lang="scss">
.doc-folder {
  width: 300px;
  max-width: 100%;
  outline: none;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.doc-folder:focus-visible .folder-stage {
  border-radius: 16px;
  box-shadow: 0 0 0 2px rgba(0, 190, 180, 0.45);
}

.folder-stage {
  position: relative;
  z-index: 2;
  width: 300px;
  height: 292px;
  perspective: 1120px;
  perspective-origin: 50% 62%;
}

.folder-body {
  position: absolute;
  left: 50%;
  top: 78px;
  width: 224px;
  height: 196px;
  margin-left: -112px;
  transition: transform 0.34s ease-out;
}

.folder-ground {
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: -4px;
  height: 26px;
  border-radius: 50%;
  background: rgba(20, 60, 58, 0.3);
  filter: blur(12px);
  z-index: 1;
}

.folder-back {
  position: absolute;
  left: 0;
  top: 0;
  z-index: 4;
  overflow: visible;
  filter: drop-shadow(0 12px 18px rgba(9, 60, 57, 0.2));
}

.folder-tab-label {
  fill: rgba(255, 255, 255, 0.94);
  font-family: inherit;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

/* ---------- 纸张（三张动作卡） ---------- */
.folder-paper {
  position: absolute;
  left: 0;
  top: 0;
  width: 104px;
  height: 124px;
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0;
  border: 0;
  border-radius: 7px;
  background: linear-gradient(180deg, #ffffff 0%, #f7fafa 100%);
  color: #0f7d77;
  box-shadow:
    0 0 0 0.5px rgba(0, 0, 0, 0.06),
    0 2px 4px rgba(0, 0, 0, 0.1),
    0 10px 18px -12px rgba(9, 55, 53, 0.5);
  cursor: pointer;
  pointer-events: none;
  clip-path: inset(0 0 0 0);
  margin: -62px 0 0 -52px;
  transform: translate(var(--x, 112px), var(--y, 134px)) rotate(var(--r, 0deg)) scale(var(--s, 1));
  transition: transform 0.5s cubic-bezier(0.25, 1.1, 0.35, 1), opacity 0.18s ease, box-shadow 0.3s ease;
  will-change: transform;
}

/* 静止态卡片完全藏在文件夹内：即使有 1~2px 的角度溢出也看不到 */
.doc-folder:not(.is-peek):not(.is-open):not(.is-shredding) .folder-paper {
  opacity: 0;
}

.paper-label {
  font-size: 12px;
  font-weight: 500;
  color: #4c5558;
  letter-spacing: 0.02em;
}

.paper-lines {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 54px;

  i {
    height: 3px;
    border-radius: 999px;
    background: rgba(15, 125, 119, 0.13);
  }

  i:last-child {
    width: 74%;
  }
}

.paper--preview {
  --x: 148px;
  --y: 137px;
  --r: 3deg;
  z-index: 13;
}

.paper--download {
  --x: 74px;
  --y: 134px;
  --r: -3.5deg;
  z-index: 12;
}

.paper--link {
  --x: 112px;
  --y: 131px;
  --r: 0.5deg;
  z-index: 11;
}

/* peek：纸张按 FLAP_REST 放大 1.3 倍探出，角度 ×1.6（源码 placeFlap peek 分支） */
.doc-folder.is-peek {
  .paper--preview {
    --x: 159px;
    --y: 114px;
    --r: 4.8deg;
  }

  .paper--download {
    --x: 63px;
    --y: 106px;
    --r: -5.6deg;
  }

  .paper--link {
    --x: 112px;
    --y: 97px;
    --r: 0.8deg;
  }
}

/* open：源码 row 布局 fit=0.94，step = docW*fit + 12 */
.doc-folder.is-open .folder-paper {
  pointer-events: auto;
}

.doc-folder.is-open {
  .paper--preview {
    --x: 4px;
    --y: 44px;
    --r: 0deg;
    transition-delay: 0.02s;
  }

  .paper--download {
    --x: 112px;
    --y: 44px;
    --r: 0deg;
    transition-delay: 0.065s;
  }

  .paper--link {
    --x: 220px;
    --y: 44px;
    --r: 0deg;
    transition-delay: 0.02s;
  }
}

.folder-paper:hover {
  box-shadow:
    0 0 0 0.5px rgba(0, 0, 0, 0.06),
    0 4px 8px rgba(0, 0, 0, 0.12),
    0 20px 28px -14px rgba(9, 55, 53, 0.6);
  color: var(--brand-accent, #00beb4);
}

.folder-paper:focus-visible {
  outline: 2px solid var(--brand-accent, #00beb4);
  outline-offset: 3px;
}

/* ---------- 删除：三张卡片依次飞进删除按钮被粉碎 ---------- */
/* 第一阶段：整张卡飞到删除按钮入口并缩小 */
.folder-paper.is-flying {
  transform: translate(
      calc(var(--x, 112px) + var(--eat-x, 0px)),
      calc(var(--y, 134px) + var(--eat-y, 0px))
    )
    rotate(0deg) scale(var(--eat-s, 0.52));
  box-shadow: 0 8px 16px -6px rgba(9, 55, 53, 0.55);
  pointer-events: none;
  cursor: default;
  z-index: 52;
  transition:
    transform 0.46s cubic-bezier(0.4, 0.02, 0.7, 0.2),
    box-shadow 0.3s ease;
}

/* 第二阶段：到达按钮后像纸一样被从上往下"吃"进碎纸口 */
.folder-paper.is-eaten {
  clip-path: inset(100% 0 0 0);
  pointer-events: none;
  cursor: default;
  transition: clip-path 0.36s cubic-bezier(0.55, 0.02, 0.72, 0.2);
}

/* ---------- 前盖 ---------- */
.folder-front-wrap {
  position: absolute;
  left: 0;
  top: 72.8px;
  width: 224px;
  height: 123.2px;
  z-index: 40;
  perspective: 900px;
  perspective-origin: 50% 100%;
  pointer-events: none;
}

.folder-front {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 12px;
  background: linear-gradient(160deg, #55c6c0 0%, #2fb3ac 48%, #2ba49d 100%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.35),
    inset 0 0 0 1px rgba(255, 255, 255, 0.06),
    0 10px 20px -14px rgba(4, 64, 60, 0.8);
  transform-origin: 50% 100%;
  transform: rotateX(0deg);
  transition: transform 0.52s cubic-bezier(0.3, 0.9, 0.28, 1);
  backface-visibility: hidden;
  overflow: hidden;
}

.folder-shine {
  position: absolute;
  left: -30%;
  top: -40%;
  width: 80%;
  height: 180%;
  background: linear-gradient(100deg, rgba(255, 255, 255, 0) 0%, rgba(255, 255, 255, 0.2) 50%, rgba(255, 255, 255, 0) 100%);
  transform: rotate(8deg);
}

.doc-folder.is-peek .folder-front {
  transform: rotateX(-15deg);
}

.doc-folder.is-open .folder-front,
.doc-folder.is-shredding .folder-front {
  transform: rotateX(-64deg);
}

/* ---------- Uiverse Shred 删除按钮 ---------- */
.shred-wrapper {
  position: absolute;
  left: 50%;
  bottom: -16px;
  z-index: 62;
  display: flex;
  justify-content: center;
  font-size: 14px;
  opacity: 0;
  transform: translate(-50%, 6px) scale(0.94);
  pointer-events: none;
  transition: opacity 0.24s ease, transform 0.32s cubic-bezier(0.3, 1.2, 0.6, 1);
}

.doc-folder.is-peek .shred-wrapper,
.doc-folder.is-open .shred-wrapper {
  opacity: 1;
  transform: translate(-50%, 0) scale(1);
  pointer-events: auto;
  transition-delay: 0.06s;
}

.btn-delete {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  user-select: none;
  border: none;
  border-radius: 10px;
  padding: 0.5rem 1.4rem 0.5rem 1rem;
  background-color: rgb(255, 0, 0);
  background-image: radial-gradient(circle at 50% -50%, #fff, 10%, #0000);
  font-family: "Inter", sans-serif;
  font-weight: 600;
  font-size: 0.9em;
  color: #fff;
  cursor: pointer;
  box-shadow:
    0px 1px 1px rgba(3, 7, 18, 0.16),
    0px 3px 4px rgba(3, 7, 18, 0.14),
    0px 7px 9px rgba(3, 7, 18, 0.11),
    0px 12px 16px rgba(3, 7, 18, 0.09),
    0px 19px 26px rgba(3, 7, 18, 0.07),
    0px 27px 37px rgba(3, 7, 18, 0.05),
    0px 37px 50px rgba(3, 7, 18, 0.02);
  transition: box-shadow 0.3s ease, transform 0.15s ease;

  &:hover {
    box-shadow:
      0px 3px 4px rgba(3, 7, 18, 0.13),
      0px 14px 17px rgba(3, 7, 18, 0.11),
      0px 31px 38px rgba(3, 7, 18, 0.09),
      0px 55px 67px rgba(3, 7, 18, 0.07),
      0px 86px 105px rgba(3, 7, 18, 0.06),
      0px 124px 151px rgba(3, 7, 18, 0.04),
      0px 169px 206px rgba(3, 7, 18, 0.02);
    transform: scale(1.07);
  }

  &:focus {
    outline: none;
  }

  &::after,
  &::before {
    content: "";
    position: absolute;
    top: calc(50% - 10px);
    left: 0;
    width: 10px;
    aspect-ratio: 1 / 2;
    border-radius: 35%;
    background-color: inherit;
    opacity: 1;
    transition: left 0.3s ease, right 0.3s ease, opacity 0.3s ease;
  }

  &::after {
    left: auto;
    right: 0;
  }

  &:hover::after {
    opacity: 1;
    right: -4px;
  }

  &:hover::before {
    opacity: 1;
    left: -4px;
  }

  &:active::after {
    right: 0;
  }

  &:active::before {
    left: 0;
  }
}

.btn-svg {
  width: 24px;
  height: 24px;
  fill: #fff9;
  stroke: #fff0;
  transition: transform 0.3s ease;
}

.btn-delete:hover .btn-svg {
  transform: rotate(-8deg) scale(1.8);
}

.shredded {
  position: absolute;
  left: 50%;
  top: 100%;
  width: 84px;
  height: 82px;
  margin-left: -42px;
  margin-top: -6px;
  overflow: visible;
  z-index: -1;
  stroke: #fff;
  fill: none;
  stroke-width: 0;
  filter: drop-shadow(0 0 2px rgba(12, 44, 52, 0.55)) drop-shadow(0 1px 1px rgba(12, 44, 52, 0.35));
  pointer-events: none;
}

.shred {
  stroke: inherit;
  fill: inherit;
  stroke-linecap: round;
}

.shred-wrapper.is-busy .shredded {
  animation: shred 1.5s cubic-bezier(1, 0.6, 0.6, 1) reverse;
}

@keyframes shred {
  0% {
    stroke-width: 9;
    opacity: 0;
    clip-path: inset(0% 0% 0% 0%);
  }

  30% {
    opacity: 1;
  }

  100% {
    stroke-width: 9;
    clip-path: inset(0% 0% 100% 0%);
  }
}

/* 已停用的文档：底部按钮是"启用"，常驻显示 */
.enable-btn {
  position: absolute;
  left: 50%;
  bottom: -12px;
  z-index: 60;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 26px;
  padding: 0 12px;
  border: 0;
  border-radius: 999px;
  background:
    radial-gradient(circle at 50% -50%, rgba(255, 255, 255, 0.75), 12%, transparent 70%),
    linear-gradient(180deg, #16c6bb 0%, #0aa79d 100%);
  color: #fff;
  font-size: 11px;
  line-height: 1;
  cursor: pointer;
  transform: translate(-50%, 0);
  box-shadow: 0 8px 16px -10px rgba(10, 120, 112, 0.9);
  transition: transform 0.2s ease;

  &:hover {
    transform: translate(-50%, 0) scale(1.04);
  }
}

.doc-folder.is-shredding .folder-body {
  animation: folderShake 0.4s ease-in-out both;
}

.doc-folder.is-shredding .folder-back,
.doc-folder.is-shredding .folder-front {
  transition: opacity 0.26s ease 0.18s;
  opacity: 0.12;
}

.doc-folder.is-shredding .folder-paper {
  opacity: 0;
  transition: opacity 0.24s ease;
}

@keyframes folderShake {
  0%,
  100% {
    transform: translateX(0);
  }
  25% {
    transform: translateX(-3px);
  }
  55% {
    transform: translateX(3px);
  }
  80% {
    transform: translateX(-2px);
  }
}

/* ---------- 文案 ---------- */
.folder-meta {
  width: 224px;
  margin: 10px auto 0;
  padding: 0 2px;
}

.folder-name {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: 13px;
  font-weight: 600;
  line-height: 18px;
  color: #1b1c1e;
  word-break: break-all;

  &:hover {
    color: #0f9d95;
  }
}

.folder-line {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  font-size: 11.5px;
  color: #75787a;
  min-width: 0;
}

.folder-line--muted {
  color: #9a9ea1;
}

.folder-space {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 104px;
}

.folder-dim {
  color: #8b8f92;
}

.folder-dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: #c9cccf;
  flex: none;
}

.folder-time {
  margin-top: 5px;
  font-size: 11px;
  color: #b0b4b7;
}

.folder-error {
  margin-top: 5px;
  font-size: 11px;
  color: #c0453f;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-chip {
  flex: none;
  padding: 0 7px;
  border-radius: 999px;
  font-size: 10.5px;
  line-height: 18px;
  font-weight: 500;

  &.tone-ready {
    background: #e6f7ee;
    color: #2f8f57;
  }

  &.tone-failed {
    background: #fdecec;
    color: #c0453f;
  }

  &.tone-indexing {
    background: #fff5e2;
    color: #b57b12;
  }

  &.tone-uploaded {
    background: #eef1f4;
    color: #5b6670;
  }

  &.tone-disabled {
    background: #f1f1f2;
    color: #9a9ea2;
  }
}

.doc-folder.is-disabled {
  .folder-back,
  .folder-front,
  .folder-paper,
  .folder-floor {
    filter: grayscale(0.7);
    opacity: 0.66;
  }

  .folder-meta {
    opacity: 0.7;
  }
}
</style>
