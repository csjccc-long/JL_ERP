<template>
  <div ref="wrapRef" class="ridge">
    <canvas ref="cvRef" class="ridge__canvas" @pointermove="onMove" @pointerleave="onLeave" />
    <div v-if="tip.show" class="ridge__tip" :style="{ transform: 'translate(' + tip.x + 'px,' + tip.y + 'px)' }">
      <p class="ridge__tip-title">{{ tip.label }}</p>
      <p v-for="row in tip.rows" :key="row.name" class="ridge__tip-row">
        <span class="ridge__tip-dot" :style="{ background: row.color }" />
        <span class="ridge__tip-name">{{ row.name }}</span>
        <span class="ridge__tip-value">{{ row.display }}</span>
      </p>
    </div>
  </div>
</template>

<script setup name="RidgeChart">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue"

const props = defineProps({
  /** x 轴标签（日期） */
  labels: { type: Array, default: () => [] },
  /** [{ name, color, values }]，第一个序列在最前面（离视点最近） */
  series: { type: Array, default: () => [] },
  /** 标签 / 提示的数值格式化；不传用内置紧凑格式 */
  valueFormat: { type: Function, default: null },
  /** 入场动画时长（ms） */
  animationDuration: { type: Number, default: 900 }
})

const wrapRef = ref(null)
const cvRef = ref(null)
const tip = ref({ show: false, label: "", rows: [], x: 0, y: 0 })

let ctx = null
let layer = null
let lctx = null
let width = 0
let height = 0
let rafId = 0
let observer = null
let progress = 1
let hoverIndex = -1
/** 最近一次布局，供命中测试复用 */
let layout = null

const FALLBACK_COLORS = ["#00beb4", "#f7863b", "#5b8ff9", "#9270ca"]

const clamp01 = value => (value < 0 ? 0 : value > 1 ? 1 : value)
const easeOutCubic = t => 1 - Math.pow(1 - t, 3)

function hexToRgb(hex) {
  const raw = String(hex || "#00beb4").replace("#", "")
  const full = raw.length === 3 ? raw.split("").map(c => c + c).join("") : raw
  return [
    parseInt(full.slice(0, 2), 16) || 0,
    parseInt(full.slice(2, 4), 16) || 0,
    parseInt(full.slice(4, 6), 16) || 0
  ]
}

function tint(rgb, factor, alpha) {
  const a = alpha === undefined ? 1 : alpha
  const r = Math.max(0, Math.min(255, Math.round(rgb[0] * factor)))
  const g = Math.max(0, Math.min(255, Math.round(rgb[1] * factor)))
  const b = Math.max(0, Math.min(255, Math.round(rgb[2] * factor)))
  return "rgba(" + r + "," + g + "," + b + "," + a + ")"
}

/** 轴向数值：1.2 万 / 860 */
function compact(value) {
  const abs = Math.abs(value)
  if (abs >= 10000) {
    const n = value / 10000
    return (Math.abs(n) >= 10 ? n.toFixed(0) : n.toFixed(1).replace(/\.0$/, "")) + " 万"
  }
  if (abs >= 1000) return (value / 1000).toFixed(1).replace(/\.0$/, "") + "k"
  return String(Math.round(value))
}

function formatValue(value) {
  if (props.valueFormat) return props.valueFormat(value)
  return Number(value).toLocaleString("zh-CN")
}

/** 轻度高斯平滑：单日尖峰也能长成有坡度的山，而不是一根针；峰值保持不变 */
function soften(list) {
  const n = list.length
  if (n < 3) return list
  const sigma = 0.58
  const radius = 2
  const out = new Array(n)
  for (let i = 0; i < n; i++) {
    let sum = 0
    let weight = 0
    for (let k = -radius; k <= radius; k++) {
      const j = i + k
      const v = j < 0 ? list[0] : j >= n ? list[n - 1] : list[j]
      const w = Math.exp(-(k * k) / (2 * sigma * sigma))
      sum += v * w
      weight += w
    }
    out[i] = sum / weight
  }
  const before = Math.max(...list)
  const after = Math.max(...out)
  if (before > 0 && after > 0) {
    const keep = before / after
    for (let i = 0; i < n; i++) out[i] *= keep
  }
  return out
}

/** Catmull-Rom 重采样：把折线揉成山脊曲线（值不小于 0） */
function resample(values, segments) {
  const list = soften((values || []).map(v => Math.max(0, Number(v) || 0)))
  const n = list.length
  if (!n) return []
  if (n === 1) {
    const out = []
    const steps = 26
    for (let i = 0; i <= steps; i++) {
      const t = i / steps
      out.push({ x: t, v: list[0] * Math.exp(-Math.pow(t - 0.5, 2) / 0.011) })
    }
    return out
  }
  const out = []
  const catmull = (p0, p1, p2, p3, t) => {
    const t2 = t * t
    const t3 = t2 * t
    return 0.5 * (2 * p1 + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3)
  }
  for (let i = 0; i < n - 1; i++) {
    const p0 = list[Math.max(0, i - 1)]
    const p1 = list[i]
    const p2 = list[i + 1]
    const p3 = list[Math.min(n - 1, i + 2)]
    for (let s = 0; s < segments; s++) {
      const t = s / segments
      out.push({ x: (i + t) / (n - 1), v: Math.max(0, catmull(p0, p1, p2, p3, t)) })
    }
  }
  out.push({ x: 1, v: list[n - 1] })
  return out
}

/** 山体沿深度方向的剖面：前坡陡、脊线圆、后坡长（山脊靠近观察者一侧） */
const PEAK_AT = 0.5
const profile = t => {
  const half = t <= PEAK_AT ? t / PEAK_AT : (1 - t) / (1 - PEAK_AT)
  return Math.sin(Math.max(0, Math.min(1, half)) * Math.PI * 0.5)
}

const model = computed(() => {
  const labels = props.labels || []
  const series = (props.series || []).filter(item => item && Array.isArray(item.values))
  const max = Math.max(1, ...series.map(item => Math.max(0, ...item.values.map(v => Number(v) || 0))))
  const segments = labels.length <= 10 ? 8 : labels.length <= 24 ? 5 : 3
  const points = series.map(item => resample(item.values, segments))
  return { labels, series, max, points }
})

function computeLayout(w, h) {
  const padL = 48
  const padR = 34
  const padT = 22
  const padB = 28
  const cw = Math.max(20, w - padL - padR)
  const ch = Math.max(20, h - padT - padB)
  const depthDX = Math.min(cw * 0.3, Math.max(56, cw * 0.24))
  const spanX = cw - depthDX
  const depthDY = Math.min(ch * 0.4, Math.max(46, ch * 0.34))
  /** 窄容器里压低山体，免得变成一根针 */
  const aspect = Math.min(1, Math.max(0.7, cw / 520))
  const spanZ = (ch - depthDY - 8) * aspect
  const baseY = padT + ch
  return { padL, padR, padT, padB, cw, ch, depthDX, spanX, depthDY, spanZ, baseY }
}

function drawScene(target, t) {
  const { labels, series, max, points } = model.value
  const w = width
  const h = height
  if (!w || !h) return
  const L = computeLayout(w, h)
  layout = L
  const { padL, padT, spanX, depthDX, depthDY, spanZ, baseY } = L
  const c = target
  c.clearRect(0, 0, w, h)
  if (!labels.length || !series.length) return

  const xAt = x => padL + x * spanX
  const sx = (x, d) => padL + x * spanX + d * depthDX
  const sy = (z, d) => baseY - z * spanZ - d * depthDY

  // 地面：一块向后收的平行四边形
  c.beginPath()
  c.moveTo(sx(0, 0), sy(0, 0))
  c.lineTo(sx(1, 0), sy(0, 0))
  c.lineTo(sx(1, 1), sy(0, 1))
  c.lineTo(sx(0, 1), sy(0, 1))
  c.closePath()
  const ground = c.createLinearGradient(sx(0, 0), sy(0, 0), sx(0, 1), sy(0, 1))
  ground.addColorStop(0, "rgba(0,190,180,0.04)")
  ground.addColorStop(1, "rgba(0,190,180,0.008)")
  c.fillStyle = ground
  c.fill()
  c.strokeStyle = "rgba(22,22,24,0.07)"
  c.lineWidth = 1
  c.stroke()

  // 前立面的横向刻度（0 / 25 / 50 / 75 / 100%）
  c.setLineDash([3, 4])
  c.lineWidth = 1
  c.font = "11px -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif"
  c.textAlign = "right"
  c.textBaseline = "middle"
  for (let k = 0; k <= 4; k++) {
    const ratio = k / 4
    const y = sy(ratio, 0)
    c.strokeStyle = k === 0 ? "rgba(22,22,24,0.14)" : "rgba(22,22,24,0.06)"
    c.beginPath()
    c.moveTo(sx(0, 0), y)
    c.lineTo(sx(1, 0), y)
    c.stroke()
    c.fillStyle = "#9a9da1"
    c.fillText(compact((max * ratio) | 0), padL - 10, y)
  }
  c.setLineDash([])

  // 序列按体量从后往前画：体量大的在后面，形成层叠的山脉
  const order = series
    .map((item, index) => ({ index, peak: Math.max(0, ...item.values.map(v => Number(v) || 0)) }))
    .sort((a, b) => b.peak - a.peak)

  const count = order.length
  const span = count === 1 ? 0.96 : 0.44
  const step = count === 1 ? 0 : (0.96 - span) / (count - 1)
  const bands = count > 1 ? 26 : 40

  /** 由远及近绘制：rank 越大越靠后，先画 */
  const layered = order.map((entry, rank) => ({ entry, rank })).sort((a, b) => b.rank - a.rank)

  layered.forEach(({ entry, rank }) => {
    const item = series[entry.index]
    const pts = points[entry.index]
    if (!pts.length) return
    const rgb = hexToRgb(item.color || FALLBACK_COLORS[entry.index % FALLBACK_COLORS.length])
    const d0 = rank * step
    const d1 = d0 + span
    const crestDepth = (d0 + d1) / 2
    const values = pts.map(p => (p.v / max) * progress)
    /** 沿纵深方向的剖面：中脊最高、前后山脚归零，山体才有迎光的坡面而不是一片平板 */
    const bellAt = (t) => Math.pow(Math.cos((t + t - 1) * (Math.PI / 2)), 1.9)

    // 山脚落地投影：一圈模糊的暗影，让山体"站"在地面上而不是浮着
    let peakIndex = 0
    for (let i = 1; i < values.length; i++) if (values[i] > values[peakIndex]) peakIndex = i
    const shadowX = sx(pts[peakIndex].x, crestDepth)
    const shadowY = sy(0, crestDepth) + 2
    const shadowRX = Math.max(18, span * depthDX * 0.62)
    const shadowRY = Math.max(5, span * depthDY * 0.4)
    c.save()
    c.filter = "blur(6px)"
    const shadowGrad = c.createRadialGradient(shadowX, shadowY, 0, shadowX, shadowY, shadowRX)
    shadowGrad.addColorStop(0, "rgba(6, 62, 60, 0.20)")
    shadowGrad.addColorStop(1, "rgba(6, 62, 60, 0)")
    c.fillStyle = shadowGrad
    c.beginPath()
    c.ellipse(shadowX, shadowY, shadowRX, shadowRY, 0, 0, Math.PI * 2)
    c.fill()
    c.restore()

    for (let j = bands - 1; j >= 0; j--) {
      const t0 = j / bands
      const t1 = (j + 1) / bands
      /** 纵深占地随高度收放：低值区收成一条脊线，山脚不会在地面上摊成一整片色块 */
      const footAt = (i) => Math.pow(Math.max(0, Math.min(1, values[i] || 0)), 0.5)
      const b0 = bellAt(t0)
      const b1 = bellAt(t1)
      for (let i = 0; i < pts.length - 1; i++) {
        const z1 = values[i]
        const z2 = values[i + 1]
        const x1 = pts[i].x
        const x2 = pts[i + 1].x
        const zA = z1 * b0
        const zB = z2 * b0
        const zC = z2 * b1
        const zD = z1 * b1
        if (Math.max(zA, zB, zC, zD) < 0.03) continue
        /** 用面法线做明暗：光照从左上前方来，脊线最亮、前坡次之、后坡压暗 */
        const nx = -(((zB + zC) / 2 - (zA + zD) / 2) / Math.max(0.001, (x2 - x1))) * (spanZ / spanX)
        const nz = -(((zC + zD) / 2 - (zA + zB) / 2) / Math.max(0.0001, t1 - t0)) * (spanZ / Math.max(1, depthDY))
        const len = Math.hypot(nx, 1, nz) || 1
        const dot = Math.max(0, (-0.45 * nx + 0.8 - 0.4 * nz) / len)
        const factor = 0.58 + 0.72 * dot
        const dA = crestDepth + (t0 - 0.5) * span * footAt(i)
        const dB = crestDepth + (t0 - 0.5) * span * footAt(i + 1)
        const dC = crestDepth + (t1 - 0.5) * span * footAt(i + 1)
        const dD = crestDepth + (t1 - 0.5) * span * footAt(i)
        c.beginPath()
        c.moveTo(sx(x1, dA), sy(zA, dA))
        c.lineTo(sx(x2, dB), sy(zB, dB))
        c.lineTo(sx(x2, dC), sy(zC, dC))
        c.lineTo(sx(x1, dD), sy(zD, dD))
        c.closePath()
        const fill = tint(rgb, factor)
        c.fillStyle = fill
        c.fill()
        c.strokeStyle = fill
        c.lineWidth = 1
        c.lineJoin = "round"
        c.stroke()
      }
    }

    // 脊线：一条被光打亮的细边，山形由此立起来
    c.beginPath()
    let started = false
    pts.forEach((point, i) => {
      if (values[i] < 0.006) {
        started = false
        return
      }
      const px = sx(point.x, crestDepth)
      const py = sy(values[i] * profile(PEAK_AT), crestDepth)
      if (!started) {
        c.moveTo(px, py)
        started = true
      } else {
        c.lineTo(px, py)
      }
    })
    c.strokeStyle = tint(rgb, 1.3, 0.62)
    c.lineWidth = 1.2
    c.lineJoin = "round"
    c.stroke()
  })

  // x 轴标签
  c.textAlign = "center"
  c.textBaseline = "top"
  c.fillStyle = "#9a9da1"
  let lastLabelX = -Infinity
  labels.forEach((label, i) => {
    const x = labels.length === 1 ? xAt(0.5) : xAt(i / (labels.length - 1))
    if (x - lastLabelX < 52) return
    if (x > padL + spanX + 6) return
    lastLabelX = x
    c.fillText(String(label), x, baseY + 9)
  })
}

function drawHover() {
  if (!ctx || !layout) return
  const { padL, spanX, depthDX, depthDY, spanZ, baseY } = layout
  ctx.clearRect(0, 0, width, height)
  if (layer) ctx.drawImage(layer, 0, 0, width, height)
  if (hoverIndex < 0) return
  const { labels, series, max, points } = model.value
  if (!series.length || !points.length) return
  const index = Math.min(labels.length - 1, Math.max(0, hoverIndex))
  const count = series.length
  const span = count === 1 ? 0.96 : 0.44
  const step = count === 1 ? 0 : (0.96 - span) / (count - 1)
  const sorted = series
    .map((item, i) => ({ i, peak: Math.max(0, ...item.values.map(v => Number(v) || 0)) }))
    .sort((a, b) => b.peak - a.peak)
  sorted.forEach((entry, rank) => {
    const item = series[entry.i]
    const pts = points[entry.i]
    if (!pts.length) return
    const d = rank * step + span / 2
    const anchor = index === 0 ? pts[0] : pts[Math.min(pts.length - 1, Math.round((index / Math.max(1, labels.length - 1)) * (pts.length - 1)))]
    const px = padL + anchor.x * spanX + d * depthDX
    const py = baseY - (anchor.v / max) * spanZ - d * depthDY
    ctx.beginPath()
    ctx.arc(px, py, 3.6, 0, Math.PI * 2)
    ctx.fillStyle = item.color || FALLBACK_COLORS[entry.i % FALLBACK_COLORS.length]
    ctx.fill()
    ctx.lineWidth = 1.6
    ctx.strokeStyle = "#fff"
    ctx.stroke()
  })
}

function paint(t) {
  if (!ctx) return
  drawScene(ctx, t)
}

function snapshot() {
  if (!layer || !ctx) return
  lctx.clearRect(0, 0, width, height)
  lctx.drawImage(ctx.canvas, 0, 0, width, height)
}

function animate() {
  if (rafId) cancelAnimationFrame(rafId)
  const duration = Math.max(240, props.animationDuration)
  const start = performance.now()
  const tick = now => {
    const p = clamp01((now - start) / duration)
    progress = easeOutCubic(p)
    paint(progress)
    if (p < 1) {
      rafId = requestAnimationFrame(tick)
    } else {
      rafId = 0
      snapshot()
      if (hoverIndex >= 0) drawHover()
    }
  }
  rafId = requestAnimationFrame(tick)
}

function resize() {
  const wrap = wrapRef.value
  const canvas = cvRef.value
  if (!wrap || !canvas) return
  const rect = wrap.getBoundingClientRect()
  const w = Math.max(1, Math.round(rect.width))
  const h = Math.max(1, Math.round(rect.height))
  const dpr = Math.min(2, window.devicePixelRatio || 1)
  if (w === width && h === height && canvas.width === Math.round(w * dpr)) {
    paint(progress)
    snapshot()
    return
  }
  width = w
  height = h
  canvas.width = Math.round(w * dpr)
  canvas.height = Math.round(h * dpr)
  canvas.style.width = w + "px"
  canvas.style.height = h + "px"
  ctx = canvas.getContext("2d")
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  if (!layer) {
    layer = document.createElement("canvas")
    lctx = layer.getContext("2d")
  }
  layer.width = canvas.width
  layer.height = canvas.height
  lctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  paint(progress)
  snapshot()
}

function onMove(event) {
  const canvas = cvRef.value
  if (!canvas || !layout) return
  const rect = canvas.getBoundingClientRect()
  const mx = event.clientX - rect.left
  const my = event.clientY - rect.top
  const { labels, series, max, points } = model.value
  if (!labels.length || !series.length) return
  const ratio = (mx - layout.padL) / Math.max(1, layout.spanX)
  const index = Math.round(ratio * Math.max(0, labels.length - 1))
  const next = Math.min(labels.length - 1, Math.max(0, index))
  if (next !== hoverIndex) {
    hoverIndex = next
    drawHover()
  }
  const rows = series.map((item, i) => ({
    name: item.name,
    color: item.color || FALLBACK_COLORS[i % FALLBACK_COLORS.length],
    display: formatValue(Number(item.values[next]) || 0)
  }))
  const px = Math.min(width - 96, Math.max(8, mx + 14))
  const py = Math.min(height - 46, Math.max(4, my - 44))
  tip.value = { show: true, label: String(labels[next]), rows, x: px, y: py }
}

function onLeave() {
  hoverIndex = -1
  tip.value = { ...tip.value, show: false }
  drawHover()
}

onMounted(() => {
  resize()
  observer = new ResizeObserver(() => resize())
  if (wrapRef.value) observer.observe(wrapRef.value)
  animate()
})

onBeforeUnmount(() => {
  if (rafId) cancelAnimationFrame(rafId)
  if (observer) observer.disconnect()
})

watch(
  () => [props.labels, props.series],
  () => {
    hoverIndex = -1
    tip.value = { ...tip.value, show: false }
    animate()
  },
  { deep: true }
)
</script>

<style scoped>
.ridge {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 120px;
}

.ridge__canvas {
  display: block;
  width: 100%;
  height: 100%;
}

.ridge__tip {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 4;
  min-width: 118px;
  padding: 8px 10px;
  border-radius: 10px;
  background: rgba(22, 22, 24, 0.92);
  color: #f4f4f5;
  font-size: 12px;
  line-height: 1.5;
  pointer-events: none;
  box-shadow: 0 8px 22px rgba(16, 24, 32, 0.18);
}

.ridge__tip-title {
  margin: 0 0 4px;
  color: rgba(244, 244, 245, 0.66);
  font-size: 11px;
}

.ridge__tip-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
}

.ridge__tip-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex: none;
}

.ridge__tip-name {
  color: rgba(244, 244, 245, 0.78);
}

.ridge__tip-value {
  margin-left: auto;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
</style>
