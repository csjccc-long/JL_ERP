<template>
  <div ref="stageRef" class="ck-stage">
    <canvas ref="canvasRef" class="ck-canvas" tabindex="0" role="img" :aria-label="ariaLabel" />
    <div ref="tipRef" class="ck-tip" :class="{ 'is-on': activeIndex >= 0 }" role="tooltip" :aria-hidden="activeIndex < 0">
      <template v-if="activeCell">
        <b>{{ activeCell.title }}</b>
        <span class="ck-tip__meta">{{ activeCell.meta }}</span>
      </template>
      <span v-else class="ck-tip__pad">&nbsp;</span>
      <i class="ck-tip__arrow" aria-hidden="true" />
    </div>
  </div>
</template>

<script setup name="CheckinSkyline">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  buildGrid,
  camera,
  clamp01,
  dayMs,
  easeInOutCubic,
  lerp,
  monthLabels,
  riseAt,
  smoothstep,
  barHeight,
  WAVE
} from './skyline'

const props = defineProps({
  /** [{ date, count, checked }]，只含有记录的日子 */
  days: { type: Array, default: () => [] },
  /** 网格首日（服务端对齐到周一） */
  start: { type: String, default: '' },
  /** 网格末日，一般是今天 */
  end: { type: String, default: '' },
  /** 受控视图 */
  view: { type: String, default: '3d' },
  /** 图例选中的等级，-1 表示不高亮 */
  focusLevel: { type: Number, default: -1 },
  /** 形变时长 ms */
  duration: { type: Number, default: 1250 },
  /** 3D 柱高倍数 */
  heightScale: { type: Number, default: 1 },
  /** { 'yyyy-MM-dd': 失败条数 }，命中那天画红圈 */
  marks: { type: Object, default: () => ({}) },
  /** 当前选中的日期，画一圈青色描边 */
  selected: { type: String, default: '' },
  locale: { type: String, default: 'zh-CN' }
})

const emit = defineEmits(['update:view', 'select'])

const stageRef = ref(null)
const canvasRef = ref(null)
const tipRef = ref(null)
const activeIndex = ref(-1)

const model = computed(() => buildGrid(props.days, props.start, props.end, 1))

const nf = computed(() => new Intl.NumberFormat(props.locale))
const dfy = computed(() => new Intl.DateTimeFormat(props.locale, { month: 'long', day: 'numeric', year: 'numeric', timeZone: 'UTC' }))

const activeCell = computed(() => {
  const c = model.value.cells[activeIndex.value]
  if (!c) return null
  const counts = props.days.find(d => d.date === c.date)
  const checked = !!(counts && counts.checked)
  return {
    title: dfy.value.format(dayMs(c.date)),
    meta: (checked ? '已打卡' : '未打卡') + ' · ' + nf.value.format(c.count) + ' 次动作'
  }
})

const ariaLabel = computed(() => {
  const total = props.days.reduce((sum, d) => sum + (Number(d.count) || 0), 0)
  return '最近一年打卡热力图，共 ' + nf.value.format(total) + ' 次动作，左右方向键可逐日查看'
})

const engine = ref(null)

watch(
  () => [props.days, props.start, props.end, props.heightScale],
  () => engine.value && engine.value.load(),
  { deep: true }
)
watch(
  () => props.view,
  () => engine.value && engine.value.kick()
)
watch(
  () => props.focusLevel,
  () => engine.value && engine.value.kick()
)
watch(
  () => props.marks,
  () => engine.value && engine.value.load(),
  { deep: true }
)
watch(
  () => props.selected,
  value => engine.value && engine.value.select(value)
)
watch(activeIndex, () => nextTick(() => engine.value && engine.value.tipWidth(tipRef.value ? tipRef.value.offsetWidth : 0)))

onMounted(() => {
  const stage = stageRef.value
  const canvas = canvasRef.value
  const tip = tipRef.value
  if (!stage || !canvas || !tip) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const reduceMq = window.matchMedia('(prefers-reduced-motion: reduce)')
  let reduced = reduceMq.matches

  // morph：t 是线性时间 0（平面）-> 1（天际线）
  let t = 0
  let target = props.view === '3d' ? 1 : 0
  let entered = false
  let yaw = 0
  let elev = 0
  let yawGoal = 0
  let elevGoal = 0
  // 布局
  let W = 0
  let H2 = 0
  let H3 = 0
  let Hmax = 0
  let lastH = -1
  let dpr = 1
  let gutter = 30
  let labelW = 30
  let font = '10px sans-serif'
  // 颜色：[空格, 1, 2, 3, 4] × rgb
  const col = new Float32Array(15)
  const colGoal = new Float32Array(15)
  let colReady = false
  const fg = [244, 244, 245]
  const bg = [22, 20, 30]
  // 单元格
  let n = 0
  let weeks = 0
  let wk = new Float32Array(0)
  let dy = new Float32Array(0)
  let lv = new Uint8Array(0)
  let hgt = new Float32Array(0)
  let hgtGoal = new Float32Array(0)
  let zs = new Float32Array(0)
  let hover = new Float32Array(0)
  let dim = new Float32Array(0)
  let pulse = new Float32Array(0)
  let pulseIdx = -1
  let polys = new Float32Array(0)
  let faces = new Uint8Array(0)
  // 每根柱子顶面的屏幕中心与当前缩放：鼠标落在缝隙里时用来吸附最近的格子
  let cxs = new Float32Array(0)
  let cys = new Float32Array(0)
  let bxs = new Float32Array(0)
  let bys = new Float32Array(0)
  let snapS = 1
  let order = []
  let months = []
  let weekdayRows = []
  let dates = []
  let failSet = new Set()
  let selectedDate = ''
  // 交互
  let hovered = -1
  let pinned = -1
  let activeIdx = -1
  let tipW = 0
  let raf = 0
  let last = 0

  const LEVELS = [
    [40, 40, 52],
    [13, 58, 56],
    [15, 111, 106],
    [0, 190, 180],
    [126, 243, 234]
  ]

  const load = () => {
    const m = model.value
    n = m.cells.length
    weeks = m.weeks
    const scale = Math.max(4, Math.round((m.busy || m.max) * 1.15))
    if (wk.length !== n) {
      wk = new Float32Array(n)
      dy = new Float32Array(n)
      lv = new Uint8Array(n)
      hgt = new Float32Array(n)
      hgtGoal = new Float32Array(n)
      zs = new Float32Array(n)
      hover = new Float32Array(n)
      dim = new Float32Array(n)
      pulse = new Float32Array(n)
      polys = new Float32Array(n * 24)
      faces = new Uint8Array(n)
      cxs = new Float32Array(n)
      cys = new Float32Array(n)
      bxs = new Float32Array(n)
      bys = new Float32Array(n)
      order = Array.from({ length: n }, (_, i) => i)
    }
    for (let i = 0; i < n; i++) {
      const c = m.cells[i]
      wk[i] = c.week
      dy[i] = c.day
      lv[i] = c.level
      // 高度基准取 95 分位而不是全年最大值：一天的异常尖峰不该把其余日子压平
      hgtGoal[i] = barHeight(Math.min(c.count, scale * 1.2), scale, 1)
    }
    if (reduced) hgt.set(hgtGoal)
    dates = new Array(n)
    for (let i = 0; i < n; i++) dates[i] = m.cells[i].date
    // 失败的日子由看板的 fails 传进来：平面图上描红圈，天际线里给顶面加一层红
    failSet = new Set(Object.keys(props.marks || {}))
    selectedDate = props.selected || ''
    months = monthLabels(m.cells, m.weeks, props.locale)
    const wf = new Intl.DateTimeFormat(props.locale, { weekday: 'short', timeZone: 'UTC' })
    weekdayRows = []
    for (let d = 0; d < 7 && d < n; d++) weekdayRows.push({ day: d, label: wf.format(dayMs(m.cells[d].date)) })
    if (hovered >= n) hovered = -1
    if (pinned >= n) pinned = -1
    relayout()
    kick()
  }

  const retheme = () => {
    for (let k = 0; k < 5; k++) {
      const c = LEVELS[k]
      colGoal[k * 3] = c[0]
      colGoal[k * 3 + 1] = c[1]
      colGoal[k * 3 + 2] = c[2]
    }
    if (!colReady || reduced) {
      col.set(colGoal)
      colReady = true
    }
    font = '400 10px ' + (getComputedStyle(stage).fontFamily || 'sans-serif')
    ctx.font = font
    labelW = Math.ceil(Math.max(16, ...weekdayRows.map(r => ctx.measureText(r.label).width))) + 8
  }

  // 当前相机下场景的投影范围（柱子按 zOf 取高）
  const extent = (cam, e, full) => {
    const w = lerp(0.78, 0.9, e)
    const off = (1 - w) / 2
    let minx = Infinity
    let maxx = -Infinity
    let miny = Infinity
    let maxy = -Infinity
    const add = (x, y, z) => {
      const px = x * cam.cs - y * cam.sn
      const py = (x * cam.sn + y * cam.cs) * cam.se - z * cam.ce
      if (px < minx) minx = px
      if (px > maxx) maxx = px
      if (py < miny) miny = py
      if (py > maxy) maxy = py
    }
    for (let i = 0; i < n; i++) {
      const x0 = wk[i] + off
      const y0 = dy[i] + off
      const z = full ? hgt[i] * e : zs[i]
      add(x0, y0, z)
      add(x0 + w, y0, z)
      add(x0, y0 + w, z)
      add(x0 + w, y0 + w, 0)
      add(x0, y0 + w, 0)
      add(x0 + w, y0, 0)
    }
    // 3D 里月份标签沿前缘排开，也要留位置
    add(0, 7 + 1.5 * e, 0)
    add(weeks, 7 + 1.5 * e, 0)
    return { minx, maxx, miny, maxy }
  }

  const relayout = () => {
    const w = Math.round(stage.clientWidth)
    if (!w || !n) return
    W = w
    // 窄容器把星期名那一列让给网格
    gutter = W < 520 ? 0 : labelW
    dpr = Math.min(2, window.devicePixelRatio || 1)
    const b2 = extent(camera(0), 0, true)
    H2 = 16 + 6 + ((b2.maxy - b2.miny) / (b2.maxx - b2.minx)) * (W - gutter - 4)
    const b3 = extent(camera(1), 1, true)
    const natural = ((b3.maxy - b3.miny) / (b3.maxx - b3.minx)) * (W - 30) + 30
    H3 = Math.max(Math.min(natural, W * 0.62, 500), Math.min(natural, 200))
    Hmax = Math.ceil(Math.max(H2, H3))
    canvas.width = Math.round(W * dpr)
    canvas.height = Math.round(Hmax * dpr)
    canvas.style.width = W + 'px'
    canvas.style.height = Hmax + 'px'
    lastH = -1
    draw()
  }

  const quadPath = (o, r) => {
    const p = polys
    if (r < 0.3) {
      ctx.moveTo(p[o], p[o + 1])
      ctx.lineTo(p[o + 2], p[o + 3])
      ctx.lineTo(p[o + 4], p[o + 5])
      ctx.lineTo(p[o + 6], p[o + 7])
      ctx.closePath()
      return
    }
    ctx.moveTo((p[o + 6] + p[o]) / 2, (p[o + 7] + p[o + 1]) / 2)
    for (let k = 0; k < 4; k++) {
      const b = (k + 1) % 4
      ctx.arcTo(p[o + k * 2], p[o + k * 2 + 1], p[o + b * 2], p[o + b * 2 + 1], r)
    }
    ctx.closePath()
  }

  const draw = () => {
    if (!W || !n) return
    const e = easeInOutCubic(t)
    const cam = camera(e, yaw, elev)
    const Hc = lerp(H2, H3, e)
    if (Math.abs(Hc - lastH) > 0.2) {
      stage.style.height = Hc.toFixed(1) + 'px'
      lastH = Hc
    }
    for (let i = 0; i < n; i++) zs[i] = riseAt(t, wk[i], weeks, dy[i]) * hgt[i]
    const b = extent(cam, e, false)
    const pad = lerp(2, 16, e)
    const left = pad + gutter * (1 - e)
    const top = pad + 18 * (1 - e)
    const aw = W - left - pad
    const ah = Hc - top - pad
    const bw = Math.max(1e-6, b.maxx - b.minx)
    const bh = Math.max(1e-6, b.maxy - b.miny)
    const s = Math.min(aw / bw, ah / bh)
    snapS = s
    const ox = left + (aw - bw * s) / 2 - b.minx * s
    const oy = top + (ah - bh * s) / 2 - b.miny * s
    const { cs, sn, se, ce } = cam
    const px = (x, y) => ox + (x * cs - y * sn) * s
    const py = (x, y, z) => oy + ((x * sn + y * cs) * se - z * ce) * s

    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
    ctx.clearRect(0, 0, W, Hmax)

    order.sort((a, c) => (wk[a] + 0.5) * sn + (dy[a] + 0.5) * cs - ((wk[c] + 0.5) * sn + (dy[c] + 0.5) * cs))

    const w = lerp(0.78, 0.9, e)
    const off = (1 - w) / 2
    const radius = lerp(0.17, 0.03, e) * s
    const lift = 0.7 * e
    const ex = col[0]
    const ey = col[1]
    const ez = col[2]

    for (let k = 0; k < n; k++) {
      const i = order[k]
      const x0 = wk[i] + off
      const y0 = dy[i] + off
      const x1 = x0 + w
      const y1 = y0 + w
      const z = zs[i] + hover[i] * lift + pulse[i] * 2.2 * e
      const o = i * 24
      polys[o] = px(x0, y0)
      polys[o + 1] = py(x0, y0, z)
      polys[o + 2] = px(x1, y0)
      polys[o + 3] = py(x1, y0, z)
      polys[o + 4] = px(x1, y1)
      polys[o + 5] = py(x1, y1, z)
      polys[o + 6] = px(x0, y1)
      polys[o + 7] = py(x0, y1, z)
      const cxp = px(wk[i] + 0.5, dy[i] + 0.5)
      cxs[i] = cxp
      cys[i] = py(wk[i] + 0.5, dy[i] + 0.5, z)
      bxs[i] = cxp
      bys[i] = py(wk[i] + 0.5, dy[i] + 0.5, 0)
      polys[o + 8] = px(x0, y1)
      polys[o + 9] = py(x0, y1, 0)
      polys[o + 10] = px(x1, y1)
      polys[o + 11] = py(x1, y1, 0)
      polys[o + 12] = polys[o + 4]
      polys[o + 13] = polys[o + 5]
      polys[o + 14] = polys[o + 6]
      polys[o + 15] = polys[o + 7]
      polys[o + 16] = px(x1, y0)
      polys[o + 17] = py(x1, y0, 0)
      polys[o + 18] = polys[o + 10]
      polys[o + 19] = polys[o + 11]
      polys[o + 20] = polys[o + 4]
      polys[o + 21] = polys[o + 5]
      polys[o + 22] = polys[o + 2]
      polys[o + 23] = polys[o + 3]

      const tall = z * ce * s
      let f = 0
      if (tall > 0.35 && w * cs * s > 0.35) f |= 1
      if (tall > 0.35 && w * sn * s > 0.35) f |= 2
      faces[i] = f

      const L = lv[i] * 3
      let r = col[L]
      let g = col[L + 1]
      let bl = col[L + 2]
      const d = dim[i]
      if (d > 0.002) {
        r += (ex - r) * 0.72 * d
        g += (ey - g) * 0.72 * d
        bl += (ez - bl) * 0.72 * d
      }
      const hv = hover[i]
      if (hv > 0.002) {
        const m = 0.16 * hv
        r += (fg[0] - r) * m
        g += (fg[1] - g) * m
        bl += (fg[2] - bl) * m
      }
      const pv = pulse[i]
      if (pv > 0.002) {
        r += (126 - r) * 0.5 * pv
        g += (243 - g) * 0.5 * pv
        bl += (234 - bl) * 0.5 * pv
      }
      // 这一天有失败的待办或自动化：顶面掺一点红，再描一圈红边
      const fail = failSet.has(dates[i])
      if (fail) {
        r += (248 - r) * 0.16
        g += (113 - g) * 0.16
        bl += (113 - bl) * 0.16
      }
      if (f & 1) {
        ctx.beginPath()
        quadPath(o + 8, 0)
        ctx.fillStyle = 'rgb(' + Math.round(r * 0.8) + ',' + Math.round(g * 0.8) + ',' + Math.round(bl * 0.8) + ')'
        ctx.fill()
      }
      if (f & 2) {
        ctx.beginPath()
        quadPath(o + 16, 0)
        ctx.fillStyle = 'rgb(' + Math.round(r * 0.62) + ',' + Math.round(g * 0.62) + ',' + Math.round(bl * 0.62) + ')'
        ctx.fill()
      }
      ctx.beginPath()
      quadPath(o, radius)
      ctx.fillStyle = 'rgb(' + Math.round(r) + ',' + Math.round(g) + ',' + Math.round(bl) + ')'
      ctx.fill()
      if (hv > 0.02) {
        ctx.strokeStyle = 'rgba(244,244,245,' + (0.85 * hv).toFixed(3) + ')'
        ctx.lineWidth = 1.5
        ctx.stroke()
      } else if (pv > 0.02) {
        ctx.strokeStyle = 'rgba(126,243,234,' + (0.9 * pv).toFixed(3) + ')'
        ctx.lineWidth = 1.5
        ctx.stroke()
      }
      if (fail) {
        ctx.strokeStyle = 'rgba(248,113,113,0.9)'
        ctx.lineWidth = Math.max(0.8, Math.min(1.5, s * 0.075))
        ctx.stroke()
      }
      if (dates[i] === selectedDate) {
        ctx.strokeStyle = 'rgba(126,243,234,0.95)'
        ctx.lineWidth = Math.max(1, Math.min(1.8, s * 0.095))
        ctx.stroke()
      }
    }

    // 标签：2D 在顶部与左侧，3D 走到前缘；只淡入淡出，不跳变
    ctx.font = font
    const a2 = 1 - smoothstep(0, 0.4, e)
    const a3 = smoothstep(0.62, 1, e)
    const muted = 'rgba(244,244,245,'
    if (a2 > 0.004) {
      ctx.fillStyle = muted + (0.62 * a2).toFixed(3) + ')'
      ctx.textAlign = 'left'
      ctx.textBaseline = 'bottom'
      let edge = -Infinity
      for (const m of months) {
        const x = px(m.week + off, -0.3)
        const tw = ctx.measureText(m.label).width
        if (x < edge || x + tw > W) continue
        ctx.fillText(m.label, x, py(m.week + off, -0.3, 0) - 3)
        edge = x + tw + 6
      }
      ctx.textAlign = 'right'
      ctx.textBaseline = 'middle'
      // 行距太窄时行名会互相压住，索性不画，把宽度让给格子
      if (gutter > 0 && s >= 12) for (const r of weekdayRows) ctx.fillText(r.label, px(0, r.day + 0.5) - 6, py(0, r.day + 0.5, 0))
    }
    if (a3 > 0.004) {
      ctx.fillStyle = muted + (0.62 * a3).toFixed(3) + ')'
      ctx.textAlign = 'left'
      ctx.textBaseline = 'top'
      let edge = -Infinity
      for (const m of months) {
        const x = px(m.week + 0.5, 7.3)
        const tw = ctx.measureText(m.label).width
        if (x < edge || x + tw > W) continue
        ctx.fillText(m.label, x, py(m.week + 0.5, 7.3, 0) + 2)
        edge = x + tw + 10
      }
    }

    // 提示框跟着当前格子走
    if (activeIdx >= 0 && activeIdx < n) {
      const i = activeIdx
      const z = zs[i] + hover[i] * lift
      const tx = px(wk[i] + 0.5, dy[i] + 0.5)
      const ty = Math.min(py(wk[i] + off, dy[i] + off, z), py(wk[i] + off + w, dy[i] + off, z), py(wk[i] + off, dy[i] + off + w, z))
      const half = tipW / 2
      const cx = Math.min(W - half - 2, Math.max(half + 2, tx))
      tip.style.transform = 'translate(' + (cx - half).toFixed(1) + 'px,' + (ty - 8).toFixed(1) + 'px) translateY(-100%)'
      tip.style.setProperty('--arrow', (tx - cx + half).toFixed(1) + 'px')
    }
  }

  const tick = now => {
    raf = 0
    const dt = Math.min(0.05, Math.max(0, (now - last) / 1000))
    last = now
    let moving = false

    if (t !== target) {
      const step = reduced ? 1 : (dt * 1000) / Math.max(1, props.duration)
      t = target > t ? Math.min(target, t + step) : Math.max(target, t - step)
      moving = true
    }

    const ko = reduced ? 1 : 1 - Math.exp(-dt * 12)
    yaw += (yawGoal - yaw) * ko
    elev += (elevGoal - elev) * ko
    if (Math.abs(yawGoal - yaw) > 1e-4 || Math.abs(elevGoal - elev) > 1e-4) moving = true
    else {
      yaw = yawGoal
      elev = elevGoal
    }

    const kc = reduced ? 1 : 1 - Math.exp(-dt * 7)
    for (let k = 0; k < 15; k++) {
      const d = colGoal[k] - col[k]
      if (Math.abs(d) > 0.4) {
        col[k] += d * kc
        moving = true
      } else col[k] = colGoal[k]
    }

    const kh = reduced ? 1 : 1 - Math.exp(-dt * 16)
    const kd = reduced ? 1 : 1 - Math.exp(-dt * 10)
    const kg = reduced ? 1 : 1 - Math.exp(-dt * 7)
    const leg = props.focusLevel
    for (let i = 0; i < n; i++) {
      const hg = i === activeIdx ? 1 : 0
      const dg = leg >= 0 && lv[i] !== leg ? 1 : 0
      const h = hover[i]
      const d = dim[i]
      const gh = hgtGoal[i] - hgt[i]
      if (Math.abs(gh) > 0.004) {
        hgt[i] += gh * kg
        moving = true
      } else hgt[i] = hgtGoal[i]
      if (pulse[i] > 0.001) {
        pulse[i] = Math.max(0, pulse[i] - dt * 0.9)
        moving = true
      }
      if (h !== hg) {
        hover[i] = Math.abs(hg - h) < 0.003 ? hg : h + (hg - h) * kh
        moving = true
      }
      if (d !== dg) {
        dim[i] = Math.abs(dg - d) < 0.003 ? dg : d + (dg - d) * kd
        moving = true
      }
    }

    draw()
    if (moving) raf = requestAnimationFrame(tick)
  }

  const kick = () => {
    if (raf) return
    last = performance.now()
    raf = requestAnimationFrame(tick)
  }

  const refreshActive = () => {
    const next = hovered >= 0 ? hovered : pinned
    if (next === activeIdx) return
    activeIdx = next
    activeIndex.value = next
    kick()
  }

  const pointInQuad = (o, x, y) => {
    let sign = 0
    const p = polys
    for (let k = 0; k < 4; k++) {
      const ax = p[o + k * 2]
      const ay = p[o + k * 2 + 1]
      const bx = p[o + ((k + 1) % 4) * 2]
      const by = p[o + ((k + 1) % 4) * 2 + 1]
      const cross = (bx - ax) * (y - ay) - (by - ay) * (x - ax)
      if (Math.abs(cross) < 1e-9) continue
      const s = cross > 0 ? 1 : -1
      if (sign === 0) sign = s
      else if (s !== sign) return false
    }
    return sign !== 0
  }

  const hit = (x, y) => {
    for (let k = n - 1; k >= 0; k--) {
      const i = order[k]
      const o = i * 24
      if (pointInQuad(o, x, y)) return i
      if (faces[i] & 1 && pointInQuad(o + 8, x, y)) return i
      if (faces[i] & 2 && pointInQuad(o + 16, x, y)) return i
    }
    // 没落在任何面上（缝隙、被遮挡的侧面）：吸附到最近的柱子，
    // 柱子按「底座中心 -> 顶面中心」这条线段算距离，高塔的侧壁也能命中
    const r = snapS * (t < 0.35 ? 1.45 : 1)
    let best = -1
    let bestD = r * r
    for (let i = 0; i < n; i++) {
      const ax = bxs[i]
      const ay = bys[i]
      const bx = cxs[i]
      const by = cys[i]
      const vx = bx - ax
      const vy = by - ay
      const len = vx * vx + vy * vy
      let t2 = len > 1e-6 ? ((x - ax) * vx + (y - ay) * vy) / len : 0
      t2 = t2 < 0 ? 0 : t2 > 1 ? 1 : t2
      const dx = ax + vx * t2 - x
      const dy = ay + vy * t2 - y
      const d = dx * dx + dy * dy
      if (d < bestD) {
        bestD = d
        best = i
      }
    }
    return best
  }

  const local = ev => {
    const r = canvas.getBoundingClientRect()
    return [ev.clientX - r.left, ev.clientY - r.top]
  }

  let drag = null

  const onDown = ev => {
    if (ev.button !== 0) return
    const can = target === 1
    drag = { id: ev.pointerId, x: ev.clientX, y: ev.clientY, yaw: yawGoal, elev: elevGoal, moved: false, orbit: can, mouse: ev.pointerType === 'mouse' }
    if (can) {
      try {
        canvas.setPointerCapture(ev.pointerId)
      } catch (e) {
        /* 指针捕获只是锦上添花 */
      }
    }
  }

  const onMove = ev => {
    if (drag && drag.orbit && ev.pointerId === drag.id) {
      const dx = ev.clientX - drag.x
      const dyy = ev.clientY - drag.y
      if (drag.moved || Math.hypot(dx, dyy) > 4) {
        drag.moved = true
        yawGoal = Math.min(1.43, Math.max(0.14, drag.yaw + dx * 0.006))
        if (drag.mouse) elevGoal = Math.min(1.08, Math.max(0.31, drag.elev + dyy * 0.004))
        canvas.style.cursor = 'grabbing'
        hovered = -1
        refreshActive()
        kick()
        return
      }
    }
    if (ev.pointerType !== 'mouse') return
    const [x, y] = local(ev)
    const i = hit(x, y)
    if (i !== hovered) {
      hovered = i
      refreshActive()
    }
    canvas.style.cursor = i >= 0 ? 'pointer' : 'default'
  }

  const onUp = ev => {
    if (!drag || ev.pointerId !== drag.id) return
    const wasMoved = drag.moved
    drag = null
    if (canvas.hasPointerCapture(ev.pointerId)) canvas.releasePointerCapture(ev.pointerId)
    canvas.style.cursor = 'default'
    if (wasMoved) return
    const [x, y] = local(ev)
    const i = hit(x, y)
    pinned = i === pinned ? -1 : i
    if (ev.pointerType !== 'mouse') hovered = -1
    refreshActive()
    // 点中某一天：把日期交给外层，右侧时间线与抽屉都跟着这一天走
    if (i >= 0 && i < n) emit('select', dates[i])
  }

  const onCancel = () => {
    drag = null
  }

  const onLeave = () => {
    if (drag) return
    hovered = -1
    refreshActive()
  }

  const onDbl = () => {
    yawGoal = 0
    elevGoal = 0
    kick()
  }

  const onKey = ev => {
    const keys = ['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'Home', 'End', 'Escape', 'Enter', ' ']
    if (!keys.includes(ev.key) || !n) return
    ev.preventDefault()
    if (ev.key === 'Escape') {
      pinned = -1
      hovered = -1
      refreshActive()
      return
    }
    let i = pinned >= 0 ? pinned : activeIdx >= 0 ? activeIdx : n - 1
    if (pinned >= 0 || activeIdx >= 0) {
      if (ev.key === 'ArrowLeft') i -= 7
      if (ev.key === 'ArrowRight') i += 7
      if (ev.key === 'ArrowUp') i -= 1
      if (ev.key === 'ArrowDown') i += 1
      if (ev.key === 'Home') i = 0
      if (ev.key === 'End') i = n - 1
    }
    i = Math.max(0, Math.min(n - 1, i))
    pinned = i
    hovered = -1
    refreshActive()
    if (ev.key === 'Enter' || ev.key === ' ') emit('select', dates[i])
  }

  const onBlur = () => {
    pinned = -1
    refreshActive()
  }

  const setTarget = () => {
    const goal = props.view === '3d' ? 1 : 0
    if (!entered) return
    if (goal !== target) {
      target = goal
      if (goal === 0) {
        yawGoal = 0
        elevGoal = 0
      }
      kick()
    }
  }

  retheme()
  load()
  relayout()

  // 第一次出现在视野里时，平面视图升起成天际线
  const enter = () => {
    if (entered) return
    entered = true
    if (reduced) t = target
    setTarget()
  }
  let io = null
  if ('IntersectionObserver' in window) {
    io = new IntersectionObserver(
      entries => {
        if (entries.some(en => en.isIntersecting)) {
          enter()
          if (io) io.disconnect()
        }
      },
      { threshold: 0.35 }
    )
    io.observe(stage)
  } else enter()

  const ro = new ResizeObserver(() => {
    if (Math.round(stage.clientWidth) !== W) relayout()
  })
  ro.observe(stage)

  const onReduce = () => {
    reduced = reduceMq.matches
    kick()
  }
  reduceMq.addEventListener('change', onReduce)

  canvas.addEventListener('pointerdown', onDown)
  canvas.addEventListener('pointermove', onMove)
  canvas.addEventListener('pointerup', onUp)
  canvas.addEventListener('pointercancel', onCancel)
  canvas.addEventListener('pointerleave', onLeave)
  canvas.addEventListener('dblclick', onDbl)
  canvas.addEventListener('keydown', onKey)
  canvas.addEventListener('blur', onBlur)

  engine.value = {
    kick: () => {
      setTarget()
      kick()
    },
    load: () => {
      load()
      retheme()
      relayout()
    },
    tipWidth: w => {
      tipW = w
      draw()
    },
    // 选中的一天由面板决定，这里只负责记住并重绘
    select: date => {
      selectedDate = date || ''
      draw()
    },
    pulseToday: () => {
      // 今天那一格（网格最后一列的最后一行附近）亮一下
      const m = model.value
      for (let i = 0; i < n; i++) {
        if (m.cells[i].date === props.end) {
          pulseIdx = i
          pulse[i] = 1
          break
        }
      }
      kick()
    }
  }

  onBeforeUnmount(() => {
    if (raf) cancelAnimationFrame(raf)
    if (io) io.disconnect()
    ro.disconnect()
    reduceMq.removeEventListener('change', onReduce)
    canvas.removeEventListener('pointerdown', onDown)
    canvas.removeEventListener('pointermove', onMove)
    canvas.removeEventListener('pointerup', onUp)
    canvas.removeEventListener('pointercancel', onCancel)
    canvas.removeEventListener('pointerleave', onLeave)
    canvas.removeEventListener('dblclick', onDbl)
    canvas.removeEventListener('keydown', onKey)
    canvas.removeEventListener('blur', onBlur)
    engine.value = null
  })
})

defineExpose({
  pulseToday: () => engine.value && engine.value.pulseToday()
})
</script>

<style lang="scss" scoped>
.ck-stage {
  position: relative;
  width: 100%;
  overflow: hidden;
  border-radius: 10px;
  outline-offset: 4px;
  transition: height 0.12s linear;
}

.ck-stage:has(:focus-visible) {
  outline: 2px solid rgba(126, 243, 234, 0.5);
}

.ck-canvas {
  position: absolute;
  top: 0;
  left: 0;
  display: block;
  max-width: none;
  outline: none;
  touch-action: pan-y;
}

.ck-tip {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 3;
  display: flex;
  align-items: baseline;
  gap: 6px;
  padding: 6px 9px;
  border: 1px solid rgba(126, 243, 234, 0.22);
  border-radius: 7px;
  background: rgba(16, 14, 22, 0.92);
  box-shadow: 0 10px 26px rgba(0, 0, 0, 0.45);
  color: var(--pf-ink, #f4f4f5);
  font-size: 12px;
  line-height: 1;
  white-space: nowrap;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.ck-tip.is-on {
  opacity: 1;
}

.ck-tip b {
  font-weight: 600;
}

.ck-tip__meta {
  color: rgba(244, 244, 245, 0.58);
}

.ck-tip__pad {
  opacity: 0;
}

.ck-tip__arrow {
  position: absolute;
  top: 100%;
  left: var(--arrow, 50%);
  width: 0;
  height: 0;
  margin-left: -5px;
  border-top: 5px solid rgba(16, 14, 22, 0.92);
  border-right: 5px solid transparent;
  border-left: 5px solid transparent;
}

@media (prefers-reduced-motion: reduce) {
  .ck-stage,
  .ck-tip {
    transition: none;
  }
}
</style>
