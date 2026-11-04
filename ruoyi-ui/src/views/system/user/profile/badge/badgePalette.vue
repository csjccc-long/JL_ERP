<template>
  <div class="palette-field">
    <span class="palette-field__label">卡片配色</span>
    <div class="palette-field__row" role="radiogroup" aria-label="卡片配色">
      <button
        v-for="item in palettes"
        :key="item.key"
        type="button"
        class="dot"
        :class="{ 'is-active': item.key === modelValue }"
        :title="item.label"
        :aria-label="item.label"
        :aria-pressed="item.key === modelValue"
        @click="pick(item.key)"
      >
        <canvas :ref="el => bindCanvas(item.key, el)" class="dot__art" width="52" height="52" />
      </button>
    </div>
    <span class="palette-field__now">{{ meta.label }}</span>
  </div>
</template>

<script setup name="BadgePalette">
import { BADGE_PALETTES, paletteOf, paintWash } from "./badgeArt"

const props = defineProps({
  modelValue: { type: String, default: "aurora" }
})

const emit = defineEmits(["update:modelValue"])

const palettes = BADGE_PALETTES
const meta = computed(() => paletteOf(props.modelValue))

const dots = {}

const bindCanvas = (key, el) => {
  if (!el) return
  dots[key] = el
  paintWash(el, paletteOf(key), 7)
}

const pick = key => {
  if (key !== props.modelValue) emit("update:modelValue", key)
}
</script>

<style lang="scss" scoped>
.palette-field {
  display: grid;
  grid-template-columns: auto auto minmax(0, 1fr);
  align-items: center;
  gap: 8px 12px;
  padding: 4px 0 2px;
}

.palette-field__label {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: rgba(244, 244, 245, 0.42);
}

.palette-field__row {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.dot {
  position: relative;
  flex: none;
  width: 24px;
  height: 24px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: none;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.dot__art {
  display: block;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.16);
}

.dot:hover {
  transform: translateY(-2px);
}

.dot.is-active {
  box-shadow:
    0 0 0 2px #16141d,
    0 0 0 3.5px rgba(244, 244, 245, 0.92);
}

.dot:focus-visible {
  outline: none;
  box-shadow:
    0 0 0 2px #16141d,
    0 0 0 3px #00beb4;
}

.palette-field__now {
  justify-self: start;
  font-size: 11.5px;
  font-weight: 600;
  color: rgba(244, 244, 245, 0.7);
}

/* 窄一点的表单里让色块独占一行，免得被标签挤成两截 */
@media (max-width: 680px) {
  .palette-field {
    grid-template-columns: auto auto;
    justify-content: space-between;
  }

  .palette-field__label {
    grid-column: 1;
    grid-row: 1;
  }

  .palette-field__now {
    grid-column: 2;
    grid-row: 1;
    justify-self: end;
  }

  .palette-field__row {
    grid-column: 1 / -1;
    grid-row: 2;
    flex-wrap: wrap;
  }
}
</style>
