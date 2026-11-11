<template>
  <span
    ref="rootRef"
    class="bell-toggle"
    :class="{ 'is-disabled': disabled }"
    :data-on="on ? 'true' : 'false'"
    :data-pressed="pressing ? '' : undefined"
    :style="cssVars"
  >
    <button
      ref="buttonRef"
      type="button"
      class="bell-toggle__button"
      :aria-pressed="on"
      :aria-label="label || offLabel"
      :disabled="disabled"
      @pointerdown="onPointerDown"
      @pointerup="release"
      @pointercancel="release"
      @pointerleave="release"
      @keydown="onKeyDown"
      @click="toggle"
    >
      <span class="bell-toggle__bell" aria-hidden="true">
        <span ref="glyphRef" class="bell-toggle__glyph" :style="{ transformOrigin: '50% ' + ringPivot + '%' }">
          <slot name="icon">
            <svg v-if="clapper" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M6 16.5V10a6 6 0 0 1 12 0v6.5l1.6 2.3H4.4L6 16.5z" />
              <path d="M12 2.5V4" />
            </svg>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M6 16.5V10a6 6 0 0 1 12 0v6.5l1.6 2.3H4.4L6 16.5z" />
              <path d="M10.4 19.8a1.7 1.7 0 0 0 3.2 0" />
            </svg>
          </slot>
        </span>

        <span v-if="clapper" ref="clapperRef" class="bell-toggle__clapper" :style="{ transformOrigin: '50% ' + ringPivot + '%' }">
          <svg viewBox="0 0 24 24"><circle cx="12" cy="20.4" r="1.7" fill="currentColor" /></svg>
        </span>

        <template v-if="waves">
          <svg ref="waveLeft" class="bell-toggle__wave bell-toggle__wave--left" viewBox="0 0 14 14">
            <path d="M14 8a6 6 0 0 0-6 6" />
            <path d="M14 4A10 10 0 0 0 4 14" />
          </svg>
          <svg ref="waveRight" class="bell-toggle__wave bell-toggle__wave--right" viewBox="0 0 14 14">
            <path d="M0 8a6 6 0 0 1 6 6" />
            <path d="M0 4a10 10 0 0 1 10 10" />
          </svg>
        </template>

        <span v-if="badge" class="bell-toggle__badge" :data-show="showBadge ? '' : undefined">
          <span :key="count" class="bell-toggle__digit">{{ count > 9 ? '9+' : count }}</span>
        </span>
      </span>

      <span class="bell-toggle__say" aria-hidden="true">
        <span ref="offRef" class="bell-toggle__face bell-toggle__face--off">{{ offLabel }}</span>
        <span ref="onRef" class="bell-toggle__face bell-toggle__face--on">{{ onLabel }}</span>
      </span>
    </button>
  </span>
</template>

<script setup name="BellToggle">
/**
 * 铃铛开关 / 通知按钮
 * 移植自 react-bits BellToggle：铃铛摆动 + 声波扩散 + 标签宽度裁切交叉淡入 + 计数徽标弹入
 * 纯 Vue3 + Web Animations API 实现，无第三方动画依赖
 */
const SIZES = {
  sm: [36, 12.5, 14, 15, 10],
  md: [44, 13.5, 16, 19, 9],
  lg: [52, 15, 18, 23, 10]
}
const SEG_EASE = 'cubic-bezier(0.77, 0, 0.175, 1)'
const WARP = 0.6
const SPRING_MS = 300
const WOBBLE = { amplitude: 0.4, passes: 3, duration: 420 }

const props = defineProps({
  offLabel: { type: String, default: '通知我' },
  onLabel: { type: String, default: '已开启通知' },
  /** 无障碍标签，缺省用 offLabel */
  label: { type: String, default: null },
  color: { type: String, default: '#f5f5f5' },
  background: { type: String, default: '#27272a' },
  onColor: { type: String, default: '#18181b' },
  onBackground: { type: String, default: '#f5f5f5' },
  size: { type: String, default: 'md' },
  radius: { type: Number, default: 22 },
  ringAmplitude: { type: Number, default: 17 },
  ringPasses: { type: Number, default: 5 },
  ringDecay: { type: Number, default: 1 },
  ringDuration: { type: Number, default: 820 },
  ringPivot: { type: Number, default: 16 },
  crossfadeMs: { type: Number, default: 200 },
  revealBounce: { type: Number, default: 0 },
  count: { type: Number, default: 0 },
  badge: { type: Boolean, default: true },
  /** 未开启时也显示计数（关闭态就是通知入口时需要） */
  badgeAlways: { type: Boolean, default: false },
  badgeColor: { type: String, default: '#ef4444' },
  badgeTextColor: { type: String, default: '#ffffff' },
  waves: { type: Boolean, default: true },
  clapper: { type: Boolean, default: false },
  /** 受控展开态；undefined 表示非受控 */
  pressed: { type: null, default: undefined },
  defaultPressed: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['update:pressed', 'change'])

const rootRef = ref(null)
const buttonRef = ref(null)
const glyphRef = ref(null)
const clapperRef = ref(null)
const waveLeft = ref(null)
const waveRight = ref(null)
const offRef = ref(null)
const onRef = ref(null)

const inner = ref(props.defaultPressed)
const pressing = ref(false)
const reduce = ref(false)
const t = ref(0)
const wOff = ref(0)
const wOn = ref(0)
const pending = ref(null)
const lastInput = ref('pointer')
let rafId = 0
let resizeObserver = null

const on = computed(() => (props.pressed === undefined || props.pressed === null ? inner.value : props.pressed))
const showBadge = computed(() => props.badge && props.count > 0 && (props.badgeAlways || on.value))
const metrics = computed(() => SIZES[props.size] || SIZES.md)

const cssVars = computed(() => {
  const [h, fs, iconSize, px, gap] = metrics.value
  return {
    '--bt-clip': clip.value + 'px',
    '--bt-color': props.color,
    '--bt-bg': props.background,
    '--bt-on-color': props.onColor,
    '--bt-on-bg': props.onBackground,
    '--bt-badge': props.badgeColor,
    '--bt-badge-ink': props.badgeTextColor,
    '--bt-radius': props.radius + 'px',
    '--bt-fade': props.crossfadeMs + 'ms',
    '--bt-h': h + 'px',
    '--bt-fs': fs + 'px',
    '--bt-icon': iconSize + 'px',
    '--bt-px': px + 'px',
    '--bt-gap': gap + 'px'
  }
})

/** 展开时裁掉多余宽度，让胶囊随文案宽度伸缩 */
const clip = computed(() => {
  const a = wOff.value
  const b = wOn.value
  return Math.max(a, b) - (a + (b - a) * t.value)
})

function measure() {
  if (offRef.value) wOff.value = offRef.value.offsetWidth
  if (onRef.value) wOn.value = onRef.value.offsetWidth
}

function animateT(target) {
  cancelAnimationFrame(rafId)
  if (reduce.value) {
    t.value = target
    return
  }
  const from = t.value
  const start = performance.now()
  const bounce = Math.min(Math.max(props.revealBounce, 0), 1)
  const c1 = bounce > 0 ? 1.70158 * (1 + bounce) : 0
  const ease = bounce > 0
    ? x => 1 + (c1 + 1) * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2)
    : x => 1 - Math.pow(1 - x, 3)
  const step = now => {
    const p = Math.min((now - start) / SPRING_MS, 1)
    t.value = from + (target - from) * ease(p)
    if (p < 1) {
      rafId = requestAnimationFrame(step)
    } else {
      t.value = target
    }
  }
  rafId = requestAnimationFrame(step)
}

/* ---------- 铃铛摆动 + 声波 ---------- */
const passOffset = (k, passes) => 1 - Math.pow(1 - (k + 2 / 3) / (passes + 1), WARP)

function ringKeyframes(from, amplitude, passes, decay) {
  const frames = [{ transform: 'rotate(' + from + 'deg)', offset: 0, easing: SEG_EASE }]
  for (let k = 0; k < passes; k++) {
    const angle = amplitude * Math.pow(1 - k / passes, decay) * (k % 2 ? 1 : -1)
    frames.push({
      transform: 'rotate(' + angle.toFixed(2) + 'deg)',
      offset: passOffset(k, passes),
      easing: SEG_EASE
    })
  }
  frames.push({ transform: 'rotate(0deg)', offset: 1 })
  return frames
}

function liveAngle(el) {
  const tf = getComputedStyle(el).transform
  if (!tf || tf === 'none') return 0
  const m = new DOMMatrix(tf)
  return (Math.atan2(m.b, m.a) * 180) / Math.PI
}

function swing(amplitude, passes, duration) {
  const el = glyphRef.value
  if (!el || reduce.value) return
  el.getAnimations().forEach(a => a.cancel())
  el.animate(ringKeyframes(liveAngle(el), amplitude, passes, props.ringDecay), { duration, easing: 'linear' })
  const c = clapperRef.value
  if (c) {
    c.getAnimations().forEach(a => a.cancel())
    c.animate(ringKeyframes(liveAngle(c), amplitude * 1.6, passes, props.ringDecay), {
      duration,
      delay: 70,
      easing: 'linear'
    })
  }
  if (!props.waves) return
  for (let k = 0; k < passes; k++) {
    const side = k % 2 ? waveRight.value : waveLeft.value
    if (!side) continue
    const strength = Math.pow(1 - k / passes, props.ringDecay)
    side.animate(
      [
        { opacity: 0, transform: 'scale(0.55)' },
        { opacity: 0.9 * strength, offset: 0.3 },
        { opacity: 0, transform: 'scale(1.25)' }
      ],
      { duration: 380, delay: passOffset(k, passes) * duration, easing: 'ease-out' }
    )
  }
}

/** 外部触发一次摇铃（新通知到达时用） */
function ring() {
  swing(props.ringAmplitude, props.ringPasses, props.ringDuration)
}

/* ---------- 交互 ---------- */
function toggle() {
  if (props.disabled) return
  pending.value = lastInput.value
  const next = !on.value
  if (props.pressed === undefined || props.pressed === null) inner.value = next
  emit('update:pressed', next)
  emit('change', next)
}

function onPointerDown(e) {
  lastInput.value = 'pointer'
  if (e.button === 0 && !props.disabled) pressing.value = true
}

function release() {
  pressing.value = false
}

function onKeyDown(e) {
  if (e.key === 'Enter' || e.key === ' ') lastInput.value = 'keyboard'
}

/* ---------- 生命周期 ---------- */
watch(on, val => {
  const pointer = pending.value === 'pointer'
  pending.value = null
  animateT(val ? 1 : 0)
  if (val && pointer) ring()
})

watch(() => props.count, (val, old) => {
  if (val > old) swing(props.ringAmplitude * WOBBLE.amplitude, WOBBLE.passes, WOBBLE.duration)
})

watch(() => [props.offLabel, props.onLabel, props.size], () => nextTick(measure))

onMounted(() => {
  reduce.value = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  t.value = on.value ? 1 : 0
  nextTick(() => {
    measure()
    resizeObserver = new ResizeObserver(measure)
    if (offRef.value) resizeObserver.observe(offRef.value)
    if (onRef.value) resizeObserver.observe(onRef.value)
    document.fonts && document.fonts.ready && document.fonts.ready.then(measure)
  })
})

onBeforeUnmount(() => {
  cancelAnimationFrame(rafId)
  if (resizeObserver) resizeObserver.disconnect()
})

defineExpose({ ring })
</script>

<style lang="scss" scoped>
.bell-toggle {
  --bt-clip: 0px;
  --bt-color: #f5f5f5;
  --bt-bg: #27272a;
  --bt-on-color: #18181b;
  --bt-on-bg: #f5f5f5;
  --bt-badge: #ef4444;
  --bt-badge-ink: #ffffff;
  --bt-radius: 22px;
  --bt-fade: 200ms;
  --bt-h: 44px;
  --bt-fs: 13.5px;
  --bt-icon: 16px;
  --bt-px: 19px;
  --bt-gap: 9px;
  --bt-press: 0.97;
  --bt-ease-out: cubic-bezier(0.23, 1, 0.32, 1);

  position: relative;
  display: inline-grid;
  grid-template-columns: max-content;
  user-select: none;
  -webkit-user-select: none;
  -webkit-touch-callout: none;
  transition: transform 160ms var(--bt-ease-out);
}

.bell-toggle[data-pressed] {
  transform: scale(var(--bt-press));
}

.bell-toggle.is-disabled {
  opacity: 0.55;
}

.bell-toggle.is-disabled[data-pressed] {
  transform: none;
}

.bell-toggle__button {
  grid-area: 1 / 1;
  display: inline-flex;
  align-items: center;
  gap: var(--bt-gap);
  height: var(--bt-h);
  margin: 0;
  padding: 0 var(--bt-px);
  border: 0;
  border-radius: var(--bt-radius);
  background: var(--bt-bg);
  color: var(--bt-color);
  font-family: inherit;
  font-size: var(--bt-fs);
  font-weight: 500;
  line-height: 1;
  letter-spacing: 0.01em;
  white-space: nowrap;
  cursor: pointer;
  outline: none;
  touch-action: manipulation;
  -webkit-tap-highlight-color: transparent;
  clip-path: inset(0 var(--bt-clip) 0 0 round var(--bt-radius));
  transition:
    background-color var(--bt-fade) ease,
    color var(--bt-fade) ease;
}

.bell-toggle__button:disabled {
  cursor: default;
}

.bell-toggle__button:focus-visible {
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--bt-color) 45%, transparent);
}

.bell-toggle[data-on='true'] .bell-toggle__button {
  background: var(--bt-on-bg);
  color: var(--bt-on-color);
}

@media (hover: hover) and (pointer: fine) {
  .bell-toggle__button:enabled:hover {
    background: color-mix(in srgb, var(--bt-color) 10%, var(--bt-bg));
  }

  .bell-toggle[data-on='true'] .bell-toggle__button:enabled:hover {
    background: color-mix(in srgb, var(--bt-on-color) 12%, var(--bt-on-bg));
  }
}

.bell-toggle__bell {
  position: relative;
  display: inline-grid;
  flex: none;
  width: var(--bt-icon);
  height: var(--bt-icon);
}

.bell-toggle__glyph {
  display: inline-grid;
  place-items: center;
  grid-area: 1 / 1;
  width: var(--bt-icon);
  height: var(--bt-icon);
}

.bell-toggle__glyph :deep(svg) {
  width: 100%;
  height: 100%;
  display: block;
}

.bell-toggle__clapper {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.bell-toggle__clapper svg {
  display: block;
  width: 100%;
  height: 100%;
}

.bell-toggle__wave {
  position: absolute;
  top: -5px;
  width: 14px;
  height: 14px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.6;
  stroke-linecap: round;
  opacity: 0;
  pointer-events: none;
}

.bell-toggle__wave--left {
  right: calc(100% - 2px);
  transform-origin: 100% 100%;
}

.bell-toggle__wave--right {
  left: calc(100% - 2px);
  transform-origin: 0% 100%;
}

.bell-toggle__badge {
  position: absolute;
  top: -7px;
  right: -6px;
  display: grid;
  place-items: center;
  min-width: 14px;
  height: 14px;
  padding: 0 3px;
  box-sizing: border-box;
  border-radius: 7px;
  background: var(--bt-badge);
  color: var(--bt-badge-ink);
  font-size: 9.5px;
  font-weight: 600;
  line-height: 1;
  opacity: 0;
  transform: translateY(5px) scale(0.6);
  pointer-events: none;
  transition:
    transform 180ms var(--bt-ease-out),
    opacity 140ms ease;
}

.bell-toggle__badge[data-show] {
  opacity: 1;
  transform: translateY(0) scale(1);
  transition:
    transform 280ms cubic-bezier(0.34, 1.56, 0.64, 1),
    opacity 120ms ease;
}

.bell-toggle__digit {
  display: block;
  opacity: 1;
  transform: translateY(0);
  transition:
    opacity 200ms ease,
    transform 200ms var(--bt-ease-out);
}

@starting-style {
  .bell-toggle__digit {
    opacity: 0;
    transform: translateY(5px);
  }
}

.bell-toggle__say {
  display: inline-grid;
  grid-template-columns: max-content;
  height: 18px;
  line-height: 18px;
}

.bell-toggle__face {
  grid-area: 1 / 1;
  justify-self: start;
  opacity: 0;
  filter: blur(2px);
  transition:
    opacity var(--bt-fade) ease,
    filter var(--bt-fade) ease;
}

.bell-toggle[data-on='false'] .bell-toggle__face--off,
.bell-toggle[data-on='true'] .bell-toggle__face--on {
  opacity: 1;
  filter: blur(0);
}

@media (prefers-reduced-motion: reduce) {
  .bell-toggle[data-pressed] {
    transform: none;
  }

  .bell-toggle__face {
    filter: none !important;
    transition: opacity var(--bt-fade) ease;
  }

  .bell-toggle__badge,
  .bell-toggle__badge[data-show] {
    transition: opacity 140ms ease;
    transform: none;
  }

  .bell-toggle__digit {
    transition: opacity 200ms ease;
    transform: none;
  }
}
</style>
