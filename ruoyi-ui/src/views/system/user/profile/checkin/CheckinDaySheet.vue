<template>
  <!-- 打卡卡片内部弹出来的当天记录：竖线 + 圆点 + 时间戳，悬停读详情，点一下钉住 -->
  <section class="sh" role="dialog" aria-label="当天的待办与自动化记录">
    <header class="sh__head">
      <span class="sh__mark" aria-hidden="true">
        <svg viewBox="0 0 16 16" width="17" height="17">
          <circle cx="4" cy="4" r="1.4" fill="currentColor" />
          <circle cx="4" cy="8" r="1.4" fill="currentColor" />
          <circle cx="4" cy="12" r="1.4" fill="currentColor" />
          <path d="M7.4 4h6M7.4 8h6M7.4 12h4" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
        </svg>
      </span>
      <div class="sh__titles">
        <h4>当天记录</h4>
        <p>{{ dateLabel }}<span class="sh__sep">·</span>{{ weekday }}<span class="sh__sep">·</span>{{ counts.total }} 条</p>
      </div>
      <button type="button" class="sh__close" aria-label="关闭" @click="$emit('close')">
        <svg viewBox="0 0 16 16" width="13" height="13" aria-hidden="true">
          <path d="m4 4 8 8M12 4l-8 8" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
        </svg>
      </button>
    </header>

    <div class="sh__bar">
      <span class="sh__dot" :class="checked ? 'is-on' : 'is-off'" aria-hidden="true" />
      <span>{{ checked ? '已打卡' + (checkinTime ? ' ' + checkinTime : '') : '当天未打卡' }}</span>
      <span class="sh__sep">·</span>
      <span>待办 {{ counts.todo }}</span>
      <span class="sh__sep">·</span>
      <span>自动化 {{ counts.automation }}</span>
      <template v-if="counts.failed">
        <span class="sh__sep">·</span>
        <span class="sh__fail">失败 {{ counts.failed }}</span>
      </template>
    </div>

    <div v-loading="loading" element-loading-background="rgba(20, 18, 27, 0.4)" class="sh__list">
      <template v-if="groups.length">
        <template v-for="group in groups" :key="group.label">
          <p class="sh__group">{{ group.label }}</p>
          <ol class="sh__tl">
            <li
              v-for="item in group.items"
              :key="item.id"
              class="sh__item"
              :class="['is-' + item.status.toLowerCase(), { 'is-open': openId === item.id }]"
              @mouseenter="hoverId = item.id"
              @mouseleave="hoverId = ''"
              @click="toggle(item.id)"
            >
              <span class="sh__point" aria-hidden="true" />
              <span class="sh__time">{{ item.time || '--:--' }}</span>
              <span class="sh__badge" :class="'sh__badge--' + item.status.toLowerCase()">{{ statusText(item.status) }}</span>
              <span class="sh__title">{{ item.title }}</span>
              <svg class="sh__chev" viewBox="0 0 16 16" width="14" height="14" aria-hidden="true">
                <path d="m4 6 4 4 4-4" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
              <div class="sh__detail">
                <div class="sh__detail-in">
                  <p class="sh__text">{{ item.detail }}</p>
                  <p class="sh__ref">
                    <span>{{ kindText(item.kind) }}</span>
                    <template v-if="item.ref"><span class="sh__sep">·</span><span>{{ item.ref }}</span></template>
                    <template v-if="item.owner"><span class="sh__sep">·</span><span>{{ item.owner }}</span></template>
                    <template v-if="item.finishedTime"><span class="sh__sep">·</span><span>{{ item.time }} → {{ item.finishedTime }}</span></template>
                  </p>
                </div>
              </div>
            </li>
          </ol>
        </template>
      </template>
      <p v-else-if="!loading" class="sh__empty">这一天没有待办或自动化记录</p>
    </div>

    <footer class="sh__foot">
      <span>悬停读详情 · 点一下钉住</span>
      <span v-if="counts.failed" class="sh__fail">当天有 {{ counts.failed }} 条失败</span>
    </footer>
  </section>
</template>

<script setup name="CheckinDaySheet">
import { computed, onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps({
  date: { type: String, default: '' },
  day: { type: Object, default: null },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['close'])

const hoverId = ref('')
const openId = ref('')

const counts = computed(() => {
  const c = (props.day && props.day.counts) || {}
  return {
    total: c.total || 0,
    todo: c.todo || 0,
    automation: c.automation || 0,
    failed: c.failed || 0
  }
})

const checked = computed(() => !!(props.day && props.day.checked))
const checkinTime = computed(() => (props.day && props.day.checkinTime) || '')
const weekday = computed(() => (props.day && props.day.weekday) || '')

const dateLabel = computed(() => {
  const raw = (props.day && props.day.date) || props.date
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(raw || '')
  return m ? Number(m[2]) + '月' + Number(m[3]) + '日' : raw || '—'
})

// 上午 / 下午 / 晚上，像时间线里的月份分组那样把当天切成几段
const groups = computed(() => {
  const items = (props.day && props.day.items) || []
  const buckets = [
    { label: '上午', items: [] },
    { label: '下午', items: [] },
    { label: '晚上', items: [] }
  ]
  for (const item of items) {
    const hour = Number((item.time || '').slice(0, 2))
    const idx = !item.time || Number.isNaN(hour) ? 0 : hour < 12 ? 0 : hour < 18 ? 1 : 2
    buckets[idx].items.push(item)
  }
  return buckets.filter(b => b.items.length)
})

const statusText = status =>
  ({ DONE: '完成', RUNNING: '进行中', PENDING: '待处理', FAILED: '失败' })[status] || status

const kindText = kind => ({ TODO: '待办', AUTOMATION: '自动化', APPROVAL: '审批' })[kind] || kind

const toggle = id => {
  openId.value = openId.value === id ? '' : id
}

// 打开时默认展开第一条，跟参考图一样留一段可读的详情
watch(
  () => props.date,
  () => {
    hoverId.value = ''
    const first = (props.day && props.day.items && props.day.items[0]) || null
    openId.value = first ? first.id : ''
  },
  { immediate: true }
)

const onKey = ev => {
  if (ev.key === 'Escape') emit('close')
}
window.addEventListener('keydown', onKey)
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))
</script>

<style lang="scss" scoped>
/* ---------- 打卡卡片内弹出的当天记录：竖线 + 圆点 + 时间徽章 ---------- */
.sh {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 6;
  max-height: 100%;
  display: flex;
  flex-direction: column;
  width: 452px;
  max-width: 100%;
  padding: 16px 16px 8px;
  border: 1px solid rgba(255, 255, 255, 0.09);
  border-radius: 16px;
  background: linear-gradient(180deg, #1b1923 0%, #141220 100%);
  box-shadow: -18px 0 46px rgba(0, 0, 0, 0.45), inset 0 1px 0 rgba(255, 255, 255, 0.04);
  animation: sh-in 0.32s cubic-bezier(0.22, 0.68, 0.32, 1) both;
}

@keyframes sh-in {
  from {
    opacity: 0;
    transform: translateX(18px) scale(0.985);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

.sh__head {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  flex: none;
}

.sh__mark {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  color: var(--pf-muted, rgba(244, 244, 245, 0.55));
  background: rgba(255, 255, 255, 0.05);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.06);
}

.sh__titles {
  flex: 1;
  min-width: 0;
}

.sh__titles h4 {
  margin: 0;
  color: var(--pf-ink, #f4f4f5);
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.01em;
}

.sh__titles p {
  margin: 5px 0 0;
  color: var(--pf-muted, rgba(244, 244, 245, 0.55));
  font-size: 12px;
}

.sh__close {
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  padding: 0;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  color: var(--pf-muted, rgba(244, 244, 245, 0.55));
  background: transparent;
  cursor: pointer;
  transition: color 0.2s ease, background-color 0.2s ease;
}

.sh__close:hover {
  color: var(--pf-ink, #f4f4f5);
  background: rgba(255, 255, 255, 0.06);
}

.sh__bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 5px;
  flex: none;
  margin: 12px 0 4px;
  padding: 8px 10px;
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 10px;
  color: var(--pf-muted, rgba(244, 244, 245, 0.55));
  font-size: 11.5px;
  background: rgba(255, 255, 255, 0.025);
}

.sh__dot {
  width: 6px;
  height: 6px;
  margin-right: 2px;
  border-radius: 50%;
}

.sh__dot.is-on {
  background: var(--pf-accent, #00beb4);
  box-shadow: 0 0 0 3px rgba(0, 190, 180, 0.16);
}

.sh__dot.is-off {
  background: rgba(244, 244, 245, 0.25);
}

.sh__sep {
  margin: 0 3px;
  opacity: 0.4;
}

.sh__fail {
  color: #f87171;
}

.sh__list {
  flex: 1;
  min-height: 0;
  margin-top: 4px;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;
}

.sh__group {
  margin: 12px 0 2px;
  color: var(--pf-faint, rgba(244, 244, 245, 0.38));
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.16em;
}

.sh__tl {
  position: relative;
  margin: 0;
  padding: 0 0 0 2px;
  list-style: none;
}

.sh__tl::before {
  content: '';
  position: absolute;
  top: 14px;
  bottom: 14px;
  left: 7px;
  width: 1px;
  background: rgba(255, 255, 255, 0.09);
}

.sh__item {
  position: relative;
  display: grid;
  grid-template-columns: 16px 42px auto 1fr 16px;
  align-items: center;
  gap: 0 6px;
  padding: 9px 8px 9px 0;
  border-radius: 10px;
  cursor: pointer;
  transition: background-color 0.25s ease;
}

.sh__item:hover,
.sh__item.is-open {
  background: rgba(255, 255, 255, 0.038);
}

.sh__point {
  justify-self: center;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #2c2b33;
  box-shadow: 0 0 0 3px rgba(20, 18, 27, 0.9);
  transition: background-color 0.25s ease, box-shadow 0.25s ease;
}

.sh__item.is-done .sh__point {
  background: var(--pf-accent, #00beb4);
}

.sh__item.is-running .sh__point {
  background: var(--pf-accent-glow, #7ef3ea);
  box-shadow: 0 0 0 3px rgba(20, 18, 27, 0.9), 0 0 0 4px rgba(126, 243, 234, 0.2);
  animation: sh-pulse 1.8s ease-in-out infinite;
}

.sh__item.is-failed .sh__point {
  background: #f87171;
}

.sh__item.is-pending .sh__point {
  background: #fbbf24;
}

@keyframes sh-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.45;
  }
}

.sh__time {
  color: var(--pf-faint, rgba(244, 244, 245, 0.38));
  font-size: 11px;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.02em;
}

.sh__badge {
  padding: 1px 6px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: var(--pf-muted, rgba(244, 244, 245, 0.55));
  font-size: 10.5px;
  white-space: nowrap;
  background: rgba(255, 255, 255, 0.04);
}

.sh__badge--done {
  color: #7ef3ea;
  border-color: rgba(0, 190, 180, 0.28);
  background: rgba(0, 190, 180, 0.1);
}

.sh__badge--failed {
  color: #fca5a5;
  border-color: rgba(248, 113, 113, 0.34);
  background: rgba(248, 113, 113, 0.12);
}

.sh__badge--pending {
  color: #fcd34d;
  border-color: rgba(251, 191, 36, 0.28);
  background: rgba(251, 191, 36, 0.1);
}

.sh__badge--running {
  color: #7ef3ea;
  border-color: rgba(126, 243, 234, 0.26);
  background: rgba(126, 243, 234, 0.08);
}

.sh__title {
  overflow: hidden;
  color: var(--pf-ink, #f4f4f5);
  font-size: 13.5px;
  font-weight: 500;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.sh__item.is-failed .sh__title {
  color: #fca5a5;
}

.sh__item.is-open .sh__title {
  font-weight: 600;
}

.sh__chev {
  justify-self: end;
  color: var(--pf-faint, rgba(244, 244, 245, 0.38));
  transition: transform 0.3s ease, color 0.25s ease;
}

.sh__item.is-open .sh__chev {
  color: var(--pf-muted, rgba(244, 244, 245, 0.55));
  transform: rotate(180deg);
}

.sh__detail {
  grid-column: 2 / -1;
  display: grid;
  grid-template-rows: 0fr;
  transition: grid-template-rows 0.32s cubic-bezier(0.22, 0.68, 0.32, 1);
}

.sh__item:hover .sh__detail,
.sh__item.is-open .sh__detail {
  grid-template-rows: 1fr;
}

.sh__detail-in {
  overflow: hidden;
}

.sh__text {
  margin: 6px 0 0;
  color: var(--pf-muted, rgba(244, 244, 245, 0.55));
  font-size: 12.5px;
  line-height: 1.65;
}

.sh__ref {
  margin: 5px 0 2px;
  color: var(--pf-faint, rgba(244, 244, 245, 0.32));
  font-size: 10px;
  letter-spacing: 0.02em;
}

.sh__empty {
  display: grid;
  place-items: center;
  height: 100%;
  min-height: 120px;
  margin: 0;
  color: var(--pf-faint, rgba(244, 244, 245, 0.38));
  font-size: 12px;
}

.sh__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex: none;
  padding-top: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.07);
  color: var(--pf-faint, rgba(244, 244, 245, 0.38));
  font-size: 11px;
}

@media (max-width: 900px) {
  .sh {
    position: static;
    width: 100%;
    max-height: none;
  }
}
</style>
