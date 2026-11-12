<template>
  <div ref="rootRef" class="cobe-globe">
    <canvas
      ref="canvasRef"
      class="cobe-globe__canvas"
      @pointerdown="handlePointerDown"
      @pointermove="handleHover"
      @pointerleave="clearHover"
    />
    <template v-if="showLabels">
      <div
        v-for="marker in markers"
        v-show="marker.label"
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

const emit = defineEmits(["ready", "marker-click", "marker-hover"]);

const rootRef = ref(null);
const canvasRef = ref(null);
const showLabels = ref(true);
const hoveredId = ref("");

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
const pressPoint = { x: 0, y: 0, t: 0, moved: false };
/** 当前帧实际使用的旋转角，用于把标记点投影回屏幕坐标 */
const rotation = { phi: props.phi, theta: props.theta };

const THETA_MIN = -0.4;
const THETA_MAX = 0.4;

function cobeMarkers() {
  return props.markers.map((marker) => ({
    location: marker.location,
    size: (marker.size ?? props.markerSize) * (hoveredId.value === marker.id && !pointer.active ? 1.4 : 1),
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
  const nextPhi = phi + offsets.phi + dragOffset.phi;
  const nextTheta = props.theta + offsets.theta + dragOffset.theta;
  rotation.phi = nextPhi;
  rotation.theta = nextTheta;
  globe.update({
    phi: nextPhi,
    theta: nextTheta,
    markers: cobeMarkers(),
    arcs: cobeArcs()
  });
  frameId = requestAnimationFrame(animate);
}

/**
 * 把 [lat, lng] 投影成 canvas 上的像素坐标。
 * 与 cobe 内部（O/W 函数）保持同一套旋转与正交投影，点选标记时才能对得上。
 */
function projectMarker(marker, width, height) {
  const [lat, lng] = marker.location;
  const latRad = (lat * Math.PI) / 180;
  const lngRad = (lng * Math.PI) / 180 - Math.PI;
  const radius = 0.8 + props.markerElevation;
  const x = -Math.cos(latRad) * Math.cos(lngRad) * radius;
  const y = Math.sin(latRad) * radius;
  const z = Math.cos(latRad) * Math.sin(lngRad) * radius;

  const cosTheta = Math.cos(rotation.theta);
  const sinTheta = Math.sin(rotation.theta);
  const cosPhi = Math.cos(rotation.phi);
  const sinPhi = Math.sin(rotation.phi);

  const px = cosPhi * x + sinPhi * z;
  const py = sinPhi * sinTheta * x + cosTheta * y - cosPhi * sinTheta * z;
  const depth = -sinPhi * cosTheta * x + sinTheta * y + cosPhi * cosTheta * z;

  return {
    x: ((px + 1) / 2) * width,
    y: ((1 - py) / 2) * height,
    visible: depth >= 0 || px * px + py * py >= 0.64
  };
}

/** 找到鼠标下最近的可见标记（点太小，给足 16px 的命中半径） */
function hitTest(clientX, clientY) {
  const canvas = canvasRef.value;
  if (!canvas || !props.markers.length) return null;
  const rect = canvas.getBoundingClientRect();
  if (!rect.width) return null;

  const x = clientX - rect.left;
  const y = clientY - rect.top;
  let best = null;
  let bestDistance = Infinity;

  for (const marker of props.markers) {
    const point = projectMarker(marker, rect.width, rect.height);
    if (!point.visible) continue;
    const radius = Math.max(16, (marker.size ?? props.markerSize) * rect.width + 10);
    const distance = Math.hypot(point.x - x, point.y - y);
    if (distance <= radius && distance < bestDistance) {
      bestDistance = distance;
      best = marker;
    }
  }
  return best;
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
  pressPoint.x = event.clientX;
  pressPoint.y = event.clientY;
  pressPoint.t = Date.now();
  pressPoint.moved = false;
  paused = true;
  if (canvasRef.value) canvasRef.value.style.cursor = "grabbing";
}

/** 悬停在标记上时把光标变成手型，并让该点轻微放大 */
function handleHover(event) {
  if (pointer.active) return;
  const hit = hitTest(event.clientX, event.clientY);
  const nextId = hit ? hit.id : "";
  if (nextId !== hoveredId.value) {
    hoveredId.value = nextId;
    emit("marker-hover", hit || null);
  }
  if (canvasRef.value) canvasRef.value.style.cursor = hit ? "pointer" : "grab";
}

function clearHover() {
  if (hoveredId.value) emit("marker-hover", null);
  hoveredId.value = "";
  if (canvasRef.value) canvasRef.value.style.cursor = "grab";
}

function handlePointerMove(event) {
  if (!pointer.active) return;
  const deltaX = event.clientX - pointer.x;
  const deltaY = event.clientY - pointer.y;
  if (
    !pressPoint.moved &&
    (Math.abs(event.clientX - pressPoint.x) > 4 || Math.abs(event.clientY - pressPoint.y) > 4)
  ) {
    pressPoint.moved = true;
  }
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

function handlePointerUp(event) {
  if (pointer.active) {
    offsets.phi += dragOffset.phi;
    offsets.theta += dragOffset.theta;
    dragOffset.phi = 0;
    dragOffset.theta = 0;
    lastPointer.t = 0;
  }

  const wasClick = !pressPoint.moved && Date.now() - pressPoint.t < 800;
  pointer.active = false;
  paused = false;
  if (canvasRef.value) canvasRef.value.style.cursor = "grab";

  if (!wasClick) return;
  const hit = hitTest(event.clientX, event.clientY);
  if (hit) emit("marker-click", hit);
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
