<template>
  <div
    class="pass-wrap"
    @pointerenter="onEnter"
    @pointermove="onMove"
    @pointerleave="onLeave"
  >
    <article class="pass" :class="{ 'is-hover': hover }" :style="styleVars" :aria-label="dept.deptName + ' 部门通行证'">
      <img class="pass__photo" :src="theme.photo" :alt="dept.deptName + ' 部门场景'" draggable="false" />
      <span class="pass__scrim" aria-hidden="true" />
      <span class="pass__grain" aria-hidden="true" />
      <span class="pass__sheen" aria-hidden="true" />

      <header class="pass__top">
        <span class="pass__brand">JINLIU GROUP</span>
        <span class="pass__kind">DEPT PASS <i aria-hidden="true" /> {{ theme.no }} / {{ theme.styleCount }}</span>
      </header>

      <div class="pass__center">
        <div class="pass__seal">
          <DeptEmblem
            class="pass__emblem"
            :name="artKey"
            :label="dept.deptName + ' 徽记'"
            :weight="2.9"
          />
        </div>
        <h3 class="pass__name" :title="dept.deptName">{{ dept.deptName }}</h3>
        <p class="pass__en">{{ catalog.en }}</p>
      </div>

      <footer class="pass__foot">
        <div class="pass__meta">
          <span class="pass__label">负责人</span>
          <span class="pass__value">{{ leader }}</span>
        </div>
        <div class="pass__meta">
          <span class="pass__label">上级</span>
          <span class="pass__value">{{ parentName }}</span>
        </div>
        <span class="pass__serial">{{ code }}</span>
      </footer>

      <span class="pass__edge" aria-hidden="true" />
    </article>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import DeptEmblem from './DeptEmblem.vue'
import { DEPT_CATALOG, badgeCode, resolveKey } from './deptArt'
import { passStyleOf } from './deptPassArt'

const props = defineProps({
  dept: { type: Object, required: true }
})

const artKey = computed(() => resolveKey(props.dept.deptName) || 'default')
const theme = computed(() => passStyleOf(props.dept, artKey.value))
const catalog = computed(() => DEPT_CATALOG[artKey.value] || DEPT_CATALOG.default)
const code = computed(() => badgeCode(props.dept))
const leader = computed(() => props.dept.leader || '待定')
const parentName = computed(() => props.dept.parentName || '集团总部')

const hover = ref(false)
const tilt = reactive({ rx: 0, ry: 0, mx: 50, my: 50, lift: 0, sheen: 0 })

const styleVars = computed(() => ({
  '--pass-accent': theme.value.accent,
  '--pass-rx': tilt.rx + 'deg',
  '--pass-ry': tilt.ry + 'deg',
  '--pass-lift': tilt.lift + 'px',
  '--pass-mx': tilt.mx + '%',
  '--pass-my': tilt.my + '%',
  '--pass-sheen': tilt.sheen
}))

const canTilt = () => {
  if (typeof window === 'undefined' || !window.matchMedia) return false
  return !window.matchMedia('(prefers-reduced-motion: reduce)').matches && window.matchMedia('(pointer: fine)').matches
}

const onEnter = () => {
  hover.value = true
  if (!canTilt()) return
  tilt.lift = -5
  tilt.sheen = 0.55
}

const onMove = event => {
  if (!canTilt()) return
  const box = event.currentTarget.getBoundingClientRect()
  if (!box.width || !box.height) return
  const px = Math.min(1, Math.max(0, (event.clientX - box.left) / box.width))
  const py = Math.min(1, Math.max(0, (event.clientY - box.top) / box.height))
  tilt.ry = (px - 0.5) * 9
  tilt.rx = (0.5 - py) * 7
  tilt.mx = px * 100
  tilt.my = py * 100
}

const onLeave = () => {
  hover.value = false
  tilt.rx = 0
  tilt.ry = 0
  tilt.lift = 0
  tilt.sheen = 0
}
</script>

<style scoped>
.pass-wrap {
  width: 100%;
  min-width: 0;
  perspective: 1100px;
}

.pass {
  position: relative;
  width: 100%;
  aspect-ratio: 856 / 540;
  overflow: hidden;
  isolation: isolate;
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 21px;
  background: #0b0d11;
  color: #fffaf0;
  box-shadow:
    0 32px 60px -30px rgba(0, 0, 0, 0.92),
    0 12px 24px -16px rgba(0, 0, 0, 0.7),
    0 1px 0 rgba(255, 255, 255, 0.22) inset;
  transform: rotateX(var(--pass-rx)) rotateY(var(--pass-ry)) translateY(var(--pass-lift));
  transform-style: preserve-3d;
  transition: transform 0.28s cubic-bezier(0.2, 0.75, 0.2, 1), box-shadow 0.28s ease;
  will-change: transform;
}

.pass.is-hover {
  box-shadow:
    0 42px 70px -32px rgba(0, 0, 0, 0.98),
    0 16px 30px -18px rgba(0, 0, 0, 0.75),
    0 1px 0 rgba(255, 255, 255, 0.3) inset;
}

.pass__photo {
  position: absolute;
  inset: 0;
  z-index: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: saturate(1.04) contrast(1.03) brightness(0.96);
  transform: scale(1.02);
  transition: transform 0.7s cubic-bezier(0.2, 0.7, 0.2, 1);
  user-select: none;
  pointer-events: none;
}

.pass.is-hover .pass__photo {
  transform: scale(1.06);
}

.pass__scrim {
  position: absolute;
  inset: 0;
  z-index: 1;
  background:
    linear-gradient(
      180deg,
      rgba(7, 9, 13, 0.62) 0%,
      rgba(7, 9, 13, 0.18) 24%,
      rgba(7, 9, 13, 0.04) 46%,
      rgba(7, 9, 13, 0.24) 64%,
      rgba(6, 8, 12, 0.78) 84%,
      rgba(5, 7, 10, 0.95) 100%
    ),
    radial-gradient(48% 54% at 50% 44%, rgba(6, 8, 12, 0.26) 0%, rgba(6, 8, 12, 0.05) 62%, transparent 82%);
  pointer-events: none;
}

.pass__grain {
  position: absolute;
  inset: 0;
  z-index: 2;
  opacity: 0.12;
  mix-blend-mode: overlay;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='140' height='140'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.86' numOctaves='2' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='140' height='140' filter='url(%23n)'/%3E%3C/svg%3E");
  pointer-events: none;
}

.pass__sheen {
  position: absolute;
  inset: 0;
  z-index: 6;
  opacity: var(--pass-sheen);
  background: radial-gradient(circle at var(--pass-mx) var(--pass-my), rgba(255, 255, 255, 0.55) 0%, rgba(255, 255, 255, 0.12) 30%, transparent 60%);
  mix-blend-mode: soft-light;
  transition: opacity 0.25s ease;
  pointer-events: none;
}

.pass__top {
  position: absolute;
  top: 5.8%;
  right: 6%;
  left: 6%;
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
}

.pass__brand {
  overflow: hidden;
  color: #fffdf8;
  font-size: clamp(8px, 0.62vw, 10px);
  font-weight: 800;
  letter-spacing: 0.22em;
  white-space: nowrap;
  opacity: 0.86;
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.75);
}

.pass__kind {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  flex: none;
  color: #fffdf8;
  font-size: clamp(6px, 0.5vw, 7.6px);
  font-weight: 700;
  letter-spacing: 0.14em;
  white-space: nowrap;
  opacity: 0.62;
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.8);
}

.pass__kind i {
  display: inline-block;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--pass-accent);
  box-shadow: 0 0 6px color-mix(in srgb, var(--pass-accent) 70%, transparent);
  opacity: 0.95;
}

.pass__center {
  position: absolute;
  top: 10%;
  right: 9%;
  bottom: 18%;
  left: 9%;
  z-index: 3;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  padding-bottom: 1.5%;
  min-height: 0;
}

.pass__seal {
  flex: none;
  display: grid;
  place-items: center;
  width: 24%;
  max-width: 132px;
  min-width: 54px;
  aspect-ratio: 1;
  filter: drop-shadow(0 10px 22px rgba(0, 0, 0, 0.52));
}

.pass__emblem {
  width: 100%;
  height: 100%;
  color: #fffdf5;
  filter: drop-shadow(0 1px 0 rgba(0, 0, 0, 0.45)) drop-shadow(0 0 10px rgba(0, 0, 0, 0.42));
}

.pass__name {
  max-width: 100%;
  margin: 1.4% 0 0;
  overflow: hidden;
  color: #fffdf8;
  font-family: "Songti SC", "STSong", "Noto Serif SC", "SimSun", serif;
  font-size: clamp(20px, 1.7vw, 30px);
  font-weight: 700;
  letter-spacing: 0.05em;
  line-height: 1.1;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-shadow:
    0 2px 16px rgba(0, 0, 0, 0.92),
    0 1px 2px rgba(0, 0, 0, 0.85);
}

.pass__en {
  max-width: 100%;
  margin: 2px 0 0;
  overflow: hidden;
  color: rgba(255, 253, 248, 0.74);
  font-size: clamp(6.4px, 0.5vw, 8.4px);
  font-weight: 700;
  letter-spacing: 0.2em;
  line-height: 1.3;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.9);
}

.pass__foot {
  position: absolute;
  right: 6%;
  bottom: 5.4%;
  left: 6%;
  z-index: 3;
  display: flex;
  align-items: flex-end;
  gap: 6%;
  padding-top: 1.8%;
  border-top: 1px solid rgba(255, 255, 255, 0.22);
}

.pass__foot::before {
  content: "";
  position: absolute;
  z-index: -1;
  inset: -14px -18px -10px;
  background: linear-gradient(180deg, rgba(6, 8, 12, 0) 0%, rgba(6, 8, 12, 0.55) 40%, rgba(6, 8, 12, 0.94) 100%);
  pointer-events: none;
}

.pass__meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.pass__label {
  color: rgba(255, 253, 248, 0.56);
  font-size: clamp(8px, 0.55vw, 10px);
  font-weight: 700;
  letter-spacing: 0.12em;
  line-height: 1;
}

.pass__value {
  max-width: 120px;
  overflow: hidden;
  color: #fffdf8;
  font-size: clamp(11px, 0.8vw, 13px);
  font-weight: 700;
  line-height: 1.15;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.8);
}

.pass__serial {
  margin-left: auto;
  color: rgba(255, 253, 248, 0.56);
  font-size: clamp(8px, 0.6vw, 10px);
  font-weight: 700;
  letter-spacing: 0.1em;
  line-height: 1.5;
  white-space: nowrap;
}

.pass__edge {
  position: absolute;
  inset: 0;
  z-index: 7;
  border-radius: inherit;
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.28) inset,
    0 0 0 1px rgba(0, 0, 0, 0.18) inset,
    0 -1px 0 rgba(0, 0, 0, 0.45) inset;
  pointer-events: none;
}

@media (prefers-reduced-motion: reduce) {
  .pass {
    transform: none;
    transition: box-shadow 0.2s ease;
  }

  .pass__photo {
    transition: none;
  }
}
</style>
