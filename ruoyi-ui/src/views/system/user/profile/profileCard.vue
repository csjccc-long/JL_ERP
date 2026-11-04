<template>
  <div ref="rootRef" class="profile-card" :style="rootStyle">
    <div class="profile-card__body">
      <div class="profile-card__tile">
        <span class="profile-card__sheen" aria-hidden="true" />
        <span class="profile-card__holo" aria-hidden="true">
          <span class="profile-card__holo-glow" />
          <span class="profile-card__holo-shift" />
        </span>

        <div class="profile-card__stage">
          <div class="profile-card__arch">
            <slot />
          </div>
        </div>

        <span class="profile-card__scrim" aria-hidden="true" />

        <div class="profile-card__info">
          <h3 v-if="name" class="profile-card__name">{{ name }}</h3>
          <p v-if="title" class="profile-card__title">{{ title }}</p>
        </div>

        <span class="profile-card__tile-rim" aria-hidden="true" />
      </div>

      <div v-if="showUserInfo" class="profile-card__footer">
        <span v-if="miniAvatarUrl" class="profile-card__mini">
          <img :src="miniAvatarUrl" alt="" draggable="false" />
        </span>
        <div class="profile-card__meta">
          <span v-if="handle" class="profile-card__handle">@{{ handle }}</span>
          <span v-if="status" class="profile-card__status">
            <span class="profile-card__status-text">{{ status }}</span>
          </span>
        </div>
        <button v-if="contactText" type="button" class="profile-card__contact" @click="emit('contact')">
          <svg-icon icon-class="user" />
          <span>{{ contactText }}</span>
        </button>
      </div>

      <span class="profile-card__glare" aria-hidden="true" />
      <span class="profile-card__shade" aria-hidden="true" />
      <span class="profile-card__rim" aria-hidden="true" />
    </div>
  </div>
</template>

<script setup>
import logoPattern from "@/assets/logo/logo-pattern.png"

const props = defineProps({
  name: { type: String, default: "" },
  title: { type: String, default: "" },
  handle: { type: String, default: "" },
  status: { type: String, default: "" },
  statusColor: { type: String, default: "#22c55e" },
  miniAvatarUrl: { type: String, default: "" },
  contactText: { type: String, default: "" },
  showUserInfo: { type: Boolean, default: true },
  radius: { type: Number, default: 16 },
  holo: { type: Number, default: 0.8 },
  glare: { type: Number, default: 0.5 },
  enableTilt: { type: Boolean, default: true },
  tiltStrength: { type: Number, default: 12 },
  parallax: { type: Number, default: 8 },
  intro: { type: Boolean, default: true }
})

const emit = defineEmits(["contact"])

const rootRef = ref(null)
const clamp = (value, min, max) => Math.min(max, Math.max(min, value))

const GRAIN = 'url("data:image/svg+xml,' + encodeURIComponent(
  "<svg xmlns='http://www.w3.org/2000/svg' width='500' height='500'><filter id='g' x='0' y='0' width='100%' height='100%' color-interpolation-filters='sRGB'><feTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='2' stitchTiles='stitch'/><feColorMatrix values='0.55 0 0 0 -0.24 0.55 0 0 0 -0.24 0.55 0 0 0 -0.24 0 0 0 0 1'/></filter><rect width='100%' height='100%' filter='url(#g)'/></svg>"
) + '")'

const settingsRef = { value: { tilt: props.enableTilt, strength: props.tiltStrength, depth: props.parallax } }
const engineRef = { paint: () => {}, destroy: () => {} }

watch(
  () => [props.enableTilt, props.tiltStrength, props.parallax],
  () => {
    settingsRef.value = { tilt: props.enableTilt, strength: props.tiltStrength, depth: props.parallax }
    engineRef.paint()
  }
)

const rootStyle = computed(() => ({
  "--pc-radius": Math.max(0, props.radius) + "px",
  "--pc-backdrop": "var(--pc-tile)",
  "--pc-holo": clamp(props.holo, 0, 1),
  "--pc-glare": clamp(props.glare, 0, 1),
  "--pc-parallax": Math.max(0, props.parallax) + "px",
  "--pc-status": props.statusColor,
  "--pc-grain": GRAIN,
  "--pc-pattern": 'url("' + logoPattern + '")',
  "--pc-pattern-size": "180% auto",
  "--pc-holo-gain": 1.25,
  "--pc-holo-detail": 0.22
}))

onMounted(() => {
  const root = rootRef.value
  if (!root) return

  const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches
  const size = { w: root.offsetWidth || 340, h: root.offsetHeight || 460 }
  const x = { p: 0, v: 0, t: 0 }
  const y = { p: 0, v: 0, t: 0 }
  const light = { p: 0, t: 0 }
  let raf = 0
  let last = 0
  let slowUntil = 0
  let touchId = null

  const paint = () => {
    const cfg = settingsRef.value
    const angle = cfg.tilt && !reduced ? cfg.strength : 0
    const reach = Math.min(1, Math.hypot(x.p, y.p))
    const depth = reduced ? 0 : cfg.depth
    const style = root.style
    style.setProperty("--pc-light-x", ((x.p + 1) * 0.5 * size.w).toFixed(1) + "px")
    style.setProperty("--pc-light-y", ((y.p + 1) * 0.5 * size.h).toFixed(1) + "px")
    style.setProperty("--pc-pointer-x", ((x.p + 1) * 50).toFixed(2) + "%")
    style.setProperty("--pc-pointer-y", ((y.p + 1) * 50).toFixed(2) + "%")
    style.setProperty("--pc-band-x", (50 + x.p * 15).toFixed(2) + "%")
    style.setProperty("--pc-band-y", (50 + y.p * 15).toFixed(2) + "%")
    style.setProperty("--pc-rotate-x", (y.p * angle).toFixed(3) + "deg")
    style.setProperty("--pc-rotate-y", (-x.p * angle).toFixed(3) + "deg")
    style.setProperty("--pc-shift-x", (x.p * depth).toFixed(2) + "px")
    style.setProperty("--pc-shift-y", (y.p * depth).toFixed(2) + "px")
    style.setProperty("--pc-reach", reach.toFixed(4))
    style.setProperty("--pc-tilt", (reach * light.p).toFixed(4))
    style.setProperty("--pc-active", light.p.toFixed(4))
  }

  const frame = now => {
    const dt = Math.min(0.05, (now - last) / 1000 || 0)
    last = now
    const slow = now < slowUntil
    const stiffness = slow ? 24 : 120
    const damping = slow ? 9 : 16
    const steps = Math.max(1, Math.ceil(dt / (1 / 120)))
    const h = dt / steps
    for (let i = 0; i < steps; i++) {
      for (const axis of [x, y]) {
        axis.v += (stiffness * (axis.t - axis.p) - damping * axis.v) * h
        axis.p += axis.v * h
      }
    }
    const tau = light.t > light.p ? 0.18 : slow ? 0.9 : 0.45
    light.p += (light.t - light.p) * (1 - Math.exp(-dt / tau))
    paint()

    const moving =
      Math.abs(x.t - x.p) > 0.0005 ||
      Math.abs(y.t - y.p) > 0.0005 ||
      Math.abs(x.v) > 0.0005 ||
      Math.abs(y.v) > 0.0005 ||
      Math.abs(light.t - light.p) > 0.002
    if (moving) {
      raf = requestAnimationFrame(frame)
    } else {
      x.p = x.t
      y.p = y.t
      light.p = light.t
      paint()
      raf = 0
    }
  }

  const wake = () => {
    if (raf) return
    last = performance.now()
    raf = requestAnimationFrame(frame)
  }

  const aim = (nx, ny, on) => {
    x.t = clamp(nx, -1, 1)
    y.t = clamp(ny, -1, 1)
    light.t = on === false ? 0 : 1
    slowUntil = 0
    wake()
  }

  const aimAt = event => {
    const rect = root.getBoundingClientRect()
    if (!rect.width || !rect.height) return
    aim(((event.clientX - rect.left) / rect.width) * 2 - 1, ((event.clientY - rect.top) / rect.height) * 2 - 1)
  }

  const release = () => aim(0, 0, false)

  const onEnter = event => {
    if (event.pointerType === "mouse") aimAt(event)
  }
  const onMove = event => {
    if (event.pointerType === "mouse" || event.pointerId === touchId) aimAt(event)
  }
  const onLeave = event => {
    if (event.pointerType === "mouse") release()
  }
  const onDown = event => {
    if (event.pointerType === "mouse") return
    touchId = event.pointerId
    aimAt(event)
  }
  const onUp = event => {
    if (event.pointerId !== touchId) return
    touchId = null
    release()
  }

  root.addEventListener("pointerenter", onEnter)
  root.addEventListener("pointermove", onMove)
  root.addEventListener("pointerleave", onLeave)
  root.addEventListener("pointerdown", onDown)
  root.addEventListener("pointerup", onUp)
  root.addEventListener("pointercancel", onUp)

  const observer = new ResizeObserver(() => {
    size.w = root.offsetWidth
    size.h = root.offsetHeight
    paint()
  })
  observer.observe(root)

  engineRef.paint = paint
  engineRef.destroy = () => {
    cancelAnimationFrame(raf)
    observer.disconnect()
    root.removeEventListener("pointerenter", onEnter)
    root.removeEventListener("pointermove", onMove)
    root.removeEventListener("pointerleave", onLeave)
    root.removeEventListener("pointerdown", onDown)
    root.removeEventListener("pointerup", onUp)
    root.removeEventListener("pointercancel", onUp)
  }

  if (props.intro && !reduced) {
    x.p = 0.62
    y.p = -0.55
    light.p = 1
    slowUntil = performance.now() + 1400
    paint()
    wake()
  } else {
    paint()
  }
})

onBeforeUnmount(() => engineRef.destroy())
</script>

<style lang="scss" scoped>
.profile-card {
  --pc-light-x: 50%;
  --pc-light-y: 50%;
  --pc-pointer-x: 50%;
  --pc-pointer-y: 50%;
  --pc-band-x: 50%;
  --pc-band-y: 50%;
  --pc-reach: 0;
  --pc-rotate-x: 0deg;
  --pc-rotate-y: 0deg;
  --pc-shift-x: 0px;
  --pc-shift-y: 0px;
  --pc-tilt: 0;
  --pc-active: 0;
  --pc-frame: rgba(38, 35, 46, 0.66);
  --pc-frame-edge: rgba(255, 255, 255, 0.08);
  --pc-frame-highlight: rgba(255, 255, 255, 0.07);
  --pc-frame-shadow: 0 22px 44px -20px rgba(0, 0, 0, 0.8), 0 6px 16px -8px rgba(0, 0, 0, 0.5);
  --pc-tile: linear-gradient(180deg, #3a3644 0%, #27242f 100%);
  --pc-tile-edge: rgba(255, 255, 255, 0.07);
  --pc-tile-highlight: rgba(255, 255, 255, 0.12);
  --pc-tile-shadow: 0 4px 10px -4px rgba(0, 0, 0, 0.7);
  --pc-ink: #f4f4f5;
  --pc-muted: rgba(244, 244, 245, 0.6);
  --pc-hover: brightness(1.12);
  --pc-press: brightness(0.8);
  --pc-shade: rgba(0, 0, 0, 0.4);
  --pc-blend: color-dodge;
  --pc-holo-filter: hue-rotate(0deg);
  --pc-sheen: rgba(255, 255, 255, 0.16);
  position: relative;
  width: 100%;
  max-width: 340px;
  margin: 0 auto;
  container-type: inline-size;
  perspective: 1000px;
  touch-action: pan-y;
  -webkit-tap-highlight-color: transparent;
}

.profile-card__body {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 10px;
  border-radius: calc(var(--pc-radius) + 10px);
  background: var(--pc-frame);
  box-shadow: var(--pc-frame-shadow);
  -webkit-backdrop-filter: blur(20px) saturate(1.6);
  backdrop-filter: blur(20px) saturate(1.6);
  transform: rotateX(var(--pc-rotate-x)) rotateY(var(--pc-rotate-y));
  will-change: transform;
}

.profile-card__tile {
  position: relative;
  aspect-ratio: 4 / 5;
  border-radius: var(--pc-radius);
  background: var(--pc-backdrop);
  box-shadow: var(--pc-tile-shadow);
  overflow: hidden;
  isolation: isolate;
}

.profile-card__sheen,
.profile-card__holo,
.profile-card__scrim,
.profile-card__tile-rim,
.profile-card__glare,
.profile-card__shade,
.profile-card__rim {
  position: absolute;
  inset: 0;
  border-radius: inherit;
  pointer-events: none;
}

.profile-card__sheen {
  background: radial-gradient(
    farthest-corner circle at calc(var(--pc-light-x) - 10px) calc(var(--pc-light-y) - 10px),
    var(--pc-sheen) 0%,
    transparent 55%
  );
  opacity: calc(var(--pc-holo) * var(--pc-active));
}

.profile-card__holo {
  --pc-spectrum: repeating-linear-gradient(
    0deg,
    hsl(2, 100%, 73%) 5%,
    hsl(53, 100%, 69%) 10%,
    hsl(93, 100%, 69%) 15%,
    hsl(176, 100%, 76%) 20%,
    hsl(228, 100%, 74%) 25%,
    hsl(260, 100%, 76%) 30%,
    hsl(2, 100%, 73%) 35%
  );
  --pc-bars: repeating-linear-gradient(
    -45deg,
    #0e152e 0%,
    hsl(180, 10%, 60%) 3.8%,
    hsl(180, 29%, 66%) 4.5%,
    hsl(180, 10%, 60%) 5.2%,
    #0e152e 10%,
    #0e152e 12%
  );
  --pc-falloff: radial-gradient(
    farthest-corner circle at var(--pc-pointer-x) var(--pc-pointer-y),
    hsla(0, 0%, 0%, 0.1) 12%,
    hsla(0, 0%, 0%, 0.15) 20%,
    hsla(0, 0%, 0%, 0.25) 120%
  );
  background-image: var(--pc-spectrum), var(--pc-bars), var(--pc-falloff);
  background-position:
    0 var(--pc-band-y),
    var(--pc-band-x) var(--pc-band-y),
    center;
  background-size:
    500% 500%,
    300% 300%,
    200% 200%;
  background-blend-mode: color, hard-light;
  mix-blend-mode: var(--pc-blend);
  filter: brightness(calc(0.66 + 0.19 * var(--pc-active))) contrast(calc(1.33 + 0.17 * var(--pc-active)))
    saturate(calc(0.33 + 0.17 * var(--pc-active))) var(--pc-holo-filter);
  opacity: calc(var(--pc-holo) * var(--pc-holo-gain) * (0.5 + 0.5 * var(--pc-active)));
  -webkit-mask-image: var(--pc-pattern);
  -webkit-mask-size: var(--pc-pattern-size);
  -webkit-mask-position: calc(50% - var(--pc-shift-x) * 1.5) calc(65% - var(--pc-shift-y) * 1.5);
  -webkit-mask-repeat: repeat;
  mask-image: var(--pc-pattern);
  mask-size: var(--pc-pattern-size);
  mask-position: calc(50% - var(--pc-shift-x) * 1.5) calc(65% - var(--pc-shift-y) * 1.5);
  mask-repeat: repeat;
}

.profile-card__holo-glow,
.profile-card__holo-shift {
  position: absolute;
  inset: 0;
  opacity: calc(var(--pc-active) * var(--pc-holo-detail));
}

.profile-card__holo-glow {
  background-image:
    linear-gradient(
      45deg,
      hsl(176, 100%, 76%),
      hsl(228, 100%, 74%),
      hsl(260, 100%, 76%),
      hsl(2, 100%, 73%),
      hsl(53, 100%, 69%),
      hsl(93, 100%, 69%)
    ),
    radial-gradient(circle at var(--pc-pointer-x) var(--pc-pointer-y), hsl(0, 0%, 70%) 0%, hsla(0, 0%, 30%, 0.2) 90%),
    var(--pc-grain);
  background-size:
    250% 250%,
    100% 100%,
    220px 220px;
  background-position:
    var(--pc-pointer-x) var(--pc-pointer-y),
    center,
    0 0;
  background-blend-mode: color-dodge;
  filter: brightness(calc(2 - var(--pc-reach))) contrast(calc(var(--pc-reach) + 2))
    saturate(calc(0.5 + var(--pc-reach)));
  mix-blend-mode: luminosity;
}

.profile-card__holo-shift {
  background-image: var(--pc-spectrum), var(--pc-bars), var(--pc-falloff);
  background-position:
    0 var(--pc-band-y),
    calc(var(--pc-band-x) * 0.4) calc(var(--pc-band-y) * 0.5),
    center;
  background-size:
    200% 300%,
    700% 700%,
    100% 100%;
  background-blend-mode: color, hard-light;
  mix-blend-mode: difference;
  filter: brightness(0.8) contrast(1.5);
}

.profile-card__stage {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 104px;
  display: flex;
  justify-content: center;
  padding: 0 20px;
  transform: translate3d(var(--pc-shift-x), var(--pc-shift-y), 0);
  pointer-events: none;
}

.profile-card__arch {
  position: relative;
  display: flex;
  width: 70%;
  aspect-ratio: 1.85 / 1;
  border-radius: 999px 999px 0 0;
  overflow: hidden;
  background: linear-gradient(180deg, #4a4553 0%, #2b2733 100%);
  box-shadow:
    0 16px 26px -16px rgba(0, 0, 0, 0.8),
    inset 0 0 0 1px rgba(255, 255, 255, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.16);
  pointer-events: auto;
}

.profile-card__scrim {
  background: linear-gradient(
    to top,
    rgba(0, 0, 0, 0.6) 0%,
    rgba(0, 0, 0, 0.57) 6%,
    rgba(0, 0, 0, 0.5) 12%,
    rgba(0, 0, 0, 0.4) 19%,
    rgba(0, 0, 0, 0.28) 26%,
    rgba(0, 0, 0, 0.16) 33%,
    rgba(0, 0, 0, 0.07) 40%,
    rgba(0, 0, 0, 0.02) 46%,
    rgba(0, 0, 0, 0) 52%
  );
}

.profile-card__info {
  position: absolute;
  left: 20px;
  right: 20px;
  bottom: 18px;
  color: #ffffff;
}

.profile-card__name {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.25;
  letter-spacing: -0.01em;
  color: inherit;
}

.profile-card__title {
  margin: 4px 0 0;
  font-size: 14px;
  line-height: 1.45;
  color: rgba(255, 255, 255, 0.78);
}

.profile-card__tile-rim {
  box-shadow:
    inset 0 0 0 1px var(--pc-tile-edge),
    inset 0 1px 0 var(--pc-tile-highlight);
}

.profile-card__footer {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding: 6px 16px 8px;
}

.profile-card__mini {
  position: relative;
  flex: none;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--pc-tile);
  overflow: hidden;
}

.profile-card__mini::after {
  content: "";
  position: absolute;
  inset: 0;
  border-radius: inherit;
  box-shadow: inset 0 0 0 1px var(--pc-tile-edge);
}

.profile-card__mini img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center top;
}

.profile-card__meta {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.profile-card__handle {
  overflow: hidden;
  font-size: 13.5px;
  font-weight: 600;
  line-height: 1.25;
  color: var(--pc-ink);
  white-space: nowrap;
  text-overflow: ellipsis;
}

.profile-card__status {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  font-size: 12px;
  line-height: 1.25;
  color: var(--pc-muted);
  white-space: nowrap;
}

.profile-card__status-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-card__status::before {
  content: "";
  flex: none;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--pc-status);
}

.profile-card__contact {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: none;
  height: 36px;
  padding: 0 12px;
  border: none;
  border-radius: calc(var(--pc-radius) * 0.75);
  background: var(--pc-tile);
  box-shadow:
    inset 0 0 0 1px var(--pc-tile-edge),
    inset 0 1px 0 var(--pc-tile-highlight),
    var(--pc-tile-shadow);
  color: var(--pc-ink);
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition:
    filter 0.2s ease,
    transform 0.2s ease;
}

.profile-card__contact :deep(.svg-icon) {
  font-size: 16px;
}

.profile-card__contact:active {
  filter: var(--pc-press);
  transform: scale(0.96);
  transition-duration: 0s;
}

.profile-card__contact:focus-visible {
  outline: 2px solid var(--pc-ink);
  outline-offset: 2px;
}

.profile-card__glare {
  mix-blend-mode: overlay;
  background: radial-gradient(
    farthest-corner circle at var(--pc-light-x) var(--pc-light-y),
    rgba(255, 255, 255, 0.8) 0%,
    rgba(255, 255, 255, 0.36) 20%,
    rgba(255, 255, 255, 0) 60%
  );
  opacity: calc(var(--pc-glare) * var(--pc-active));
}

.profile-card__shade {
  background: radial-gradient(
    farthest-corner circle at var(--pc-light-x) var(--pc-light-y),
    transparent 30%,
    var(--pc-shade) 100%
  );
  opacity: calc(var(--pc-glare) * var(--pc-tilt));
}

.profile-card__rim {
  box-shadow:
    inset 0 0 0 1px var(--pc-frame-edge),
    inset 0 1px 0 var(--pc-frame-highlight);
}

:deep(.user-info-head) {
  position: relative;
  flex: 1;
  min-width: 0;
  border-radius: 999px 999px 0 0;
}

@container (max-width: 300px) {
  .profile-card__contact {
    gap: 4px;
    padding: 0 10px;
    font-size: 12px;
  }

  .profile-card__contact :deep(.svg-icon) {
    font-size: 14px;
  }

  .profile-card__handle {
    font-size: 12.5px;
  }

  .profile-card__status {
    font-size: 11.5px;
  }

  .profile-card__footer {
    gap: 8px;
    padding: 6px 13px 8px;
  }

  .profile-card__mini {
    width: 32px;
    height: 32px;
  }
}

@media (hover: hover) {
  .profile-card__contact:hover {
    filter: var(--pc-hover);
  }

  .profile-card__contact:hover:active {
    filter: var(--pc-press);
  }
}
</style>
