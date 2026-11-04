<template>
  <div
    ref="blockRef"
    class="badge-block"
    :class="{ 'is-flipping': flipping, 'is-interactive': interactive, 'is-dragging': dragging }"
    @pointerdown="onPointerDown"
    @pointermove="onPointerMove"
    @pointerup="onPointerUp"
    @pointercancel="onPointerUp"
    @pointerleave="onPointerLeave"
  >
    <span class="badge__contact" aria-hidden="true" />
    <span class="badge__shadow" aria-hidden="true" />
    <div class="badge__tilt" :class="{ 'is-tilting': sheen.on }" :style="tiltStyle">
      <div
        ref="cardRef"
        class="badge"
        :class="{ 'is-flipped': flipped }"
        :style="styleVars"
        @transitionend="onTransitionEnd"
      >
        <span
          v-for="z in edgeLayers"
          :key="z"
          class="badge__edge"
          :style="{ transform: 'translateZ(' + z + 'px)', filter: 'brightness(' + (0.56 + ((z + 5.5) / 11) * 0.44).toFixed(3) + ')' }"
          aria-hidden="true"
        />

        <section ref="frontRef" class="badge__face badge__face--front">
          <canvas ref="washRef" class="badge__wash" width="720" height="454" aria-hidden="true" />
          <span class="badge__holo" aria-hidden="true" />
          <span class="badge__grain" aria-hidden="true" />
          <span class="badge__frost" aria-hidden="true" />
          <span class="badge__scrim" aria-hidden="true" />
          <button
            v-if="interactive || sheetOpen"
            type="button"
            class="badge__slot badge__slot--toggle"
            :class="{ 'is-open': sheetOpen }"
            :aria-expanded="sheetOpen ? 'true' : 'false'"
            :aria-label="sheetOpen ? '收起输入面板' : '展开输入面板'"
            @pointerdown.stop
            @click.stop="emit('toggle-sheet')"
          >
            <span class="badge__slot-bar" aria-hidden="true" />
            <svg class="badge__slot-arrow" :class="{ 'is-up': sheetOpen }" viewBox="0 0 10 6" aria-hidden="true">
              <path
                d="M1 1.2 5 5.2 9 1.2"
                fill="none"
                stroke="currentColor"
                stroke-width="1.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </button>
          <span
            class="badge__focus"
            :class="{ 'is-visible': frontFrame.visible }"
            :style="frontFrameStyle"
            aria-hidden="true"
          />

          <div class="badge__body">
            <header class="badge__head">
              <span class="badge__logo" aria-hidden="true">
                <i class="badge__logo-base" />
              </span>
              <span class="badge__kind">员工工卡</span>
            </header>

            <div ref="digitsRef" class="badge__hero">
              <span class="badge__sr">{{ phoneAria }}</span>
              <span class="badge__digits" aria-hidden="true">
                <span
                  v-for="(slot, index) in phoneSlots"
                  :key="index"
                  class="badge__digit"
                  :class="{ 'is-filled': slot.filled, 'is-break': index === 2 || index === 6 }"
                >
                  <span class="badge__digit-col">
                    <span class="badge__digit-row badge__digit-ghost" data-char="•">•</span>
                    <span class="badge__digit-row" :data-char="slot.char">{{ slot.char }}</span>
                  </span>
                </span>
              </span>
            </div>

            <footer class="badge__foot">
              <div class="badge__person">
                <div ref="whoRef" class="badge__field badge__field--who">
                  <span class="badge__label">持卡人</span>
                  <span class="badge__name">{{ name || "未填写" }}</span>
                </div>
                <span ref="sexRef" class="badge__sex">{{ sexLabel }}</span>
              </div>
              <div ref="mailRef" class="badge__field badge__field--mail">
                <span class="badge__label">邮箱</span>
                <span class="badge__mail-value">{{ email || "未填写" }}</span>
              </div>
            </footer>
          </div>
          <span class="badge__sheen" aria-hidden="true" />
        </section>

        <section ref="backRef" class="badge__face badge__face--back">
          <canvas ref="washBackRef" class="badge__wash" width="720" height="454" aria-hidden="true" />
          <span class="badge__holo" aria-hidden="true" />
          <span class="badge__grain" aria-hidden="true" />
          <span class="badge__scrim" aria-hidden="true" />
          <button
            v-if="interactive || sheetOpen"
            type="button"
            class="badge__slot badge__slot--toggle"
            :class="{ 'is-open': sheetOpen }"
            :aria-expanded="sheetOpen ? 'true' : 'false'"
            :aria-label="sheetOpen ? '收起输入面板' : '展开输入面板'"
            @pointerdown.stop
            @click.stop="emit('toggle-sheet')"
          >
            <span class="badge__slot-bar" aria-hidden="true" />
            <svg class="badge__slot-arrow" :class="{ 'is-up': sheetOpen }" viewBox="0 0 10 6" aria-hidden="true">
              <path
                d="M1 1.2 5 5.2 9 1.2"
                fill="none"
                stroke="currentColor"
                stroke-width="1.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </button>
          <span
            class="badge__focus"
            :class="{ 'is-visible': backFrame.visible }"
            :style="backFrameStyle"
            aria-hidden="true"
          />

          <div class="badge__body badge__body--back">
            <header class="badge__head">
              <span class="badge__logo" aria-hidden="true">
                <i class="badge__logo-base" />
              </span>
              <span class="badge__kind">密码</span>
            </header>

            <div ref="secretRef" class="badge__secret">
              <span class="badge__label">{{ secretLabel }}</span>
              <span class="badge__secret-dots" :class="{ 'is-empty': !secretLength }">{{ secretDots }}</span>
            </div>

            <p class="badge__hint">密码只在输入框中显示为圆点，不会写在卡片上</p>
          </div>
          <span class="badge__sheen" aria-hidden="true" />
        </section>
      </div>
    </div>
  </div>
</template>

<script setup name="EmployeeBadge">
import discMask from "@/assets/logo/badge-disc.png"
import swirlMask from "@/assets/logo/badge-swirl.png"
import { paletteOf, paintWash, washTone } from "./badgeArt"

const props = defineProps({
  name: { type: String, default: "" },
  phone: { type: String, default: "" },
  email: { type: String, default: "" },
  sex: { type: String, default: "" },
  palette: { type: String, default: "aurora" },
  /** 翻转：密码修改时展示背面 */
  flipped: { type: Boolean, default: false },
  /** 当前聚焦字段：phone / holder / sex / email / secret */
  activeField: { type: String, default: "" },
  /** 当前正在输入的密码字段名（旧密码 / 新密码 / 确认密码） */
  secretLabel: { type: String, default: "新密码" },
  /** 当前输入的密码位数，用于背面圆点 */
  secretLength: { type: Number, default: 0 },
  /** 闲置态：卡片可以随意拖动旋转、点击翻看另一面 */
  interactive: { type: Boolean, default: false },
  /** 表单面板是否展开，卡片上的按钮据此切换文案与箭头方向 */
  sheetOpen: { type: Boolean, default: false }
})

const emit = defineEmits(["toggle-sheet"])

const blockRef = ref(null)
const washRef = ref(null)
const washBackRef = ref(null)
const cardRef = ref(null)
const frontRef = ref(null)
const backRef = ref(null)
const digitsRef = ref(null)
const whoRef = ref(null)
const sexRef = ref(null)
const mailRef = ref(null)
const secretRef = ref(null)

const meta = computed(() => paletteOf(props.palette))

/* 卡片按银行卡的真实尺寸做：85.6×53.98mm，比例 1.586，厚 0.76mm ≈ 5px（560px 宽时）。
   厚度用一叠 translateZ 的圆角薄片堆出来，正反面各贴在自己那一侧。 */
/* 卡芯切片：11 层已经够撑出 5.5px 的厚度，层数再多只是给 GPU 加活 */
const EDGE_STEPS = Array.from({ length: 12 }, (_, i) => Number((-5.5 + i).toFixed(1)))
const edgeLayers = EDGE_STEPS

/** 静止姿态：像握在手里的实物卡，微微侧着、露出一点侧面厚度。 */
const REST = { x: 10, y: -14 }
/** 指针悬停的 3D 倾斜：鼠标在哪，卡片哪一角就朝观者抬起。 */
const tilt = ref({ ...REST })
const sheen = ref({ x: 26, y: 15, on: false })
/** 翻面过程：投影跟着卡片一起“收窄再展开”，不是死在地上的一块 */
const flipping = ref(false)
/** 闲置态的“手感”：拖出来的姿态独立于指针悬停，松手后不弹回，可以一直转着看 */
const spin = ref({ x: 0, y: 0 })
const dragging = ref(false)
let grab = null
let flipTimer = 0
let pointer = null
let tiltFrame = 0

const clamp = (value, min, max) => Math.min(max, Math.max(min, value))

/** 卡片最终姿态 = 指针悬停的微倾斜 + 用户自己拖出来的旋转 */
const pose = computed(() => ({
  x: tilt.value.x + spin.value.x,
  y: tilt.value.y + spin.value.y
}))

const canTilt = () => {
  if (typeof window === "undefined" || !window.matchMedia) return false
  return (
    window.matchMedia("(hover: hover) and (pointer: fine)").matches &&
    !window.matchMedia("(prefers-reduced-motion: reduce)").matches
  )
}

const applyTilt = () => {
  tiltFrame = 0
  if (!pointer || !blockRef.value) return
  const rect = blockRef.value.getBoundingClientRect()
  if (!rect.width || !rect.height) return
  const nx = Math.max(-1, Math.min(1, ((pointer.x - rect.left) / rect.width) * 2 - 1))
  const ny = Math.max(-1, Math.min(1, ((pointer.y - rect.top) / rect.height) * 2 - 1))
  tilt.value = { x: REST.x - ny * 6, y: REST.y + nx * 8 }
  sheen.value = { x: ((nx + 1) / 2) * 100, y: ((ny + 1) / 2) * 100, on: true }
}

/** 按住拖动：横向转过头、纵向点头，像在手里翻看一张实体卡 */
const onPointerDown = event => {
  if (!props.interactive || event.button !== 0) return
  grab = {
    id: event.pointerId,
    x: event.clientX,
    y: event.clientY,
    baseX: spin.value.x,
    baseY: spin.value.y,
    travel: 0
  }
  dragging.value = true
  pointer = null
  if (blockRef.value && blockRef.value.setPointerCapture) {
    blockRef.value.setPointerCapture(event.pointerId)
  }
}

const trackDrag = event => {
  const dx = event.clientX - grab.x
  const dy = event.clientY - grab.y
  grab.travel = Math.max(grab.travel, Math.abs(dx) + Math.abs(dy))
  spin.value = {
    x: clamp(grab.baseX - dy * 0.32, -42, 42),
    y: grab.baseY + dx * 0.42
  }
  sheen.value = {
    x: clamp(50 + (spin.value.y % 180) * 0.28, 0, 100),
    y: clamp(50 + (spin.value.x % 90) * 0.55, 0, 100),
    on: true
  }
}

const onPointerUp = event => {
  if (!grab) return
  const wasClick = grab.travel < 6
  if (blockRef.value && blockRef.value.releasePointerCapture && event.pointerId !== undefined) {
    try {
      blockRef.value.releasePointerCapture(grab.id)
    } catch (e) {
      // 指针已经离开，忽略
    }
  }
  grab = null
  dragging.value = false
  sheen.value = { x: 26, y: 15, on: false }
  /* 没怎么移动就当成一次点击：翻到另一面 */
  if (wasClick && props.interactive) spin.value = { ...spin.value, y: spin.value.y + 180 }
  /* 松手后纵向姿态自己回正，横向停在你转到的角度 */
  else spin.value = { ...spin.value, x: 0 }
}

const onPointerMove = event => {
  if (grab) {
    trackDrag(event)
    return
  }
  if (event.pointerType === "touch" || !canTilt()) return
  pointer = { x: event.clientX, y: event.clientY }
  if (!tiltFrame) tiltFrame = window.requestAnimationFrame(applyTilt)
}

const onPointerLeave = () => {
  pointer = null
  if (tiltFrame) {
    window.cancelAnimationFrame(tiltFrame)
    tiltFrame = 0
  }
  if (grab) return
  tilt.value = { ...REST }
  sheen.value = { x: 26, y: 15, on: false }
}

const tiltStyle = computed(() => ({
  "--tilt-x": pose.value.x.toFixed(2) + "deg",
  "--tilt-y": pose.value.y.toFixed(2) + "deg",
  "--sheen-x": sheen.value.x.toFixed(1) + "%",
  "--sheen-y": sheen.value.y.toFixed(1) + "%",
  "--sheen-o": sheen.value.on ? 1 : 0.34,
  "--holo-angle": (118 + tilt.value.y * 2.2).toFixed(1) + "deg",
  /* 凸字的受光方向：指针移到哪边，字面的亮面就转到哪边，像有一束光在卡面上扫过 */
  "--emboss-angle": (180 + (sheen.value.x - 50) * 1.5).toFixed(1) + "deg",
  /* 高光边跟着光源走：光在左，亮边就压在字的左侧 */
  "--emboss-rim-shadow":
    ((sheen.value.x - 50) * 0.05).toFixed(2) +
    "px " +
    (((sheen.value.y - 50) * 0.05) - 1).toFixed(2) +
    "px 0 rgba(255, 255, 255, 0.85)",
  /* 灯光在左上方：卡片越往右转，影子越往右移，卡片才有“站住”的感觉 */
  "--shadow-x": clamp(pose.value.y * -0.7, -30, 30).toFixed(2) + "px",
  "--shadow-y": (10 + pose.value.x * 0.6).toFixed(2) + "px",
  /* 侧过 90° 时卡面几乎不吃地，影子跟着收窄 */
  "--shadow-sx": (0.55 + 0.4 * Math.abs(Math.cos((pose.value.y * Math.PI) / 180))).toFixed(3),
  transform: "rotateX(" + pose.value.x.toFixed(2) + "deg) rotateY(" + pose.value.y.toFixed(2) + "deg)"
}))

const toRgba = (hex, alpha) => {
  const h = String(hex).replace("#", "")
  const full = h.length === 3 ? h[0] + h[0] + h[1] + h[1] + h[2] + h[2] : h
  const n = parseInt(full, 16)
  return "rgba(" + ((n >> 16) & 255) + ", " + ((n >> 8) & 255) + ", " + (n & 255) + ", " + alpha + ")"
}

const hexToInt = hex => {
  const h = String(hex).replace("#", "")
  const full = h.length === 3 ? h[0] + h[0] + h[1] + h[1] + h[2] + h[2] : h
  return parseInt(full, 16)
}

const hexToHsl = hex => {
  const n = hexToInt(hex)
  const r = ((n >> 16) & 255) / 255
  const g = ((n >> 8) & 255) / 255
  const b = (n & 255) / 255
  const max = Math.max(r, g, b)
  const min = Math.min(r, g, b)
  const l = (max + min) / 2
  const d = max - min
  if (!d) return { h: 0, s: 0, l }
  const s = l > 0.5 ? d / (2 - max - min) : d / (max + min)
  let h
  if (max === r) h = ((g - b) / d + (g < b ? 6 : 0)) / 6
  else if (max === g) h = ((b - r) / d + 2) / 6
  else h = ((r - g) / d + 4) / 6
  return { h: h * 360, s, l }
}

const hslToHex = (h, s, l) => {
  const hue = ((h % 360) + 360) % 360 / 360
  const sat = Math.min(1, Math.max(0, s))
  const lum = Math.min(1, Math.max(0, l))
  const q = lum < 0.5 ? lum * (1 + sat) : lum + sat - lum * sat
  const p = 2 * lum - q
  const conv = t => {
    let x = t
    if (x < 0) x += 1
    if (x > 1) x -= 1
    if (x < 1 / 6) return p + (q - p) * 6 * x
    if (x < 1 / 2) return q
    if (x < 2 / 3) return p + (q - p) * (2 / 3 - x) * 6
    return p
  }
  const ch = [conv(hue + 1 / 3), conv(hue), conv(hue - 1 / 3)].map(v =>
    Math.round(v * 255).toString(16).padStart(2, "0")
  )
  return "#" + ch.join("")
}


const relativeLum = hex => {
  const h = String(hex).replace("#", "")
  const full = h.length === 3 ? h[0] + h[0] + h[1] + h[1] + h[2] + h[2] : h
  const n = parseInt(full, 16)
  const ch = [(n >> 16) & 255, (n >> 8) & 255, n & 255].map(v => {
    const s = v / 255
    return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4)
  })
  return 0.2126 * ch[0] + 0.7152 * ch[1] + 0.0722 * ch[2]
}

const contrastRatio = (a, b) => {
  const hi = Math.max(a, b) + 0.05
  const lo = Math.min(a, b) + 0.05
  return hi / lo
}

const INK_ON_DARK = "#f9f6f1"
const INK_ON_LIGHT = "#171316"
const artLum = ref(0.2)
/** 卡面实际平均色相：logo 取同一个色相，避免和卡面颜色差太多。 */
const artHue = ref(null)

const look = computed(() => {
  const p = meta.value
  const lum = artLum.value
  let ink = p.ink
  if (contrastRatio(relativeLum(ink), lum) < 4) {
    ink = lum < 0.42 ? INK_ON_DARK : INK_ON_LIGHT
  }
  return { ink, inkLight: relativeLum(ink) > 0.5, accent: p.accent }
})

const styleVars = computed(() => {
  const { ink, inkLight, accent } = look.value
  const accentLight = relativeLum(accent) > 0.5
  return {
    "--badge-ink": ink,
    "--badge-ink-soft": toRgba(ink, 0.78),
    "--badge-ink-faint": toRgba(ink, 0.62),
    "--badge-accent": accent,
    "--badge-accent-ink": accentLight ? "#1b1712" : "#ffffff",
    "--badge-chip": inkLight ? "rgba(255, 255, 255, 0.14)" : "rgba(255, 255, 255, 0.56)",
    "--badge-hair": toRgba(ink, inkLight ? 0.26 : 0.22),
    "--badge-slot": inkLight ? "rgba(0, 0, 0, 0.46)" : "rgba(26, 20, 24, 0.3)",
    "--badge-scrim": inkLight ? "rgba(6, 8, 12, 0.7)" : "rgba(255, 255, 255, 0.66)",
    "--badge-scrim-soft": inkLight ? "rgba(6, 8, 12, 0.32)" : "rgba(255, 255, 255, 0.36)",
    "--badge-veil": inkLight ? "rgba(0, 0, 0, 0.6)" : "rgba(255, 255, 255, 0.6)",
    "--badge-empty": toRgba(ink, 0.34),
    "--badge-secret": inkLight ? "rgba(255, 255, 255, 0.16)" : "rgba(255, 255, 255, 0.62)",
    /* 卡芯（侧面）颜色：真实卡片是白色 PVC 芯 + 印刷面，所以侧面始终偏浅，
       深色卡上才会露出一道浅色塑料边，才看得出厚度 */
    /* 凸字：卡面上的字像压出来的一层塑料。顶边一条高光，下面用 5 层 1px 的实心
       暗影叠出侧壁，再拖一层接触影和一层散影，侧壁才不会糊成一条淡边。 */
    "--emboss": (
      inkLight
        ? [
            "var(--emboss-rim-shadow, 0 -1px 0 rgba(255, 255, 255, 0.85))",
            "0 1px 0 rgba(0, 0, 0, 0.5)",
            "0 2px 0 rgba(0, 0, 0, 0.46)",
            "0 3px 0 rgba(0, 0, 0, 0.4)",
            "0 4px 0 rgba(0, 0, 0, 0.34)",
            "0 5px 0 rgba(0, 0, 0, 0.26)",
            "0 7px 4px rgba(0, 0, 0, 0.28)",
            "0 11px 12px rgba(0, 0, 0, 0.24)",
            "0 18px 22px rgba(0, 0, 0, 0.18)"
          ]
        : [
            "0 -1px 0 rgba(255, 255, 255, 0.92)",
            "0 1px 0 rgba(0, 0, 0, 0.32)",
            "0 2px 0 rgba(0, 0, 0, 0.28)",
            "0 3px 0 rgba(0, 0, 0, 0.24)",
            "0 4px 0 rgba(0, 0, 0, 0.2)",
            "0 6px 4px rgba(0, 0, 0, 0.24)"
          ]
    ).join(", "),
    "--emboss-soft": (
      inkLight
        ? [
            "0 -1px 0 rgba(255, 255, 255, 0.5)",
            "0 1px 0 rgba(0, 0, 0, 0.42)",
            "0 2px 0 rgba(0, 0, 0, 0.28)"
          ]
        : [
            "0 -1px 0 rgba(255, 255, 255, 0.6)",
            "0 1px 0 rgba(0, 0, 0, 0.2)"
          ]
    ).join(", "),
    /* 小字号（邮箱）：侧壁要薄一半，不然 11px 的字会被自己压出的阴影糊掉 */
    "--emboss-sm": (
      inkLight
        ? [
            "0 -1px 0 rgba(255, 255, 255, 0.4)",
            "0 1px 0 rgba(0, 0, 0, 0.42)",
            "0 2px 0 rgba(0, 0, 0, 0.3)",
            "0 3px 2px rgba(0, 0, 0, 0.22)"
          ]
        : [
            "0 -1px 0 rgba(255, 255, 255, 0.7)",
            "0 1px 0 rgba(0, 0, 0, 0.26)",
            "0 2px 2px rgba(0, 0, 0, 0.2)"
          ]
    ).join(", "),
    "--emboss-face": inkLight
      ? "linear-gradient(var(--emboss-angle, 180deg), #ffffff 0%, #fdf8f1 22%, #ecdfcc 52%, #c3ad92 78%, #94806a 100%)"
      : "linear-gradient(var(--emboss-angle, 180deg), #4a4139 0%, #3b332c 58%, #241f1b 100%)",
    "--emboss-rim": inkLight ? "rgba(255, 255, 255, 0.55)" : "rgba(255, 255, 255, 0.5)",
    "--badge-edge": hslToHex(
      artHue.value === null ? hexToHsl(accent).h : artHue.value,
      0.16,
      inkLight ? 0.42 : 0.74
    ),
    ...logoVars.value
  }
})

/** logo 只用 logo 的白色圆盘：条纹镂空，盘面取卡面同色相的低饱和玻璃色。 */
const logoVars = computed(() => {
  const p = meta.value
  const angle = ((((p.angle * 180) / Math.PI) % 360) + 360) % 360
  // 同族配色：直接取卡面实际色相（画完再采样），只抬一点饱和度和明暗层次
  const baseHue = artHue.value === null ? hexToHsl(p.accent).h : artHue.value
  // 饱和度压到很低，明度只比卡面抬/压一点点：像同色系的磨砂玻璃，不抢卡面
  const f = p.light ? 0.13 : 0.16
  const lum = artLum.value
  const l0 = Math.min(0.7, Math.max(0.38, lum > 0.6 ? lum - 0.18 : lum + 0.16))
  const at = v => Math.min(0.82, Math.max(0.06, v))
  return {
    "--badge-logo-angle": angle.toFixed(1) + "deg",
    "--badge-logo-a": hslToHex(baseHue - 14, f, at(l0 - 0.05)),
    "--badge-logo-b": hslToHex(baseHue + 8, f * 1.15, at(l0 + 0.06)),
    "--badge-logo-c": hslToHex(baseHue + 24, f * 0.9, at(l0 - 0.01)),
    "--badge-logo-d": hslToHex(baseHue + 2, f, at(l0 + 0.1)),
    "--badge-logo-disc": 'url("' + discMask + '")',
    "--badge-logo-swirl": 'url("' + swirlMask + '")'
  }
})

const sexLabel = computed(() => {
  if (props.sex === "0") return "男"
  if (props.sex === "1") return "女"
  return "—"
})

const phoneAria = computed(() => {
  const raw = String(props.phone || "").trim()
  return raw ? "手机号 " + raw : "手机号未填写"
})

/** 手机号按 3-4-4 拆成 11 个格子，填一位就翻上去一位。 */
const phoneSlots = computed(() => {
  const digits = String(props.phone || "").replace(/\D/g, "").slice(0, 11)
  return Array.from({ length: 11 }, (_, i) => ({
    char: digits[i] || "",
    filled: i < digits.length
  }))
})

const secretDots = computed(() => {
  const n = Math.min(Math.max(0, Number(props.secretLength) || 0), 20)
  return n ? "•".repeat(n) : "待输入"
})

/** 聚焦提示框：跟着当前字段走，位置按卡片实际排版量出来，不写死像素 */
const frontFrame = ref({ visible: false, left: 0, top: 0, width: 0, height: 0 })
const backFrame = ref({ visible: false, left: 0, top: 0, width: 0, height: 0 })

const frameStyle = frame =>
  "left:" + frame.left.toFixed(1) + "px;top:" + frame.top.toFixed(1) + "px;width:" + frame.width.toFixed(1) + "px;height:" + frame.height.toFixed(1) + "px"

const frontFrameStyle = computed(() => frameStyle(frontFrame.value))
const backFrameStyle = computed(() => frameStyle(backFrame.value))

/** 元素在卡面里的排版坐标：卡片是 3D 倾斜的，getBoundingClientRect 是投影后的屏幕坐标，
    直接拿来定位会让提示框跟着倾斜一起错位，所以沿 offsetParent 累加布局坐标。 */
const localRectOf = (node, faceEl) => {
  if (!node || !faceEl) return null
  let x = 0
  let y = 0
  let el = node
  while (el && el !== faceEl) {
    x += el.offsetLeft
    y += el.offsetTop
    el = el.offsetParent
  }
  if (el !== faceEl || !node.offsetWidth || !node.offsetHeight) return null
  return { left: x, top: y, right: x + node.offsetWidth, bottom: y + node.offsetHeight }
}

/** 只量内容范围（有 selector 时按子节点取并集），避免把内边距也算进框里 */
const boundsOf = (el, selector, faceEl) => {
  if (!el) return null
  const nodes = selector ? Array.from(el.querySelectorAll(selector)) : []
  const rects = (nodes.length ? nodes : [el]).map(node => localRectOf(node, faceEl)).filter(Boolean)
  if (!rects.length) return null
  return {
    left: Math.min(...rects.map(rect => rect.left)),
    right: Math.max(...rects.map(rect => rect.right)),
    top: Math.min(...rects.map(rect => rect.top)),
    bottom: Math.max(...rects.map(rect => rect.bottom))
  }
}

/** 内容范围换算成卡片内的框；limitRight 用来避让右边的性别胶囊 */
const frameOf = (faceEl, bounds, padX, padY, limitRight) => {
  if (!faceEl || !bounds) return null
  const faceW = faceEl.offsetWidth
  const faceH = faceEl.offsetHeight
  if (!faceW) return null
  let left = bounds.left - padX
  let right = bounds.right + padX
  if (limitRight != null) right = Math.min(right, limitRight)
  left = Math.max(5, left)
  right = Math.min(faceW - 5, right)
  const top = Math.max(5, bounds.top - padY)
  const bottom = Math.min(faceH - 5, bounds.bottom + padY)
  if (right - left < 8 || bottom - top < 8) return null
  return { visible: true, left, top, width: right - left, height: bottom - top }
}

/** 每个字段的框：手机号只框住有数字的格子，其余按内容实测 */
const FIELD_FRAMES = {
  phone: () => [
    digitsRef.value,
    digitsRef.value && digitsRef.value.querySelector(".badge__digit.is-filled") ? ".badge__digit.is-filled" : ".badge__digits",
    5,
    4
  ],
  holder: () => [whoRef.value, null, 8, 6],
  sex: () => [sexRef.value, null, 6, 5],
  email: () => [mailRef.value, null, 8, 6]
}

const HIDDEN = { visible: false, left: 0, top: 0, width: 0, height: 0 }

const measure = () => {
  const field = props.activeField
  frontFrame.value = HIDDEN
  backFrame.value = HIDDEN
  if (!field) return
  if (field === "secret") {
    if (!props.flipped) return
    const frame = frameOf(backRef.value, boundsOf(secretRef.value, null, backRef.value), 9, 8)
    if (frame) backFrame.value = frame
    return
  }
  if (props.flipped) return
  const config = FIELD_FRAMES[field]
  if (!config) return
  const [el, selector, padX, padY] = config()
  const chip = field === "holder" ? localRectOf(sexRef.value, frontRef.value) : null
  const limitRight = chip ? chip.left - 6 : null
  const frame = frameOf(frontRef.value, boundsOf(el, selector, frontRef.value), padX, padY, limitRight)
  if (frame) frontFrame.value = frame
}

const remeasure = () => {
  nextTick(() => {
    window.requestAnimationFrame(measure)
  })
}

watch(() => [props.activeField, props.flipped, props.name, props.phone, props.email, props.sex], remeasure)

let observer = null

const paintCard = () => {
  if (washRef.value) paintWash(washRef.value, meta.value, 7)
  if (washBackRef.value) paintWash(washBackRef.value, meta.value, 13)
  if (washRef.value) {
    // 取整张卡面的平均色相：局部采样会把冷暖混出偏色，整卡平均更贴合观感
    const tone = washTone(washRef.value)
    artLum.value = tone.lum
    artHue.value = tone.sat > 0.03 ? tone.hue : null
  }
}

watch(meta, () => paintCard())

onMounted(() => {
  paintCard()
  if (typeof ResizeObserver !== "undefined" && cardRef.value) {
    observer = new ResizeObserver(() => measure())
    observer.observe(cardRef.value)
  }
  remeasure()
})

onBeforeUnmount(() => {
  if (observer) observer.disconnect()
})

watch(
  () => props.flipped,
  () => {
    flipping.value = true
    window.clearTimeout(flipTimer)
    flipTimer = window.setTimeout(() => {
      flipping.value = false
    }, 820)
  }
)

watch(
  () => props.interactive,
  value => {
    /* 表单展开时把卡片交还给翻面动画，先归位 */
    if (!value) spin.value = { x: 0, y: 0 }
  }
)

onBeforeUnmount(() => window.clearTimeout(flipTimer))

const onTransitionEnd = event => {
  if (event.propertyName !== "transform") return
  remeasure()
}
</script>

<style lang="scss" scoped>
.badge-block {
  position: relative;
  width: 100%;
  max-width: 560px;
  margin: 0 auto;
  /* 透视放在外层，倾斜和翻转才有真实的景深 */
  perspective: 1150px;
  perspective-origin: 50% 48%;
}

/* 倾斜层：指针在卡片上移动时，卡面像握在手里的塑料卡一样跟着转 */
.badge__tilt {
  position: relative;
  z-index: 1;
  transform-style: preserve-3d;
  transform: rotateX(var(--tilt-x, 0deg)) rotateY(var(--tilt-y, 0deg));
  transition: transform 0.5s cubic-bezier(0.22, 0.7, 0.25, 1);
  will-change: transform;
}

/* 指针跟随时用短过渡，既顺滑又不拖手 */
.badge__tilt.is-tilting {
  transition: transform 0.12s linear;
}

/* 闲置态：一张可以抓在手里的卡 */
.badge-block.is-interactive {
  cursor: grab;
  touch-action: none;
  -webkit-user-select: none;
  user-select: none;
}

.badge-block.is-interactive.is-dragging {
  cursor: grabbing;
}

/* 拖动时不要过渡，手指到哪卡片到哪 */
.badge-block.is-dragging .badge__tilt,
.badge-block.is-dragging .badge__shadow,
.badge-block.is-dragging .badge__contact {
  transition: none;
}

/* 两层影子：远影负责“飘着”的体积，近影是卡片压住桌面的接触阴影。
   两层都跟着倾斜量挪位置，翻面时一起收窄再铺开，影子就不会是一块死黑。 */
.badge__shadow {
  position: absolute;
  inset: 5% 1% -13%;
  border-radius: 32px;
  background: radial-gradient(115% 125% at 50% 34%, rgba(0, 0, 0, 0.86) 0%, rgba(0, 0, 0, 0.52) 56%, rgba(0, 0, 0, 0) 100%);
  filter: blur(26px);
  transform: translate3d(var(--shadow-x, 0px), calc(var(--shadow-y, 10px) + 2px), 0) scale(var(--shadow-sx, 0.95));
  opacity: 0.95;
  z-index: 0;
  pointer-events: none;
  transition:
    transform 0.5s cubic-bezier(0.22, 0.7, 0.25, 1),
    opacity 0.5s ease;
  will-change: transform, opacity;
}

.badge__contact {
  position: absolute;
  inset: 14% 10% -2%;
  border-radius: 24px;
  background: rgba(0, 0, 0, 0.62);
  filter: blur(12px);
  transform: translate3d(calc(var(--shadow-x, 0px) * 0.45), calc(var(--shadow-y, 10px) * 0.55), 0) scale(0.9);
  opacity: 0.5;
  z-index: 0;
  pointer-events: none;
  transition:
    transform 0.5s cubic-bezier(0.22, 0.7, 0.25, 1),
    opacity 0.5s ease;
  will-change: transform, opacity;
}

/* 翻面时卡片立成一条线，两块影子跟着收窄、抬离桌面，落回时再铺开。
   关键帧时间轴用 linear，落点按卡片 transform 的 cubic-bezier(0.2, 0.75, 0.2, 1)
   解出来的真实进度排，影子才会跟卡片同进同出；影子只收窄到 0.36 并同时变虚，
   不会像纸片一样突然拍扁。 */
.badge-block.is-flipping .badge__shadow {
  animation: badge-flip-shadow 0.8s linear both;
}

.badge-block.is-flipping .badge__contact {
  animation: badge-flip-contact 0.8s linear both;
}

@keyframes badge-flip-shadow {
  0% {
    transform: translate3d(var(--shadow-x, 0px), calc(var(--shadow-y, 10px) + 2px), 0) scale(0.95);
    opacity: 0.95;
  }
  8% {
    transform: translate3d(calc(var(--shadow-x, 0px) + 3px), calc(var(--shadow-y, 10px) + 3px), 0) scale(0.93, 0.95);
    opacity: 0.88;
  }
  16% {
    transform: translate3d(calc(var(--shadow-x, 0px) + 6px), calc(var(--shadow-y, 10px) + 4px), 0) scale(0.9, 0.94);
    opacity: 0.78;
  }
  24% {
    transform: translate3d(calc(var(--shadow-x, 0px) + 4px), calc(var(--shadow-y, 10px) + 3px), 0) scale(0.92, 0.95);
    opacity: 0.86;
  }
  32% {
    transform: translate3d(calc(var(--shadow-x, 0px) + 2px), calc(var(--shadow-y, 10px) + 2px), 0) scale(0.94, 0.95);
    opacity: 0.92;
  }
  48% {
    transform: translate3d(calc(var(--shadow-x, 0px) + 1px), calc(var(--shadow-y, 10px) + 2px), 0) scale(0.95);
    opacity: 0.94;
  }
  64%,
  100% {
    transform: translate3d(var(--shadow-x, 0px), calc(var(--shadow-y, 10px) + 2px), 0) scale(0.95);
    opacity: 0.95;
  }
}

@keyframes badge-flip-contact {
  0% {
    transform: translate3d(calc(var(--shadow-x, 0px) * 0.45), calc(var(--shadow-y, 10px) * 0.55), 0) scale(0.9);
    opacity: 0.5;
  }
  8% {
    transform: translate3d(calc(var(--shadow-x, 0px) * 0.45 - 2px), calc(var(--shadow-y, 10px) * 0.55 + 3px), 0) scale(0.78, 0.88);
    opacity: 0.42;
  }
  16% {
    transform: translate3d(calc(var(--shadow-x, 0px) * 0.45 - 4px), calc(var(--shadow-y, 10px) * 0.55 + 5px), 0) scale(0.62, 0.84);
    opacity: 0.3;
  }
  24% {
    transform: translate3d(calc(var(--shadow-x, 0px) * 0.45 - 3px), calc(var(--shadow-y, 10px) * 0.55 + 3px), 0) scale(0.78, 0.88);
    opacity: 0.42;
  }
  32% {
    transform: translate3d(calc(var(--shadow-x, 0px) * 0.45 - 1px), calc(var(--shadow-y, 10px) * 0.55 + 1px), 0) scale(0.86, 0.89);
    opacity: 0.47;
  }
  48%,
  100% {
    transform: translate3d(calc(var(--shadow-x, 0px) * 0.45), calc(var(--shadow-y, 10px) * 0.55), 0) scale(0.9);
    opacity: 0.5;
  }
}

.badge {
  --badge-half: 6px;
  position: relative;
  width: 100%;
  /* 银行卡真实比例 85.6 : 53.98 */
  aspect-ratio: 856 / 540;
  border-radius: 21px;
  transform-style: preserve-3d;
  transition: transform 0.8s cubic-bezier(0.2, 0.75, 0.2, 1);
}

.badge.is-flipped {
  transform: rotateY(180deg);
}

/* 卡芯：一叠圆角薄片堆出 5px 厚度，边缘才会像真的塑料卡一样立起来 */
.badge__edge {
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: var(--badge-edge);
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
}

.badge__face {
  position: absolute;
  inset: 0;
  border-radius: inherit;
  overflow: hidden;
  isolation: isolate;
  color: var(--badge-ink);
  background: #15131c;
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
  box-shadow:
    0 2px 10px -4px rgba(0, 0, 0, 0.55),
    inset 0 1px 0 rgba(255, 255, 255, 0.32),
    inset 0 0 0 1px rgba(255, 255, 255, 0.12);
}

.badge__face--front {
  transform: translateZ(var(--badge-half));
}

/* 倒角：四条边各自吃到一点光，侧棱才不会像贴纸 */
.badge__face::after {
  content: "";
  position: absolute;
  inset: 0;
  z-index: 7;
  border-radius: inherit;
  pointer-events: none;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.3),
    inset 0 -1px 0 rgba(0, 0, 0, 0.4),
    inset 1px 0 0 rgba(255, 255, 255, 0.1),
    inset -1px 0 0 rgba(0, 0, 0, 0.28);
}

.badge__face--back {
  transform: rotateY(180deg) translateZ(var(--badge-half));
}

/* 指纹级的镜面反光：跟着指针走，卡面才有“塑料+覆膜”的质感 */
.badge__sheen {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: var(--sheen-o, 0);
  mix-blend-mode: soft-light;
  background:
    radial-gradient(
      30% 44% at var(--sheen-x, 34%) var(--sheen-y, 20%),
      rgba(255, 255, 255, 0.6) 0%,
      rgba(255, 255, 255, 0.16) 32%,
      rgba(255, 255, 255, 0) 58%
    ),
    linear-gradient(
      var(--holo-angle, 118deg),
      rgba(255, 255, 255, 0.34) 0%,
      rgba(255, 255, 255, 0) 38%,
      rgba(255, 255, 255, 0) 72%,
      rgba(255, 255, 255, 0.18) 100%
    );
  transition: opacity 0.35s ease;
}

.badge__wash {
  position: absolute;
  inset: 0;
  display: block;
  width: 100%;
  height: 100%;
}

.badge__holo {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(
    var(--holo-angle, 118deg),
    rgba(255, 255, 255, 0.4) 0%,
    rgba(255, 255, 255, 0) 26%,
    rgba(255, 255, 255, 0) 64%,
    rgba(255, 255, 255, 0.14) 100%
  );
  mix-blend-mode: soft-light;
}

.badge__grain {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.14;
  mix-blend-mode: overlay;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='180' height='180'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='3'/%3E%3C/filter%3E%3Crect width='180' height='180' filter='url(%23n)' opacity='0.5'/%3E%3C/svg%3E");
}

.badge__frost {
  position: absolute;
  inset: 0;
  pointer-events: none;
  backdrop-filter: blur(16px) saturate(1.06);
  -webkit-backdrop-filter: blur(16px) saturate(1.06);
  -webkit-mask-image:
    linear-gradient(96deg, #000 0%, rgba(0, 0, 0, 0.72) 36%, transparent 66%),
    linear-gradient(0deg, #000 0%, transparent 34%);
  mask-image:
    linear-gradient(96deg, #000 0%, rgba(0, 0, 0, 0.72) 36%, transparent 66%),
    linear-gradient(0deg, #000 0%, transparent 34%);
}

.badge__scrim {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(96deg, var(--badge-scrim) 0%, var(--badge-scrim-soft) 34%, transparent 66%),
    linear-gradient(0deg, var(--badge-scrim) 0%, transparent 32%),
    linear-gradient(180deg, var(--badge-scrim-soft) 0%, transparent 34%);
}

/* 卡顶凹槽：本来是卡片的物理卡槽，现在兼任“展开 / 收起输入面板”的开关 */
.badge__slot {
  position: absolute;
  top: 9px;
  left: 50%;
  width: 52px;
  height: 8px;
  border-radius: 999px;
  background: var(--badge-slot);
  box-shadow:
    inset 0 1px 2px rgba(0, 0, 0, 0.55),
    0 1px 0 rgba(255, 255, 255, 0.26);
  transform: translateX(-50%);
}

.badge__slot--toggle {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 76px;
  height: 30px;
  padding: 0;
  border: 0;
  border-radius: 10px;
  background: none;
  box-shadow: none;
  z-index: 5;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.badge__slot-bar {
  display: block;
  width: 52px;
  height: 8px;
  border-radius: 999px;
  background: var(--badge-slot);
  box-shadow:
    inset 0 1px 2px rgba(0, 0, 0, 0.55),
    0 1px 0 rgba(255, 255, 255, 0.26);
  transition: box-shadow 0.22s ease, background 0.22s ease;
}

.badge__slot-arrow {
  width: 10px;
  height: 7px;
  margin-top: 3px;
  color: var(--badge-ink-soft);
  opacity: 0.72;
  transition: opacity 0.22s ease, transform 0.28s cubic-bezier(0.2, 0.78, 0.2, 1);
}

.badge__slot-arrow.is-up {
  transform: rotate(180deg);
}

.badge__slot--toggle:hover .badge__slot-bar,
.badge__slot--toggle:focus-visible .badge__slot-bar {
  box-shadow:
    inset 0 1px 2px rgba(0, 0, 0, 0.5),
    0 1px 0 rgba(255, 255, 255, 0.32),
    0 0 0 1.5px rgba(255, 255, 255, 0.42),
    0 0 12px rgba(255, 255, 255, 0.28);
}

.badge__slot--toggle:hover .badge__slot-arrow {
  opacity: 1;
  color: var(--badge-ink);
}

.badge__slot--toggle:focus-visible {
  outline: none;
}

.badge__slot--toggle.is-open .badge__slot-bar {
  box-shadow:
    inset 0 1px 2px rgba(0, 0, 0, 0.5),
    0 1px 0 rgba(255, 255, 255, 0.32),
    0 0 0 1.5px rgba(255, 255, 255, 0.28);
}

/* 聚焦提示框：白色细边 + 柔光，跟着当前输入的那个字段走 */
.badge__focus {
  position: absolute;
  z-index: 6;
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  box-shadow:
    0 0 5px rgba(255, 255, 255, 0.55),
    inset 0 0 6px rgba(255, 255, 255, 0.16);
  opacity: 0;
  pointer-events: none;
  transition:
    left 0.3s cubic-bezier(0.2, 0.7, 0.3, 1),
    top 0.3s cubic-bezier(0.2, 0.7, 0.3, 1),
    width 0.3s cubic-bezier(0.2, 0.7, 0.3, 1),
    height 0.3s cubic-bezier(0.2, 0.7, 0.3, 1),
    opacity 0.2s ease;
}

.badge__focus.is-visible {
  opacity: 1;
}

.badge__body {
  position: absolute;
  inset: 26px 26px 22px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 12px;
}

.badge__body--back {
  inset: 24px 26px 20px;
}

.badge__head {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* logo：只保留 logo 的白色圆盘，用同族晕染上色（不做白光反射） */
.badge__logo {
  position: relative;
  display: block;
  flex: none;
  width: 44px;
  height: 44px;
}

.badge__logo i {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  -webkit-mask-image: var(--badge-logo-disc), var(--badge-logo-swirl);
  mask-image: var(--badge-logo-disc), var(--badge-logo-swirl);
  -webkit-mask-size: 100% 100%, 100% 100%;
  mask-size: 100% 100%, 100% 100%;
  -webkit-mask-repeat: no-repeat, no-repeat;
  mask-repeat: no-repeat, no-repeat;
  /* swirl 是 disc 的子集，xor 正好等于 disc 减 swirl：条纹变成镂空 */
  -webkit-mask-composite: xor;
  mask-composite: exclude;
}

.badge__logo-base {
  background:
    radial-gradient(
      circle at 30% 24%,
      var(--badge-logo-b) 0%,
      rgba(255, 255, 255, 0) 58%
    ),
    conic-gradient(
      from var(--badge-logo-angle),
      var(--badge-logo-a),
      var(--badge-logo-b) 18%,
      var(--badge-logo-c) 38%,
      var(--badge-logo-d) 58%,
      var(--badge-logo-c) 78%,
      var(--badge-logo-a)
    );
  filter: blur(0.6px) saturate(1.05);
}

.badge__kind {
  margin-left: auto;
  flex: none;
  padding: 4px 9px;
  border: 1px solid var(--badge-hair);
  border-radius: 999px;
  background: var(--badge-chip);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  font-size: 8.5px;
  font-weight: 600;
  letter-spacing: 0.18em;
  text-shadow: var(--emboss-soft);
}

/* 手机号：11 个格子，输入一位就翻上去一位 */
.badge__hero {
  position: relative;
  align-self: flex-start;
  padding: 4px 6px;
  margin: -4px -6px;
}

.badge__digits {
  display: flex;
  align-items: center;
  font-size: clamp(20px, 2.6vw, 27px);
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.005em;
  font-variant-numeric: tabular-nums;
  text-shadow: 0 1px 4px var(--badge-veil);
}

.badge__digit {
  display: inline-flex;
  justify-content: center;
  width: 0.66em;
  /* 窗口只留一行高，提示框才不会框到多出来的半行 */
  height: 1.05em;
  overflow: hidden;
}

.badge__digit.is-break {
  margin-right: 0.36em;
}

.badge__digit-col {
  display: flex;
  flex-direction: column;
  height: 2.1em;
  transition: transform 0.22s cubic-bezier(0.16, 1, 0.3, 1);
}

.badge__digit.is-filled .badge__digit-col {
  transform: translateY(-50%);
}

.badge__digit-row {
  position: relative;
  display: block;
  height: 1.05em;
  line-height: 1.05em;
  text-align: center;
  /* 像银行卡那种凸印数字：顶面高光 + 向下拖出的厚度 */
  text-shadow: var(--emboss);
}

/* 字面单独一层：竖向渐变的“顶面”，向上错 1px，底下的实心侧壁就露出来了 */
.badge__digit-row::after {
  content: attr(data-char);
  position: absolute;
  inset: 0;
  color: transparent;
  /* 上层是一道跟着指针走的镜面反光，压在渐变字面上，凸字的顶面才会“亮一下” */
  background-image:
    radial-gradient(
      120% 90% at var(--sheen-x, 34%) -10%,
      rgba(255, 255, 255, 0.95) 0%,
      rgba(255, 255, 255, 0.35) 34%,
      rgba(255, 255, 255, 0) 66%
    ),
    var(--emboss-face);
  -webkit-background-clip: text;
  background-clip: text;
  transform: translateY(-1px);
  pointer-events: none;
}

.badge__digit-ghost::after {
  opacity: 0.5;
}

.badge__digit-row:first-child {
  color: var(--badge-empty);
}

.badge__digit-ghost {
  font-weight: 500;
}

.badge__sr {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip-path: inset(50%);
  white-space: nowrap;
}

.badge__foot {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding-top: 10px;
  border-top: 1px solid var(--badge-hair);
}

.badge__person {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.badge__field {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
  padding: 3px 5px;
  margin: -3px -5px;
}

.badge__label {
  font-size: 7.5px;
  font-weight: 600;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--badge-ink-faint);
  text-shadow: var(--emboss-soft);
}

.badge__name {
  font-size: clamp(15px, 1.9vw, 18px);
  font-weight: 700;
  line-height: 1.15;
  max-width: 190px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-shadow: var(--emboss);
}

.badge__sex {
  flex: none;
  padding: 3px 8px;
  border: 1px solid var(--badge-hair);
  border-radius: 999px;
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: var(--badge-ink-soft);
  text-shadow: var(--emboss-soft);
}

.badge__field--mail {
  align-items: flex-end;
  max-width: 58%;
}

.badge__mail-value {
  font-size: 11.5px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-shadow: var(--emboss-sm);
}

/* 背面：密码修改时翻过来看这一面 */
.badge__secret {
  align-self: stretch;
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding: 11px 16px 13px;
  border: 1px solid var(--badge-hair);
  border-radius: 14px;
  background: var(--badge-secret);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}

.badge__secret-dots {
  display: block;
  min-height: 22px;
  font-size: 22px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.3em;
  color: var(--badge-ink);
  text-shadow: var(--emboss);
}

.badge__secret-dots.is-empty {
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.12em;
  color: var(--badge-ink-faint);
}

.badge__hint {
  margin: 0;
  font-size: 9.5px;
  line-height: 1.5;
  letter-spacing: 0.04em;
  color: var(--badge-ink-faint);
  text-shadow: var(--emboss-soft);
}

@media (max-width: 720px) {
  .badge {
    --badge-half: 2px;
  }

  .badge__body {
    inset: 22px 18px 16px;
  }

  .badge__body--back {
    inset: 20px 18px 16px;
  }

  .badge__logo {
    width: 32px;
    height: 32px;
  }

  .badge__name {
    max-width: 130px;
  }

  .badge__field--mail {
    max-width: 52%;
  }
}
</style>