<template>
  <svg
    class="role-emblem"
    viewBox="0 0 64 64"
    role="img"
    :aria-label="label"
    xmlns="http://www.w3.org/2000/svg"
  >
    <g class="emblem-fill" fill="currentColor" stroke="none">
      <template v-for="(shape, i) in filled" :key="'f' + i">
        <path v-if="shape.t === 'path'" :d="shape.d" :transform="shape.transform" />
        <circle v-else-if="shape.t === 'circle'" :cx="shape.cx" :cy="shape.cy" :r="shape.r" />
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
          :transform="shape.transform"
        />
        <circle
          v-else-if="shape.t === 'circle'"
          :cx="shape.cx" :cy="shape.cy" :r="shape.r"
          :stroke-opacity="shape.opacity ?? 1"
          :transform="shape.transform"
        />
        <rect
          v-else-if="shape.t === 'rect'"
          :x="shape.x" :y="shape.y" :width="shape.width" :height="shape.height" :rx="shape.rx"
          :stroke-opacity="shape.opacity ?? 1"
          :transform="shape.transform"
        />
        <line
          v-else-if="shape.t === 'line'"
          :x1="shape.x1" :y1="shape.y1" :x2="shape.x2" :y2="shape.y2"
          :stroke-opacity="shape.opacity ?? 1"
          :transform="shape.transform"
        />
      </template>
    </g>
  </svg>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  name: { type: String, default: 'star' },
  label: { type: String, default: '角色徽记' },
  weight: { type: [Number, String], default: 2.3 }
})

const p = (d, extra = {}) => ({ t: 'path', d, ...extra })
const c = (cx, cy, r, extra = {}) => ({ t: 'circle', cx, cy, r, ...extra })
const l = (x1, y1, x2, y2, extra = {}) => ({ t: 'line', x1, y1, x2, y2, ...extra })
const rect = (x, y, width, height, rx, extra = {}) => ({ t: 'rect', x, y, width, height, rx, ...extra })

const EMBLEMS = {
  crown: [
    p('M12 25 L21 41 H43 L52 25 L40 33 L32 17 L24 33 Z'),
    p('M18 47 H46'),
    c(12, 25, 1.9, { fill: true }),
    c(32, 17, 1.9, { fill: true }),
    c(52, 25, 1.9, { fill: true })
  ],
  shield: [
    p('M32 9 L51 16 V31 C51 43 43 51 32 56 C21 51 13 43 13 31 V16 Z'),
    p('M24 32 L30 38 L41 26')
  ],
  cart: [
    p('M11 15 H18 L23 42 H48'),
    p('M21 22 H53 L49 36 H24'),
    p('M31 27 V33'),
    p('M37 27 V33'),
    c(26, 49, 3.4),
    c(45, 49, 3.4)
  ],
  people: [
    c(25, 23, 7),
    c(43, 26, 5.4),
    p('M12 52 C12 41 18 35 25 35 C32 35 38 41 38 52'),
    p('M39 52 C39 43 44 38 50 38 C55 38 58 42 58 48')
  ],
  box: [
    p('M32 10 L53 21 V43 L32 54 L11 43 V21 Z'),
    p('M11 21 L32 32 L53 21'),
    p('M32 32 V54'),
    p('M21.5 15.5 L42.5 26.5')
  ],
  scale: [
    p('M32 12 V52'),
    p('M20 52 H44'),
    p('M16 20 H48'),
    c(32, 12, 2.6),
    p('M16 20 L9 34 H23 Z'),
    p('M48 20 L41 34 H55 Z'),
    p('M9 34 C9 39 23 39 23 34'),
    p('M41 34 C41 39 55 39 55 34')
  ],
  book: [
    p('M32 16 C26 11 17 11 11 14 V48 C18 45 26 46 32 51 C38 46 46 45 53 48 V14 C47 11 38 11 32 16 Z'),
    p('M32 16 V51'),
    p('M17 21 C22 20 27 21 30 23'),
    p('M17 29 C22 28 27 29 30 31'),
    p('M47 21 C42 20 37 21 34 23'),
    p('M47 29 C42 28 37 29 34 31')
  ],
  user: [
    c(32, 22, 8),
    p('M15 53 C15 41 22 34 32 34 C42 34 49 41 49 53')
  ],
  chart: [
    p('M12 50 H54'),
    p('M12 50 V12'),
    p('M18 40 V30'),
    p('M28 40 V24'),
    p('M38 40 V32'),
    p('M48 40 V18'),
    p('M43 18 H53 V28')
  ],
  megaphone: [
    p('M12 28 V40 L24 43 V25 Z'),
    p('M24 25 L48 14 V50 L24 43'),
    p('M30 44 L33 54 H40 L36 45'),
    p('M52 24 C55 28 55 36 52 40', { opacity: 0.6 })
  ],
  chip: [
    rect(14, 14, 36, 36, 8),
    rect(24, 24, 16, 16, 2),
    p('M24 14 V20'),
    p('M31 14 V20'),
    p('M38 14 V20'),
    p('M24 44 V50'),
    p('M31 44 V50'),
    p('M38 44 V50'),
    p('M14 24 H20'),
    p('M14 31 H20'),
    p('M14 38 H20'),
    p('M44 24 H50'),
    p('M44 31 H50'),
    p('M44 38 H50'),
    c(32, 32, 2.4)
  ],
  check: [
    c(32, 32, 23),
    p('M20 33 L28 41 L45 22')
  ],
  server: [
    rect(13, 12, 38, 17, 4),
    rect(13, 35, 38, 17, 4),
    c(20, 20.5, 1.8, { fill: true }),
    c(20, 43.5, 1.8, { fill: true }),
    l(41, 20.5, 47, 20.5),
    l(41, 43.5, 47, 43.5)
  ],
  headset: [
    p('M14 36 V30 C14 20 22 12 32 12 C42 12 50 20 50 30 V36'),
    rect(8, 33, 10, 15, 5),
    rect(46, 33, 10, 15, 5),
    p('M14 48 C14 53 19 56 25 56'),
    c(27, 56, 2.6)
  ],
  search: [
    c(28, 28, 14),
    p('M39 39 L53 53')
  ],
  star: [
    p('M32 8 L39.5 24.5 L57 26.5 L44 38.5 L47.5 56 L32 47 L16.5 56 L20 38.5 L7 26.5 L24.5 24.5 Z', { fill: true })
  ]
}

const shapes = computed(() => EMBLEMS[props.name] || EMBLEMS.star)
const filled = computed(() => shapes.value.filter(shape => shape.fill))
const stroked = computed(() => shapes.value.filter(shape => !shape.fill))
</script>

<style scoped>
.role-emblem {
  width: 100%;
  height: 100%;
  overflow: visible;
  color: inherit;
}

.emblem-fill {
  color: inherit;
  opacity: 0.92;
}

.emblem-line {
  filter: drop-shadow(0 1px 0 rgba(0, 0, 0, 0.18));
}
</style>
