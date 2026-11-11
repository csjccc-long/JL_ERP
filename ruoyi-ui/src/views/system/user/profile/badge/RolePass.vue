<template>
  <div
    class="rolepass-wrap"
    @pointerenter="onEnter"
    @pointermove="onMove"
    @pointerleave="onLeave"
  >
    <article
      class="rolepass"
      :class="['is-' + art.tone, { 'is-hover': hover }]"
      :style="styleVars"
      :aria-label="roleName + ' 角色卡'"
    >
      <div class="rolepass__tilt">
        <div class="rolepass__card">
          <img class="rolepass__photo" :src="art.photo" alt="" draggable="false" />
          <span class="rolepass__scrim" aria-hidden="true" />

          <span class="rolepass__guilloche" aria-hidden="true">
            <svg viewBox="0 0 240 240">
              <g fill="none" stroke="currentColor">
                <circle cx="120" cy="120" r="106" stroke-opacity="0.32" />
                <circle cx="120" cy="120" r="88" stroke-opacity="0.42" stroke-dasharray="2 7" />
                <circle cx="120" cy="120" r="70" stroke-opacity="0.3" />
                <ellipse cx="120" cy="120" rx="114" ry="58" transform="rotate(22 120 120)" stroke-opacity="0.38" />
                <ellipse cx="120" cy="120" rx="114" ry="58" transform="rotate(-22 120 120)" stroke-opacity="0.24" />
                <ellipse cx="120" cy="120" rx="116" ry="34" transform="rotate(48 120 120)" stroke-opacity="0.28" />
                <ellipse cx="120" cy="120" rx="116" ry="34" transform="rotate(-48 120 120)" stroke-opacity="0.22" />
                <path d="M120 16 C172 58 172 182 120 224 C68 182 68 58 120 16 Z" stroke-opacity="0.26" />
              </g>
            </svg>
          </span>

          <span class="rolepass__foil" aria-hidden="true" />
          <span class="rolepass__frame" aria-hidden="true" />
          <span class="rolepass__grain" aria-hidden="true" />
          <span class="rolepass__sheen" aria-hidden="true" />

          <header class="rolepass__head">
            <span class="rolepass__brand">JINLIU GROUP</span>
            <span class="rolepass__series">
              ROLE CARD <i aria-hidden="true" /> {{ pad(art.styleNo) }} / {{ pad(art.styleCount) }}
            </span>
          </header>

          <svg class="rolepass__signal" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M7 6a12 12 0 0 1 0 12" />
            <path d="M11 4a16 16 0 0 1 0 16" />
            <path d="M15 2a20 20 0 0 1 0 20" />
          </svg>

          <div class="rolepass__body">
            <span class="rolepass__seal">
              <RoleEmblem
                class="rolepass__emblem"
                :name="art.emblem"
                :label="roleName + ' 徽记'"
                :weight="2.6"
              />
            </span>
            <div class="rolepass__identity">
              <p class="rolepass__en">{{ art.en }}</p>
              <h4 class="rolepass__name" :title="roleName">{{ roleName }}</h4>
              <p class="rolepass__tagline">{{ art.tagline }}</p>
            </div>
          </div>

          <footer class="rolepass__foot">
            <div class="rolepass__meta">
              <span class="rolepass__label">角色代码</span>
              <span class="rolepass__value" :title="roleKey">{{ roleKey }}</span>
            </div>
            <div class="rolepass__meta">
              <span class="rolepass__label">数据范围</span>
              <span class="rolepass__value">{{ scope }}</span>
            </div>
            <div class="rolepass__meta">
              <span class="rolepass__label">状态</span>
              <span class="rolepass__value">{{ status }}</span>
            </div>
            <span class="rolepass__serial">{{ code }}</span>
          </footer>
        </div>
      </div>
    </article>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import RoleEmblem from './RoleEmblem.vue'
import { dataScopeLabel, roleArtOf, roleCode } from './roleArt'

const props = defineProps({
  role: { type: Object, required: true },
  index: { type: Number, default: 0 },
  total: { type: Number, default: 1 }
})

const art = computed(() => roleArtOf(props.role, props.index))
const roleName = computed(() => props.role.roleName || '未命名角色')
const roleKey = computed(() => props.role.roleKey || art.value.short)
const scope = computed(() => dataScopeLabel(props.role.dataScope))
const status = computed(() => (String(props.role.status ?? '0') === '0' ? '正常' : '停用'))
const code = computed(() => roleCode(props.role))

const hover = ref(false)
const tilt = reactive({ rx: 0, ry: 0, mx: 50, my: 50, lift: 0, sheen: 0 })

const styleVars = computed(() => ({
  '--rp-accent': art.value.accent,
  '--rp-rx': tilt.rx + 'deg',
  '--rp-ry': tilt.ry + 'deg',
  '--rp-lift': tilt.lift + 'px',
  '--rp-mx': tilt.mx + '%',
  '--rp-my': tilt.my + '%',
  '--rp-sheen': tilt.sheen
}))

const pad = value => String(value || 0).padStart(2, '0')

const canTilt = () => {
  if (typeof window === 'undefined' || !window.matchMedia) return false
  return !window.matchMedia('(prefers-reduced-motion: reduce)').matches && window.matchMedia('(pointer: fine)').matches
}

const onEnter = () => {
  hover.value = true
  if (!canTilt()) return
  tilt.lift = -4
  tilt.sheen = 0.5
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
.rolepass-wrap {
  width: 100%;
  min-width: 0;
  perspective: 1100px;
}

.rolepass {
  --rp-ink: #fffdf8;
  --rp-muted: rgba(255, 253, 248, 0.62);
  --rp-line: rgba(255, 255, 255, 0.18);
  width: 100%;
}

.rolepass.is-light {
  --rp-ink: #241b0f;
  --rp-muted: rgba(36, 27, 15, 0.62);
  --rp-line: rgba(36, 27, 15, 0.18);
}

.rolepass__tilt {
  position: relative;
  width: 100%;
  aspect-ratio: 856 / 540;
  transform: rotateX(var(--rp-rx)) rotateY(var(--rp-ry)) translateY(var(--rp-lift));
  transform-style: preserve-3d;
  transition: transform 0.28s cubic-bezier(0.2, 0.75, 0.2, 1);
  will-change: transform;
}

.rolepass__card {
  position: absolute;
  inset: 0;
  overflow: hidden;
  isolation: isolate;
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 21px;
  background: #0b0d11;
  color: var(--rp-ink);
  box-shadow:
    0 32px 60px -30px rgba(0, 0, 0, 0.92),
    0 12px 24px -16px rgba(0, 0, 0, 0.7),
    0 1px 0 rgba(255, 255, 255, 0.22) inset;
  transition: box-shadow 0.28s ease;
}

.rolepass.is-light .rolepass__card {
  border-color: rgba(80, 58, 26, 0.22);
  box-shadow:
    0 30px 54px -32px rgba(0, 0, 0, 0.55),
    0 10px 20px -16px rgba(0, 0, 0, 0.34),
    0 1px 0 rgba(255, 255, 255, 0.8) inset;
}

.rolepass.is-hover .rolepass__card {
  box-shadow:
    0 42px 70px -32px rgba(0, 0, 0, 0.98),
    0 16px 30px -18px rgba(0, 0, 0, 0.75),
    0 1px 0 rgba(255, 255, 255, 0.3) inset;
}

.rolepass__photo {
  position: absolute;
  inset: 0;
  z-index: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: saturate(1.04) contrast(1.04) brightness(0.92);
  transform: scale(1.01);
  transition: transform 0.7s cubic-bezier(0.2, 0.7, 0.2, 1);
  user-select: none;
  pointer-events: none;
}

.rolepass.is-hover .rolepass__photo {
  transform: scale(1.045);
}

.rolepass.is-light .rolepass__photo {
  filter: saturate(0.94) contrast(1.02) brightness(1.05);
}

.rolepass__scrim {
  position: absolute;
  inset: 0;
  z-index: 1;
  background:
    linear-gradient(
      112deg,
      rgba(5, 7, 11, 0.94) 0%,
      rgba(5, 7, 11, 0.78) 30%,
      rgba(5, 7, 11, 0.48) 54%,
      rgba(5, 7, 11, 0.3) 74%,
      rgba(5, 7, 11, 0.72) 100%
    ),
    linear-gradient(
      180deg,
      rgba(5, 7, 11, 0.56) 0%,
      rgba(5, 7, 11, 0.04) 26%,
      rgba(5, 7, 11, 0.06) 58%,
      rgba(5, 7, 11, 0.68) 84%,
      rgba(5, 7, 11, 0.9) 100%
    );
  pointer-events: none;
}

.rolepass.is-light .rolepass__scrim {
  background:
    linear-gradient(
      112deg,
      rgba(255, 250, 240, 0.94) 0%,
      rgba(255, 250, 240, 0.8) 32%,
      rgba(255, 250, 240, 0.48) 58%,
      rgba(255, 250, 240, 0.26) 76%,
      rgba(255, 250, 240, 0.74) 100%
    ),
    linear-gradient(180deg, rgba(255, 250, 240, 0.5) 0%, transparent 34%, rgba(255, 250, 240, 0.4) 82%, rgba(255, 250, 240, 0.72) 100%);
}

.rolepass__guilloche {
  position: absolute;
  top: 2%;
  right: -13%;
  z-index: 1;
  width: 56%;
  color: var(--rp-accent);
  opacity: 0.52;
  mix-blend-mode: screen;
  pointer-events: none;
}

.rolepass.is-light .rolepass__guilloche {
  opacity: 0.4;
  mix-blend-mode: multiply;
}

.rolepass__guilloche svg {
  display: block;
  width: 100%;
  height: auto;
}

.rolepass__foil {
  position: absolute;
  top: 36%;
  right: 12%;
  z-index: 1;
  width: clamp(26px, 8.2%, 46px);
  aspect-ratio: 1;
  border: 1px solid rgba(255, 255, 255, 0.36);
  border-radius: 50%;
  background: conic-gradient(
    from 208deg,
    #ffd7a3 0deg,
    #b6f0ff 62deg,
    #d9b8ff 128deg,
    #ffbfd8 192deg,
    #f4ffb0 258deg,
    #ffd7a3 360deg
  );
  box-shadow:
    0 0 0 1px rgba(0, 0, 0, 0.18) inset,
    0 4px 12px rgba(0, 0, 0, 0.34);
  opacity: 0.48;
  mix-blend-mode: color-dodge;
  pointer-events: none;
}

.rolepass.is-light .rolepass__foil {
  border-color: rgba(70, 48, 18, 0.24);
  opacity: 0.4;
  mix-blend-mode: multiply;
}

.rolepass__frame {
  position: absolute;
  inset: 2.8%;
  z-index: 2;
  border: 1px solid var(--rp-line);
  border-radius: 15px;
  opacity: 0.55;
  pointer-events: none;
}

.rolepass__grain {
  position: absolute;
  inset: 0;
  z-index: 2;
  opacity: 0.1;
  mix-blend-mode: overlay;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='140' height='140'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.86' numOctaves='2' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='140' height='140' filter='url(%23n)'/%3E%3C/svg%3E");
  pointer-events: none;
}

.rolepass__sheen {
  position: absolute;
  inset: 0;
  z-index: 6;
  opacity: var(--rp-sheen);
  background: radial-gradient(
    circle at var(--rp-mx) var(--rp-my),
    rgba(255, 255, 255, 0.5) 0%,
    rgba(255, 255, 255, 0.1) 30%,
    transparent 62%
  );
  mix-blend-mode: soft-light;
  transition: opacity 0.25s ease;
  pointer-events: none;
}

.rolepass__head {
  position: absolute;
  top: 6.6%;
  right: 8%;
  left: 8%;
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
}

.rolepass__brand {
  color: var(--rp-ink);
  font-size: clamp(8px, 0.62vw, 10px);
  font-weight: 800;
  letter-spacing: 0.2em;
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.36);
}

.rolepass.is-light .rolepass__brand {
  text-shadow: none;
}

.rolepass__series {
  color: var(--rp-muted);
  font-size: clamp(6.4px, 0.5vw, 8.2px);
  font-weight: 600;
  letter-spacing: 0.16em;
  white-space: nowrap;
}

.rolepass__series i {
  display: inline-block;
  width: 3px;
  height: 3px;
  margin: 0 3px;
  border-radius: 50%;
  background: var(--rp-accent);
  vertical-align: middle;
}

.rolepass__signal {
  position: absolute;
  top: 18.5%;
  right: 8%;
  z-index: 3;
  width: clamp(16px, 4.4%, 24px);
  height: auto;
  fill: none;
  stroke: var(--rp-accent);
  stroke-width: 1.7;
  stroke-linecap: round;
  opacity: 0.84;
}

.rolepass__body {
  position: absolute;
  top: 31.5%;
  right: 8%;
  left: 8%;
  z-index: 3;
  display: flex;
  align-items: center;
  gap: clamp(10px, 2.4%, 18px);
  min-width: 0;
}

.rolepass__seal {
  display: grid;
  flex: none;
  place-items: center;
  width: clamp(42px, 12%, 70px);
  aspect-ratio: 1;
  color: var(--rp-ink);
  filter: drop-shadow(0 8px 18px rgba(0, 0, 0, 0.46));
}

.rolepass.is-light .rolepass__seal {
  filter: drop-shadow(0 6px 14px rgba(60, 40, 10, 0.2));
}

.rolepass__emblem {
  width: 100%;
  height: 100%;
}

.rolepass__identity {
  min-width: 0;
}

.rolepass__en {
  margin: 0 0 2px;
  overflow: hidden;
  color: var(--rp-accent);
  font-size: clamp(7px, 0.52vw, 8.6px);
  font-weight: 700;
  letter-spacing: 0.2em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rolepass__name {
  margin: 0;
  overflow: hidden;
  color: var(--rp-ink);
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', 'SimSun', serif;
  font-size: clamp(21px, 2.1vw, 34px);
  font-weight: 700;
  letter-spacing: 0.06em;
  line-height: 1.08;
  text-overflow: ellipsis;
  text-shadow: 0 2px 18px rgba(0, 0, 0, 0.52);
  white-space: nowrap;
}

.rolepass.is-light .rolepass__name {
  text-shadow: 0 1px 0 rgba(255, 255, 255, 0.4);
}

.rolepass__tagline {
  margin: clamp(3px, 0.7%, 6px) 0 0;
  overflow: hidden;
  color: var(--rp-muted);
  font-size: clamp(8.4px, 0.68vw, 11px);
  font-weight: 500;
  letter-spacing: 0.08em;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rolepass__foot {
  position: absolute;
  right: 8%;
  bottom: 5.6%;
  left: 8%;
  z-index: 3;
  display: flex;
  align-items: flex-end;
  gap: clamp(12px, 3.4%, 30px);
  padding-top: 2%;
  border-top: 1px solid var(--rp-line);
}

.rolepass__meta {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.rolepass__label {
  color: var(--rp-muted);
  font-size: clamp(6.4px, 0.46vw, 7.6px);
  font-weight: 500;
  letter-spacing: 0.14em;
}

.rolepass__value {
  overflow: hidden;
  max-width: 132px;
  color: var(--rp-ink);
  font-size: clamp(9px, 0.7vw, 11.4px);
  font-weight: 600;
  letter-spacing: 0.02em;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rolepass__serial {
  margin-left: auto;
  color: var(--rp-muted);
  font-family: 'SFMono-Regular', 'Cascadia Mono', Consolas, monospace;
  font-size: clamp(7px, 0.54vw, 9px);
  letter-spacing: 0.08em;
  white-space: nowrap;
}

@media (max-width: 560px) {
  .rolepass__card {
    border-radius: 16px;
  }

  .rolepass__frame {
    border-radius: 12px;
  }

  .rolepass__body {
    top: 35%;
    gap: 10px;
  }

  .rolepass__name {
    font-size: clamp(19px, 5.6vw, 25px);
  }

  .rolepass__tagline {
    letter-spacing: 0.04em;
  }

  .rolepass__foot {
    gap: 10px;
  }

  .rolepass__value {
    max-width: 84px;
  }

  .rolepass__serial {
    font-size: 7px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .rolepass__tilt,
  .rolepass__photo {
    transition: none;
  }
}
</style>
