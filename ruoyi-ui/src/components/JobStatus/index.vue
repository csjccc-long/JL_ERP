<template>
  <span
    class="job-check"
    :class="{ 'is-on': isOn, 'is-off': !isOn, 'is-anim': animating, 'is-disabled': disabled }"
  >
    <input
      :id="inputId"
      ref="inputRef"
      class="job-check__input"
      type="checkbox"
      :checked="isOn"
      :disabled="disabled"
      :aria-label="isOn ? '任务状态：正常，点击停用' : '任务状态：暂停，点击启用'"
      @change="onChange"
    />
    <label class="job-check__label" :for="inputId">
      <slot />
    </label>
  </span>
</template>

<script setup name="JobStatus">
import { computed, onBeforeUnmount, ref, watch } from "vue"

let seq = 0

const props = defineProps({
  /** 任务状态："0" 正常（勾选，画出对勾）/ "1" 暂停（未勾选，划掉文字） */
  status: { type: String, default: "0" },
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(["toggle"])

const isOn = computed(() => props.status === "0")

/** label[for] 需要唯一 id，表格里每行一个实例 */
const inputId = "job-check-" + ++seq
const inputRef = ref(null)

/**
 * 动效只在状态真正变化时播放（首屏不逐行闪）。
 * 与 Uiverse 原例一致：动画由状态变化驱动，keyframes 完全沿用原代码。
 */
const animating = ref(false)
let timer = null

function startTimer() {
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => {
    animating.value = false
  }, 800)
}

function playOnce() {
  if (animating.value) {
    // 上一次动画还没走完：先摘掉再下一帧挂上，保证能重播
    animating.value = false
    requestAnimationFrame(() => {
      animating.value = true
      startTimer()
    })
    return
  }
  animating.value = true
  startTimer()
}

function onChange(event) {
  if (props.disabled) {
    if (event && event.target) event.target.checked = isOn.value
    return
  }
  emit("toggle", isOn.value ? "1" : "0")
  // 父级「确认后再生效」：先把原生勾选态复原，等 status 变化再同步
  if (inputRef.value) inputRef.value.checked = isOn.value
}

watch(
  () => props.status,
  (value, oldValue) => {
    if (value === oldValue) return
    playOnce()
  }
)

onBeforeUnmount(() => {
  if (timer) clearTimeout(timer)
})
</script>

<style scoped>
/*
 * 复刻 Uiverse「#checklist」（by JkHuger）：
 * 15px 透明 checkbox + 文字 label，label::before 是左侧 8px 短横，
 * label::after 是 4px 圆点（勾选时按 firework 炸成六向粒子）。
 * 语义映射：正常 = 勾选（画出对勾 + 烟花），暂停 = 未勾选（短横拉长成删除线）。
 */
.job-check {
  --check: #00beb4;
  --disabled: #c3c8de;
  --strike: #b3b8bd;
  --text: #414856;
  --text-off: #a9aeba;

  position: relative;
  display: inline-flex;
  align-items: center;
  flex: none;
  max-width: 100%;
  min-width: 0;
  user-select: none;

  &.is-disabled {
    cursor: not-allowed;
    opacity: 0.55;
  }
}

/* 15×15 的透明 checkbox：对勾由它的两段 ::before / ::after 画出 */
.job-check__input {
  -webkit-appearance: none;
  appearance: none;
  position: relative;
  flex: none;
  width: 15px;
  height: 15px;
  margin: 0 15px 0 0;
  padding: 0;
  border: 0;
  outline: none;
  border-radius: 3px;
  background: transparent;
  display: grid;
  align-items: center;
  cursor: pointer;

  &:focus-visible {
    outline: 2px solid rgba(0, 190, 180, 0.45);
    outline-offset: 3px;
  }
}

.job-check__input::before,
.job-check__input::after {
  content: "";
  position: absolute;
  height: 2px;
  top: auto;
  background: var(--check);
  border-radius: 2px;
}

/* 短笔画：右端为旋转支点 */
.job-check__input::before {
  width: 0;
  right: 60%;
  transform-origin: right bottom;
}

/* 长笔画：左端为旋转支点 */
.job-check__input::after {
  width: 0;
  left: 40%;
  transform-origin: left bottom;
}

/* 正常（勾选）：两段笔画落到对勾的终态 */
.is-on .job-check__input::before {
  width: 5px;
  top: 8px;
  transform: rotate(45deg);
}

.is-on .job-check__input::after {
  width: 10px;
  top: 8px;
  transform: rotate(-45deg);
}

/* 暂停（未勾选）：格子里保留一小段灰色短横，别让复选框看起来是个空位 */
.is-off:not(.is-anim) .job-check__input::before {
  left: 1px;
  top: 6px;
  width: 8px;
  background: var(--disabled);
}

.job-check__label {
  position: relative;
  display: block;
  min-width: 0;
  max-width: 100%;
  color: var(--text);
  cursor: pointer;
  transition: color 0.3s ease;
}

.job-check__label::before,
.job-check__label::after {
  content: "";
  position: absolute;
  /* 划线/粒子不参与命中测试，否则会盖住名字链接的点击区域 */
  pointer-events: none;
}

/* 左侧 8px 短横（正常态的颜色标记；暂停时拉长成删除线） */
.job-check__label::before {
  top: 50%;
  margin-top: -1px;
  left: -27px;
  width: 8px;
  height: 2px;
  border-radius: 2px;
  background: var(--check);
  transition: background 0.3s ease;
}

/* 4px 圆点：平时透明，暂停瞬间按 firework 炸开 */
.job-check__label::after {
  top: 8px;
  left: -25px;
  width: 4px;
  height: 4px;
  border-radius: 50%;
}

/* 正常：对勾已占用格子，短横收起 */
.is-on .job-check__label::before {
  width: 0;
}

/* 暂停：文字置灰 + 短横拉成贯穿文字的删除线 */
.is-off .job-check__label {
  color: var(--text-off);
  padding-right: 4px;
}

.is-off .job-check__label::before {
  left: -2px;
  width: 100%;
  background: var(--strike);
}

/* ---------- 动效（与 Uiverse 原 keyframes 一致） ---------- */

.is-anim.is-on .job-check__input::before {
  animation: check-01 0.4s ease forwards;
}

.is-anim.is-on .job-check__input::after {
  animation: check-02 0.4s ease forwards;
}

.is-anim.is-off .job-check__label {
  animation: move 0.3s ease 0.1s forwards;
}

.is-anim.is-off .job-check__label::before {
  animation: slice 0.4s ease forwards;
}

.is-anim.is-on .job-check__label::before {
  animation: slice-back 0.32s ease forwards;
}

.is-anim.is-on .job-check__label::after {
  animation: firework 0.5s ease forwards 0.1s;
}

@keyframes move {
  50% {
    padding-left: 8px;
    padding-right: 0px;
  }

  100% {
    padding-right: 4px;
  }
}

@keyframes slice {
  0% {
    width: 8px;
    left: -27px;
    background: var(--check);
  }

  60% {
    width: 100%;
    left: 4px;
  }

  100% {
    width: 100%;
    left: -2px;
    padding-left: 0;
  }
}

@keyframes slice-back {
  0% {
    width: 100%;
    left: -2px;
  }

  100% {
    width: 0;
    left: -2px;
  }
}

@keyframes check-01 {
  0% {
    width: 4px;
    top: auto;
    transform: rotate(0);
  }

  50% {
    width: 0px;
    top: auto;
    transform: rotate(0);
  }

  51% {
    width: 0px;
    top: 8px;
    transform: rotate(45deg);
  }

  100% {
    width: 5px;
    top: 8px;
    transform: rotate(45deg);
  }
}

@keyframes check-02 {
  0% {
    width: 4px;
    top: auto;
    transform: rotate(0);
  }

  50% {
    width: 0px;
    top: auto;
    transform: rotate(0);
  }

  51% {
    width: 0px;
    top: 8px;
    transform: rotate(-45deg);
  }

  100% {
    width: 10px;
    top: 8px;
    transform: rotate(-45deg);
  }
}

@keyframes firework {
  0% {
    opacity: 1;
    box-shadow:
      0 0 0 -2px var(--check),
      0 0 0 -2px var(--check),
      0 0 0 -2px var(--check),
      0 0 0 -2px var(--check),
      0 0 0 -2px var(--check),
      0 0 0 -2px var(--check);
  }

  30% {
    opacity: 1;
  }

  100% {
    opacity: 0;
    box-shadow:
      0 -15px 0 0 var(--check),
      14px -8px 0 0 var(--check),
      14px 8px 0 0 var(--check),
      0 15px 0 0 var(--check),
      -14px 8px 0 0 var(--check),
      -14px -8px 0 0 var(--check);
  }
}

@media (prefers-reduced-motion: reduce) {
  .is-anim .job-check__input::before,
  .is-anim .job-check__input::after,
  .is-anim .job-check__label,
  .is-anim .job-check__label::before,
  .is-anim .job-check__label::after {
    animation: none;
  }
}
</style>
