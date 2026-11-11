/**
 * 打卡天际线的纯计算部分：日期网格、等级、镜头、投影。
 * 与视图无关，便于单独验证。
 */

export const DAY_MS = 86400000

export const clamp01 = v => (v > 0 ? (v < 1 ? v : 1) : 0)
export const lerp = (a, b, t) => a + (b - a) * t
export const easeInOutCubic = x => {
  const t = clamp01(x)
  return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2
}
export const easeOutCubic = x => 1 - Math.pow(1 - clamp01(x), 3)
export const smoothstep = (a, b, x) => {
  const t = clamp01((x - a) / (b - a))
  return t * t * (3 - 2 * t)
}

/** UTC 零点 -> "YYYY-MM-DD" */
export const toKey = ms => new Date(ms).toISOString().slice(0, 10)

/** 任意日期 -> UTC 零点时间戳；"YYYY-MM-DD" 按字面解析，不做时区换算 */
export const dayMs = v => {
  if (typeof v === 'number') return Math.floor(v / DAY_MS) * DAY_MS
  if (typeof v === 'string') {
    const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(v)
    if (m) return Date.UTC(+m[1], +m[2] - 1, +m[3])
    v = new Date(v)
  }
  return Date.UTC(v.getFullYear(), v.getMonth(), v.getDate())
}

/**
 * 网格：列 = 周，行 = 星期（weekStart=1 时行 0 是周一）。
 * 从 start 排到 end，补满整周；缺失的日期按 0 处理，重复日期累加。
 */
export const buildGrid = (days, start, end, weekStart = 1) => {
  const counts = new Map()
  const checked = new Set()
  for (const d of days || []) {
    if (!d || typeof d.date !== 'string') continue
    const ms = dayMs(d.date)
    const c = Number(d.count)
    if (!Number.isFinite(ms) || !Number.isFinite(c)) continue
    const k = toKey(ms)
    counts.set(k, (counts.get(k) || 0) + (c > 0 ? c : 0))
    if (d.checked) checked.add(k)
  }
  let from = dayMs(start)
  const to = dayMs(end)
  from -= ((new Date(from).getUTCDay() - weekStart + 7) % 7) * DAY_MS
  const cells = []
  for (let ms = from, i = 0; ms <= to; ms += DAY_MS, i++) {
    const date = toKey(ms)
    cells.push({ date, count: counts.get(date) || 0, level: 0, week: Math.floor(i / 7), day: i % 7, checked: checked.has(date) })
  }
  const nz = cells.filter(c => c.count > 0).map(c => c.count).sort((a, b) => a - b)
  const busy = nz.length ? nz[Math.floor(0.95 * (nz.length - 1))] : 0
  // 分级按非零日子的分位数切，无论数据是尖峰型还是均匀型，四档都能铺开
  const at = q => (nz.length ? nz[Math.floor(q * (nz.length - 1))] : 0)
  const q1 = at(0.35)
  const q2 = at(0.6)
  const q3 = at(0.82)
  for (const c of cells) c.level = c.count <= 0 ? 0 : c.count <= q1 ? 1 : c.count <= q2 ? 2 : c.count <= q3 ? 3 : 4
  return {
    cells,
    weeks: cells.length ? cells[cells.length - 1].week + 1 : 0,
    busy,
    max: nz.length ? nz[nz.length - 1] : 0
  }
}

/** 0 = 没动过，1-4 = 按 busy 的四等分；达到或超过 busy 就是 4 */
export const levelOf = (count, busy) => (count <= 0 ? 0 : busy <= 0 ? 4 : 1 + Math.min(3, Math.floor((count / busy) * 4)))

/** 每列第一天的月份与上个月不同时打一个标签；开头太挤的那个丢掉 */
export const monthLabels = (cells, weeks, locale = 'zh-CN') => {
  const fmt = new Intl.DateTimeFormat(locale, { month: 'short', timeZone: 'UTC' })
  const out = []
  let prev = -1
  for (let w = 0; w < weeks; w++) {
    const c = cells[w * 7]
    if (!c) break
    const m = +c.date.slice(5, 7)
    if (m !== prev) out.push({ week: w, label: fmt.format(dayMs(c.date)) })
    prev = m
  }
  if (out.length > 1 && out[1].week - out[0].week < 3) out.shift()
  return out
}

/** 柱高（网格单位）：空格子是一层薄板，最忙的一天约 7.6 格高 */
export const barHeight = (count, max, scale = 1) =>
  count > 0 && max > 0 ? 0.4 + Math.pow(count / max, 0.85) * 7.2 * scale : 0.2

/** 形变中柱子等待的比例：这一波从最旧的一周扫到最新的一周 */
export const WAVE = 0.42

export const riseAt = (t, week, weeks, day) => {
  const d = (weeks > 1 ? week / (weeks - 1) : 0) * 0.36 + (day / 6) * 0.06
  return easeOutCubic((t - d) / (1 - WAVE))
}

/** 四级热力色：深青 -> 亮青，与页面 --pf-accent 同一条色相 */
export const LEVEL_COLORS = ['#0d3a38', '#0f6f6a', '#00beb4', '#7ef3ea']

/** 空格子的底色 */
export const EMPTY_COLOR = 'rgba(255, 255, 255, 0.075)'

export const YAW_3D = Math.PI / 4
export const ELEV_3D = (34 * Math.PI) / 180
export const YAW_RANGE = [(8 * Math.PI) / 180, (82 * Math.PI) / 180]
export const ELEV_RANGE = [(18 * Math.PI) / 180, (62 * Math.PI) / 180]

/**
 * e=0 正俯视：x 横向、y 纵向、高度看不见，就是一张平铺热力图；
 * e=1 是等距视角。拖动产生的偏移按 e 的比例生效，平面视图永远不会被拖歪。
 */
export const camera = (e, dYaw = 0, dElev = 0) => {
  const yaw = Math.min(YAW_RANGE[1], Math.max(0, lerp(0, YAW_3D + dYaw, e)))
  const elev = lerp(Math.PI / 2, Math.min(ELEV_RANGE[1], Math.max(ELEV_RANGE[0], ELEV_3D + dElev)), e)
  return { cs: Math.cos(yaw), sn: Math.sin(yaw), se: Math.sin(elev), ce: Math.cos(elev) }
}

/** 世界坐标（x = 周，y = 星期，z = 高度）-> 屏幕坐标（缩放前） */
export const project = (c, x, y, z) => [x * c.cs - y * c.sn, (x * c.sn + y * c.cs) * c.se - z * c.ce]

export const mixRGB = (a, b, t) => [a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t]
export const luminance = c => (0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]) / 255
