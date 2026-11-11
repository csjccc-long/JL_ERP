<template>
  <svg
    class="dept-emblem"
    viewBox="0 0 64 64"
    role="img"
    :aria-label="label"
    xmlns="http://www.w3.org/2000/svg"
  >
    <g class="emblem-fill" fill="currentColor" stroke="none">
      <template v-for="(shape, i) in filled" :key="'f' + i">
        <path v-if="shape.t === 'path'" :d="shape.d" :transform="shape.transform" />
        <circle v-else-if="shape.t === 'circle'" :cx="shape.cx" :cy="shape.cy" :r="shape.r" />
        <ellipse v-else-if="shape.t === 'ellipse'" :cx="shape.cx" :cy="shape.cy" :rx="shape.rx" :ry="shape.ry" :transform="shape.transform" />
        <rect
          v-else-if="shape.t === 'rect'"
          :x="shape.x" :y="shape.y" :width="shape.width" :height="shape.height" :rx="shape.rx"
          :transform="shape.transform"
        />
      </template>
    </g>

    <g
      class="emblem-line"
      fill="none"
      stroke="currentColor"
      :stroke-width="weight"
      stroke-linecap="round"
      stroke-linejoin="round"
    >
      <template v-for="(shape, i) in stroked" :key="'s' + i">
        <path
          v-if="shape.t === 'path'"
          :d="shape.d"
          :stroke-opacity="shape.opacity ?? 1"
          :stroke-dasharray="shape.dash || null"
          :transform="shape.transform"
        />
        <circle
          v-else-if="shape.t === 'circle'"
          :cx="shape.cx" :cy="shape.cy" :r="shape.r"
          :stroke-opacity="shape.opacity ?? 1"
          :stroke-dasharray="shape.dash || null"
          :transform="shape.transform"
        />
        <ellipse
          v-else-if="shape.t === 'ellipse'"
          :cx="shape.cx" :cy="shape.cy" :rx="shape.rx" :ry="shape.ry"
          :stroke-opacity="shape.opacity ?? 1"
          :stroke-dasharray="shape.dash || null"
          :transform="shape.transform"
        />
        <rect
          v-else-if="shape.t === 'rect'"
          :x="shape.x" :y="shape.y" :width="shape.width" :height="shape.height" :rx="shape.rx"
          :stroke-opacity="shape.opacity ?? 1"
          :stroke-dasharray="shape.dash || null"
          :transform="shape.transform"
        />
        <line
          v-else-if="shape.t === 'line'"
          :x1="shape.x1" :y1="shape.y1" :x2="shape.x2" :y2="shape.y2"
          :stroke-opacity="shape.opacity ?? 1"
          :stroke-dasharray="shape.dash || null"
          :transform="shape.transform"
        />
      </template>
    </g>
  </svg>
</template>

<script setup>
import { computed } from 'vue'
import { emblemOf } from './deptArt'

const props = defineProps({
  name: { type: String, default: 'default' },
  label: { type: String, default: '部门徽记' },
  weight: { type: [Number, String], default: 2.1 }
})

const art = computed(() => emblemOf(props.name))

const allShapes = computed(() => {
  const a = art.value
  return [
    ...(a.paths || []),
    ...(a.circles || []),
    ...(a.rects || []),
    ...(a.lines || []),
    ...(a.ellipses || []),
    ...(a.polygons || [])
  ]
})

const filled = computed(() => allShapes.value.filter(s => s.fill))
const stroked = computed(() => allShapes.value.filter(s => !s.fill))
</script>

<style scoped>
.dept-emblem {
  width: 100%;
  height: 100%;
  overflow: visible;
  color: inherit;
}

.emblem-fill {
  color: inherit;
  opacity: 0.9;
}

.emblem-line {
  filter: drop-shadow(0 1px 0 rgba(0, 0, 0, 0.35));
}
</style>
