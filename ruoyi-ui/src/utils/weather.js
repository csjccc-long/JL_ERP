/**
 * 当日天气：Open-Meteo 免密钥接口，失败静默返回 null。
 * 结果按自然日缓存到 localStorage，失败缓存 30 分钟避免反复打网络。
 */
const CACHE_KEY = "erp:weather:now:v1"
const FAIL_KEY = "erp:weather:now:fail:v1"

const WMO = [
  { test: [0], kind: "clear", label: "晴" },
  { test: [1], kind: "partly", label: "晴间多云" },
  { test: [2], kind: "partly", label: "多云" },
  { test: [3], kind: "overcast", label: "阴" },
  { test: [45, 48], kind: "fog", label: "雾" },
  { test: [51, 53, 55, 56, 57], kind: "drizzle", label: "毛毛雨" },
  { test: [61, 63], kind: "rain", label: "小雨" },
  { test: [65], kind: "heavy-rain", label: "大雨" },
  { test: [66, 67], kind: "rain", label: "冻雨" },
  { test: [71, 73, 77], kind: "snow", label: "小雪" },
  { test: [75], kind: "snow", label: "大雪" },
  { test: [80, 81], kind: "rain", label: "阵雨" },
  { test: [82], kind: "heavy-rain", label: "强阵雨" },
  { test: [85, 86], kind: "snow", label: "阵雪" },
  { test: [95], kind: "storm", label: "雷阵雨" },
  { test: [96, 99], kind: "storm", label: "雷暴冰雹" }
]

export function describeWeather(code) {
  const hit = WMO.find(item => item.test.includes(Number(code)))
  return hit ? { kind: hit.kind, label: hit.label } : { kind: "rain", label: "降雨" }
}

function readJson(storage, key) {
  try {
    const raw = storage.getItem(key)
    return raw ? JSON.parse(raw) : null
  } catch (error) {
    return null
  }
}

function todayKey() {
  const now = new Date()
  return now.getFullYear() + "-" + (now.getMonth() + 1) + "-" + now.getDate()
}

export async function loadCurrentWeather(options = {}) {
  const city = options.city || "深圳"
  const latitude = options.latitude ?? 22.5431
  const longitude = options.longitude ?? 114.0579
  const timezone = options.timezone || "Asia/Shanghai"
  const storage = options.storage || (typeof window !== "undefined" ? window.localStorage : null)

  if (storage) {
    const cached = readJson(storage, CACHE_KEY)
    if (cached && cached.day === todayKey() && cached.city === city) return cached.data
    const failed = readJson(storage, FAIL_KEY)
    if (failed && Date.now() - failed.at < 30 * 60 * 1000) return null
  }

  const url =
    "https://api.open-meteo.com/v1/forecast?latitude=" + latitude +
    "&longitude=" + longitude +
    "&current=temperature_2m,weather_code,is_day&timezone=" + encodeURIComponent(timezone)

  const controller = typeof AbortController !== "undefined" ? new AbortController() : null
  const timer = controller ? setTimeout(() => controller.abort(), 6000) : null
  try {
    const response = await fetch(url, { signal: controller ? controller.signal : undefined })
    if (!response.ok) throw new Error("weather " + response.status)
    const payload = await response.json()
    const current = payload.current || {}
    const info = describeWeather(current.weather_code)
    const data = {
      city,
      kind: info.kind,
      label: info.label,
      code: Number(current.weather_code ?? 0),
      temp: Math.round(Number(current.temperature_2m ?? 0)),
      isDay: Number(current.is_day ?? 1) === 1
    }
    if (storage) {
      storage.setItem(CACHE_KEY, JSON.stringify({ day: todayKey(), city, data }))
      storage.removeItem(FAIL_KEY)
    }
    return data
  } catch (error) {
    if (storage) storage.setItem(FAIL_KEY, JSON.stringify({ at: Date.now() }))
    return null
  } finally {
    if (timer) clearTimeout(timer)
  }
}
