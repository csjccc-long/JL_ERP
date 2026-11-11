<template>
  <transition name="upload-progress">
    <div v-if="visible" class="upload-progress" role="status" aria-live="polite" aria-atomic="true">
      <div class="upload-progress__card">
        <image-gen-loader
          :progress="percent"
          :text="title"
          :caption="caption"
          :cell-size="3"
          :gap="1"
          :band-height="42"
          :wave-amplitude="9"
          :colors="colors"
          :overlay-opacity="0"
        />
      </div>
    </div>
  </transition>
</template>

<script setup name="UploadProgress">
import { computed } from 'vue'
import ImageGenLoader from '@/components/ImageGenLoader/index.vue'
import { uploadProgress } from '@/utils/upload-progress'

const items = computed(() => uploadProgress.items)
const visible = computed(() => items.value.length > 0)
const count = computed(() => items.value.length)
const single = computed(() => (count.value === 1 ? items.value[0] : null))
const uploading = computed(() => items.value.some((item) => item.status === 'uploading'))
const doneCount = computed(() => items.value.filter((item) => item.status === 'done').length)
const errorCount = computed(() => items.value.filter((item) => item.status === 'error').length)
const failed = computed(() => !uploading.value && errorCount.value > 0)

/** 多文件按体积加权，避免大文件被小文件的进度稀释 */
const percent = computed(() => {
  const list = items.value
  if (!list.length) return 0
  let weightSum = 0
  let valueSum = 0
  for (const item of list) {
    const weight = item.size > 0 ? item.size : 1
    weightSum += weight
    valueSum += weight * (item.status === 'done' ? 100 : item.percent || 0)
  }
  if (!weightSum) return 0
  return Math.max(0, Math.min(100, Math.round(valueSum / weightSum)))
})

const title = computed(() => {
  if (failed.value) return '上传失败'
  if (!uploading.value) return count.value > 1 ? '上传完成' : '上传完成'
  return count.value > 1 ? `正在上传 ${count.value} 个文件` : '正在上传'
})

const caption = computed(() => {
  if (single.value) return single.value.name
  if (failed.value) return `${errorCount.value} 个文件上传失败`
  return `${doneCount.value}/${count.value} 个文件已完成`
})

const colors = computed(() => {
  if (failed.value) return ['#fda4af', '#9f1239']
  if (!uploading.value) return ['#5eead4', '#0f766e']
  return ['#22d3ee', '#1e40af']
})
</script>

<style scoped>
.upload-progress {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 3000;
  pointer-events: none;
}

.upload-progress__card {
  width: 312px;
  height: 182px;
  overflow: hidden;
  border-radius: 16px;
  background: #fff;
  border: 1px solid rgba(22, 22, 24, 0.07);
  box-shadow:
    0 20px 48px rgba(15, 43, 58, 0.18),
    0 2px 8px rgba(15, 43, 58, 0.08);
}

.upload-progress-enter-active,
.upload-progress-leave-active {
  transition:
    opacity 0.26s ease,
    transform 0.32s cubic-bezier(0.34, 1.3, 0.64, 1);
}

.upload-progress-enter-from,
.upload-progress-leave-to {
  opacity: 0;
  transform: translateY(16px) scale(0.97);
}

@media (max-width: 640px) {
  .upload-progress {
    right: 12px;
    left: 12px;
    bottom: 12px;
  }

  .upload-progress__card {
    width: 100%;
    height: 168px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .upload-progress-enter-active,
  .upload-progress-leave-active {
    transition: opacity 0.2s ease;
  }

  .upload-progress-enter-from,
  .upload-progress-leave-to {
    transform: none;
  }
}
</style>
