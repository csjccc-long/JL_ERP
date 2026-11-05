<template>
  <div class="app-container workbench">
    <header class="workbench-head">
      <div class="head-text">
        <h1 class="greeting">{{ greeting }}，{{ nickName }}</h1>
        <p class="subline">
          <span>{{ today }}</span>
          <span class="dot">·</span>
          <span v-if="todoCount > 0">今天有 <b>{{ todoCount }}</b> 项业务待处理</span>
          <span v-else>今天的业务都已处理完毕</span>
        </p>
      </div>
      <div class="head-actions">
        <el-button @click="go('/knowledge/assistant')">
          <svg-icon icon-class="message" class="btn-icon" />
          询问智能助手
        </el-button>
        <el-button type="primary" @click="go('/erp/request')">
          <svg-icon icon-class="edit" class="btn-icon" />
          新建采购申请
        </el-button>
      </div>
    </header>

    <section class="metric-grid" v-loading="loadingOverview">
      <button
        v-for="card in metricCards"
        :key="card.key"
        type="button"
        class="metric"
        :class="'metric--' + card.tone"
        @click="go(card.path)"
      >
        <span class="metric-icon"><svg-icon :icon-class="card.icon" /></span>
        <span class="metric-body">
          <span class="metric-label">{{ card.label }}</span>
          <span class="metric-value">{{ card.value }}</span>
          <span class="metric-tip">{{ card.tip }}</span>
        </span>
      </button>
    </section>

    <el-row :gutter="16" class="workbench-row">
      <el-col :xs="24" :lg="16">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel-head">
              <div class="panel-title">
                待办任务
                <span v-if="pendingList.length" class="count-pill">{{ pendingList.length }}</span>
              </div>
              <el-button link type="primary" @click="go('/erp/dashboard')">
                业务驾驶舱
                <el-icon class="link-arrow"><arrow-right /></el-icon>
              </el-button>
            </div>
          </template>

          <div v-loading="loadingPending">
            <ul v-if="pendingList.length" class="task-list">
              <li v-for="item in pendingList.slice(0, 6)" :key="item.businessId + '-' + item.businessType" class="task-item" @click="openPending(item)">
                <span class="task-type">
                  <dict-tag :options="erp_business_type" :value="item.businessType" />
                </span>
                <span class="task-main">
                  <span class="task-title">{{ item.title || '未命名单据' }}</span>
                  <span class="task-meta">
                    {{ item.applicantName || '系统' }}
                    <span class="dot">·</span>
                    {{ item.createTime || '-' }}
                  </span>
                </span>
                <span class="task-status">{{ item.status }}</span>
                <el-icon class="task-arrow"><arrow-right /></el-icon>
              </li>
            </ul>
            <el-empty v-else description="暂无待办任务，业务流转顺畅" :image-size="72" />
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel-head">
              <div class="panel-title">快捷入口</div>
              <span class="panel-hint">常用业务一键直达</span>
            </div>
          </template>
          <div class="shortcut-grid">
            <button v-for="entry in shortcuts" :key="entry.path" type="button" class="shortcut" @click="go(entry.path)">
              <span class="shortcut-icon"><svg-icon :icon-class="entry.icon" /></span>
              <span class="shortcut-label">{{ entry.label }}</span>
            </button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="workbench-row">
      <el-col :xs="24" :lg="16">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel-head">
              <div class="panel-title">入库金额趋势</div>
              <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
                <el-radio-button :value="7">近 7 天</el-radio-button>
                <el-radio-button :value="30">近 30 天</el-radio-button>
                <el-radio-button :value="90">近 90 天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChartRef" class="trend-chart"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel-head">
              <div class="panel-title">
                低库存预警
                <span v-if="lowStockList.length" class="count-pill count-pill--warn">{{ lowStockList.length }}</span>
              </div>
              <el-button link type="primary" @click="go('/erp/inventory')">库存查询</el-button>
            </div>
          </template>
          <ul v-if="lowStockList.length" class="stock-list">
            <li v-for="item in lowStockList.slice(0, 6)" :key="item.warehouseName + item.materialName" class="stock-item">
              <span class="stock-main">
                <span class="stock-name">{{ item.materialName }}</span>
                <span class="stock-meta">{{ item.warehouseName }}</span>
              </span>
              <span class="stock-qty">{{ item.availableQuantity }}<em>{{ item.unit }}</em></span>
            </li>
          </ul>
          <el-empty v-else description="库存水位健康" :image-size="72" />
        </el-card>
      </el-col>
    </el-row>

    <section class="overview-strip" v-loading="loadingOverview">
      <div v-for="item in overviewItems" :key="item.label" class="overview-cell">
        <span class="overview-label">{{ item.label }}</span>
        <span class="overview-value">{{ item.value }}</span>
      </div>
    </section>
  </div>
</template>

<script setup name="Index">
import * as echarts from "echarts"
import { getOverview, getPending, getLowStock, getReceiptTrend } from "@/api/erp/dashboard"
import useUserStore from "@/store/modules/user"

const { erp_business_type } = useDict("erp_business_type")

const router = useRouter()
const userStore = useUserStore()
const nickName = computed(() => userStore.nickName || "管理员")

const loadingOverview = ref(true)
const loadingPending = ref(false)
const overview = ref({})
const pendingList = ref([])
const lowStockList = ref([])
const trendDays = ref(30)
const trendChartRef = ref(null)
let trendChart = null

const shortcuts = [
  { label: "供应商档案", path: "/erp/supplier", icon: "peoples" },
  { label: "合同台账", path: "/erp/contract", icon: "documentation" },
  { label: "采购申请", path: "/erp/request", icon: "clipboard" },
  { label: "采购订单", path: "/erp/order", icon: "shopping" },
  { label: "到货入库", path: "/erp/receipt", icon: "download" },
  { label: "库存查询", path: "/erp/inventory", icon: "build" },
  { label: "物料资料", path: "/erp/base/material", icon: "component" },
  { label: "智能助手", path: "/knowledge/assistant", icon: "message" }
]

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return "夜深了"
  if (hour < 9) return "早上好"
  if (hour < 12) return "上午好"
  if (hour < 14) return "中午好"
  if (hour < 18) return "下午好"
  return "晚上好"
})

const today = computed(() => {
  const now = new Date()
  const week = ["日", "一", "二", "三", "四", "五", "六"][now.getDay()]
  return `${now.getFullYear()} 年 ${now.getMonth() + 1} 月 ${now.getDate()} 日 星期${week}`
})

const todoCount = computed(() => {
  const data = overview.value
  return Number(data.requestPending || 0) + Number(data.orderPending || 0) + Number(data.orderOverdue || 0)
})

function formatAmount(value) {
  const num = Number(value || 0)
  if (num >= 10000) {
    return (num / 10000).toFixed(2) + " 万"
  }
  return num.toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const metricCards = computed(() => {
  const data = overview.value
  return [
    {
      key: "requestPending",
      label: "待审批申请",
      value: data.requestPending ?? 0,
      tip: "采购申请待您处理",
      icon: "clipboard",
      tone: "accent",
      path: "/erp/request"
    },
    {
      key: "orderPending",
      label: "待审批订单",
      value: data.orderPending ?? 0,
      tip: "部分到货 " + (data.orderPartial ?? 0) + " 单",
      icon: "shopping",
      tone: "warning",
      path: "/erp/order"
    },
    {
      key: "orderOverdue",
      label: "逾期未到货",
      value: data.orderOverdue ?? 0,
      tip: "期望到货日期已过",
      icon: "time",
      tone: "danger",
      path: "/erp/order"
    },
    {
      key: "lowStockCount",
      label: "低库存物料",
      value: data.lowStockCount ?? 0,
      tip: "需关注补货计划",
      icon: "build",
      tone: "success",
      path: "/erp/inventory"
    }
  ]
})

const overviewItems = computed(() => {
  const data = overview.value
  return [
    { label: "供应商总数", value: data.supplierTotal ?? 0 },
    { label: "生效合同", value: data.contractActive ?? 0 },
    { label: "30 天内到期合同", value: data.contractExpiring ?? 0 },
    { label: "库存金额", value: formatAmount(data.inventoryAmount) },
    { label: "本月入库金额", value: formatAmount(data.monthlyReceiptAmount) },
    { label: "累计入库单", value: data.receiptTotal ?? 0 }
  ]
})

function go(path) {
  router.push(path)
}

function openPending(item) {
  const map = {
    contract: "/erp/contract",
    request: "/erp/request",
    order: "/erp/order",
    receipt: "/erp/receipt"
  }
  router.push(map[item.businessType] || "/erp/dashboard")
}

function loadOverview() {
  loadingOverview.value = true
  getOverview().then(response => {
    const data = response.data || {}
    overview.value = data.cards || data
  }).finally(() => {
    loadingOverview.value = false
  })
}

function loadPending() {
  loadingPending.value = true
  getPending().then(response => {
    pendingList.value = response.data || []
  }).finally(() => {
    loadingPending.value = false
  })
}

function loadLowStock() {
  getLowStock().then(response => {
    lowStockList.value = response.data || []
  })
}

function loadTrend() {
  getReceiptTrend(trendDays.value).then(response => {
    renderTrend(response.data || [])
  })
}

function renderTrend(list) {
  if (!trendChartRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  const dates = list.map(item => item.bizDate || item.date || item.days || "")
  const amounts = list.map(item => Number(item.amount || item.receiptAmount || 0))
  trendChart.setOption({
    tooltip: {
      trigger: "axis",
      backgroundColor: "rgba(22,22,24,0.92)",
      borderWidth: 0,
      textStyle: { color: "#f4f4f5", fontSize: 12 },
      axisPointer: { lineStyle: { color: "rgba(0,190,180,0.5)" } }
    },
    grid: { left: 8, right: 16, top: 18, bottom: 6, containLabel: true },
    xAxis: {
      type: "category",
      boundaryGap: false,
      data: dates,
      axisLine: { lineStyle: { color: "rgba(22,22,24,0.1)" } },
      axisTick: { show: false },
      axisLabel: { color: "#9a9da1", fontSize: 11, rotate: dates.length > 15 ? 30 : 0 }
    },
    yAxis: {
      type: "value",
      splitLine: { lineStyle: { color: "rgba(22,22,24,0.06)", type: "dashed" } },
      axisLabel: { color: "#9a9da1", fontSize: 11 }
    },
    series: [
      {
        name: "入库金额",
        type: "line",
        smooth: true,
        showSymbol: dates.length <= 31,
        symbol: "circle",
        symbolSize: 5,
        data: amounts,
        lineStyle: { width: 2, color: "#00beb4" },
        itemStyle: { color: "#00beb4", borderWidth: 2, borderColor: "#fff" },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: "rgba(0,190,180,0.20)" },
            { offset: 1, color: "rgba(0,190,180,0)" }
          ])
        }
      }
    ]
  }, true)
}

let chartResizeObserver = null
let chartResizeFrame = 0

function resizeChart() {
  if (!trendChart) return
  const el = trendChartRef.value
  if (!el || !el.clientWidth || !el.clientHeight) return
  trendChart.resize()
}

/** 合并同一帧内的多次尺寸变化（侧边栏 280ms 过渡期间会连续触发） */
function scheduleChartResize() {
  if (chartResizeFrame) cancelAnimationFrame(chartResizeFrame)
  chartResizeFrame = requestAnimationFrame(() => {
    chartResizeFrame = 0
    resizeChart()
  })
}

/** 监听图表容器自身的尺寸变化：折叠/展开侧边栏、窗口缩放、栅格换行都会触发 */
function observeChart() {
  if (chartResizeObserver || typeof ResizeObserver === "undefined" || !trendChartRef.value) return
  chartResizeObserver = new ResizeObserver(scheduleChartResize)
  chartResizeObserver.observe(trendChartRef.value)
}

onMounted(() => {
  loadOverview()
  loadPending()
  loadLowStock()
  loadTrend()
  window.addEventListener("resize", scheduleChartResize)
  observeChart()
})

onActivated(() => {
  loadOverview()
  loadPending()
  loadLowStock()
  loadTrend()
  nextTick(() => {
    observeChart()
    resizeChart()
  })
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", scheduleChartResize)
  if (chartResizeObserver) {
    chartResizeObserver.disconnect()
    chartResizeObserver = null
  }
  if (chartResizeFrame) {
    cancelAnimationFrame(chartResizeFrame)
    chartResizeFrame = 0
  }
  trendChart && trendChart.dispose()
  trendChart = null
})
</script>

<style lang="scss" scoped>
.workbench {
  max-width: 1560px;
  margin: 0 auto;
}

.workbench-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;

  .greeting {
    margin: 0;
    font-size: 22px;
    font-weight: 600;
    letter-spacing: 0.2px;
    color: #161618;
  }

  .subline {
    margin: 6px 0 0;
    font-size: 13px;
    color: #75787a;

    b {
      color: #161618;
      font-weight: 600;
    }
  }

  .dot {
    margin: 0 6px;
    color: #b2b5b7;
  }

  .head-actions {
    display: flex;
    gap: 10px;
    flex-shrink: 0;
  }

  .btn-icon {
    margin-right: 6px;
    font-size: 14px;
  }
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.metric {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 18px;
  border: 1px solid rgba(22, 22, 24, 0.08);
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.04);
  text-align: left;
  cursor: pointer;
  font-family: inherit;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;

  &:hover {
    border-color: rgba(0, 190, 180, 0.3);
    box-shadow: 0 10px 24px rgba(16, 24, 40, 0.08);
    transform: translateY(-1px);
  }
}

.metric-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  font-size: 18px;
  flex-shrink: 0;
}

.metric-body {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.metric-label {
  font-size: 13px;
  color: #75787a;
}

.metric-value {
  margin: 4px 0 2px;
  font-size: 26px;
  font-weight: 600;
  line-height: 1.15;
  color: #161618;
  font-variant-numeric: tabular-nums;
}

.metric-tip {
  font-size: 12px;
  color: #9a9da1;
}

.metric--accent .metric-icon {
  background: rgba(0, 190, 180, 0.1);
  color: #00a89f;
}

.metric--warning .metric-icon {
  background: rgba(250, 173, 20, 0.12);
  color: #c98a09;
}

.metric--danger .metric-icon {
  background: rgba(216, 73, 75, 0.1);
  color: #c03b3d;
}

.metric--success .metric-icon {
  background: rgba(59, 160, 92, 0.1);
  color: #2f8a4c;
}

.workbench-row {
  margin-bottom: 16px;
}

.panel {
  height: 100%;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #161618;
}

.panel-hint {
  font-size: 12px;
  color: #b2b5b7;
}

.count-pill {
  min-width: 20px;
  height: 18px;
  padding: 0 6px;
  border-radius: 9px;
  background: rgba(0, 190, 180, 0.12);
  color: #00a89f;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
  font-weight: 600;
}

.count-pill--warn {
  background: rgba(250, 173, 20, 0.14);
  color: #b87600;
}

.link-arrow {
  margin-left: 2px;
  font-size: 12px;
}

.task-list,
.stock-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 6px;
  border-bottom: 1px solid rgba(22, 22, 24, 0.05);
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.15s ease;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: #f6fbfa;
  }

  .task-type {
    flex-shrink: 0;
  }

  .task-main {
    display: flex;
    flex-direction: column;
    gap: 3px;
    min-width: 0;
    flex: 1;
  }

  .task-title {
    font-size: 13.5px;
    color: #2f3133;
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .task-meta {
    font-size: 12px;
    color: #9a9da1;
  }

  .task-status {
    flex-shrink: 0;
    font-size: 12px;
    color: #75787a;
    background: #f4f5f6;
    border-radius: 6px;
    padding: 2px 8px;
  }

  .task-arrow {
    flex-shrink: 0;
    color: #c8cacc;
    font-size: 12px;
  }
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.shortcut {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 12px 4px;
  border: none;
  border-radius: 10px;
  background: transparent;
  cursor: pointer;
  font-family: inherit;
  transition: background-color 0.15s ease;

  &:hover {
    background: #f4f5f6;

    .shortcut-icon {
      color: #00a89f;
      background: rgba(0, 190, 180, 0.1);
    }
  }
}

.shortcut-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: #f4f5f6;
  color: #5c5f63;
  font-size: 17px;
  transition: color 0.15s ease, background-color 0.15s ease;
}

.shortcut-label {
  font-size: 12px;
  color: #5c5f63;
  white-space: nowrap;
}

.trend-chart {
  height: 244px;
  width: 100%;
}

.stock-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 11px 6px;
  border-bottom: 1px solid rgba(22, 22, 24, 0.05);

  &:last-child {
    border-bottom: none;
  }
}

.stock-main {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.stock-name {
  font-size: 13px;
  color: #2f3133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stock-meta {
  font-size: 12px;
  color: #9a9da1;
}

.stock-qty {
  flex-shrink: 0;
  font-size: 15px;
  font-weight: 600;
  color: #c98a09;
  font-variant-numeric: tabular-nums;

  em {
    margin-left: 2px;
    font-size: 11px;
    font-style: normal;
    font-weight: 400;
    color: #9a9da1;
  }
}

.overview-strip {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 1px;
  overflow: hidden;
  border: 1px solid rgba(22, 22, 24, 0.08);
  border-radius: 12px;
  background: rgba(22, 22, 24, 0.06);
}

.overview-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px 18px;
  background: #fff;
}

.overview-label {
  font-size: 12px;
  color: #75787a;
}

.overview-value {
  font-size: 17px;
  font-weight: 600;
  color: #161618;
  font-variant-numeric: tabular-nums;
}

@media (max-width: 1200px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .overview-strip {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .workbench-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }

  .shortcut-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .overview-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
