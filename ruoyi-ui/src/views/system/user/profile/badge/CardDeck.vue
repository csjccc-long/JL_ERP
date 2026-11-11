<template>
  <div
    ref="root"
    class="card-deck"
    :class="{ 'is-focused': !!active, 'is-hovered': hovered }"
    :style="{ height: frameHeight + 'px' }"
    @pointerenter="onPointerEnter"
    @pointermove="onPointerMove"
    @pointerleave="onPointerLeave"
    @click="onDeckClick"
  >
    <div class="card-deck__stage" :style="stageStyle">
      <div
        v-for="(key, position) in order"
        :key="key"
        :ref="el => setSlotRef(key, el)"
        class="card-deck__slot"
        :class="[`is-${key}`, { 'is-active': active === key, 'is-front': !active && position === 0 }]"
        :style="slotStyle(key, position)"
        :data-deck-key="key"
      >
        <slot :name="key" />
        <span
          v-if="active !== key"
          class="card-deck__shade"
          :style="{ opacity: shadeFor(position) }"
          aria-hidden="true"
        />
      </div>
    </div>
  </div>
</template>

<script setup name="CardDeck">
import { computed, onBeforeUnmount, onMounted, ref } from "vue"

const props = defineProps({
  /** 当前展开查看的卡片 key；为空时显示堆叠 */
  active: {
    type: String,
    default: ""
  },
  /** 卡片 key 顺序；第一张是堆叠的最前面一张 */
  order: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(["select", "promote"])

const root = ref(null)
const rootWidth = ref(560)
const hovered = ref(false)
const slotRefs = new Map()
let raf = 0
let resizeObserver = null

/** CardSwap 参考里的默认节奏：900px 透视、6° 斜切、后方卡片向上/向右错开 */
const PERSPECTIVE = 900
const CARD_DISTANCE = 30
const VERTICAL_DISTANCE = 36
const DEPTH = 64
const SKEW = 6
const PAD = 8
const DIM = 0.45
const HOVER_SPREAD = 0.12

const setSlotRef = (key, element) => {
  if (element) slotRefs.set(key, element)
  else slotRefs.delete(key)
}

const cardWidth = computed(() => Math.min(560, rootWidth.value || 560))
const cardHeight = computed(() => (cardWidth.value * 540) / 856)
const spread = computed(() => (hovered.value ? 1 + HOVER_SPREAD : 1))

/** 与参考实现一致的包围盒：把斜切和透视缩放都算进去 */
const bounds = computed(() => {
  const width = cardWidth.value
  const height = cardHeight.value
  const visible = Math.max(1, props.order.length)
  const boundsSpread = 1 + HOVER_SPREAD
  const slope = Math.tan((SKEW * Math.PI) / 180)
  let minX = Infinity
  let maxX = -Infinity
  let minY = Infinity
  let maxY = -Infinity

  for (let index = 0; index < visible; index += 1) {
    const depthScale = PERSPECTIVE / (PERSPECTIVE + index * DEPTH)
    for (const u of [-width / 2, width / 2]) {
      for (const v of [-height / 2, height / 2]) {
        const x = (index * CARD_DISTANCE * boundsSpread + u) * depthScale
        const y = (-index * VERTICAL_DISTANCE * boundsSpread + v + u * slope) * depthScale
        minX = Math.min(minX, x)
        maxX = Math.max(maxX, x)
        minY = Math.min(minY, y)
        maxY = Math.max(maxY, y)
      }
    }
  }

  return {
    width: Math.ceil(maxX - minX + PAD * 2),
    height: Math.ceil(maxY - minY + PAD * 2),
    originX: PAD - minX,
    originY: PAD - minY
  }
})

/** 桌面不缩小前卡；移动端才把整组缩进容器 */
const scale = computed(() => {
  const available = rootWidth.value || 560
  return available < 500 ? Math.min(1, available / bounds.value.width) : 1
})

const frameHeight = computed(() =>
  props.active
    ? cardHeight.value + PAD * 2
    : bounds.value.height * scale.value
)

const stageStyle = computed(() => ({
  width: `${bounds.value.width}px`,
  height: `${bounds.value.height}px`,
  transform: `translate(-50%, -50%) scale(${props.active ? 1 : scale.value})`
}))

const baseStyle = computed(() => ({
  left: `${bounds.value.originX}px`,
  top: `${bounds.value.originY}px`,
  width: `${cardWidth.value}px`,
  height: `${cardHeight.value}px`,
  marginLeft: `${-cardWidth.value / 2}px`,
  marginTop: `${-cardHeight.value / 2}px`
}))

const stackTransform = position => {
  const q = Math.max(0, Math.min(position, props.order.length - 1))
  const x = q * CARD_DISTANCE * spread.value
  const y = -q * VERTICAL_DISTANCE * spread.value
  const z = -q * DEPTH
  return `translate3d(${x.toFixed(2)}px, ${y.toFixed(2)}px, ${z}px) skewY(${SKEW}deg)`
}

const slotStyle = (key, position) => {
  if (props.active) {
    if (key === props.active) {
      const x = bounds.value.width / 2 - bounds.value.originX
      const y = bounds.value.height / 2 - bounds.value.originY
      return {
        ...baseStyle.value,
        zIndex: 20,
        transform: `translate3d(${x.toFixed(2)}px, ${y.toFixed(2)}px, 0)`,
        opacity: 1,
        pointerEvents: "auto"
      }
    }
    return {
      ...baseStyle.value,
      zIndex: 1,
      transform: stackTransform(position),
      opacity: 0,
      pointerEvents: "none"
    }
  }

  return {
    ...baseStyle.value,
    zIndex: 10 + (props.order.length - position) * 2,
    transform: stackTransform(position),
    opacity: 1,
    pointerEvents: "auto"
  }
}

const shadeFor = position => {
  if (position <= 0) return 0
  const ratio = position / Math.max(1, props.order.length - 1)
  return Math.min(DIM, DIM * 0.62 * ratio)
}

function onDeckClick(event) {
  if (props.active) return
  let key = event.target.closest?.("[data-deck-key]")?.dataset.deckKey

  // 3D 舞台平面会挡住后方卡片的原生命中，这里按槽位矩形做一次兜底
  if (!key) {
    const x = event.clientX
    const y = event.clientY
    for (const item of props.order) {
      const element = slotRefs.get(item)
      if (!element) continue
      const rect = element.getBoundingClientRect()
      if (x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom) {
        key = item
        break
      }
    }
  }

  if (!key) return
  const position = props.order.indexOf(key)
  if (position < 0) return
  if (position === 0) emit("select", key)
  else emit("promote", key)
}

function onPointerEnter(event) {
  if (props.active || event.pointerType === "touch") return
  hovered.value = true
}

function onPointerMove(event) {
  if (props.active || !root.value || event.pointerType === "touch") return
  hovered.value = true
  const box = root.value.getBoundingClientRect()
  if (!box.width || !box.height) return
  const nx = ((event.clientX - box.left) / box.width - 0.5) * 2
  const ny = ((event.clientY - box.top) / box.height - 0.5) * 2
  const px = nx * 0.5 * 0.35 * bounds.value.width
  const py = ny * 0.5 * 0.35 * bounds.value.height

  if (raf) cancelAnimationFrame(raf)
  raf = requestAnimationFrame(() => {
    raf = 0
    if (!root.value) return
    root.value.style.setProperty("--px", `${px.toFixed(1)}px`)
    root.value.style.setProperty("--py", `${py.toFixed(1)}px`)
  })
}

function resetPointer() {
  hovered.value = false
  if (raf) {
    cancelAnimationFrame(raf)
    raf = 0
  }
  if (root.value) {
    root.value.style.setProperty("--px", "0px")
    root.value.style.setProperty("--py", "0px")
  }
}

function onPointerLeave() {
  if (props.active) return
  resetPointer()
}

function updateWidth() {
  if (root.value) rootWidth.value = root.value.clientWidth || 560
}

onMounted(() => {
  updateWidth()
  if (typeof ResizeObserver !== "undefined") {
    resizeObserver = new ResizeObserver(updateWidth)
    resizeObserver.observe(root.value)
  }
})

onBeforeUnmount(() => {
  if (resizeObserver) resizeObserver.disconnect()
  if (raf) cancelAnimationFrame(raf)
  slotRefs.clear()
})
</script>

<style scoped>
.card-deck {
  position: relative;
  width: 100%;
  perspective: 900px;
  perspective-origin: calc(50% + var(--px, 0px)) calc(50% + var(--py, 0px));
  transition: height 0.55s cubic-bezier(0.2, 0.78, 0.2, 1);
  touch-action: pan-y;
}

.card-deck:not(.is-focused) {
  cursor: pointer;
}

.card-deck__stage {
  position: absolute;
  top: 50%;
  left: 50%;
  transform-style: preserve-3d;
  pointer-events: none;
  transition:
    transform 0.55s cubic-bezier(0.2, 0.78, 0.2, 1),
    width 0.4s ease,
    height 0.4s ease;
}

.card-deck__slot {
  position: absolute;
  transform-origin: 50% 50%;
  transform-style: preserve-3d;
  transition:
    transform 0.62s cubic-bezier(0.22, 1.16, 0.36, 1),
    opacity 0.38s ease;
  will-change: transform, opacity;
  pointer-events: auto;
}

.card-deck.is-focused .card-deck__slot {
  cursor: default;
}

/* 堆叠状态只让槽位接收点击，内部卡片不抢指针 */
.card-deck:not(.is-focused) .card-deck__slot :deep(.badge-block),
.card-deck:not(.is-focused) .card-deck__slot :deep(.pass-wrap),
.card-deck:not(.is-focused) .card-deck__slot :deep(.rolepass-wrap) {
  pointer-events: none;
}

.card-deck__shade {
  position: absolute;
  inset: 0;
  z-index: 6;
  border-radius: 21px;
  background: #05070b;
  pointer-events: none;
  transition: opacity 0.35s ease;
}

@media (max-width: 560px) {
  .card-deck__shade {
    border-radius: 16px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .card-deck,
  .card-deck__stage,
  .card-deck__slot {
    transition-duration: 0.01ms;
  }
}
</style>
