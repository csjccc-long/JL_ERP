<template>
  <span class="fullscreen-toggle" :title="title">
    <label class="fs-switch">
      <input
        ref="inputRef"
        class="fs-switch__input"
        type="checkbox"
        role="switch"
        :checked="isFullscreen"
        :aria-checked="isFullscreen"
        :aria-label="title"
        @change="handleChange"
      />
      <svg class="fs-switch__icon fs-switch__icon--expand" viewBox="0 0 448 512" aria-hidden="true" focusable="false">
        <path d="M32 32C14.3 32 0 46.3 0 64v96c0 17.7 14.3 32 32 32s32-14.3 32-32V96h64c17.7 0 32-14.3 32-32s-14.3-32-32-32H32zM64 352c0-17.7-14.3-32-32-32s-32 14.3-32 32v96c0 17.7 14.3 32 32 32h96c17.7 0 32-14.3 32-32s-14.3-32-32-32H64V352zM320 32c-17.7 0-32 14.3-32 32s14.3 32 32 32h64v64c0 17.7 14.3 32 32 32s32-14.3 32-32V64c0-17.7-14.3-32-32-32H320zM448 352c0-17.7-14.3-32-32-32s-32 14.3-32 32v64H320c-17.7 0-32 14.3-32 32s14.3 32 32 32h96c17.7 0 32-14.3 32-32V352z"></path>
      </svg>
      <svg class="fs-switch__icon fs-switch__icon--compress" viewBox="0 0 448 512" aria-hidden="true" focusable="false">
        <path d="M160 64c0-17.7-14.3-32-32-32s-32 14.3-32 32v64H32c-17.7 0-32 14.3-32 32s14.3 32 32 32h96c17.7 0 32-14.3 32-32V64zM32 320c-17.7 0-32 14.3-32 32s14.3 32 32 32H96v64c0 17.7 14.3 32 32 32s32-14.3 32-32V352c0-17.7-14.3-32-32-32H32zM352 64c0-17.7-14.3-32-32-32s-32 14.3-32 32v96c0 17.7 14.3 32 32 32h96c17.7 0 32-14.3 32-32s-14.3-32-32-32H352V64zM320 320c-17.7 0-32 14.3-32 32v96c0 17.7 14.3 32 32 32s32-14.3 32-32V384h64c17.7 0 32-14.3 32-32s-14.3-32-32-32H320z"></path>
      </svg>
    </label>
  </span>
</template>

<script setup name="FullscreenToggle">
import { computed, nextTick, ref, watch } from 'vue'
import { useFullscreen } from '@vueuse/core'

const { isFullscreen, toggle } = useFullscreen()
const inputRef = ref(null)

const title = computed(() => (isFullscreen.value ? '退出全屏' : '全屏'))

// 浏览器可能拒绝全屏（如未授权的 iframe），此时把复选框拨回真实状态
function syncInput() {
  const el = inputRef.value
  if (el) el.checked = isFullscreen.value
}

async function handleChange() {
  try {
    await toggle()
  } catch (e) {
    // 忽略：保持原状态
  }
  await nextTick()
  syncInput()
}

watch(isFullscreen, syncInput)
</script>

<style lang="scss" scoped>
.fullscreen-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 0 8px;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  transition: background 0.3s;

  &:hover {
    background: rgba(0, 0, 0, 0.025);
  }
}

.fs-switch {
  --fs-size: 20px;
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: var(--fs-size);
  height: var(--fs-size);
  font-size: var(--fs-size);
  line-height: 1;
  color: #5a5e66;
  fill: currentColor;
  cursor: pointer;
  user-select: none;
  transition: color 0.2s ease;

  &:hover {
    color: #161618;
  }

  &__icon {
    position: absolute;
    inset: 0;
    width: 1em;
    height: 1em;
    animation: fs-pop 0.5s;
  }

  &__icon--compress {
    display: none;
  }

  input:checked ~ .fs-switch__icon--expand {
    display: none;
  }

  input:checked ~ .fs-switch__icon--compress {
    display: block;
  }

  &__input {
    position: absolute;
    width: 0;
    height: 0;
    opacity: 0;
    cursor: pointer;
  }

  &:has(input:focus-visible) {
    outline: 2px solid var(--el-color-primary, #00beb4);
    outline-offset: 4px;
    border-radius: 4px;
  }
}

@keyframes fs-pop {
  0% {
    transform: scale(0);
    opacity: 0;
  }

  50% {
    transform: scale(1.2);
  }

  100% {
    transform: scale(1);
    opacity: 1;
  }
}

@media (prefers-reduced-motion: reduce) {
  .fs-switch__icon {
    animation: none;
  }
}
</style>
