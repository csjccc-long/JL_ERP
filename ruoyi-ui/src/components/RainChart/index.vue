<template>
  <div ref="wrapRef" class="rain-scene" :class="{ 'is-ready': ready }">
    <p class="rain-scene__hint">拖动旋转 · 双击复位</p>
    <div v-if="weather && ready" class="rain-scene__weather" :data-kind="previewKind || weather.kind">
      <span class="rain-scene__weather-dot" />
      <span class="rain-scene__weather-text">{{ weatherText }}</span>
      <span class="rain-scene__weather-temp">{{ weather.temp }}°</span>
    </div>

    <button
      v-if="pickerVisible && !pickerOpen"
      type="button"
      class="rain-scene__picker-toggle"
      @pointerdown.stop
      @click.stop="pickerOpen = true"
    >
      天气预览
    </button>

    <section
      v-if="pickerVisible && pickerOpen"
      class="rain-scene__picker"
      @pointerdown.stop
      @click.stop
      @dblclick.stop
    >
      <header class="rain-scene__picker-head">
        <strong>天气预览</strong>
        <span class="rain-scene__picker-actions">
          <button type="button" @click="resetPreview">重置</button>
          <button type="button" @click="pickerOpen = false">收起</button>
        </span>
      </header>
      <div class="rain-scene__picker-kinds">
        <button
          v-for="item in WEATHER_KINDS"
          :key="item.value"
          type="button"
          :class="{ 'is-active': previewKind === item.value }"
          @click="pickKind(item.value)"
        >
          {{ item.label }}
        </button>
      </div>
      <label v-for="slider in TUNING_SLIDERS" :key="slider.key" class="rain-scene__picker-row">
        <span>{{ slider.label }}</span>
        <input
          type="range"
          :min="slider.min"
          :max="slider.max"
          :step="slider.step"
          :value="tuning[slider.key]"
          @input="onTune(slider.key, $event.target.value)"
        />
        <em>{{ Number(tuning[slider.key]).toFixed(slider.digits) }}</em>
      </label>
    </section>
    <p v-if="failed" class="rain-scene__fallback">当前环境不支持 WebGL，无法渲染雨幕。</p>
  </div>
</template>

<script>
// Rainform 的渲染器是单例，绑定在第一个 #scene-root 上；舞台必须跨组件实例复用，
// 否则切换时间范围导致组件重建时，新容器里不会再出现 canvas。
const rainformHost = { stage: null, api: null, promise: null }

export { rainformHost }
</script>

<script setup name="RainChart">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue"
import { loadCurrentWeather } from "@/utils/weather"

/*
 * 雨幕趋势图：直接内嵌 Rainform（数据成雨）原项目场景。
 *
 * Required Notice: Rainform / 数据成雨 © 2026 afterimage — https://rainform.pages.dev/
 * 源码以 PolyForm Noncommercial 1.0.0 授权，仅限非商业用途；商业产品集成需另行获得授权。
 * 这里的 rainform/main.js 与 rainform/rainform.css 来自上游仓库
 * https://github.com/afterimage-lab/Rainform （sha 3cd4d24），只做了「容器尺寸、数据入口、
 * 天气氛围、静音」四处适配，场景、着色器与交互保持原实现。
 */

const props = defineProps({
  labels: { type: Array, default: () => [] },
  dates: { type: Array, default: () => [] },
  values: { type: Array, default: () => [] },
  counts: { type: Array, default: () => [] },
  valueFormat: { type: Function, default: null },
  // 天气：默认按深圳当日实况自动获取；也可从外部直接传入 { kind, label, temp, city }
  weather: { type: Object, default: null },
  city: { type: String, default: "深圳" },
  latitude: { type: Number, default: 22.5431 },
  longitude: { type: Number, default: 114.0579 },
  // 天气预览调节器（URL ?wx=1 亦可打开）
  weatherPicker: { type: Boolean, default: false }
})

const wrapRef = ref(null)
const ready = ref(false)
const failed = ref(false)
const weather = ref(null)

// —— 天气预览调节器 ——
// 只在 URL 带 ?wx=1（或外部传 weather-picker）时出现，正式界面不受影响。
const WEATHER_KINDS = [
  { value: "partly", label: "多云" },
  { value: "overcast", label: "阴" },
  { value: "fog", label: "雾" },
  { value: "drizzle", label: "毛毛雨" },
  { value: "rain", label: "雨" },
  { value: "heavy-rain", label: "大雨" },
  { value: "snow", label: "雪" },
  { value: "storm", label: "雷暴" }
]
const KIND_LABEL = Object.fromEntries(WEATHER_KINDS.map((item) => [item.value, item.label]))
const TUNING_SLIDERS = [
  { key: "bead", label: "雨珠亮度", min: 0.05, max: 1.6, step: 0.01, digits: 2 },
  { key: "fog", label: "雾浓度", min: 0.004, max: 0.09, step: 0.001, digits: 3 },
  { key: "speed", label: "下落速度", min: 0.3, max: 1.6, step: 0.01, digits: 2 }
]
const PREVIEW_KEY = "erp:rainform:preview-weather"
const pickerVisible = ref(false)
const pickerOpen = ref(false)
const previewKind = ref("")
const tuning = ref({ bead: 1, fog: 0.022, speed: 1 })
const weatherText = computed(() => {
  if (previewKind.value) return `预览 · ${KIND_LABEL[previewKind.value] || previewKind.value}`
  if (!weather.value) return ""
  return `${weather.value.city} · ${weather.value.label}`
})

function syncTuning(preset) {
  if (!preset) return
  tuning.value = {
    bead: Number(preset.bead ?? 1),
    fog: Number(preset.fog ?? 0.022),
    speed: Number(preset.speed ?? 1)
  }
}

function applyKind(kind) {
  previewKind.value = kind
  const preset = host.api?.setWeather(kind)
  syncTuning(preset)
}

function pickKind(kind) {
  try {
    sessionStorage.setItem(PREVIEW_KEY, kind)
  } catch {}
  applyKind(kind)
}

function onTune(key, value) {
  const next = Number(value)
  tuning.value = { ...tuning.value, [key]: next }
  host.api?.setWeatherTuning({ [key]: next })
}

function resetPreview() {
  try {
    sessionStorage.removeItem(PREVIEW_KEY)
  } catch {}
  previewKind.value = ""
  host.api?.clearWeatherTuning?.()
  const kind = weather.value?.kind || "rain"
  const preset = host.api?.setWeather(kind)
  syncTuning(preset)
}

// 保持与原项目一致的 DOM 结构：场景挂载点 + 悬停读数。
const SCENE_MARKUP = `
  <main class="rainfall-dashboard">
    <div id="scene-root" class="scene-root"></div>
    <nav id="scene-toolbar" class="scene-toolbar" aria-hidden="true"></nav>
    <div id="selection-readout" class="selection-readout" aria-live="off">
      <span class="readout-time">--</span>
      <strong class="readout-value">--</strong>
      <span class="readout-unit"></span>
    </div>
  </main>
`

const HIDDEN_STYLE = "position:fixed;left:-20000px;top:0;width:900px;height:360px;visibility:hidden;"

let observer = null
const host = rainformHost

function tickFormat(value) {
  const num = Number(value || 0)
  if (Math.abs(num) >= 10000) return (num / 10000).toFixed(1) + "万"
  if (Math.abs(num) >= 1000) return Math.round(num / 1000) + "k"
  return String(Math.round(num))
}

function valueFormat(value) {
  if (typeof props.valueFormat === "function") return props.valueFormat(Number(value) || 0)
  return tickFormat(value)
}

// 原项目把「降雨量 10 mm/h」当作满幅（VISUAL_RAINFALL_REFERENCE），
// 所以把业务金额等比压到 0..10 的强度域，再由坐标轴格式化回金额。
const INTENSITY_PEAK = 10
// 上游场景按 25 个逐时点铺设雨链；列数一变，单列雨链密度就会变化。
// 这里统一重采样到 25 列，保证 7/30/90 天看到的雨幕厚度一致。
const SCENE_COLUMNS = 25

function resampleSeries(list, size) {
  const source = list.map((item) => Number(item) || 0)
  const last = source.length - 1
  if (last < 1) return source
  const out = []
  for (let index = 0; index < size; index += 1) {
    const position = (index / (size - 1)) * last
    const left = Math.floor(position)
    const right = Math.min(last, left + 1)
    const ratio = position - left
    out.push(source[left] + (source[right] - source[left]) * ratio)
  }
  return out
}

function resampleLabels(labels, size) {
  if (!Array.isArray(labels) || !labels.length) return []
  const last = labels.length - 1
  const out = []
  for (let index = 0; index < size; index += 1) {
    out.push(String(labels[Math.round((index / (size - 1)) * last)] ?? ""))
  }
  return out
}

function buildIntensitySeries(values) {
  const numbers = resampleSeries(values, SCENE_COLUMNS)
  const peak = Math.max(...numbers, 1)
  const moneyPerUnit = peak / INTENSITY_PEAK
  return {
    intensities: numbers.map((value) => value / moneyPerUnit),
    toMoney: (value) => Number(value || 0) * moneyPerUnit
  }
}

function ensureStage() {
  if (host.promise) return host.promise
  window.__rainformConfig = {
    embedded: true,
    disableAudio: true,
    disableEditor: true,
    disableTuning: true,
    labels: props.labels.slice(),
    formatValue: valueFormat,
    formatTick: tickFormat,
    unitLabel: "元",
    axisTitle: "入库金额",
    axisSubtitle: "每日"
  }
  const stage = document.createElement("div")
  stage.className = "rainform-host"
  stage.style.cssText = HIDDEN_STYLE
  stage.innerHTML = SCENE_MARKUP
  document.body.appendChild(stage)
  host.stage = stage
  host.promise = import("./rainform/main.js")
    .then(() => {
      host.api = window.__rainform
      return stage
    })
    .catch((error) => {
      host.promise = null
      throw error
    })
  return host.promise
}

function pushSeries() {
  if (!host.api || !props.values.length) return
  const { intensities, toMoney } = buildIntensitySeries(props.values)
  host.api.setSeries(intensities, {
    labels: resampleLabels(props.labels, SCENE_COLUMNS),
    formatValue: (value) => valueFormat(toMoney(value)),
    formatTick: (value) => tickFormat(toMoney(value)),
    unitLabel: "元",
    title: "入库金额",
    subtitle: "每日"
  })
}

async function mountScene() {
  try {
    await ensureStage()
  } catch (error) {
    console.error("[RainChart] Rainform 场景加载失败", error)
    failed.value = true
    return
  }
  if (!wrapRef.value || !host.stage) return
  host.stage.style.cssText = ""
  wrapRef.value.appendChild(host.stage)
  host.api?.setActive(true)
  pushSeries()
  if (weather.value?.kind) host.api?.setWeather(weather.value.kind)
  if (pickerVisible.value) {
    let saved = ""
    try {
      saved = sessionStorage.getItem(PREVIEW_KEY) || ""
    } catch {}
    if (saved && KIND_LABEL[saved]) applyKind(saved)
  }
  observer = new ResizeObserver(() => host.api?.resize())
  observer.observe(wrapRef.value)
  ready.value = true
  requestAnimationFrame(() => host.api?.resize())
}

async function loadWeather() {
  if (props.weather) {
    weather.value = props.weather
    return
  }
  const result = await loadCurrentWeather({
    city: props.city,
    latitude: props.latitude,
    longitude: props.longitude
  })
  if (result) weather.value = result
}

onMounted(() => {
  try {
    pickerVisible.value = props.weatherPicker || new URLSearchParams(window.location.search).has("wx")
  } catch {
    pickerVisible.value = props.weatherPicker
  }
  // 窄屏默认折叠成一个胶囊，避免调节器盖住整个雨景。
  pickerOpen.value = pickerVisible.value && window.innerWidth >= 900
  mountScene()
  loadWeather()
})

onBeforeUnmount(() => {
  observer?.disconnect()
  observer = null
  host.api?.setActive(false)
  if (host.stage) {
    host.stage.style.cssText = HIDDEN_STYLE
    if (host.stage.parentElement !== document.body) document.body.appendChild(host.stage)
  }
  ready.value = false
})

watch(
  () => [props.values, props.labels],
  () => {
    if (ready.value) pushSeries()
  }
)

watch(
  () => props.weather,
  (next) => {
    if (!next) return
    weather.value = next
    if (ready.value && next.kind && !previewKind.value) host.api?.setWeather(next.kind)
  }
)

watch(weather, (next) => {
  if (ready.value && next?.kind && !previewKind.value) host.api?.setWeather(next.kind)
})
</script>

<style lang="scss" scoped>
.rain-scene {
  position: absolute;
  inset: 0;
  overflow: hidden;
  border-radius: 6px;
  background: #000;
  isolation: isolate;
}

.rain-scene :deep(canvas) {
  display: block;
  width: 100%;
  height: 100%;
  outline: none;
  touch-action: none;
}

.rain-scene__hint {
  position: absolute;
  right: 14px;
  bottom: 10px;
  z-index: 3;
  margin: 0;
  font-size: 11px;
  letter-spacing: 0.02em;
  color: rgba(250, 251, 255, 0.42);
  pointer-events: none;
  /* 默认淡出，避免和 x 轴末端刻度压在一起；悬停时才提示交互。 */
  opacity: 0;
  transition: opacity 0.3s ease;
}

.rain-scene:hover .rain-scene__hint {
  opacity: 1;
}

.rain-scene__weather {
  position: absolute;
  top: 10px;
  right: 12px;
  z-index: 3;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  border-radius: 999px;
  border: 1px solid rgba(250, 251, 255, 0.12);
  background: rgba(8, 12, 20, 0.62);
  backdrop-filter: blur(10px);
  font-size: 11px;
  line-height: 1.2;
  color: rgba(250, 251, 255, 0.78);
  pointer-events: none;
}

.rain-scene__weather-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #c2cbdb;
  box-shadow: 0 0 8px rgba(194, 203, 219, 0.8);
}

.rain-scene__weather[data-kind="clear"] .rain-scene__weather-dot {
  background: #ffd479;
  box-shadow: 0 0 10px rgba(255, 212, 121, 0.9);
}

.rain-scene__weather[data-kind="snow"] .rain-scene__weather-dot {
  background: #ffffff;
}

.rain-scene__weather[data-kind="fog"] .rain-scene__weather-dot {
  background: #cfd6de;
  box-shadow: 0 0 10px rgba(207, 214, 222, 0.85);
  filter: blur(0.3px);
}

.rain-scene__weather[data-kind="heavy-rain"] .rain-scene__weather-dot {
  background: #5fb0ff;
  box-shadow: 0 0 10px rgba(95, 176, 255, 0.9);
}

.rain-scene__weather[data-kind="storm"] .rain-scene__weather-dot {
  background: #9db7ff;
  box-shadow: 0 0 10px rgba(157, 183, 255, 0.9);
}

.rain-scene__weather-temp {
  color: rgba(250, 251, 255, 0.55);
}

.rain-scene__picker {
  position: absolute;
  left: 12px;
  bottom: 12px;
  z-index: 4;
  width: 226px;
  padding: 10px 11px 9px;
  border-radius: 12px;
  border: 1px solid rgba(250, 251, 255, 0.14);
  background: rgba(6, 9, 15, 0.82);
  backdrop-filter: blur(12px);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.45);
  color: rgba(250, 251, 255, 0.8);
  font-size: 11px;
  line-height: 1.25;
}

.rain-scene__picker-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.rain-scene__picker-head strong {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: rgba(250, 251, 255, 0.92);
}

.rain-scene__picker-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.rain-scene__picker-head button {
  padding: 0;
  border: 0;
  background: none;
  font-size: 11px;
  color: rgba(0, 190, 180, 0.95);
  cursor: pointer;
}

.rain-scene__picker-toggle {
  position: absolute;
  left: 12px;
  bottom: 12px;
  z-index: 4;
  padding: 5px 11px;
  border-radius: 999px;
  border: 1px solid rgba(250, 251, 255, 0.16);
  background: rgba(6, 9, 15, 0.78);
  backdrop-filter: blur(10px);
  font-size: 11px;
  color: rgba(250, 251, 255, 0.8);
  cursor: pointer;
}

.rain-scene__picker-toggle:hover {
  border-color: rgba(0, 190, 180, 0.8);
  color: #e8fffd;
}

.rain-scene__picker-kinds {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 4px;
  margin-bottom: 9px;
}

.rain-scene__picker-kinds button {
  padding: 4px 0;
  border-radius: 6px;
  border: 1px solid rgba(250, 251, 255, 0.12);
  background: rgba(250, 251, 255, 0.05);
  font-size: 11px;
  color: rgba(250, 251, 255, 0.72);
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease, border-color 0.18s ease;
}

.rain-scene__picker-kinds button:hover {
  background: rgba(250, 251, 255, 0.12);
  color: #fff;
}

.rain-scene__picker-kinds button.is-active {
  border-color: rgba(0, 190, 180, 0.9);
  background: rgba(0, 190, 180, 0.22);
  color: #e8fffd;
}

.rain-scene__picker-row {
  display: grid;
  grid-template-columns: 56px 1fr 34px;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  color: rgba(250, 251, 255, 0.62);
}

.rain-scene__picker-row em {
  font-style: normal;
  font-variant-numeric: tabular-nums;
  text-align: right;
  color: rgba(250, 251, 255, 0.82);
}

.rain-scene__picker-row input[type="range"] {
  width: 100%;
  height: 14px;
  margin: 0;
  accent-color: #00beb4;
  cursor: pointer;
}

.rain-scene__fallback {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0;
  font-size: 12px;
  color: rgba(250, 251, 255, 0.6);
  background: #000;
}

@media (max-width: 640px) {
  .rain-scene__hint {
    display: none;
  }

  .rain-scene__weather {
    top: 8px;
    right: 8px;
    padding: 4px 8px;
    font-size: 10px;
  }

  .rain-scene__picker {
    left: 8px;
    bottom: 8px;
    width: 176px;
    padding: 8px 9px 7px;
    font-size: 10px;
  }

  .rain-scene__picker-kinds button,
  .rain-scene__picker-head strong,
  .rain-scene__picker-head button {
    font-size: 10px;
  }

  .rain-scene__picker-row {
    grid-template-columns: 46px 1fr 30px;
    gap: 4px;
  }
}
</style>

<style>
/* Rainform 的舞台在原始项目里铺满整屏，这里改成填满卡片。 */
.rainform-host {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: #000;
  font-family: Inter, "PingFang SC", "Microsoft YaHei", system-ui, sans-serif;
}

.rainform-host .rainfall-dashboard {
  width: 100%;
  height: 100%;
}

.rainform-host .scene-root {
  width: 100%;
  height: 100%;
}

/* 原始项目的悬浮读数在卡片里会跟右上角读数冲突，隐藏掉。 */
.rainform-host .selection-readout {
  display: none !important;
}
</style>
