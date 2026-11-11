<template>
  <div class="ig-loader" :class="{ 'is-indeterminate': progress === null || progress === undefined }">
    <div class="ig-loader__shade" :style="{ opacity: overlayOpacity }"></div>
    <canvas ref="canvasRef" class="ig-loader__canvas"></canvas>
    <div class="ig-loader__label" :class="{ 'is-inverted': textInverted }">
      <span class="ig-loader__title">{{ text }}</span>
      <span v-if="showPercent" class="ig-loader__percent">{{ percentText }}%</span>
    </div>
    <p v-if="caption" class="ig-loader__caption" :class="{ 'is-inverted': captionInverted }">{{ caption }}</p>
  </div>
</template>

<script setup name="ImageGenLoader">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";

const props = defineProps({
  /** 0~100；传 null 表示不确定进度（纯波浪循环） */
  progress: { type: Number, default: null },
  text: { type: String, default: "Loading" },
  /** 单元格边长与间距（px），与原组件一致 */
  cellSize: { type: Number, default: 3 },
  gap: { type: Number, default: 1 },
  /** 波峰高亮带厚度（px） */
  bandHeight: { type: Number, default: 48 },
  /** 波面起伏幅度（px） */
  waveAmplitude: { type: Number, default: 10 },
  /** [起始色, 结束色] */
  colors: { type: Array, default: () => ["#22d3ee", "#1e40af"] },
  /** 底图压暗程度 */
  overlayOpacity: { type: Number, default: 0.2 },
  /** 缓动曲线，用于进度过渡 */
  easing: { type: Array, default: () => [0.65, 0, 0.35, 1] },
  showPercent: { type: Boolean, default: true },
  /** 底部副标题（例如文件名） */
  caption: { type: String, default: "" }
});

const canvasRef = ref(null);
let ctx = null;
let raf = 0;
let ro = null;
let width = 0;
let height = 0;
let dpr = 1;
let startTs = 0;
/** 显示用的进度，向真实进度平滑逼近 */
let shown = 0;
let lastTs = 0;
/** 文字走 DOM 而不是 canvas，保证任何水位下笔画都干净利落 */
const percentText = ref(0);
const textInverted = ref(false);
const captionInverted = ref(false);

const targetProgress = computed(() => {
  if (props.progress === null || props.progress === undefined) return null;
  return Math.min(100, Math.max(0, props.progress));
});

function parseColor(input) {
  if (typeof input !== "string") return [34, 211, 238];
  const hex = input.trim();
  if (hex.startsWith("#")) {
    const v = hex.slice(1);
    const full = v.length === 3 ? v.split("").map((c) => c + c).join("") : v;
    return [parseInt(full.slice(0, 2), 16), parseInt(full.slice(2, 4), 16), parseInt(full.slice(4, 6), 16)];
  }
  const m = hex.match(/rgba?\(([^)]+)\)/);
  if (m) {
    const parts = m[1].split(",").map((n) => parseFloat(n));
    return [parts[0] || 0, parts[1] || 0, parts[2] || 0];
  }
  return [34, 211, 238];
}

const fromColor = computed(() => parseColor(props.colors?.[0] || "#22d3ee"));
const toColor = computed(() => parseColor(props.colors?.[1] || "#1e40af"));

function mix(a, b, t) {
  const k = Math.min(1, Math.max(0, t));
  return [
    Math.round(a[0] + (b[0] - a[0]) * k),
    Math.round(a[1] + (b[1] - a[1]) * k),
    Math.round(a[2] + (b[2] - a[2]) * k)
  ];
}

function resize() {
  const el = canvasRef.value;
  if (!el) return;
  const rect = el.getBoundingClientRect();
  if (!rect.width || !rect.height) return;
  dpr = Math.min(2, window.devicePixelRatio || 1);
  width = rect.width;
  height = rect.height;
  el.width = Math.round(width * dpr);
  el.height = Math.round(height * dpr);
  ctx = el.getContext("2d");
  if (ctx) ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
}

function draw(ts) {
  raf = requestAnimationFrame(draw);
  if (!ctx || !width || !height) return;
  if (!startTs) startTs = ts;
  const dt = lastTs ? Math.min(64, ts - lastTs) : 16;
  lastTs = ts;
  const time = (ts - startTs) / 1000;

  const target = targetProgress.value;
  if (target === null) {
    shown = 52 + Math.sin(time * 0.9) * 12;
  } else {
    // 指数逼近，让 axios 的忽大忽小回调看起来顺滑
    const k = 1 - Math.pow(0.0015, dt / 1000);
    shown += (target - shown) * k;
    if (Math.abs(target - shown) < 0.15) shown = target;
  }

  ctx.clearRect(0, 0, width, height);

  const pitch = Math.max(2, props.cellSize + props.gap);
  const size = props.cellSize;
  const cols = Math.ceil(width / pitch);
  const rows = Math.ceil(height / pitch);
  const amp = props.waveAmplitude;
  const baseY = height * (1 - shown / 100);
  const a = fromColor.value;
  const b = toColor.value;
  const wavelength = Math.max(width * 0.55, 220);

  for (let row = 0; row < rows; row++) {
    const cy = row * pitch + size / 2;
    for (let col = 0; col < cols; col++) {
      const cx = col * pitch + size / 2;
      const surface =
        baseY +
        Math.sin((cx / wavelength) * Math.PI * 2 - time * 1.6) * amp +
        Math.sin((cx / wavelength) * Math.PI * 2 * 0.5 + time * 0.9) * amp * 0.35;
      const d = cy - surface;
      const x = col * pitch;

      if (d >= 0) {
        const depth = Math.min(1, d / Math.max(1, height * 0.6));
        const crest = Math.max(0, 1 - d / Math.max(1, props.bandHeight));
        const color = mix(a, b, 0.1 + (col / Math.max(1, cols - 1)) * 0.72 + depth * 0.25 - crest * 0.12);
        const light = 0.94 - depth * 0.18 + crest * 0.12;
        ctx.fillStyle = `rgba(${color[0]}, ${color[1]}, ${color[2]}, ${light.toFixed(3)})`;
        ctx.fillRect(x, row * pitch, size, size);
      } else {
        const fade = Math.max(0, 1 - Math.abs(d) / 90);
        ctx.fillStyle = `rgba(15, 42, 54, ${(0.05 + fade * 0.04).toFixed(3)})`;
        ctx.fillRect(x, row * pitch, size, size);
      }
    }
  }

  // 文字水线位置交给 DOM 层：水位盖过文字中线时翻成白色（带阴影），否则深墨色
  const waveTop = baseY - amp;
  const textY = height / 2 - (props.showPercent ? 10 : 0);
  const nextPercent = Math.round(shown);
  if (nextPercent !== percentText.value) percentText.value = nextPercent;
  const inverted = textY + 6 > waveTop + 4;
  if (inverted !== textInverted.value) textInverted.value = inverted;
  const capInvertedNow = height - 20 > waveTop + 4;
  if (capInvertedNow !== captionInverted.value) captionInverted.value = capInvertedNow;
}

watch(
  () => [props.cellSize, props.gap],
  () => resize()
);

onMounted(() => {
  resize();
  ro = new ResizeObserver(() => resize());
  if (canvasRef.value) ro.observe(canvasRef.value);
  raf = requestAnimationFrame(draw);
});

onBeforeUnmount(() => {
  cancelAnimationFrame(raf);
  if (ro) ro.disconnect();
});
</script>

<style scoped>
.ig-loader {
  position: absolute;
  inset: 0;
  overflow: hidden;
  border-radius: inherit;
}

.ig-loader__shade {
  position: absolute;
  inset: 0;
  background: #020617;
  pointer-events: none;
  transition: opacity 0.3s ease;
}

.ig-loader__canvas {
  position: absolute;
  inset: 0;
  display: block;
  width: 100%;
  height: 100%;
}

.ig-loader__label {
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  transform: translateY(-56%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  pointer-events: none;
  color: #0f2b3a;
  transition: color 0.18s ease, text-shadow 0.18s ease;
}

.ig-loader__label.is-inverted {
  color: #ffffff;
  text-shadow: 0 0 1px rgba(3, 20, 34, 0.65), 0 2px 10px rgba(3, 20, 34, 0.45);
}

.ig-loader__title {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.ig-loader__percent {
  font-size: 30px;
  font-weight: 700;
  line-height: 1.06;
  font-variant-numeric: tabular-nums;
}

.ig-loader__caption {
  position: absolute;
  left: 50%;
  bottom: 10px;
  transform: translateX(-50%);
  max-width: calc(100% - 24px);
  margin: 0;
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1.5;
  color: rgba(15, 43, 58, 0.7);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  pointer-events: none;
  transition: color 0.18s ease, background-color 0.18s ease;
}

.ig-loader__caption.is-inverted {
  color: rgba(255, 255, 255, 0.92);
  background: rgba(6, 28, 44, 0.34);
}
</style>
