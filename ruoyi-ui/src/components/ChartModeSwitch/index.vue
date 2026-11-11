<template>
  <div class="chart-mode" role="group" aria-label="图表表现形式">
    <span class="chart-mode__thumb" :class="{ 'is-3d': is3d }" aria-hidden="true" />
    <button
      type="button"
      class="chart-mode__btn"
      :class="{ 'is-on': !is3d }"
      title="平面折线"
      :aria-pressed="!is3d"
      @click="pick('2d')"
    >
      2D
    </button>
    <button
      type="button"
      class="chart-mode__btn"
      :class="{ 'is-on': is3d }"
      title="三维山形"
      :aria-pressed="is3d"
      @click="pick('3d')"
    >
      3D
    </button>
  </div>
</template>

<script setup name="ChartModeSwitch">
import { computed } from "vue"

const props = defineProps({
  modelValue: { type: String, default: "2d" }
})

const emit = defineEmits(["update:modelValue", "change"])

const is3d = computed(() => props.modelValue === "3d")

function pick(mode) {
  if (props.modelValue === mode) return
  emit("update:modelValue", mode)
  emit("change", mode)
}
</script>

<style scoped>
.chart-mode {
  position: relative;
  display: inline-flex;
  align-items: center;
  padding: 2px;
  border-radius: 999px;
  background: var(--el-fill-color-light, #f4f5f6);
  vertical-align: middle;
}

.chart-mode__thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 28px;
  height: 20px;
  border-radius: 999px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(22, 22, 24, 0.14);
  transition: transform 0.26s cubic-bezier(0.34, 1.3, 0.64, 1);
}

.chart-mode__thumb.is-3d {
  transform: translateX(28px);
}

.chart-mode__btn {
  position: relative;
  z-index: 1;
  width: 28px;
  height: 20px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #9a9da1;
  font-size: 11px;
  font-weight: 600;
  line-height: 20px;
  letter-spacing: 0.02em;
  cursor: pointer;
  transition: color 0.2s ease;
}

.chart-mode__btn.is-on {
  color: #161618;
}

.chart-mode__btn:hover {
  color: #161618;
}

.chart-mode__btn:focus-visible {
  outline: 2px solid var(--el-color-primary, #00beb4);
  outline-offset: 2px;
  border-radius: 999px;
}
</style>
