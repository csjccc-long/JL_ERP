<template>
  <div class="badge-block">
    <div ref="cardRef" class="badge" :class="{ 'is-flipped': flipped }" :style="styleVars" @transitionend="onTransitionEnd">
      <section ref="frontRef" class="badge__face badge__face--front">
        <canvas ref="washRef" class="badge__wash" width="720" height="458" aria-hidden="true" />
        <span class="badge__holo" aria-hidden="true" />
        <span class="badge__grain" aria-hidden="true" />
        <span class="badge__frost" aria-hidden="true" />
        <span class="badge__scrim" aria-hidden="true" />
        <span class="badge__slot" aria-hidden="true" />
        <span class="badge__focus" :class="{ 'is-visible': frontFrame.visible }" :style="frontFrameStyle" aria-hidden="true" />

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
                  <span class="badge__digit-row badge__digit-ghost">•</span>
                  <span class="badge__digit-row">{{ slot.char }}</span>
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
      </section>

      <section ref="backRef" class="badge__face badge__face--back">
        <canvas ref="washBackRef" class="badge__wash" width="720" height="458" aria-hidden="true" />
        <span class="badge__holo" aria-hidden="true" />
        <span class="badge__grain" aria-hidden="true" />
        <span class="badge__scrim" aria-hidden="true" />
        <span class="badge__focus" :class="{ 'is-visible': backFrame.visible }" :style="backFrameStyle" aria-hidden="true" />

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
      </section>
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
  secretLength: { type: Number, default: 0 }
})

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

/** 只量内容范围（有 selector 时按子节点取并集），避免把内边距也算进框里 */
const boundsOf = (el, selector) => {
  if (!el) return null
  const nodes = selector ? Array.from(el.querySelectorAll(selector)) : []
  const rects = (nodes.length ? nodes : [el])
    .map(node => node.getBoundingClientRect())
    .filter(rect => rect.width > 0 && rect.height > 0)
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
  const face = faceEl.getBoundingClientRect()
  if (!face.width) return null
  let left = bounds.left - face.left - padX
  let right = bounds.right - face.left + padX
  if (limitRight != null) right = Math.min(right, limitRight - face.left)
  left = Math.max(5, left)
  right = Math.min(face.width - 5, right)
  const top = Math.max(5, bounds.top - face.top - padY)
  const bottom = Math.min(face.height - 5, bounds.bottom - face.top + padY)
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
    const frame = frameOf(backRef.value, boundsOf(secretRef.value), 9, 8)
    if (frame) backFrame.value = frame
    return
  }
  if (props.flipped) return
  const config = FIELD_FRAMES[field]
  if (!config) return
  const [el, selector, padX, padY] = config()
  const chip = sexRef.value ? sexRef.value.getBoundingClientRect() : null
  const limitRight = field === "holder" && chip && chip.width ? chip.left - 6 : null
  const frame = frameOf(frontRef.value, boundsOf(el, selector), padX, padY, limitRight)
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

const onTransitionEnd = event => {
  if (event.propertyName !== "transform") return
  remeasure()
}
</script>

<style lang="scss" scoped>
.badge-block {
  width: 100%;
  max-width: 560px;
  margin: 0 auto;
  /* 透视放在外层，翻转时才有真实的景深 */
  perspective: 1500px;
}

.badge {
  position: relative;
  width: 100%;
  aspect-ratio: 440 / 254;
  border-radius: 22px;
  transform-style: preserve-3d;
  transition: transform 0.8s cubic-bezier(0.2, 0.75, 0.2, 1);
}

.badge.is-flipped {
  transform: rotateY(180deg);
}

.badge__face {
  position: absolute;
  inset: 0;
  border-radius: 22px;
  overflow: hidden;
  isolation: isolate;
  color: var(--badge-ink);
  background: #15131c;
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
  box-shadow:
    0 28px 54px -30px rgba(0, 0, 0, 0.95),
    0 8px 22px -14px rgba(0, 0, 0, 0.6),
    inset 0 0 0 1px rgba(255, 255, 255, 0.1);
}

.badge__face--back {
  transform: rotateY(180deg);
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
    118deg,
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
  display: block;
  height: 1.05em;
  line-height: 1.05em;
  text-align: center;
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
  gap: 3px;
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
}

.badge__name {
  font-size: clamp(15px, 1.9vw, 18px);
  font-weight: 700;
  line-height: 1.15;
  max-width: 190px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-shadow: 0 1px 4px var(--badge-veil);
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
  text-shadow: 0 1px 4px var(--badge-veil);
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
  text-shadow: 0 1px 4px var(--badge-veil);
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
}

@media (max-width: 720px) {
  .badge {
    aspect-ratio: 440 / 276;
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
