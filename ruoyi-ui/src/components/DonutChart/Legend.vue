<template>
  <ul class="donut-legend" role="list">
    <li
      v-for="(item, index) in data"
      :key="item.label"
      class="donut-legend__item"
      :class="{ 'is-active': item.label === activeLabel }"
      :style="{ animationDelay: (0.45 + index * 0.06).toFixed(2) + 's' }"
      @mouseenter="handleEnter(item)"
      @mouseleave="handleLeave"
    >
      <span class="donut-legend__dot" :style="{ backgroundColor: item.color }" />
      <span class="donut-legend__label" :title="item.label">{{ item.label }}</span>
      <span class="donut-legend__value">{{ formatValue(item.value) }}</span>
      <span class="donut-legend__percent">{{ percentOf(item).toFixed(0) }}%</span>
    </li>
  </ul>
</template>

<script setup>
import { computed } from "vue"

/**
 * 环形图图例
 * 与 DonutChart 配合使用：hover 图例项时抛出 hover 事件，
 * 由父级把 label 回传给 DonutChart 的 active-label，实现双向联动。
 */
const props = defineProps({
  /** [{ value, color, label }] */
  data: {
    type: Array,
    default: () => []
  },
  /** 当前高亮的 label */
  activeLabel: {
    type: String,
    default: null
  },
  /** 总值，缺省时按 data 求和 */
  total: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(["hover"])

const internalTotal = computed(() => {
  if (props.total != null) {
    return props.total
  }
  return props.data.reduce((sum, item) => sum + Number(item.value || 0), 0)
})

function percentOf(item) {
  if (!internalTotal.value) {
    return 0
  }
  return (Number(item.value || 0) / internalTotal.value) * 100
}

function formatValue(value) {
  const num = Number(value || 0)
  return num.toLocaleString("zh-CN")
}

function handleEnter(item) {
  emit("hover", item)
}

function handleLeave() {
  emit("hover", null)
}
</script>

<style scoped>
.donut-legend {
  list-style: none;
  margin: 0;
  padding: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.donut-legend__item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s ease, transform 0.2s ease;
  animation: donut-legend-in 0.42s cubic-bezier(0.22, 0.61, 0.36, 1) both;
}

.donut-legend__item.is-active {
  background-color: var(--el-fill-color-light, #f4f5f6);
}

.donut-legend__dot {
  flex: 0 0 auto;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.donut-legend__item.is-active .donut-legend__dot {
  transform: scale(1.25);
  box-shadow: 0 0 0 3px var(--el-fill-color, #f4f5f6);
}

.donut-legend__label {
  flex: 1 1 auto;
  min-width: 0;
  font-size: 13px;
  color: var(--el-text-color-regular, #3b3d40);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.donut-legend__item.is-active .donut-legend__label {
  color: var(--el-text-color-primary, #161618);
  font-weight: 600;
}

.donut-legend__value {
  flex: 0 0 auto;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary, #161618);
  font-variant-numeric: tabular-nums;
}

.donut-legend__percent {
  flex: 0 0 auto;
  width: 40px;
  text-align: right;
  font-size: 12px;
  color: var(--el-text-color-secondary, #75787a);
  font-variant-numeric: tabular-nums;
}

@keyframes donut-legend-in {
  from {
    opacity: 0;
    transform: translateX(-12px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}
</style>
