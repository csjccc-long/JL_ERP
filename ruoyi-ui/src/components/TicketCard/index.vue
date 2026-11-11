<template>
  <div
    ref="frameRef"
    class="ticket"
    :class="{ 'is-tilting': tilting, 'is-torn': tornOff }"
    :style="{ width: width + 'px', height: height + 'px' }"
    role="button"
    tabindex="0"
    @pointerenter="handleEnter"
    @pointermove="handleMove"
    @pointerleave="handleLeave"
    @click="$emit('click')"
    @keydown.enter.prevent="$emit('click')"
  >
    <div ref="shapeRef" class="ticket__shape" :style="shapeStyle">
      <div class="ticket__fill" :style="{ background: palette.back }"></div>
      <dithering-surface
        class="ticket__texture"
        :color-back="palette.back"
        :color-front="palette.front"
        :shape="texture.shape"
        :type="texture.type"
        :size="texture.size"
        :scale="texture.scale"
        :rotation="texture.rotation"
        :offset-x="texture.offsetX"
        :offset-y="texture.offsetY"
        :speed="textureSpeed"
        :paused="paused"
      />

      <div class="ticket__perforation" :style="perforationStyle"></div>

      <div v-if="fxKind !== 'stamp' && fxKind !== 'clock'" class="ticket__watermark" :style="watermarkStyle">
        <span>{{ watermark }}</span>
      </div>

      <div class="ticket__content" :style="{ color: palette.ink }">
        <div class="ticket__label" :style="labelStyle">
          <span>{{ label }}</span>
          <span class="ticket__sublabel">{{ sublabel }}</span>
        </div>
        <div class="ticket__name" :style="nameStyle">
          <div v-for="(line, index) in nameLines" :key="index">{{ line }}</div>
        </div>
        <div class="ticket__footer" :style="footerStyle">{{ footer }}</div>
      </div>

      <div
        v-if="fxKind"
        :key="playKey"
        class="ticket__fx"
        :class="'ticket__fx--' + fxKind"
        :style="fxStyle"
        aria-hidden="true"
      >
        <div v-if="fxKind === 'tear'" class="fx-tear">
          <div v-if="!tornOff" class="fx-tear__lift"></div>
          <div v-if="!tornOff" class="fx-tear__edge" :style="tearEdgeStyle"></div>
          <div v-if="!tornOff" class="fx-tear__shadow"></div>
          <div v-if="!tornOff" class="fx-tear__peel"></div>
          <div v-if="!tornOff" class="fx-tear__tip"></div>
          <div v-if="tornOff" class="fx-fringe"></div>
        </div>
        <div v-else-if="fxKind === 'stamp'" class="fx-stamp">
          <div class="fx-stamp__body" :style="stampBodyStyle">
            <span class="fx-stamp__text">{{ stub }}</span>
          </div>
        </div>
        <div v-else-if="fxKind === 'clock'" class="fx-clock">
          <div class="fx-clock__dial" :style="clockDialStyle">
            <span class="fx-clock__face"></span>
            <span class="fx-clock__hand fx-clock__hand--hour"></span>
            <span class="fx-clock__hand fx-clock__hand--minute"></span>
            <span class="fx-clock__hand fx-clock__hand--second"></span>
            <span class="fx-clock__pivot"></span>
          </div>
          <span class="fx-clock__caption">{{ stub }}</span>
        </div>
        <div v-else class="fx-pixels">
          <span
            v-for="cell in pixelCells"
            :key="cell.key"
            class="fx-pixels__cell"
            :class="{ 'is-filled': cell.filled }"
            :style="{ '--d': cell.delay, background: cell.background }"
          ></span>
        </div>
      </div>

      <div v-if="fxKind !== 'stamp' && fxKind !== 'clock'" class="ticket__stub" :style="stubStyle">
        <span>{{ stub }}</span>
      </div>

      <div ref="glareRef" class="ticket__glare" aria-hidden="true"></div>
    </div>

    <div v-if="falling" class="ticket__fall" :style="fallStyle" aria-hidden="true">
      <span class="ticket__fall-text" :style="fallTextStyle">{{ stub }}</span>
      <span class="ticket__fall-mark" :style="fallMarkStyle">{{ watermark }}</span>
    </div>
  </div>
</template>

<script setup name="TicketCard">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue"
import DitheringSurface from "@/components/DitheringSurface/index.vue"

const props = defineProps({
  width: { type: Number, default: 340 },
  label: { type: String, default: "" },
  sublabel: { type: String, default: "" },
  name: { type: String, default: "" },
  footer: { type: String, default: "" },
  stub: { type: String, default: "" },
  watermark: { type: String, default: "" },
  palette: {
    type: Object,
    default: () => ({ back: "#ef671c", front: "#ffc691", highlight: "#fe9046", ink: "#5a3520" })
  },
  texture: {
    type: Object,
    default: () => ({ shape: "warp", type: "random", size: 1.2, scale: 1, rotation: 0, offsetX: 0, offsetY: 0 })
  },
  speed: { type: Number, default: 0.4 },
  paused: { type: Boolean, default: false },
  tilt: { type: Boolean, default: true },
  /** 订单状态码，决定虚线右侧的状态动效（RECEIVED 撕票 / APPROVED 盖章 / PARTIALLY_RECEIVED 像素填充） */
  status: { type: String, default: "" },
  /** 部分到货时的已入库比例（0~1），用于像素水位 */
  progress: { type: Number, default: 0.5 }
})

defineEmits(["click"])

const ASPECT = 741 / 425
const frameRef = ref(null)
const shapeRef = ref(null)
const glareRef = ref(null)
const tilting = ref(false)
const tornOff = ref(false)
/** 撕断后脱落的那半张票（独立绘制，脱离卡片轮廓后自然下坠） */
const falling = ref(false)

const height = computed(() => Math.round((props.width / ASPECT) * 100) / 100)

const MAX_TILT = 8
const TILT_SCALE = 1.02
const GLARE = 0.18

const shapeStyle = computed(() => {
  const path = tornOff.value ? ticketTornClipPath(props.width, height.value) : ticketClipPath(props.width, height.value)
  const value = "path('" + path + "')"
  return { "clip-path": value, "-webkit-clip-path": value }
})

const perforationStyle = computed(() => ({
  left: (562 / 741) * props.width + "px",
  width: Math.max(1, (22 / 1e4) * props.width) + "px",
  backgroundImage: "repeating-linear-gradient(to bottom, " + props.palette.ink + "55 0 " + (0.012 * props.width) + "px, transparent " + (0.012 * props.width) + "px " + (0.024 * props.width) + "px)"
}))

const pad = computed(() => props.width * 0.076)

const labelStyle = computed(() => ({
  left: pad.value + "px",
  top: props.width * 0.078 + "px",
  fontSize: props.width * 0.034 + "px",
  lineHeight: props.width * 0.048 + "px",
  letterSpacing: "0.02em"
}))

const nameLines = computed(() => splitName(props.name))

const nameStyle = computed(() => {
  const size = props.width * 0.085
  const scale = fitScale(nameLines.value, {
    availableWidth: (562 / 741) * props.width - pad.value - props.width * 0.03,
    availableHeight: props.width * 0.2,
    fontSize: size,
    lineHeight: size * 1.05
  })
  return {
    left: pad.value + "px",
    top: props.width * 0.255 + "px",
    fontSize: size * scale + "px",
    lineHeight: size * 1.05 * scale + "px",
    letterSpacing: "-0.01em"
  }
})

const footerStyle = computed(() => ({
  left: pad.value + "px",
  top: props.width * 0.455 + "px",
  fontSize: props.width * 0.032 + "px",
  letterSpacing: "0.02em"
}))

const stubStyle = computed(() => ({
  left: (562 / 741) * props.width + "px",
  width: props.width - (562 / 741) * props.width + "px",
  fontSize: props.width * 0.056 + "px",
  letterSpacing: "0.02em"
}))

const watermarkStyle = computed(() => ({
  left: (562 / 741) * props.width + "px",
  width: props.width - (562 / 741) * props.width + "px",
  fontSize: props.width * 0.09 + "px",
  color: props.palette.front,
  opacity: 0.32
}))

function ticketClipPath(width, height) {
  const r = (25 / 741) * width
  const n = (21 / 741) * width
  const p = (562 / 741) * width
  return [
    "M " + r + " 0",
    "L " + (p - n) + " 0",
    "A " + n + " " + n + " 0 0 0 " + (p + n) + " 0",
    "L " + (width - r) + " 0",
    "A " + r + " " + r + " 0 0 1 " + width + " " + r,
    "L " + width + " " + (height - r),
    "A " + r + " " + r + " 0 0 1 " + (width - r) + " " + height,
    "L " + (p + n) + " " + height,
    "A " + n + " " + n + " 0 0 0 " + (p - n) + " " + height,
    "L " + r + " " + height,
    "A " + r + " " + r + " 0 0 1 0 " + (height - r),
    "L 0 " + r,
    "A " + r + " " + r + " 0 0 1 " + r + " 0",
    "Z"
  ].join(" ")
}

/** 撕掉右侧存根后的轮廓：右边是锯齿断口，左侧仍保留圆角 */
function ticketTornClipPath(width, height) {
  const r = (25 / 741) * width
  const p = (562 / 741) * width
  const steps = 26
  const parts = ["M " + r + " 0", "L " + (p - 4).toFixed(2) + " 0"]
  for (let i = 0; i <= steps; i += 1) {
    const y = (i / steps) * height
    const jitter = i % 2 === 0 ? 0 : 3.2
    const wobble = ((i * 11) % 4) * 0.8
    parts.push("L " + (p - 4 + jitter + wobble).toFixed(2) + " " + y.toFixed(2))
  }
  parts.push(
    "L " + r + " " + height,
    "A " + r + " " + r + " 0 0 1 0 " + (height - r),
    "L 0 " + r,
    "A " + r + " " + r + " 0 0 1 " + r + " 0",
    "Z"
  )
  return parts.join(" ")
}

function splitName(name) {
  const clean = String(name || "").trim()
  if (!clean) return []
  const words = clean.split(/\s+/)
  const lines = []
  words.forEach((word) => {
    if (lines.length < 2) lines.push(word)
    else lines[lines.length - 1] += " " + word
  })
  return lines
}

function charWidth(char) {
  return /[\u4e00-\u9fa5\u3000-\u303f\uff00-\uffef]/.test(char) ? 1 : 0.62
}

function textWidth(text, fontSize) {
  let total = 0
  for (const char of text) total += charWidth(char) * fontSize
  return total
}

function fitScale(lines, options) {
  if (!lines.length) return 1
  const longest = lines.reduce((max, line) => {
    const width = textWidth(line, options.fontSize)
    return width > max ? width : max
  }, 0)
  const blockHeight = lines.length * options.lineHeight
  const widthScale = longest > 0 ? options.availableWidth / longest : 1
  const heightScale = blockHeight > 0 ? options.availableHeight / blockHeight : 1
  return Math.max(0.42, Math.min(1, widthScale, heightScale))
}

const hovered = ref(false)

const textureSpeed = computed(() => (hovered.value ? 0.45 : 0))

function handleEnter() {
  hovered.value = true
  play(0)
  if (!props.tilt) return
  tilting.value = true
}

function handleMove(event) {
  if (!props.tilt) return
  const frame = frameRef.value
  const shape = shapeRef.value
  if (!frame || !shape) return
  const rect = frame.getBoundingClientRect()
  const dx = (event.clientX - rect.left) / rect.width - 0.5
  const dy = (event.clientY - rect.top) / rect.height - 0.5
  shape.style.transform = "perspective(1200px) rotateX(" + (-dy * 2 * MAX_TILT) + "deg) rotateY(" + (dx * 2 * MAX_TILT) + "deg) scale(" + TILT_SCALE + ")"
  if (glareRef.value) {
    glareRef.value.style.background = "radial-gradient(38% 55% at " + ((dx + 0.5) * 100) + "% " + ((dy + 0.5) * 100) + "%, rgba(255,255,255," + GLARE + ") 0%, rgba(255,255,255,0) 70%)"
  }
}

function handleLeave() {
  hovered.value = false
  tilting.value = false
  const shape = shapeRef.value
  if (shape) shape.style.transform = "perspective(1200px) rotateX(0deg) rotateY(0deg) scale(1)"
  if (glareRef.value) glareRef.value.style.background = "transparent"
}

/* ---------------------------------------------------------------
 * 状态动效：全部只发生在虚线（存根区）右侧
 *   RECEIVED            → 沿虚线自上而下撕开，撕完右侧存根整块消失（轮廓改成锯齿）
 *   APPROVED            → 长方形印章砸下 + 冲击环
 *   PENDING_APPROVAL    → 时钟表盘，指针持续旋转
 *   PARTIALLY_RECEIVED  → 像素块自下而上填充到已入库比例
 * ------------------------------------------------------------- */
const FX_BY_STATUS = {
  RECEIVED: "tear",
  APPROVED: "stamp",
  PENDING_APPROVAL: "clock",
  PARTIALLY_RECEIVED: "fill"
}
/** 撕口动画走完的时刻（与 .fx-tear 的 0.82s 对齐）；之后存根脱落、卡片轮廓收窄 */
const TORN_DELAY = 820
const PIXEL_COLS = 5
const PIXEL_ROWS = 16
const TEAR_WIDTH = 22
const PERF_RATIO = 562 / 741

const fxKind = computed(() => FX_BY_STATUS[props.status] || "")

const stubLeft = computed(() => PERF_RATIO * props.width)

const fxStyle = computed(() => {
  if (tornOff.value) {
    // 撕掉之后只剩贴在锯齿断口内侧的纸纤维
    return { left: Math.max(0, stubLeft.value - 12) + "px", width: "14px" }
  }
  return {
    left: Math.max(0, stubLeft.value - 2) + "px",
    width: props.width - stubLeft.value + 2 + "px"
  }
})

/**
 * 断口不是一条边，而是两侧各被扯出毛边的一段窄带：
 * 左界（卡片侧）0~5px、右界（存根侧）11~18px 都逐段抖动，带子因此宽窄不一。
 */
function buildTearBand(steps) {
  const left = []
  const right = []
  for (let i = 0; i <= steps; i += 1) {
    const y = (i / steps) * 100
    const l = 0.5 + ((i * 13) % 4) * 1.3
    const r = TEAR_WIDTH - 2 - ((i * 7) % 5) * 1.7
    left.push(l.toFixed(1) + "px " + y.toFixed(1) + "%")
    right.push(r.toFixed(1) + "px " + y.toFixed(1) + "%")
  }
  return "polygon(" + left.concat(right.reverse()).join(", ") + ")"
}

const tearBand = buildTearBand(30)

/** 未印刷的纸纤维比纸面更白：一条竖向条纹 + 中间实、两端虚的白色芯 */
const tearEdgeStyle = computed(() => ({
  width: TEAR_WIDTH + "px",
  clipPath: tearBand,
  "-webkit-clip-path": tearBand,
  background: [
    "repeating-linear-gradient(90deg, rgba(255,255,255,0.85) 0 1px, rgba(255,255,255,0) 1px 3px)",
    "linear-gradient(to right, rgba(255,255,255,0) 0%, rgba(255,255,255,0.97) 32%, rgba(255,255,255,0.97) 66%, rgba(255,255,255,0) 100%)"
  ].join(", ")
}))

function withAlpha(hex, alpha) {
  const clean = String(hex || "").replace("#", "")
  if (clean.length !== 6) return "rgba(90, 53, 32, " + alpha + ")"
  return "rgba(" + parseInt(clean.slice(0, 2), 16) + ", " + parseInt(clean.slice(2, 4), 16) + ", " + parseInt(clean.slice(4, 6), 16) + ", " + alpha + ")"
}

const pixelCells = computed(() => {
  const level = Math.max(0, Math.min(1, Number(props.progress) || 0))
  const filledRows = Math.round(level * PIXEL_ROWS)
  const on = withAlpha(props.palette.ink, 0.82)
  const off = "rgba(255, 255, 255, 0.55)"
  const cells = []
  for (let row = 0; row < PIXEL_ROWS; row += 1) {
    const fromBottom = PIXEL_ROWS - 1 - row
    const filled = fromBottom < filledRows
    for (let col = 0; col < PIXEL_COLS; col += 1) {
      cells.push({
        key: row + "-" + col,
        filled,
        // 自下而上：越靠底部的像素越先亮起
        delay: (fromBottom * 1.05 + col * 0.16).toFixed(2),
        background: filled ? on : off
      })
    }
  }
  return cells
})

const stampBodyStyle = computed(() => {
  const stubWidth = props.width - stubLeft.value
  // 长方形印章：宽度受存根区限制，高宽比约 2.6，竖向排版
  const boxWidth = Math.max(34, Math.min(stubWidth - 26, height.value * 0.2))
  const boxHeight = Math.min(height.value * 0.62, boxWidth * 2.6)
  return {
    width: boxWidth + "px",
    height: boxHeight + "px",
    fontSize: Math.max(11, boxWidth * 0.3) + "px",
    color: props.palette.ink,
    borderColor: props.palette.ink
  }
})

const fallStyle = computed(() => {
  const left = stubLeft.value
  const width = props.width - left
  const radius = (25 / 741) * props.width
  return {
    left: left + "px",
    width: width + "px",
    height: height.value + "px",
    borderTopRightRadius: radius + "px",
    borderBottomRightRadius: radius + "px",
    background: "linear-gradient(135deg, " + props.palette.front + " 0%, " + props.palette.front + " 55%, " + props.palette.back + " 100%)",
    color: props.palette.ink
  }
})

const fallTextStyle = computed(() => ({ fontSize: props.width * 0.056 + "px" }))

const fallMarkStyle = computed(() => ({ fontSize: props.width * 0.09 + "px", color: props.palette.front }))

const clockDialStyle = computed(() => {
  const stubWidth = props.width - stubLeft.value
  const size = Math.max(38, Math.min(stubWidth - 26, height.value * 0.3))
  return { width: size + "px", height: size + "px", color: props.palette.ink }
})

const playKey = ref(0)
let played = false
let revealTimer = 0
let tearTimer = 0
let fallTimer = 0
let observer = null

function play(delay) {
  if (!fxKind.value) {
    tornOff.value = false
    falling.value = false
    return
  }
  const wait = Number(delay) || 0
  if (revealTimer) {
    clearTimeout(revealTimer)
    revealTimer = 0
  }
  if (tearTimer) {
    clearTimeout(tearTimer)
    tearTimer = 0
  }
  // 重播时先把存根装回去，撕口动画走完再让它脱落
  tornOff.value = false
  falling.value = false
  if (fallTimer) {
    clearTimeout(fallTimer)
    fallTimer = 0
  }
  const start = () => {
    playKey.value += 1
  }
  if (wait) revealTimer = setTimeout(start, wait)
  else start()
  if (fxKind.value === "tear") {
    tearTimer = setTimeout(() => {
      // 撕口走完：卡片轮廓收窄到锯齿边，被撕下的存根作为独立纸片下坠消失
      tornOff.value = true
      falling.value = true
      fallTimer = setTimeout(() => {
        falling.value = false
      }, 1060)
    }, wait + TORN_DELAY)
  }
}

watch(
  () => props.status,
  (value, old) => {
    if (value !== old) play(0)
  }
)

onMounted(() => {
  if (!fxKind.value) return
  if (typeof IntersectionObserver === "undefined") {
    play(80)
    return
  }
  observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting || played) return
        played = true
        play(80 + Math.round(Math.random() * 160))
      })
    },
    { threshold: 0.35 }
  )
  if (frameRef.value) observer.observe(frameRef.value)
})

onBeforeUnmount(() => {
  if (observer) {
    observer.disconnect()
    observer = null
  }
  if (revealTimer) {
    clearTimeout(revealTimer)
    revealTimer = 0
  }
  if (tearTimer) {
    clearTimeout(tearTimer)
    tearTimer = 0
  }
  if (fallTimer) {
    clearTimeout(fallTimer)
    fallTimer = 0
  }
})
</script>

<style scoped>
.ticket {
  position: relative;
  user-select: none;
  cursor: pointer;
  filter: drop-shadow(0 10px 22px rgba(40, 26, 12, 0.18));
  transition: filter 0.32s ease;
  outline: none;
}

.ticket:hover {
  filter: drop-shadow(0 18px 32px rgba(40, 26, 12, 0.26));
}

.ticket:focus-visible .ticket__shape {
  box-shadow: 0 0 0 3px rgba(0, 190, 180, 0.55);
}

.ticket__shape {
  position: absolute;
  inset: 0;
  overflow: hidden;
  transform: perspective(1200px) rotateX(0deg) rotateY(0deg) scale(1);
  transition: transform 0.42s cubic-bezier(0.22, 1, 0.36, 1);
  will-change: transform;
  transform-style: preserve-3d;
}

.ticket.is-tilting .ticket__shape {
  transition: none;
}

.ticket__fill {
  position: absolute;
  inset: 0;
}

.ticket__texture {
  position: absolute;
  inset: 0;
}

.ticket__perforation {
  position: absolute;
  top: 0;
  bottom: 0;
  pointer-events: none;
}

.ticket__watermark {
  position: absolute;
  top: 0;
  bottom: 0;
  display: grid;
  place-items: center;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  pointer-events: none;
  letter-spacing: -0.04em;
}

.ticket__watermark span {
  writing-mode: vertical-rl;
  line-height: 1;
}

.ticket__content {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.ticket__label {
  position: absolute;
  display: flex;
  flex-direction: column;
  font-weight: 500;
  text-transform: uppercase;
  max-width: 58%;
}

.ticket__sublabel {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 600;
}

.ticket__name {
  position: absolute;
  font-weight: 700;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.ticket__footer {
  position: absolute;
  font-weight: 500;
  white-space: nowrap;
  text-transform: uppercase;
  opacity: 0.86;
}

.ticket__stub {
  position: absolute;
  top: 0;
  bottom: 0;
  z-index: 2;
  display: grid;
  place-items: center;
}

.ticket__stub span {
  writing-mode: vertical-rl;
  font-weight: 600;
  letter-spacing: 0.06em;
  opacity: 0.92;
}

/* -------- 状态动效（仅虚线右侧的存根区） -------- */
.ticket__fx {
  position: absolute;
  top: 0;
  bottom: 0;
  overflow: hidden;
  pointer-events: none;
}

.fx-tear {
  position: absolute;
  inset: 0;
  /* 撕纸不是匀速：裂纹在纤维间跳跃，一段快、一段几乎停住（粘滑 stick-slip） */
  animation: fx-tear-reveal 0.82s linear both;
}

@keyframes fx-tear-reveal {
  0% {
    clip-path: inset(0 0 100% 0);
  }
  12% {
    clip-path: inset(0 0 82% 0);
  }
  22% {
    clip-path: inset(0 0 79% 0);
  }
  40% {
    clip-path: inset(0 0 54% 0);
  }
  50% {
    clip-path: inset(0 0 51% 0);
  }
  66% {
    clip-path: inset(0 0 22% 0);
  }
  76% {
    clip-path: inset(0 0 19% 0);
  }
  90% {
    clip-path: inset(0 0 4% 0);
  }
  100% {
    clip-path: inset(0 0 0 0);
  }
}

/* 断口处的凹陷阴影：让裂开的纸有厚度，而不是一张平面色带 */
.fx-tear__lift {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 22px;
  background: linear-gradient(
    to right,
    rgba(52, 28, 10, 0) 0%,
    rgba(52, 28, 10, 0.26) 24%,
    rgba(52, 28, 10, 0.04) 50%,
    rgba(52, 28, 10, 0.26) 76%,
    rgba(52, 28, 10, 0) 100%
  );
}

.fx-tear__edge {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
}

.fx-tear__shadow {
  position: absolute;
  left: 18px;
  top: 0;
  bottom: 0;
  width: 14px;
  background: linear-gradient(to right, rgba(64, 34, 12, 0.26), rgba(64, 34, 12, 0));
}

/* 存根被拉着离开纸面时，迎着光的一侧会亮起来 */
.fx-tear__peel {
  position: absolute;
  left: 8px;
  right: 0;
  top: 0;
  bottom: 0;
  background: linear-gradient(to right, rgba(255, 255, 255, 0.3) 0%, rgba(255, 255, 255, 0.08) 26%, rgba(255, 255, 255, 0) 48%);
  animation: fx-peel-glow 0.82s ease-in both;
}

@keyframes fx-peel-glow {
  0% {
    opacity: 0;
  }
  45% {
    opacity: 0.35;
  }
  100% {
    opacity: 0.8;
  }
}

/* 沿虚线向下移动的撕口尖端 */
.fx-tear__tip {
  position: absolute;
  left: -2px;
  top: 0;
  width: 14px;
  height: 14px;
  margin-top: -7px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.95) 0%, rgba(255, 255, 255, 0.35) 45%, rgba(255, 255, 255, 0) 72%);
  /* 与裂纹同一时间轴、同一批停顿点，尖端才能“跟住”裂口 */
  animation: fx-tear-tip 0.82s linear both;
}

@keyframes fx-tear-tip {
  0% {
    top: 0;
    margin-left: 0;
    opacity: 0.9;
  }
  12% {
    top: 18%;
    margin-left: 1px;
    opacity: 0.95;
  }
  22% {
    top: 21%;
    margin-left: -1.5px;
    opacity: 0.85;
  }
  40% {
    top: 46%;
    margin-left: 1px;
    opacity: 0.95;
  }
  50% {
    top: 49%;
    margin-left: 0;
    opacity: 0.8;
  }
  66% {
    top: 78%;
    margin-left: -1px;
    opacity: 0.9;
  }
  76% {
    top: 81%;
    margin-left: 1.5px;
    opacity: 0.8;
  }
  90% {
    top: 96%;
    opacity: 0.6;
  }
  100% {
    top: 100%;
    opacity: 0.1;
  }
}

/* 被撕下的那半张票：脱离卡片轮廓后整片下坠 */
.ticket__fall {
  position: absolute;
  top: 0;
  z-index: 4;
  overflow: hidden;
  display: grid;
  place-items: center;
  pointer-events: none;
  /* 铰在撕口上：纸片先绕着裂口翻，再整体离开 */
  transform-origin: 0% 50%;
  backface-visibility: hidden;
  will-change: transform, opacity;
  box-shadow: -6px 12px 26px rgba(40, 26, 12, 0.3);
  animation: fx-fall-away 0.98s linear both;
}

.ticket__fall::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 12px;
  background-image: repeating-linear-gradient(
      90deg,
      rgba(255, 255, 255, 0.95) 0 1px,
      rgba(255, 255, 255, 0) 1px 3px
    ),
    linear-gradient(to right, rgba(255, 255, 255, 0.95), rgba(255, 255, 255, 0));
}

.ticket__fall-text {
  writing-mode: vertical-rl;
  font-weight: 600;
  letter-spacing: 0.06em;
  opacity: 0.92;
}

.ticket__fall-mark {
  position: absolute;
  opacity: 0.3;
  font-weight: 700;
  writing-mode: vertical-rl;
  letter-spacing: -0.04em;
}

@keyframes fx-fall-away {
  0% {
    opacity: 1;
    transform: perspective(1000px) translate3d(0, 0, 0) rotateZ(0deg) rotateY(0deg);
    box-shadow: -6px 12px 26px rgba(40, 26, 12, 0.3);
  }
  10% {
    opacity: 1;
    transform: perspective(1000px) translate3d(4px, 5px, 0) rotateZ(2.6deg) rotateY(-6deg);
    box-shadow: -8px 18px 30px rgba(40, 26, 12, 0.28);
  }
  30% {
    opacity: 1;
    transform: perspective(1000px) translate3d(-7px, 26%, 0) rotateZ(-4.5deg) rotateY(-14deg);
    box-shadow: -10px 28px 34px rgba(40, 26, 12, 0.22);
  }
  52% {
    opacity: 0.98;
    transform: perspective(1000px) translate3d(11px, 62%, 0) rotateZ(6deg) rotateY(-3deg);
    box-shadow: -6px 34px 30px rgba(40, 26, 12, 0.16);
  }
  74% {
    opacity: 0.78;
    transform: perspective(1000px) translate3d(-3px, 104%, 0) rotateZ(-7deg) rotateY(-16deg);
    box-shadow: -2px 30px 22px rgba(40, 26, 12, 0.1);
  }
  100% {
    opacity: 0;
    transform: perspective(1000px) translate3d(24px, 158%, 0) rotateZ(13deg) rotateY(-7deg);
    box-shadow: 0 20px 14px rgba(40, 26, 12, 0);
  }
}

/* 撕掉后贴在锯齿断口内侧的纸纤维 */
.fx-fringe {
  position: absolute;
  inset: 0;
  background-image: repeating-linear-gradient(
      90deg,
      rgba(255, 255, 255, 0.9) 0 1px,
      rgba(255, 255, 255, 0) 1px 4px
    ),
    linear-gradient(to left, rgba(255, 255, 255, 0.98) 0%, rgba(255, 255, 255, 0.62) 42%, rgba(255, 255, 255, 0) 100%);
  animation: fx-fringe-in 0.42s ease-out both;
}

@keyframes fx-fringe-in {
  0% {
    opacity: 0;
    filter: brightness(1.4) blur(0.2px);
  }
  32% {
    opacity: 1;
    filter: brightness(1.12) blur(0.2px);
  }
  100% {
    opacity: 1;
    filter: brightness(1) blur(0.2px);
  }
}

/* 撕的过程中整张纸被拉紧：微量位移 + 回弹，断口才有受力感 */
.ticket:has(.fx-tear) .ticket__shape {
  animation: fx-paper-tension 0.82s ease-in-out both;
}

@keyframes fx-paper-tension {
  0% {
    transform: translate3d(0, 0, 0);
  }
  40% {
    transform: translate3d(-1.2px, 0, 0);
  }
  62% {
    transform: translate3d(0.6px, 0, 0);
  }
  100% {
    transform: translate3d(0, 0, 0);
  }
}

/* 撕开瞬间的回弹，强调右侧被扯走 */
.ticket.is-torn {
  animation: fx-ticket-snap 0.34s cubic-bezier(0.3, 0.9, 0.4, 1);
}

@keyframes fx-ticket-snap {
  0% {
    transform: translateX(0);
  }
  35% {
    transform: translateX(-5px) rotate(-0.4deg);
  }
  100% {
    transform: translateX(0);
  }
}

/* 待审批：时钟 */
.fx-clock {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.fx-clock__dial {
  position: relative;
}

.fx-clock__face {
  position: absolute;
  inset: 0;
  border: 2px solid currentColor;
  border-radius: 50%;
  background: radial-gradient(circle at 50% 45%, rgba(255, 255, 255, 0.45), rgba(255, 255, 255, 0.06) 72%);
}

.fx-clock__face::before {
  content: "";
  position: absolute;
  inset: 4px;
  border-radius: 50%;
  background: repeating-conic-gradient(from -1deg, currentColor 0deg 1.4deg, transparent 1.4deg 30deg);
  -webkit-mask: radial-gradient(circle, transparent 0 78%, #000 79% 100%);
  mask: radial-gradient(circle, transparent 0 78%, #000 79% 100%);
  opacity: 0.72;
}

.fx-clock__hand {
  position: absolute;
  left: 50%;
  bottom: 50%;
  border-radius: 2px;
  background: currentColor;
  transform-origin: 50% 100%;
}

.fx-clock__hand--hour {
  width: 2px;
  height: 26%;
  margin-left: -1px;
  animation: fx-clock-spin 14s linear infinite;
}

.fx-clock__hand--minute {
  width: 2px;
  height: 36%;
  margin-left: -1px;
  animation: fx-clock-spin 5s linear infinite;
}

.fx-clock__hand--second {
  width: 1px;
  height: 44%;
  margin-left: -0.5px;
  opacity: 0.62;
  animation: fx-clock-spin 1.8s linear infinite;
}

.fx-clock__pivot {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 5px;
  height: 5px;
  margin: -2.5px 0 0 -2.5px;
  border-radius: 50%;
  background: currentColor;
}

.fx-clock__caption {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.08em;
  white-space: nowrap;
  opacity: 0.82;
}

@keyframes fx-clock-spin {
  to {
    transform: rotate(360deg);
  }
}

.fx-stamp {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
}

.fx-stamp__body {
  position: relative;
  display: grid;
  place-items: center;
  border: 2px solid currentColor;
  border-radius: 3px;
  font-weight: 800;
  letter-spacing: 0.14em;
  opacity: 0.9;
  transform: rotate(-8deg);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.16);
  animation: fx-stamp-slam 0.62s cubic-bezier(0.2, 1.5, 0.35, 1) both;
}

.fx-stamp__body::before {
  content: "";
  position: absolute;
  inset: 4px;
  border: 1px dashed currentColor;
  border-radius: 2px;
  opacity: 0.5;
}

.fx-stamp__body::after {
  content: "";
  position: absolute;
  inset: -4px;
  border: 2px solid currentColor;
  border-radius: 3px;
  opacity: 0;
  animation: fx-stamp-ring 0.8s ease-out 0.16s both;
}

.fx-stamp__text {
  writing-mode: vertical-rl;
  line-height: 1.05;
}

@keyframes fx-stamp-slam {
  0% {
    opacity: 0;
    transform: scale(1.9) rotate(-18deg);
  }
  45% {
    opacity: 0.95;
    transform: scale(0.94) rotate(-6deg);
  }
  65% {
    transform: scale(1.03) rotate(-9deg);
  }
  100% {
    opacity: 0.9;
    transform: scale(1) rotate(-8deg);
  }
}

@keyframes fx-stamp-ring {
  0% {
    opacity: 0.5;
    transform: scale(0.94);
  }
  100% {
    opacity: 0;
    transform: scale(1.22);
  }
}

.fx-pixels {
  position: absolute;
  inset: 8px 7px;
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  grid-template-rows: repeat(16, 1fr);
  gap: 2px;
  /* 中间留出竖排文字的通道，像素不压在字上 */
  -webkit-mask: linear-gradient(to right, #000 0 calc(50% - 13px), transparent calc(50% - 13px) calc(50% + 13px), #000 calc(50% + 13px) 100%);
  mask: linear-gradient(to right, #000 0 calc(50% - 13px), transparent calc(50% - 13px) calc(50% + 13px), #000 calc(50% + 13px) 100%);
}

.fx-pixels__cell {
  border-radius: 1px;
}

.fx-pixels__cell.is-filled {
  animation: fx-pixel-in 0.3s cubic-bezier(0.3, 1.4, 0.5, 1) both;
  animation-delay: calc(var(--d) * 42ms);
}

@keyframes fx-pixel-in {
  from {
    opacity: 0;
    transform: scale(0.3);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.ticket__glare {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: transparent;
  transition: background 0.42s ease-out;
}

.ticket.is-tilting .ticket__glare {
  transition: none;
}

@media (prefers-reduced-motion: reduce) {
  .ticket__shape {
    transition: none;
  }

  .fx-tear {
    animation: none;
    clip-path: none;
  }

  .fx-stamp__body,
  .fx-stamp__body::after {
    animation: none;
  }

  .fx-pixels__cell.is-filled {
    animation: none;
  }

  .fx-clock__hand {
    animation: none;
  }

  .ticket.is-torn {
    animation: none;
  }

  .fx-tear__tip,
  .ticket__fall {
    display: none;
  }

  .ticket:has(.fx-tear) .ticket__shape {
    animation: none;
  }
}
</style>
