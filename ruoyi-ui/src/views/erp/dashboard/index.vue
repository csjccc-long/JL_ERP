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
              <el-select v-model="trendDays" size="small" style="width: 110px" @change="loadTrend">
                <el-option label="近 7 天" :value="7" />
                <el-option label="近 30 天" :value="30" />
                <el-option label="近 90 天" :value="90" />
              </el-select>
            </div>
          </template>
          <div ref="trendChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>采购订单状态分布</span>
            </div>
          </template>
          <div ref="statusChartRef" class="chart"></div>
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

const { erp_business_type, erp_order_status, erp_audit_status } = useDict("erp_business_type", "erp_order_status", "erp_audit_status")

const loading = ref(true)
const cards = ref([])
const pendingList = ref([])
const lowStockList = ref([])
const topQuestions = ref([])
const recentFailures = ref([])
const trendDays = ref(30)
const trendChartRef = ref(null)
const statusChartRef = ref(null)
let trendChart = null
let statusChart = null

function formatAmount(value) {
  const num = Number(value || 0)
  if (num >= 10000) {
    return (num / 10000).toFixed(2) + " 万"
  }
  return num.toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function buildCards(data) {
  return [
    { key: "supplierTotal", title: "供应商总数", value: data.supplierTotal ?? 0, tip: "待准入 " + (data.supplierPending ?? 0) + " 家", color: "#409eff" },
    { key: "contractActive", title: "生效合同", value: data.contractActive ?? 0, tip: "30 天内到期 " + (data.contractExpiring ?? 0) + " 份", color: "#67c23a" },
    { key: "requestPending", title: "待审批申请", value: data.requestPending ?? 0, tip: "采购申请", color: "#e6a23c" },
    { key: "orderPending", title: "待审批订单", value: data.orderPending ?? 0, tip: "部分到货 " + (data.orderPartial ?? 0) + " 单", color: "#e6a23c" },
    { key: "orderOverdue", title: "逾期未到货", value: data.orderOverdue ?? 0, tip: "期望到货日期已过", color: "#f56c6c" },
    { key: "inventoryAmount", title: "库存金额", value: formatAmount(data.inventoryAmount), tip: "低库存物料 " + (data.lowStockCount ?? 0) + " 项", color: "#409eff" },
    { key: "monthlyReceiptAmount", title: "本月入库金额", value: formatAmount(data.monthlyReceiptAmount), tip: "累计入库 " + (data.receiptTotal ?? 0) + " 单", color: "#67c23a" }
  ]
}

function loadOverview() {
  loading.value = true
  getOverview().then(response => {
    const data = response.data || {}
    const overview = data.cards || data
    cards.value = buildCards(overview)
    renderStatusChart(data.orderStatus || overview.orderStatus || [])
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
    renderTrendChart(response.data || [])
  })
}

function renderTrendChart(list) {
  if (!trendChartRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  const dates = list.map(item => item.bizDate || item.date || item.days || "")
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
        areaStyle: {},
        data: amounts,
        itemStyle: { color: "#409eff" }
      }
    ]
  }, true)
}

function renderStatusChart(list) {
  if (!statusChartRef.value) return
  if (!statusChart) {
    statusChart = echarts.init(statusChartRef.value)
  }
  const orderDictMap = {}
  erp_order_status.value.forEach(item => {
    orderDictMap[item.value] = item.label
  })
  const data = (list || []).map(item => {
    const key = item.orderStatus || item.status
    return { name: orderDictMap[key] || key || "未知", value: Number(item.count || item.total || 0) }
  })
  statusChart.setOption({
    tooltip: { trigger: "item" },
    legend: { bottom: 0 },
    series: [
      {
        name: "订单状态",
        type: "pie",
        radius: ["40%", "65%"],
        avoidLabelOverlap: true,
        label: { formatter: "{b}: {c}" },
        data: data
      }
    ]
  }, true)
}

function resizeCharts() {
  trendChart && trendChart.resize()
  statusChart && statusChart.resize()
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
  statusChart && statusChart.dispose()
  trendChart = null
  statusChart = null
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
.chart {
  height: 300px;
  width: 100%;
}
</style>
