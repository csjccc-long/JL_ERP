<template>
  <div ref="rootRef" class="cobe-globe">
    <canvas
      ref="canvasRef"
      class="cobe-globe__canvas"
      @pointerdown="handlePointerDown"
    />
    <template v-if="showLabels">
      <div
        v-for="marker in markers"
        :key="marker.id"
        class="cobe-globe__label cobe-globe__label--marker"
        :style="markerLabelStyle(marker)"
      >
        {{ marker.label }}
        <span class="cobe-globe__arrow" />
      </div>
      <div
        v-for="arc in labelledArcs"
        :key="arc.id"
        class="cobe-globe__label cobe-globe__label--arc"
        :style="arcLabelStyle(arc)"
      >
        {{ arc.label }}
        <span class="cobe-globe__arrow cobe-globe__arrow--light" />
      </div>
    </template>
  </div>
</template>

<script setup name="CobeGlobe">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import createGlobe from "cobe";

const props = defineProps({
  /** [{ id, location: [lat, lng], label, size? }] */
  markers: { type: Array, default: () => [] },
  /** [{ id, from: [lat, lng], to: [lat, lng], label? }] */
  arcs: { type: Array, default: () => [] },
  labelOnMarkers: { type: Boolean, default: true },
  markerColor: { type: Array, default: () => [0.3, 0.45, 0.85] },
  baseColor: { type: Array, default: () => [1, 1, 1] },
  arcColor: { type: Array, default: () => [0.3, 0.45, 0.85] },
  glowColor: { type: Array, default: () => [0.94, 0.93, 0.91] },
  dark: { type: Number, default: 0 },
  mapBrightness: { type: Number, default: 10 },
  markerSize: { type: Number, default: 0.025 },
  markerElevation: { type: Number, default: 0.01 },
  arcWidth: { type: Number, default: 0.5 },
  arcHeight: { type: Number, default: 0.25 },
  speed: { type: Number, default: 0.003 },
  theta: { type: Number, default: 0.2 },
  phi: { type: Number, default: 0 },
  diffuse: { type: Number, default: 1.5 },
  mapSamples: { type: Number, default: 16000 },
  opacity: { type: Number, default: 0.7 }
});

const emit = defineEmits(["ready"]);

const rootRef = ref(null);
const canvasRef = ref(null);
const showLabels = ref(true);

const labelledArcs = computed(() => props.arcs.filter((arc) => arc.label));

let globe = null;
let frameId = 0;
let resizeObserver = null;
let phi = props.phi;
let paused = false;

const dragOffset = { phi: 0, theta: 0 };
const velocity = { phi: 0, theta: 0 };
const offsets = { phi: 0, theta: 0 };
const pointer = { x: 0, y: 0, active: false };
const lastPointer = { x: 0, y: 0, t: 0 };

const THETA_MIN = -0.4;
const THETA_MAX = 0.4;

function cobeMarkers() {
  return props.markers.map((marker) => ({
    location: marker.location,
    size: marker.size ?? props.markerSize,
    id: marker.id
  }));
}

function cobeArcs() {
  return props.arcs.map((arc) => ({ from: arc.from, to: arc.to, id: arc.id }));
}

function syncOptions() {
  if (!globe) return;
  globe.update({
    dark: props.dark,
    mapBrightness: props.mapBrightness,
    markerColor: props.markerColor,
    baseColor: props.baseColor,
    arcColor: props.arcColor,
    markerElevation: props.markerElevation,
    arcWidth: props.arcWidth,
    arcHeight: props.arcHeight
  });
}

function animate() {
  if (!globe) return;
  if (!paused) {
    phi += props.speed;
    if (Math.abs(velocity.phi) > 0.0001 || Math.abs(velocity.theta) > 0.0001) {
      offsets.phi += velocity.phi;
      offsets.theta += velocity.theta;
      velocity.phi *= 0.95;
      velocity.theta *= 0.95;
    }
    if (offsets.theta < THETA_MIN) {
      offsets.theta += (THETA_MIN - offsets.theta) * 0.1;
    } else if (offsets.theta > THETA_MAX) {
      offsets.theta += (THETA_MAX - offsets.theta) * 0.1;
    }
  }
  globe.update({
    phi: phi + offsets.phi + dragOffset.phi,
    theta: props.theta + offsets.theta + dragOffset.theta,
    markers: cobeMarkers(),
    arcs: cobeArcs()
  });
  frameId = requestAnimationFrame(animate);
}

function init() {
  const canvas = canvasRef.value;
  if (!canvas || globe) return;
  const width = canvas.offsetWidth;
  if (!width) return;

  const dpr = Math.min(window.devicePixelRatio || 1, 2);
  globe = createGlobe(canvas, {
    devicePixelRatio: dpr,
    width,
    height: width,
    phi,
    theta: props.theta,
    dark: props.dark,
    diffuse: props.diffuse,
    mapSamples: props.mapSamples,
    mapBrightness: props.mapBrightness,
    baseColor: props.baseColor,
    markerColor: props.markerColor,
    glowColor: props.glowColor,
    markerElevation: props.markerElevation,
    markers: cobeMarkers(),
    arcs: cobeArcs(),
    arcColor: props.arcColor,
    arcWidth: props.arcWidth,
    arcHeight: props.arcHeight,
    opacity: props.opacity
  });

  animate();
  requestAnimationFrame(() => {
    if (canvas) canvas.style.opacity = "1";
  });
  emit("ready");
}

function handlePointerDown(event) {
  pointer.active = true;
  pointer.x = event.clientX;
  pointer.y = event.clientY;
  paused = true;
  if (canvasRef.value) canvasRef.value.style.cursor = "grabbing";
}

function handlePointerMove(event) {
  if (!pointer.active) return;
  const deltaX = event.clientX - pointer.x;
  const deltaY = event.clientY - pointer.y;
  dragOffset.phi = deltaX / 300;
  dragOffset.theta = deltaY / 1000;

  const now = Date.now();
  if (lastPointer.t) {
    const dt = Math.max(now - lastPointer.t, 1);
    const max = 0.15;
    velocity.phi = Math.max(-max, Math.min(max, ((event.clientX - lastPointer.x) / dt) * 0.3));
    velocity.theta = Math.max(-max, Math.min(max, ((event.clientY - lastPointer.y) / dt) * 0.08));
  }
  lastPointer.x = event.clientX;
  lastPointer.y = event.clientY;
  lastPointer.t = now;
}

function handlePointerUp() {
  if (pointer.active) {
    offsets.phi += dragOffset.phi;
    offsets.theta += dragOffset.theta;
    dragOffset.phi = 0;
    dragOffset.theta = 0;
    lastPointer.t = 0;
  }
  pointer.active = false;
  paused = false;
  if (canvasRef.value) canvasRef.value.style.cursor = "grab";
}

function markerLabelStyle(marker) {
  return labelStyle(`--cobe-${marker.id}`, `--cobe-visible-${marker.id}`);
}

function arcLabelStyle(arc) {
  return labelStyle(`--cobe-arc-${arc.id}`, `--cobe-visible-arc-${arc.id}`);
}

function labelStyle(anchorName, visibleVar) {
  return {
    position: "absolute",
    "position-anchor": anchorName,
    bottom: "anchor(top)",
    left: "anchor(center)",
    translate: "-50% 0",
    marginBottom: "10px",
    opacity: `var(${visibleVar}, 0)`,
    filter: `blur(calc((1 - var(${visibleVar}, 0)) * 8px))`,
    transition: "opacity 0.8s ease, filter 0.8s ease"
  };
}

watch(
  () => [props.markerColor, props.baseColor, props.arcColor, props.dark, props.mapBrightness, props.markerElevation, props.arcWidth, props.arcHeight],
  syncOptions,
  { deep: true }
);

onMounted(() => {
  window.addEventListener("pointermove", handlePointerMove, { passive: true });
  window.addEventListener("pointerup", handlePointerUp, { passive: true });

  if (canvasRef.value && canvasRef.value.offsetWidth > 0) {
    init();
  } else {
    resizeObserver = new ResizeObserver((entries) => {
      if (entries[0]?.contentRect.width > 0) {
        resizeObserver.disconnect();
        resizeObserver = null;
        init();
      }
    });
    if (rootRef.value) resizeObserver.observe(rootRef.value);
  }
});

onBeforeUnmount(() => {
  window.removeEventListener("pointermove", handlePointerMove);
  window.removeEventListener("pointerup", handlePointerUp);
  if (frameId) cancelAnimationFrame(frameId);
  frameId = 0;
  if (resizeObserver) resizeObserver.disconnect();
  resizeObserver = null;
  if (globe) globe.destroy();
  globe = null;
});
</script>

<style scoped>
.cobe-globe {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  user-select: none;
}

.cobe-globe__canvas {
  display: block;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  opacity: 0;
  cursor: grab;
  touch-action: none;
  transition: opacity 1.2s ease;
}

.cobe-globe__label {
  padding: 3px 8px;
  border-radius: 6px;
  font-size: 11px;
  line-height: 1.5;
  font-weight: 500;
  letter-spacing: 0.02em;
  white-space: nowrap;
  pointer-events: none;
  z-index: 2;
}

.cobe-globe__label--marker {
  background: #10121a;
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(16, 18, 26, 0.22);
}

.cobe-globe__label--arc {
  background: #ffffff;
  color: #10121a;
  box-shadow: 0 2px 8px rgba(16, 18, 26, 0.12);
}

.cobe-globe__arrow {
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translate3d(-50%, -1px, 0);
  border: 5px solid transparent;
  border-top-color: #10121a;
}

.cobe-globe__arrow--light {
  border-top-color: #ffffff;
}
</style>
