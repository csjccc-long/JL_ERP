<template>
  <div
    ref="rootRef"
    class="donut-chart"
    :style="{ width: size + 'px', height: size + 'px' }"
    role="img"
    :aria-label="ariaLabel"
    @mouseleave="handleMouseLeave"
  >
    <svg class="donut-chart__svg" :width="size" :height="size" :viewBox="`0 0 ${size} ${size}`">
      <!-- 底环 -->
      <circle
        class="donut-chart__track"
        :cx="center"
        :cy="center"
        :r="radius"
        fill="none"
        stroke="var(--app-border, rgba(22, 22, 24, 0.08))"
        :stroke-width="strokeWidth"
      />

      <!-- 数据扇区 -->
      <circle
        v-for="(segment, index) in segments"
        :key="segment.label"
        class="donut-chart__segment"
        :class="{
          'is-active': isActive(segment),
          'is-resetting': !animated,
          'is-clickable': highlightOnHover
        }"
        :cx="center"
        :cy="center"
        :r="radius"
        fill="none"
        :stroke="segment.color"
        :stroke-width="strokeWidth"
        stroke-linecap="round"
        :style="segmentStyle(segment, index)"
        @mouseenter="handleSegmentEnter(segment)"
        @click="handleSegmentClick(segment)"
      />
    </svg>

    <!-- 圆心内容 -->
    <div class="donut-chart__center" :style="centerStyle">
      <slot name="center" :total="total" :hovered="hovered" :percent="hoveredPercent">
        <div class="donut-chart__center-default">
          <p class="donut-chart__center-label">{{ hovered ? hovered.label : "总计" }}</p>
          <p class="donut-chart__center-value">{{ hovered ? hovered.value : total }}</p>
          <p v-if="hovered" class="donut-chart__center-percent">{{ hoveredPercent.toFixed(0) }}%</p>
        </div>
      </slot>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from "vue"

/**
 * 环形图（环形占比图）
 * 对应 React + framer-motion 版本的动画与交互：
 * 1. 每段按顺序渐次描边生长（strokeDashoffset 过渡 + 逐段延迟）
 * 2. hover 段高亮：发光 + 轻微放大，同时抛出 hover 事件供外部联动
 * 3. 圆心内容通过 #center 插槽自定义（可拿到 total / hovered / percent）
 */
const props = defineProps({
  /** 扇区数据：[{ value, color, label }] */
  data: {
    type: Array,
    default: () => []
  },
  /** 自定义总值，缺省时按 data 求和 */
  totalValue: {
    type: Number,
    default: null
  },
  /** 直径（px） */
  size: {
    type: Number,
    default: 200
  },
  /** 环宽（px） */
  strokeWidth: {
    type: Number,
    default: 20
  },
  /** 单段生长动画时长（秒） */
  animationDuration: {
    type: Number,
    default: 1
  },
  /** 相邻扇区之间的动画延迟（秒） */
  animationDelayPerSegment: {
    type: Number,
    default: 0.05
  },
  /** 是否开启 hover 高亮 */
  highlightOnHover: {
    type: Boolean,
    default: true
  },
  /** 外部（如图例）指定的高亮扇区 label */
  activeLabel: {
    type: String,
    default: null
  },
  ariaLabel: {
    type: String,
    default: "环形占比图"
  }
})

const emit = defineEmits(["segment-hover", "segment-click"])

const rootRef = ref(null)
const animated = ref(false)
const internalHovered = ref(null)

const center = computed(() => props.size / 2)
const radius = computed(() => Math.max(props.size / 2 - props.strokeWidth / 2, 0))
const circumference = computed(() => 2 * Math.PI * radius.value)

const total = computed(() => {
  if (props.totalValue != null) {
    return props.totalValue
  }
  return props.data.reduce((sum, item) => sum + Number(item.value || 0), 0)
})

const segments = computed(() => {
  let cumulative = 0
  const result = []
  props.data.forEach(item => {
    const value = Number(item.value || 0)
    if (value <= 0) {
      return
    }
    const percent = total.value === 0 ? 0 : (value / total.value) * 100
    const length = (percent / 100) * circumference.value
    const offset = (cumulative / 100) * circumference.value
    cumulative += percent
    result.push({
      ...item,
      value,
      percent,
      length,
      offset,
      dasharray: `${length} ${circumference.value}`
    })
  })
  return result
})

const hovered = computed(() => {
  if (props.activeLabel) {
    return segments.value.find(item => item.label === props.activeLabel) || null
  }
  return internalHovered.value
})

const hoveredPercent = computed(() => (hovered.value ? hovered.value.percent : 100))

const centerStyle = computed(() => {
  const diameter = Math.max(props.size - props.strokeWidth * 2.5, 0)
  return {
    width: diameter + "px",
    height: diameter + "px"
  }
})

function isActive(segment) {
  return !!hovered.value && hovered.value.label === segment.label
}

function segmentStyle(segment, index) {
  const active = isActive(segment)
  return {
    strokeDasharray: segment.dasharray,
    // 未开始动画时，整段圆弧退到可视区外（不可见）
    strokeDashoffset: animated.value ? `${-segment.offset}` : `${circumference.value}`,
    "--donut-duration": props.animationDuration + "s",
    "--donut-delay": (index * props.animationDelayPerSegment).toFixed(3) + "s",
    filter: active ? `drop-shadow(0 0 6px ${segment.color}) brightness(1.08)` : "none",
    transform: active ? "scale(1.03)" : "scale(1)"
  }
}

async function replay() {
  animated.value = false
  await nextTick()
  // 等一帧，保证"复位"状态先被浏览器绘制，再开启过渡
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      animated.value = true
    })
  })
}

function handleSegmentEnter(segment) {
  if (!props.highlightOnHover) {
    return
  }
  internalHovered.value = segment
  emit("segment-hover", segment)
}

function handleMouseLeave() {
  internalHovered.value = null
  emit("segment-hover", null)
}

function handleSegmentClick(segment) {
  emit("segment-click", segment)
}

watch(() => props.data, replay, { deep: true })

onMounted(() => {
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      animated.value = true
    })
  })
})

defineExpose({ replay })
</script>

<style scoped>
.donut-chart {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.donut-chart__svg {
  display: block;
  overflow: visible;
  transform: rotate(-90deg);
}

.donut-chart__track {
  pointer-events: none;
}

.donut-chart__segment {
  /* 只有描边可交互，避免透明的圆形填充抢走 hover */
  pointer-events: stroke;
  /* 以整个 SVG 视图中心（环心）为缩放原点，hover 时扇区沿半径均匀"鼓出" */
  transform-box: view-box;
  transform-origin: center;
  transition:
    stroke-dashoffset var(--donut-duration, 1s) cubic-bezier(0.22, 0.61, 0.36, 1) var(--donut-delay, 0s),
    opacity 0.3s ease var(--donut-delay, 0s),
    transform 0.25s ease-out,
    filter 0.25s ease-out;
}

.donut-chart__segment.is-clickable {
  cursor: pointer;
}

.donut-chart__segment.is-resetting {
  transition: none;
}

.donut-chart__center {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  pointer-events: none;
  text-align: center;
}

.donut-chart__center-label {
  margin: 0;
  font-size: 12px;
  line-height: 1.4;
  color: var(--el-text-color-secondary, #75787a);
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.donut-chart__center-value {
  margin: 2px 0 0;
  font-size: 28px;
  font-weight: 600;
  line-height: 1.15;
  color: var(--el-text-color-primary, #161618);
  font-variant-numeric: tabular-nums;
}

.donut-chart__center-percent {
  margin: 2px 0 0;
  font-size: 13px;
  color: var(--el-text-color-secondary, #75787a);
}
</style>
