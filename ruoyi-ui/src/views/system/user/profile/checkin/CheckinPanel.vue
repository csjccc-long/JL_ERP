<template>
  <section class="ck-panel" :class="{ 'is-pending': !loading && !checked }">
    <header class="ck-head">
      <div class="ck-head__intro">
        <p class="ck-head__eyebrow">MY CHECK-INS</p>
        <h3 class="ck-head__title">打卡足迹</h3>
        <p class="ck-head__sub">
          <template v-if="board">
            <span class="ck-dot" :class="{ 'is-on': checked }" aria-hidden="true" />
            {{ checked ? '今日已打卡 · ' + timeText : '今天还没打卡，随手记一次' }}
            <span class="ck-sep">·</span>
            近一年已打卡 {{ nf(stat.checkedDays) }} 天
          </template>
          <template v-else>正在读取你的打卡记录…</template>
        </p>
      </div>

      <div class="ck-head__side">
        <div class="ck-switch" role="group" aria-label="视图切换">
          <span class="ck-switch__thumb" :class="{ 'is-3d': view === '3d' }" aria-hidden="true" />
          <button
            type="button"
            class="ck-switch__btn"
            :class="{ 'is-active': view === '2d' }"
            :aria-pressed="view === '2d'"
            title="平面热力图"
            aria-label="平面热力图"
            @click="view = '2d'"
          >
            <svg viewBox="0 0 16 16" width="13" height="13" aria-hidden="true">
              <rect x="1.5" y="1.5" width="5.5" height="5.5" rx="1.2" fill="currentColor" />
              <rect x="9" y="1.5" width="5.5" height="5.5" rx="1.2" fill="currentColor" />
              <rect x="1.5" y="9" width="5.5" height="5.5" rx="1.2" fill="currentColor" />
              <rect x="9" y="9" width="5.5" height="5.5" rx="1.2" fill="currentColor" />
            </svg>
          </button>
          <button
            type="button"
            class="ck-switch__btn"
            :class="{ 'is-active': view === '3d' }"
            :aria-pressed="view === '3d'"
            title="三维天际线"
            aria-label="三维天际线"
            @click="view = '3d'"
          >
            <svg viewBox="0 0 16 16" width="13" height="13" aria-hidden="true">
              <path d="M8 1.2 14.2 4.6v6.8L8 14.8 1.8 11.4V4.6Z" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round" />
              <path d="M1.8 4.6 8 8l6.2-3.4M8 8v6.8" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round" />
            </svg>
          </button>
        </div>

        <button
          type="button"
          class="ck-cta"
          :class="{ 'is-done': checked }"
          :disabled="loading || checking || checked"
          @click="onCheckin"
        >
          <svg v-if="!checked" viewBox="0 0 16 16" width="14" height="14" aria-hidden="true">
            <path d="m3.2 8.4 3.1 3.1 6.5-6.6" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <svg v-else viewBox="0 0 16 16" width="14" height="14" aria-hidden="true">
            <circle cx="8" cy="8" r="6.3" fill="none" stroke="currentColor" stroke-width="1.4" />
            <path d="m5.1 8.2 2 2 3.8-3.9" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <span>{{ checking ? '打卡中…' : checked ? '今日已打卡' : '今日打卡' }}</span>
        </button>
      </div>
    </header>

    <div
      v-loading="loading"
      element-loading-background="rgba(16, 14, 22, 0.35)"
      class="ck-body"
      :class="{ 'is-detail': detailOpen }"
    >
      <div class="ck-chart">
        <checkin-skyline
          v-if="board"
          ref="skylineRef"
          :days="board.days"
          :start="board.start"
          :end="board.end"
          :view="view"
          :focus-level="focusLevel"
          :marks="board.fails || {}"
          :selected="selectedDate"
          @update:view="view = $event"
          @select="onSelectDay"
        />
        <div v-else class="ck-empty">打卡记录暂时读不出来，稍后刷新本页重试</div>
      </div>

      <!-- 点某一天才出现，就浮在卡片里，不跳页面也不占常驻空间 -->
      <checkin-day-sheet
        v-if="detailOpen"
        :date="selectedDate"
        :day="day"
        :loading="dayLoading"
        @close="detailOpen = false"
      />
    </div>

    <footer class="ck-foot">
      <ul class="ck-stats">
        <li v-for="item in statItems" :key="item.label">
          <p class="ck-stats__label">{{ item.label }}</p>
          <p class="ck-stats__value">
            <b>{{ item.value }}</b>
            <span>{{ item.unit }}</span>
          </p>
          <p class="ck-stats__sub">{{ item.sub }}</p>
        </li>
      </ul>

      <div class="ck-aside">
        <div class="ck-legend" @mouseleave="focusLevel = -1">
          <span class="ck-legend__text">少</span>
          <button
            v-for="(color, i) in swatches"
            :key="i"
            type="button"
            class="ck-legend__swatch"
            :class="{ 'is-on': focusLevel === i }"
            :style="{ background: color }"
            :aria-label="i === 0 ? '只看没打卡的日子' : '只看 ' + i + ' 级热度'"
            :aria-pressed="focusLevel === i"
            @mouseenter="focusLevel = i"
            @focus="focusLevel = i"
            @blur="focusLevel = -1"
            @click="focusLevel = focusLevel === i ? -1 : i"
          />
          <span class="ck-legend__text">多</span>
        </div>
        <p class="ck-hint">{{ view === '3d' ? '拖拽旋转 · 双击复位 · 悬停看当天' : '悬停看当天 · 方向键逐日查看' }}</p>
      </div>
    </footer>


  </section>
</template>

<script setup name="CheckinPanel">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { doCheckin, getCheckinBoard, getCheckinDay } from '@/api/erp/checkin'
import { LEVEL_COLORS, EMPTY_COLOR } from './skyline'
import CheckinSkyline from './CheckinSkyline.vue'
import CheckinDaySheet from './CheckinDaySheet.vue'

const loading = ref(true)
const checking = ref(false)
const board = ref(null)
// 窄屏上 3D 的柱子太细，默认用平面热力图，用户可以自己切
const view = ref(window.innerWidth < 640 ? '2d' : '3d')
const focusLevel = ref(-1)
const skylineRef = ref(null)
// 选中的一天：图表上圈出来，右侧时间线与抽屉都读这一天的待办与自动化
const selectedDate = ref('')
const day = ref(null)
const dayLoading = ref(false)
const detailOpen = ref(false)

const swatches = [EMPTY_COLOR, ...LEVEL_COLORS]

const numberFormat = new Intl.NumberFormat('zh-CN')
const monthDay = new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', timeZone: 'UTC' })
const nf = value => numberFormat.format(Number(value) || 0)
const md = value => (value ? monthDay.format(new Date(value + 'T00:00:00Z')) : '—')

const checked = computed(() => !!(board.value && board.value.checked))
const timeText = computed(() => {
  const raw = board.value && board.value.checkinTime
  return raw ? raw.slice(5, 16) : ''
})

const stat = computed(() => {
  const s = (board.value && board.value.stats) || {}
  return {
    checkedDays: s.checkedDays || 0,
    checkedTotal: s.checkedTotal || 0,
    totalActions: s.totalActions || 0,
    currentStreak: s.currentStreak || 0,
    longestStreak: s.longestStreak || 0,
    busiest: s.busiest || 0,
    busiestDate: s.busiestDate || ''
  }
})

const statItems = computed(() => [
  { label: '已打卡', value: nf(stat.value.checkedDays), unit: '天', sub: '累计 ' + nf(stat.value.checkedTotal) + ' 天' },
  { label: '当前连续', value: nf(stat.value.currentStreak), unit: '天', sub: '最长 ' + nf(stat.value.longestStreak) + ' 天' },
  { label: '最忙的一天', value: nf(stat.value.busiest), unit: '次动作', sub: md(stat.value.busiestDate) },
  { label: '近一年动作', value: nf(stat.value.totalActions), unit: '次', sub: '登录与业务操作' }
])

let dayToken = 0

const loadDay = async date => {
  if (!date) return
  const token = ++dayToken
  dayLoading.value = true
  try {
    const res = await getCheckinDay(date)
    if (token === dayToken) day.value = res.data
  } catch (e) {
    if (token === dayToken) day.value = null
  } finally {
    if (token === dayToken) dayLoading.value = false
  }
}

const selectDay = date => {
  if (!date) return
  if (date !== selectedDate.value) {
    selectedDate.value = date
    loadDay(date)
  }
}

/** 点中某一天：右侧时间线跟上，并把当天的全部记录从右边推出来 */
const onSelectDay = date => {
  selectDay(date)
  detailOpen.value = true
}

const load = async () => {
  loading.value = true
  try {
    const res = await getCheckinBoard()
    board.value = res.data
    selectDay(res.data.today || res.data.end)
  } catch (e) {
    board.value = null
  } finally {
    loading.value = false
  }
}

const onCheckin = async () => {
  if (checking.value || checked.value) return
  checking.value = true
  try {
    const res = await doCheckin()
    board.value = res.data
    // 今天刚打了卡，右侧那张卡上的状态与条数也要跟着变
    selectDay(res.data.today || res.data.end)
    if (skylineRef.value) skylineRef.value.pulseToday()
    ElMessage.success('打卡成功，已连续 ' + nf(res.data.stats.currentStreak) + ' 天')
  } catch (e) {
    /* 请求层已经提示过错误 */
  } finally {
    checking.value = false
  }
}

onMounted(load)
</script>

<style lang="scss" scoped>
/* ---------- 个人中心下方：打卡足迹 ---------- */
.ck-panel {
  position: relative;
  margin-top: 18px;
  padding: 18px 20px 12px;
  border: 1px solid var(--pf-edge, rgba(255, 255, 255, 0.08));
  border-radius: 18px;
  background:
    radial-gradient(120% 140% at 100% 0%, rgba(0, 190, 180, 0.09) 0%, transparent 60%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.055) 0%, rgba(255, 255, 255, 0.018) 100%);
  box-shadow: 0 30px 70px -46px rgba(0, 0, 0, 0.9);
  backdrop-filter: blur(16px);
  overflow: hidden;
}

/* 个人中心右栏给卡片堆预留了 clamp(420px, 100vh - 136px, 1040px) 的高度，
   卡片本身只占一半，下面就是那片留白。宽屏时把面板提上去吃掉这段空档，
   高度不足时 clamp 归零，绝不会压到卡片上。 */
@media (min-width: 1101px) {
  .ck-panel {
    margin-top: calc(-1 * clamp(0px, 100vh - 700px, 460px));
  }
}

.ck-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 12px;
}

.ck-head__eyebrow {
  margin: 0 0 5px;
  color: var(--pf-faint, rgba(244, 244, 245, 0.38));
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.22em;
}

.ck-head__title {
  margin: 0;
  color: var(--pf-ink, #f4f4f5);
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.ck-head__sub {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 6px 0 0;
  color: var(--pf-muted, rgba(244, 244, 245, 0.55));
  font-size: 12px;
}

.ck-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(244, 244, 245, 0.24);
  box-shadow: 0 0 0 3px rgba(244, 244, 245, 0.05);
  transition: background 0.3s ease, box-shadow 0.3s ease;
}

.ck-dot.is-on {
  background: var(--pf-accent, #00beb4);
  box-shadow: 0 0 0 3px rgba(0, 190, 180, 0.16), 0 0 12px rgba(0, 190, 180, 0.6);
}

.ck-sep {
  color: rgba(244, 244, 245, 0.22);
}

.ck-head__side {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: none;
}

/* 视图切换：一枚会滑动的胶囊 */
.ck-switch {
  position: relative;
  display: inline-flex;
  padding: 2px;
  border: 1px solid var(--pf-edge, rgba(255, 255, 255, 0.08));
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.03);
}

.ck-switch__thumb {
  position: absolute;
  top: 2px;
  bottom: 2px;
  left: 2px;
  width: 30px;
  border-radius: 999px;
  background: rgba(0, 190, 180, 0.22);
  box-shadow: inset 0 0 0 1px rgba(0, 190, 180, 0.5), 0 0 14px rgba(0, 190, 180, 0.24);
  transition: transform 0.45s cubic-bezier(0.65, 0, 0.35, 1);
}

.ck-switch__thumb.is-3d {
  transform: translateX(30px);
}

.ck-switch__btn {
  position: relative;
  z-index: 1;
  display: grid;
  place-items: center;
  width: 30px;
  height: 26px;
  padding: 0;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--pf-faint, rgba(244, 244, 245, 0.38));
  cursor: pointer;
  transition: color 0.3s ease;
}

.ck-switch__btn.is-active {
  color: var(--pf-accent-glow, #7ef3ea);
}

.ck-cta {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 34px;
  padding: 0 16px;
  border: 0;
  border-radius: 999px;
  background: linear-gradient(135deg, #00beb4 0%, #5df2e6 100%);
  box-shadow: 0 10px 24px -12px rgba(0, 190, 180, 0.9);
  color: #06201f;
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.02em;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.3s ease, color 0.3s ease;
}

.ck-cta:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 14px 28px -12px rgba(0, 190, 180, 1);
}

.ck-cta:active:not(:disabled) {
  transform: translateY(0);
}

.ck-cta.is-done {
  background: rgba(0, 190, 180, 0.12);
  box-shadow: inset 0 0 0 1px rgba(0, 190, 180, 0.36);
  color: var(--pf-accent-glow, #7ef3ea);
  cursor: default;
}

.ck-cta:disabled {
  opacity: 0.72;
}

/* 足迹铺满整块；当天记录是点开后才浮在这块里的，没有点击就不占位置 */
.ck-body {
  position: relative;
  display: flex;
  flex-direction: column;
  border-radius: 12px;
}

.ck-chart {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
  transition: padding-right 0.32s cubic-bezier(0.22, 0.68, 0.32, 1);
}

/* 详情浮在右侧时，把足迹收进左边剩下的宽度，别让浮层压住月份；
   平面视图很矮，这里给浮层留出足够的高度 */
.ck-body.is-detail {
  min-height: 424px;
}

.ck-body.is-detail .ck-chart {
  padding-right: 468px;
}

.ck-empty {
  display: grid;
  place-items: center;
  min-height: 168px;
  color: var(--pf-faint, rgba(244, 244, 245, 0.38));
  font-size: 12px;
}

/* ---------- 底部：四个关键数字 + 图例 ---------- */
.ck-foot {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-top: 10px;
  padding-top: 12px;
  border-top: 1px solid var(--pf-line, rgba(255, 255, 255, 0.07));
}

.ck-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(96px, auto));
  gap: 10px 30px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.ck-stats__label {
  margin: 0;
  color: var(--pf-faint, rgba(244, 244, 245, 0.38));
  font-size: 11px;
  letter-spacing: 0.04em;
}

.ck-stats__value {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin: 3px 0 0;
}

.ck-stats__value b {
  color: var(--pf-accent-glow, #7ef3ea);
  font-size: 24px;
  font-weight: 600;
  line-height: 1;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
}

.ck-stats__value span {
  color: var(--pf-muted, rgba(244, 244, 245, 0.55));
  font-size: 11px;
}

.ck-stats__sub {
  margin: 3px 0 0;
  color: rgba(244, 244, 245, 0.32);
  font-size: 11px;
  white-space: nowrap;
}

.ck-aside {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  flex: none;
}

.ck-legend {
  display: flex;
  align-items: center;
  gap: 5px;
}

.ck-legend__text {
  color: var(--pf-faint, rgba(244, 244, 245, 0.38));
  font-size: 11px;
}

.ck-legend__swatch {
  width: 11px;
  height: 11px;
  padding: 0;
  border: 0;
  border-radius: 3px;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.08);
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.ck-legend__swatch:hover,
.ck-legend__swatch.is-on {
  transform: scale(1.22);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.18), 0 0 10px rgba(0, 190, 180, 0.5);
}

.ck-hint {
  margin: 0;
  color: rgba(244, 244, 245, 0.3);
  font-size: 11px;
  letter-spacing: 0.03em;
}

@media (max-width: 900px) {
  /* 窄屏上详情是同一个卡片里换一屏，图表先让位 */
  .ck-body.is-detail .ck-chart {
    display: none;
  }
}

@media (max-width: 900px) {
  .ck-panel {
    padding: 14px 14px 10px;
  }

  .ck-head {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .ck-head__side {
    justify-content: flex-end;
  }

  .ck-foot {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .ck-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px 18px;
  }

  .ck-aside {
    align-items: flex-start;
  }
}

@media (prefers-reduced-motion: reduce) {
  .ck-switch__thumb,
  .ck-cta,
  .ck-legend__swatch,
  .ck-dot {
    transition: none;
  }
}
</style>
