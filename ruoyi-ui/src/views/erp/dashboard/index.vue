<template>
  <div class="app-container dashboard">
    <el-row :gutter="16" v-loading="loading">
      <el-col :xs="12" :sm="8" :md="6" v-for="card in cards" :key="card.key">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-title">{{ card.title }}</div>
          <div class="metric-value" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="metric-tip">{{ card.tip }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>近 {{ trendDays }} 天入库金额趋势</span>
              <div class="card-tools">
                <chart-mode-switch v-model="trendMode" :options="trendModes" />
                <el-select v-model="trendDays" size="small" style="width: 110px" @change="loadTrend">
                  <el-option label="近 7 天" :value="7" />
                  <el-option label="近 30 天" :value="30" />
                  <el-option label="近 90 天" :value="90" />
                </el-select>
              </div>
            </div>
          </template>
          <div class="chart-box">
            <div ref="trendChartRef" class="chart" v-show="trendMode === '2d'"></div>
            <rain-chart
              v-if="trendMode === 'rain' && trendList.length"
              class="chart-rain"
              :labels="trendLabels"
              :dates="trendDateKeys"
              :values="trendAmounts"
              :counts="trendCounts"
              :value-format="formatAmount"
            />
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>采购订单状态分布</span>
              <span class="card-extra">共 {{ orderStatusTotal }} 单</span>
            </div>
          </template>
          <div class="status-body">
            <template v-if="statusSegments.length">
              <DonutChart
                :data="statusSegments"
                :size="236"
                :stroke-width="28"
                :animation-duration="1.2"
                :animation-delay-per-segment="0.06"
                :active-label="hoveredStatusLabel"
                aria-label="采购订单状态分布环形图"
                @segment-hover="handleStatusHover"
              >
                <template #center="{ total, hovered, percent }">
                  <Transition name="donut-center" mode="out-in">
                    <div :key="hovered ? hovered.label : 'total'" class="status-center">
                      <p class="status-center__label">{{ hovered ? hovered.label : "订单总数" }}</p>
                      <p class="status-center__value">{{ hovered ? hovered.value : total }}</p>
                      <p v-if="hovered" class="status-center__percent">占比 {{ Math.round(percent) }}%</p>
                    </div>
                  </Transition>
                </template>
              </DonutChart>

              <DonutChartLegend
                class="status-legend"
                :data="statusSegments"
                :total="orderStatusTotal"
                :active-label="hoveredStatusLabel"
                @hover="handleStatusHover"
              />
            </template>
            <el-empty v-else-if="!loading" description="暂无采购订单数据" :image-size="70" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>待办任务</span>
              <el-button link type="primary" @click="loadPending">刷新</el-button>
            </div>
          </template>
          <el-table :data="pendingList" size="small" max-height="320">
            <el-table-column label="业务类型" prop="businessType" width="130" align="center">
              <template #default="scope">
                <dict-tag :options="erp_business_type" :value="scope.row.businessType" />
              </template>
            </el-table-column>
            <el-table-column label="单号 / 名称" prop="title" min-width="180" show-overflow-tooltip />
            <el-table-column label="状态" prop="status" width="120" align="center" />
            <el-table-column label="申请人" prop="applicantName" width="100" align="center" />
            <el-table-column label="时间" prop="createTime" width="160" align="center" />
          </el-table>
          <el-empty v-if="pendingList.length === 0" description="暂无待办任务" :image-size="70" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>低库存预警（TOP 10）</span>
              <el-button link type="primary" @click="loadLowStock">刷新</el-button>
            </div>
          </template>
          <el-table :data="lowStockList" size="small" max-height="320">
            <el-table-column label="仓库" prop="warehouseName" width="120" align="center" show-overflow-tooltip />
            <el-table-column label="物料名称" prop="materialName" min-width="160" show-overflow-tooltip />
            <el-table-column label="可用数量" prop="availableQuantity" width="110" align="center" />
            <el-table-column label="单位" prop="unit" width="70" align="center" />
          </el-table>
          <el-empty v-if="lowStockList.length === 0" description="暂无低库存物料" :image-size="70" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>近 7 天高频问询</span>
              <el-button link type="primary" @click="loadAudit">刷新</el-button>
            </div>
          </template>
          <el-table :data="topQuestions" size="small" max-height="320">
            <el-table-column label="#" type="index" width="55" align="center" />
            <el-table-column label="问题" prop="question" min-width="220" show-overflow-tooltip />
            <el-table-column label="次数" prop="total" width="80" align="center" />
          </el-table>
          <el-empty v-if="topQuestions.length === 0" description="暂无问询记录" :image-size="70" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>最近失败问询</span>
              <el-button link type="primary" @click="loadAudit">刷新</el-button>
            </div>
          </template>
          <el-table :data="recentFailures" size="small" max-height="320">
            <el-table-column label="状态" prop="status" width="150" align="center">
              <template #default="scope">
                <dict-tag :options="erp_audit_status" :value="scope.row.status" />
              </template>
            </el-table-column>
            <el-table-column label="问题" prop="question" min-width="200" show-overflow-tooltip />
            <el-table-column label="时间" prop="createdTime" width="160" align="center" />
          </el-table>
          <el-empty v-if="recentFailures.length === 0" description="近期无失败问询" :image-size="70" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="ErpDashboard">
import * as echarts from "echarts"
import { getOverview, getPending, getLowStock, getReceiptTrend } from "@/api/erp/dashboard"
import { auditOverview } from "@/api/knowledge/audit"
import DonutChart from "@/components/DonutChart/index.vue"
import DonutChartLegend from "@/components/DonutChart/Legend.vue"
import RainChart from "@/components/RainChart/index.vue"
import ChartModeSwitch from "@/components/ChartModeSwitch/index.vue"

const { erp_business_type, erp_order_status, erp_audit_status } = useDict("erp_business_type", "erp_order_status", "erp_audit_status")

const loading = ref(true)
const cards = ref([])
const pendingList = ref([])
const lowStockList = ref([])
const topQuestions = ref([])
const recentFailures = ref([])
const trendDays = ref(30)
const trendChartRef = ref(null)
const trendList = ref([])
const trendMode = ref("rain")

/** 趋势表现的切换项：默认为「数据成雨」，保留原来的平面折线，不再提供三维山形 */
const trendModes = [
  { value: "rain", label: "雨", title: "数据成雨：入库金额化作雨幕" },
  { value: "2d", label: "2D", title: "平面折线" }
]
const statusRaw = ref([])
const hoveredStatus = ref(null)
let trendChart = null

// 采购订单状态配色（按 erp_order_status 字典语义）
const STATUS_COLORS = {
  DRAFT: "#b2b5b7",
  PENDING_APPROVAL: "#faad14",
  APPROVED: "#00beb4",
  PARTIALLY_RECEIVED: "#f7863b",
  RECEIVED: "#3ba05c",
  REJECTED: "#d8494b",
  CANCELLED: "#8f9aa6"
}
const STATUS_FALLBACK_COLORS = ["#5b8ff9", "#9270ca", "#ff9d4d", "#269a99", "#ff99c3", "#66b2ff"]

const statusSegments = computed(() => {
  const dictOrder = []
  const dictLabels = {}
  erp_order_status.value.forEach(item => {
    dictOrder.push(item.value)
    dictLabels[item.value] = item.label
  })
  const list = (statusRaw.value || []).map(item => {
    const key = item.orderStatus || item.status || ""
    return {
      key,
      label: dictLabels[key] || key || "未知",
      value: Number(item.count || item.total || 0)
    }
  })
  list.sort((a, b) => {
    const ai = dictOrder.indexOf(a.key)
    const bi = dictOrder.indexOf(b.key)
    return (ai === -1 ? 999 : ai) - (bi === -1 ? 999 : bi)
  })
  return list.map((item, index) => ({
    ...item,
    color: STATUS_COLORS[item.key] || STATUS_FALLBACK_COLORS[index % STATUS_FALLBACK_COLORS.length]
  }))
})

const orderStatusTotal = computed(() => statusSegments.value.reduce((sum, item) => sum + item.value, 0))

const hoveredStatusLabel = computed(() => hoveredStatus.value?.label ?? null)

function handleStatusHover(segment) {
  hoveredStatus.value = segment || null
}

function formatAmount(value) {
  const num = Number(value || 0)
  if (num >= 10000) {
    return (num / 10000).toFixed(2) + " 万"
  }
  return num.toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function buildCards(data) {
  return [
    { key: "supplierTotal", title: "供应商总数", value: data.supplierTotal ?? 0, tip: "待准入 " + (data.supplierPending ?? 0) + " 家", color: "#00a89f" },
    { key: "contractActive", title: "生效合同", value: data.contractActive ?? 0, tip: "30 天内到期 " + (data.contractExpiring ?? 0) + " 份", color: "#3ba05c" },
    { key: "requestPending", title: "待审批申请", value: data.requestPending ?? 0, tip: "采购申请", color: "#d99a06" },
    { key: "orderPending", title: "待审批订单", value: data.orderPending ?? 0, tip: "部分到货 " + (data.orderPartial ?? 0) + " 单", color: "#d99a06" },
    { key: "orderOverdue", title: "逾期未到货", value: data.orderOverdue ?? 0, tip: "期望到货日期已过", color: "#d8494b" },
    { key: "inventoryAmount", title: "库存金额", value: formatAmount(data.inventoryAmount), tip: "低库存物料 " + (data.lowStockCount ?? 0) + " 项", color: "#00a89f" },
    { key: "monthlyReceiptAmount", title: "本月入库金额", value: formatAmount(data.monthlyReceiptAmount), tip: "累计入库 " + (data.receiptTotal ?? 0) + " 单", color: "#3ba05c" }
  ]
}

function loadOverview() {
  loading.value = true
  getOverview().then(response => {
    const data = response.data || {}
    const overview = data.cards || data
    cards.value = buildCards(overview)
    statusRaw.value = data.orderStatus || overview.orderStatus || []
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

function loadAudit() {
  auditOverview().then(response => {
    const data = response.data || {}
    topQuestions.value = data.topQuestions || []
    recentFailures.value = data.recentFailures || []
  }).catch(() => {})
}

function loadPending() {
  getPending().then(response => {
    pendingList.value = response.data || []
  })
}

function loadLowStock() {
  getLowStock().then(response => {
    lowStockList.value = response.data || []
  })
}

function loadTrend() {
  getReceiptTrend(trendDays.value).then(response => {
    trendList.value = response.data || []
    renderTrendChart(trendList.value)
  })
}

const trendLabels = computed(() =>
  trendList.value.map(item => {
    const raw = item.statDate || item.bizDate || item.date || item.days || ""
    return raw ? String(raw).slice(5) : ""
  })
)

const trendDateKeys = computed(() =>
  trendList.value.map(item => {
    const raw = item.statDate || item.bizDate || item.date || item.days || ""
    return raw ? String(raw).slice(0, 10) : ""
  })
)

const trendAmounts = computed(() => trendList.value.map(item => Number(item.amount || item.receiptAmount || 0)))

const trendCounts = computed(() => trendList.value.map(item => Number(item.receiptCount || 0)))

watch(trendMode, mode => {
  if (mode !== "2d") return
  nextTick(() => {
    trendChart && trendChart.resize()
  })
})

function renderTrendChart(list) {
  if (!trendChartRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  const dates = list.map(item => item.statDate || item.bizDate || item.date || item.days || "")
  const amounts = list.map(item => Number(item.amount || item.receiptAmount || 0))
  trendChart.setOption({
    tooltip: { trigger: "axis" },
    grid: { left: 50, right: 20, top: 30, bottom: 40 },
    xAxis: { type: "category", data: dates, axisLabel: { rotate: dates.length > 15 ? 40 : 0 } },
    yAxis: { type: "value", name: "金额" },
    series: [
      {
        name: "入库金额",
        type: "line",
        smooth: true,
        data: amounts,
        lineStyle: { width: 2, color: "#00beb4" },
        itemStyle: { color: "#00beb4" },
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

function resizeCharts() {
  trendChart && trendChart.resize()
}

onMounted(() => {
  loadOverview()
  loadPending()
  loadLowStock()
  loadTrend()
  loadAudit()
  window.addEventListener("resize", resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", resizeCharts)
  trendChart && trendChart.dispose()
  trendChart = null
})
</script>

<style scoped>
.dashboard .metric-card {
  margin-bottom: 16px;
}
.metric-title {
  color: #909399;
  font-size: 13px;
}
.metric-value {
  font-size: 26px;
  font-weight: 600;
  margin: 6px 0;
}
.metric-tip {
  color: #c0c4cc;
  font-size: 12px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.chart-box {
  position: relative;
  height: 300px;
}
.chart {
  height: 300px;
  width: 100%;
}
.chart-rain {
  position: absolute;
  inset: 0;
}
.card-tools {
  display: flex;
  align-items: center;
  gap: 10px;
}
.card-extra {
  color: #909399;
  font-size: 12px;
  font-weight: 400;
}
.status-body {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18px;
  flex-wrap: wrap;
}
.status-legend {
  flex: 1 1 190px;
  max-width: 260px;
}
.status-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  max-width: 140px;
}
.status-center__label {
  margin: 0;
  max-width: 100%;
  font-size: 12px;
  line-height: 1.4;
  color: #75787a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.status-center__value {
  margin: 2px 0 0;
  font-size: 30px;
  font-weight: 600;
  line-height: 1.1;
  color: #161618;
  font-variant-numeric: tabular-nums;
}
.status-center__percent {
  margin: 4px 0 0;
  font-size: 12px;
  color: #00a89f;
}
.donut-center-enter-active,
.donut-center-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}
.donut-center-enter-from,
.donut-center-leave-to {
  opacity: 0;
  transform: scale(0.9);
}
</style>
