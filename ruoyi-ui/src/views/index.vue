<template>
  <div ref="pageRef" class="app-container workbench">
    <header class="workbench-head">
      <div class="head-text">
        <h1 class="greeting">{{ greeting }}，{{ nickName }}</h1>
        <p class="subline">
          <span>{{ today }}</span>
          <span class="dot">·</span>
          <span v-if="todoCount > 0">今天有 <b>{{ todoCount }}</b> 项业务待处理</span>
          <span v-else>今天的业务都已处理完毕</span>
          <template v-if="updatedAt">
            <span class="dot">·</span>
            <span class="muted">数据更新于 {{ updatedAt }}</span>
          </template>
        </p>
      </div>
      <div class="head-actions">
        <el-button circle :loading="refreshing" title="刷新数据" @click="refreshAll">
          <el-icon v-if="!refreshing"><refresh /></el-icon>
        </el-button>
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
                入库金额趋势
                <span class="panel-sub">近 {{ trendDays }} 天</span>
              </div>
              <div class="panel-tools">
                <span class="panel-total">合计 {{ formatAmount(trendTotal) }} · {{ trendCount }} 单</span>
                <chart-mode-switch v-model="trendMode" class="panel-mode" />
                <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
                  <el-radio-button :value="7">7 天</el-radio-button>
                  <el-radio-button :value="30">30 天</el-radio-button>
                  <el-radio-button :value="90">90 天</el-radio-button>
                </el-radio-group>
              </div>
            </div>
          </template>
          <div class="chart-wrap chart-wrap--trend" v-loading="loadingTrend">
            <div ref="trendChartRef" class="chart" v-show="trendMode === '2d'"></div>
            <ridge-chart
              v-if="trendMode === '3d' && !loadingTrend && trendList.length"
              class="chart-ridge"
              :labels="trendDates"
              :series="trendSeries"
              :value-format="formatAmount"
            />
            <div v-if="!loadingTrend && !trendList.length" class="chart-empty">
              <el-empty description="所选区间暂无入库记录" :image-size="56" />
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel-head">
              <div class="panel-title">
                采购订单状态
                <span class="panel-sub">共 {{ statusTotal }} 单</span>
              </div>
              <el-button link type="primary" @click="go('/erp/order')">
                订单列表
                <el-icon class="link-arrow"><arrow-right /></el-icon>
              </el-button>
            </div>
          </template>
          <div v-if="statusSegments.length" class="status-body">
            <DonutChart
              :data="statusSegments"
              :total-value="statusTotal"
              :size="180"
              :stroke-width="22"
              :animation-duration="1.1"
              :animation-delay-per-segment="0.06"
              :active-label="hoveredStatusLabel"
              aria-label="采购订单状态分布"
              @segment-hover="handleStatusHover"
            >
              <template #center="{ total, hovered }">
                <Transition name="center-swap" mode="out-in">
                  <div :key="hovered ? hovered.label : 'total'" class="status-center">
                    <p class="status-center__label">{{ hovered ? hovered.label : '订单总数' }}</p>
                    <p class="status-center__value">{{ hovered ? hovered.value : total }}</p>
                    <p class="status-center__percent">
                      {{ hovered ? '占比 ' + statusPercent(hovered.value) + '%' : '共 ' + statusSegments.length + ' 类状态' }}
                    </p>
                  </div>
                </Transition>
              </template>
            </DonutChart>
            <DonutChartLegend
              class="status-legend"
              :data="statusSegments"
              :total="statusTotal"
              :active-label="hoveredStatusLabel"
              @hover="handleLegendHover"
            />
          </div>
          <el-empty v-else description="暂无采购订单" :image-size="64" />
        </el-card>
      </el-col>
    </el-row>

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
                <span class="task-status" :class="'task-status--' + pendingStatusMeta(item.status).tone">
                  {{ pendingStatusMeta(item.status).label }}
                </span>
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
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel-head">
              <div class="panel-title">
                采购额 TOP 供应商
                <span class="panel-sub">按累计订单金额</span>
              </div>
              <el-button link type="primary" @click="go('/erp/order')">
                订单列表
                <el-icon class="link-arrow"><arrow-right /></el-icon>
              </el-button>
            </div>
          </template>
          <div class="chart-wrap chart-wrap--bar" :style="{ height: supplierChartHeight + 'px' }">
            <div ref="supplierChartRef" class="chart"></div>
            <div v-if="!supplierRows.length" class="chart-empty">
              <el-empty description="暂无采购订单数据" :image-size="56" />
            </div>
          </div>
          <p class="chart-foot" v-if="supplierRows.length">
            采购记录覆盖 {{ supplierStats.count }} 家供应商 · 累计采购额 {{ formatAmount(supplierStats.amount) }}
            <span v-if="supplierRows.length > 1" class="chart-foot__extra">
              · TOP1 占比 {{ supplierTopShare.toFixed(0) }}%
            </span>
          </p>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel-head">
              <div class="panel-title">
                库存金额构成
                <span class="panel-sub">合计 {{ formatAmount(stockTotal) }}</span>
              </div>
              <el-button link type="primary" @click="go('/erp/inventory')">
                库存查询
                <el-icon class="link-arrow"><arrow-right /></el-icon>
              </el-button>
            </div>
          </template>

          <div class="chart-wrap chart-wrap--stock">
            <div ref="stockChartRef" class="chart"></div>
            <div v-if="!stockRows.length" class="chart-empty chart-empty--plain">
              <span class="chart-empty__hint">暂无库存数据</span>
            </div>
          </div>

          <ul v-if="stockRows.length" class="stock-legend">
            <li v-for="(item, index) in stockRows" :key="item.label">
              <span class="legend-dot" :style="{ background: stockColor(index) }"></span>
              <span class="legend-name">{{ item.label }}</span>
              <span class="legend-amount">{{ formatAmount(item.amount) }}</span>
              <span class="legend-percent">{{ stockPercent(item.amount) }}%</span>
            </li>
          </ul>

          <div v-if="lowStockList.length" class="stock-alert">
            <div class="stock-alert__head">
              <span class="stock-alert__title">
                <el-icon><warning /></el-icon>
                {{ lowStockList.length }} 项低库存预警
              </span>
              <el-button link type="primary" @click="go('/erp/inventory')">去处理</el-button>
            </div>
            <ul class="stock-alert__list">
              <li v-for="item in lowStockList.slice(0, 3)" :key="item.warehouseName + item.materialName">
                <span class="stock-alert__name">{{ item.materialName }}</span>
                <span class="stock-alert__meta">{{ item.warehouseName }} · {{ item.specification || '暂无规格' }}</span>
                <span class="stock-alert__qty">{{ item.availableQuantity }}<em>{{ item.unit }}</em></span>
              </li>
            </ul>
          </div>
          <div v-else class="stock-healthy">
            <el-icon><circle-check /></el-icon>
            库存水位健康，暂无低库存物料
          </div>
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
import { listOrder } from "@/api/erp/order"
import { listBalance } from "@/api/erp/inventory"
import useUserStore from "@/store/modules/user"
import DonutChart from "@/components/DonutChart/index.vue"
import DonutChartLegend from "@/components/DonutChart/Legend.vue"
import RidgeChart from "@/components/RidgeChart/index.vue"
import ChartModeSwitch from "@/components/ChartModeSwitch/index.vue"

const { erp_business_type, erp_order_status } = useDict("erp_business_type", "erp_order_status")

const router = useRouter()
const userStore = useUserStore()
const nickName = computed(() => userStore.nickName || "管理员")

const pageRef = ref(null)
const loadingOverview = ref(true)
const loadingPending = ref(false)
const loadingTrend = ref(true)
const refreshing = ref(false)
const updatedAt = ref("")

const overview = ref({})
const pendingList = ref([])
const lowStockList = ref([])

const trendDays = ref(30)
const trendList = ref([])
const trendChartRef = ref(null)
const trendMode = ref("2d")

const statusRaw = ref([])

/** 待办状态的通俗文案与色调（后端返回的是英文枚举） */
const PENDING_STATUS = {
  DRAFT: { label: '草稿', tone: 'info' },
  PENDING: { label: '待处理', tone: 'warning' },
  PENDING_APPROVAL: { label: '待审批', tone: 'warning' },
  APPROVED: { label: '已审批', tone: 'success' },
  PARTIALLY_RECEIVED: { label: '部分到货', tone: 'warning' },
  RECEIVED: { label: '已到货', tone: 'success' },
  REJECTED: { label: '已驳回', tone: 'danger' },
  CANCELLED: { label: '已取消', tone: 'info' }
}

function pendingStatusMeta(status) {
  return PENDING_STATUS[status] || { label: status || '-', tone: 'info' }
}
const hoveredStatus = ref(null)

const supplierChartRef = ref(null)
const supplierRows = ref([])
const supplierStats = ref({ count: 0, amount: 0 })

const stockChartRef = ref(null)
const stockRows = ref([])

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
const STOCK_COLORS = ["#00beb4", "#5b8ff9", "#f7863b", "#9270ca", "#3ba05c", "#e8b30b"]

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
  return now.getFullYear() + " 年 " + (now.getMonth() + 1) + " 月 " + now.getDate() + " 日 星期" + week
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

function moneyShort(value) {
  const num = Number(value || 0)
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + " 万"
  }
  return num.toLocaleString("zh-CN")
}

function statusPercent(value) {
  if (!statusTotal.value) return 0
  return Math.round((Number(value || 0) / statusTotal.value) * 100)
}

function stockPercent(value) {
  if (!stockTotal.value) return 0
  return Math.round((Number(value || 0) / stockTotal.value) * 100)
}

function stockColor(index) {
  return STOCK_COLORS[index % STOCK_COLORS.length]
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

const trendDates = computed(() =>
  trendList.value.map(item => {
    const raw = item.statDate || item.bizDate || item.date || ""
    return raw ? String(raw).slice(5) : ""
  })
)

const trendAmounts = computed(() =>
  trendList.value.map(item => Number(item.amount || item.receiptAmount || 0))
)

/** 三维山形视图的数据源：与折线图共用同一份区间数据 */
const trendSeries = computed(() => [{ name: "入库金额", color: "#00beb4", values: trendAmounts.value }])

watch(trendMode, mode => {
  if (mode !== "2d") return
  nextTick(() => {
    charts.trend && charts.trend.resize()
  })
})

const trendTotal = computed(() =>
  trendList.value.reduce((sum, item) => sum + Number(item.amount || item.receiptAmount || 0), 0)
)

const trendCount = computed(() =>
  trendList.value.reduce((sum, item) => sum + Number(item.receiptCount || 0), 0)
)

const statusSegments = computed(() => {
  const grouped = new Map()
  statusRaw.value.forEach(item => {
    const key = item.status || item.orderStatus
    if (!key) return
    grouped.set(key, (grouped.get(key) || 0) + Number(item.total || item.count || 0))
  })
  const dict = erp_order_status.value || []
  const dictOrder = dict.map(entry => entry.value)
  const labelOf = key => {
    const hit = dict.find(entry => entry.value === key)
    return hit ? hit.label : key
  }
  const keys = dictOrder.filter(key => grouped.has(key))
  grouped.forEach((_, key) => {
    if (!keys.includes(key)) keys.push(key)
  })
  return keys.map((key, index) => ({
    key,
    label: labelOf(key),
    value: grouped.get(key),
    color: STATUS_COLORS[key] || STATUS_FALLBACK_COLORS[index % STATUS_FALLBACK_COLORS.length]
  }))
})

const statusTotal = computed(() => statusSegments.value.reduce((sum, item) => sum + item.value, 0))

const hoveredStatusLabel = computed(() => (hoveredStatus.value ? hoveredStatus.value.label : null))

const stockTotal = computed(() => stockRows.value.reduce((sum, item) => sum + item.amount, 0))

/** 条形图按条目数自适应高度：条目少时不至于被拉得过稀 */
const supplierChartHeight = computed(() => Math.min(220, Math.max(132, supplierRows.value.length * 40)))

/** TOP1 供应商占全部采购额的比例 */
const supplierTopShare = computed(() => {
  const top = supplierRows.value[0]
  if (!top || !supplierStats.value.amount) return 0
  return (top.amount / supplierStats.value.amount) * 100
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

function handleStatusHover(segment) {
  hoveredStatus.value = segment || null
}

function handleLegendHover(item) {
  hoveredStatus.value = item || null
}

function stampTime() {
  const now = new Date()
  const hh = String(now.getHours()).padStart(2, "0")
  const mm = String(now.getMinutes()).padStart(2, "0")
  updatedAt.value = hh + ":" + mm
}

function loadOverview() {
  loadingOverview.value = true
  return getOverview()
    .then(response => {
      const data = response.data || {}
      overview.value = data.cards || data
      statusRaw.value = data.orderStatus || []
      stampTime()
    })
    .catch(() => {})
    .finally(() => {
      loadingOverview.value = false
    })
}

function loadPending() {
  loadingPending.value = true
  return getPending()
    .then(response => {
      pendingList.value = response.data || []
    })
    .catch(() => {})
    .finally(() => {
      loadingPending.value = false
    })
}

function loadLowStock() {
  return getLowStock()
    .then(response => {
      lowStockList.value = response.data || []
    })
    .catch(() => {})
}

function loadTrend() {
  loadingTrend.value = true
  return getReceiptTrend(trendDays.value)
    .then(response => {
      trendList.value = response.data || []
      renderTrend()
    })
    .catch(() => {})
    .finally(() => {
      loadingTrend.value = false
    })
}

function loadSuppliers() {
  return listOrder({ pageNum: 1, pageSize: 100 })
    .then(response => {
      const rows = response.rows || []
      const grouped = new Map()
      rows.forEach(row => {
        const name = row.supplierName || "未指定供应商"
        const current = grouped.get(name) || { label: name, amount: 0, count: 0 }
        current.amount += Number(row.totalAmount || 0)
        current.count += 1
        grouped.set(name, current)
      })
      const all = Array.from(grouped.values()).sort((a, b) => b.amount - a.amount)
      supplierRows.value = all.slice(0, 5)
      supplierStats.value = {
        count: all.length,
        amount: all.reduce((sum, item) => sum + item.amount, 0)
      }
      nextTick(renderSuppliers)
    })
    .catch(() => {})
}

function loadStock() {
  return listBalance({ pageNum: 1, pageSize: 200 })
    .then(response => {
      const rows = response.rows || []
      const grouped = new Map()
      rows.forEach(row => {
        const name = row.warehouseName || "未分配仓库"
        const current = grouped.get(name) || { label: name, amount: 0, quantity: 0 }
        current.amount += Number(row.inventoryAmount || 0)
        current.quantity += Number(row.availableQuantity || 0)
        grouped.set(name, current)
      })
      stockRows.value = Array.from(grouped.values()).sort((a, b) => b.amount - a.amount)
      renderStock()
    })
    .catch(() => {})
}

const charts = {}
let resizeObserver = null
let resizeFrame = 0

function chartOf(key, el) {
  if (!el) return null
  if (!charts[key]) {
    charts[key] = echarts.init(el)
  }
  return charts[key]
}

function renderTrend() {
  const chart = chartOf("trend", trendChartRef.value)
  if (!chart) return
  const list = trendList.value
  const dates = list.map(item => {
    const raw = item.statDate || item.bizDate || item.date || ""
    return raw ? String(raw).slice(5) : ""
  })
  const amounts = list.map(item => Number(item.amount || item.receiptAmount || 0))
  chart.setOption({
    tooltip: {
      trigger: "axis",
      backgroundColor: "rgba(22,22,24,0.92)",
      borderWidth: 0,
      padding: [8, 12],
      textStyle: { color: "#f4f4f5", fontSize: 12 },
      axisPointer: { lineStyle: { color: "rgba(0,190,180,0.5)" } },
      formatter: params => {
        const index = params[0].dataIndex
        const item = list[index] || {}
        const date = item.statDate || item.bizDate || item.date || ""
        const count = Number(item.receiptCount || 0)
        return (
          date +
          "<br/>入库金额 ¥" +
          Number(params[0].value).toLocaleString("zh-CN") +
          "<br/>入库单 " +
          count +
          " 单"
        )
      }
    },
    grid: { left: 8, right: 16, top: 18, bottom: 6, containLabel: true },
    xAxis: {
      type: "category",
      boundaryGap: false,
      data: dates,
      axisLine: { lineStyle: { color: "rgba(22,22,24,0.1)" } },
      axisTick: { show: false },
      axisLabel: { color: "#9a9da1", fontSize: 11, hideOverlap: true }
    },
    yAxis: {
      type: "value",
      splitLine: { lineStyle: { color: "rgba(22,22,24,0.06)", type: "dashed" } },
      axisLabel: {
        color: "#9a9da1",
        fontSize: 11,
        formatter: value => (value >= 10000 ? value / 10000 + " 万" : value)
      }
    },
    series: [
      {
        name: "入库金额",
        type: "line",
        smooth: true,
        showSymbol: dates.length <= 31,
        symbol: "circle",
        symbolSize: dates.length === 1 ? 9 : 6,
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

function renderSuppliers() {
  const chart = chartOf("supplier", supplierChartRef.value)
  if (!chart) return
  const rows = supplierRows.value.slice().reverse()
  chart.setOption({
    tooltip: {
      trigger: "item",
      backgroundColor: "rgba(22,22,24,0.92)",
      borderWidth: 0,
      padding: [8, 12],
      textStyle: { color: "#f4f4f5", fontSize: 12 },
      formatter: params => {
        const row = rows[params.dataIndex] || {}
        return params.name + "<br/>采购额 ¥" + Number(params.value).toLocaleString("zh-CN") + "<br/>" + (row.count || 0) + " 单"
      }
    },
    grid: { left: 0, right: 76, top: 6, bottom: 0, containLabel: true },
    xAxis: { type: "value", show: false, max: value => value.max * 1.12 },
    yAxis: {
      type: "category",
      data: rows.map(row => row.label),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: "#5c5f63", fontSize: 12, width: 116, overflow: "truncate" }
    },
    series: [
      {
        type: "bar",
        data: rows.map(row => row.amount),
        barWidth: 14,
        itemStyle: {
          borderRadius: [0, 7, 7, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: "rgba(0,190,180,0.35)" },
            { offset: 1, color: "#00beb4" }
          ])
        },
        label: {
          show: true,
          position: "right",
          distance: 8,
          color: "#3b3d40",
          fontSize: 11,
          fontWeight: 600,
          formatter: params => "¥" + moneyShort(params.value)
        }
      }
    ]
  }, true)
}

function renderStock() {
  const chart = chartOf("stock", stockChartRef.value)
  if (!chart) return
  const rows = stockRows.value
  const total = stockTotal.value || 1
  chart.setOption({
    tooltip: {
      trigger: "item",
      backgroundColor: "rgba(22,22,24,0.92)",
      borderWidth: 0,
      padding: [8, 12],
      textStyle: { color: "#f4f4f5", fontSize: 12 },
      formatter: params =>
        params.seriesName +
        "<br/>库存金额 ¥" +
        Number(params.value).toLocaleString("zh-CN") +
        "<br/>占比 " +
        Math.round((params.value / total) * 100) +
        "%"
    },
    grid: { left: 0, right: 0, top: 0, bottom: 0 },
    xAxis: { type: "value", show: false, max: total },
    yAxis: { type: "category", data: ["库存"], show: false },
    series: rows.map((row, index) => {
      const share = Math.round((row.amount / total) * 100)
      return {
        name: row.label,
        type: "bar",
        stack: "total",
        barWidth: 26,
        data: [row.amount],
        itemStyle: {
          color: stockColor(index),
          borderRadius:
            rows.length === 1
              ? 13
              : index === 0
                ? [13, 0, 0, 13]
                : index === rows.length - 1
                  ? [0, 13, 13, 0]
                  : 0
        },
        label: {
          show: share >= 12,
          position: "inside",
          color: "#fff",
          fontSize: 11,
          fontWeight: 600,
          formatter: share + "%"
        }
      }
    })
  }, true)
}

function resizeCharts() {
  Object.keys(charts).forEach(key => {
    const chart = charts[key]
    if (!chart || chart.isDisposed()) return
    chart.resize()
  })
}

function scheduleResize() {
  if (resizeFrame) cancelAnimationFrame(resizeFrame)
  resizeFrame = requestAnimationFrame(() => {
    resizeFrame = 0
    resizeCharts()
  })
}

function observeLayout() {
  if (resizeObserver || typeof ResizeObserver === "undefined" || !pageRef.value) return
  resizeObserver = new ResizeObserver(scheduleResize)
  resizeObserver.observe(pageRef.value)
}

function loadAll() {
  return Promise.all([loadOverview(), loadPending(), loadLowStock(), loadTrend(), loadSuppliers(), loadStock()])
}

function refreshAll() {
  if (refreshing.value) return
  refreshing.value = true
  loadAll().finally(() => {
    refreshing.value = false
    nextTick(resizeCharts)
  })
}

onMounted(() => {
  loadAll()
  window.addEventListener("resize", scheduleResize)
  observeLayout()
  nextTick(resizeCharts)
})

onActivated(() => {
  loadAll()
  nextTick(() => {
    observeLayout()
    resizeCharts()
  })
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", scheduleResize)
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  if (resizeFrame) {
    cancelAnimationFrame(resizeFrame)
    resizeFrame = 0
  }
  Object.keys(charts).forEach(key => {
    const chart = charts[key]
    if (chart && !chart.isDisposed()) chart.dispose()
    delete charts[key]
  })
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
  flex-wrap: wrap;
  margin-bottom: 18px;

  .greeting {
    margin: 0;
    font-size: 22px;
    font-weight: 600;
    letter-spacing: 0.2px;
    color: #161618;
  }

  .subline {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
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

  .muted {
    color: #9a9da1;
  }

  .head-actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.btn-icon {
  width: 14px;
  height: 14px;
  margin-right: 6px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.metric {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  font: inherit;
  text-align: left;
  background: #fff;
  border: 1px solid rgba(22, 22, 24, 0.07);
  border-radius: 12px;
  cursor: pointer;
  transition: box-shadow 0.2s ease, transform 0.2s ease, border-color 0.2s ease;

  &:hover {
    transform: translateY(-1px);
    border-color: rgba(22, 22, 24, 0.13);
    box-shadow: 0 8px 20px rgba(22, 22, 24, 0.06);
  }

  &:focus-visible {
    outline: 2px solid var(--el-color-primary);
    outline-offset: 2px;
  }
}

.metric-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  font-size: 20px;
}

.metric-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.metric-label {
  font-size: 13px;
  color: #75787a;
}

.metric-value {
  font-size: 26px;
  font-weight: 600;
  line-height: 1.15;
  color: #161618;
  font-variant-numeric: tabular-nums;
}

.metric-tip {
  font-size: 12px;
  color: #9a9da1;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.metric--accent .metric-icon {
  background: rgba(0, 190, 180, 0.1);
  color: #00a89f;
}

.metric--warning .metric-icon {
  background: rgba(250, 173, 20, 0.12);
  color: #d98b00;
}

.metric--danger .metric-icon {
  background: rgba(216, 73, 75, 0.1);
  color: #d8494b;
}

.metric--success .metric-icon {
  background: rgba(59, 160, 92, 0.12);
  color: #3ba05c;
}

.workbench-row {
  row-gap: 16px;
}

.workbench-row + .workbench-row {
  margin-top: 16px;
}

.panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  border: 1px solid rgba(22, 22, 24, 0.07);
  border-radius: 12px;

  :deep(.el-card__header) {
    padding: 13px 18px;
    border-bottom: 1px solid rgba(22, 22, 24, 0.06);
  }

  :deep(.el-card__body) {
    display: flex;
    flex: 1;
    flex-direction: column;
    padding: 14px 18px 16px;
  }
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
  font-size: 15px;
  font-weight: 600;
  color: #161618;
}

.panel-sub {
  font-size: 12px;
  font-weight: 400;
  color: #9a9da1;
}

.panel-tools {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: none;
}

.panel-total {
  font-size: 12px;
  color: #75787a;
  font-variant-numeric: tabular-nums;
}

.panel-hint {
  font-size: 12px;
  color: #9a9da1;
}

.link-arrow {
  margin-left: 2px;
  font-size: 12px;
}

.count-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 18px;
  padding: 0 6px;
  border-radius: 9px;
  background: rgba(0, 190, 180, 0.12);
  color: #00a89f;
  font-size: 11px;
  font-weight: 600;
}

.count-pill--warn {
  background: rgba(250, 173, 20, 0.14);
  color: #d98b00;
}

.chart-wrap {
  position: relative;
}

.chart {
  width: 100%;
  height: 100%;
}

.chart-wrap--trend {
  height: 258px;
}

.chart-ridge {
  position: absolute;
  inset: 0;
}

.panel-mode {
  margin-right: 8px;
}

.chart-wrap--bar {
  min-height: 132px;
  max-height: 220px;
}

.chart-wrap--stock {
  height: 32px;
}

.chart-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.9);
}

.chart-empty--plain {
  background: #f7f8f9;
  border-radius: 8px;
}

.chart-foot {
  margin: auto 0 0;
  padding-top: 10px;
  border-top: 1px dashed rgba(22, 22, 24, 0.08);
  font-size: 12px;
  color: #9a9da1;
  font-variant-numeric: tabular-nums;
}

.chart-foot__extra {
  color: #b0b3b6;
}

.chart-empty__hint {
  font-size: 12px;
  color: #9a9da1;
}

.status-body {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 14px;
  min-height: 258px;
}

.status-legend {
  flex: 1 1 130px;
  max-width: 210px;
}

.status-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1px;
}

.status-center__label {
  margin: 0;
  max-width: 120px;
  font-size: 12px;
  color: #75787a;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.status-center__value {
  margin: 0;
  font-size: 28px;
  font-weight: 600;
  line-height: 1.2;
  color: #161618;
  font-variant-numeric: tabular-nums;
}

.status-center__percent {
  margin: 0;
  font-size: 12px;
  color: #00a89f;
}

.center-swap-enter-active,
.center-swap-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.center-swap-enter-from,
.center-swap-leave-to {
  opacity: 0;
  transform: scale(0.92);
}

.task-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.18s ease;

  & + .task-item {
    margin-top: 2px;
  }

  &:hover {
    background: #f4f5f6;

    .task-arrow {
      opacity: 1;
      transform: translateX(0);
    }
  }
}

.task-type {
  flex: none;
  width: 78px;
}

.task-main {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.task-title {
  font-size: 13px;
  color: #161618;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.task-meta {
  font-size: 12px;
  color: #9a9da1;
}

.task-status {
  flex: none;
  padding: 2px 7px;
  border-radius: 999px;
  background: #f4f5f6;
  font-size: 11px;
  line-height: 18px;
  color: #75787a;

  &--warning {
    background: #fff6e6;
    color: #b8770f;
  }

  &--success {
    background: #e9f7ee;
    color: #2f8a4d;
  }

  &--danger {
    background: #fdecec;
    color: #c24345;
  }
}

.task-arrow {
  flex: none;
  font-size: 12px;
  color: #c3c6c9;
  opacity: 0;
  transform: translateX(-4px);
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.shortcut {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 14px 4px;
  font: inherit;
  background: #fbfbfc;
  border: 1px solid rgba(22, 22, 24, 0.06);
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;

  &:hover {
    background: #fff;
    border-color: rgba(0, 190, 180, 0.35);
    box-shadow: 0 6px 16px rgba(0, 190, 180, 0.1);
    transform: translateY(-1px);
  }

  &:focus-visible {
    outline: 2px solid var(--el-color-primary);
    outline-offset: 2px;
  }
}

.shortcut-icon {
  font-size: 18px;
  color: #00a89f;
}

.shortcut-label {
  font-size: 12px;
  color: #3b3d40;
}

.stock-legend {
  list-style: none;
  margin: 14px 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;

  li {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    color: #3b3d40;
  }
}

.legend-dot {
  flex: none;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.legend-name {
  flex: 1;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.legend-amount {
  color: #161618;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.legend-percent {
  width: 40px;
  text-align: right;
  color: #9a9da1;
  font-variant-numeric: tabular-nums;
}

.stock-alert {
  margin-top: 14px;
  padding: 10px 12px;
  border: 1px solid rgba(250, 173, 20, 0.24);
  border-radius: 10px;
  background: rgba(250, 173, 20, 0.07);
}

.stock-alert__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stock-alert__title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #b57500;
}

.stock-alert__list {
  list-style: none;
  margin: 8px 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;

  li {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
  }
}

.stock-alert__name {
  color: #3b3d40;
}

.stock-alert__meta {
  flex: 1;
  color: #9a9da1;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.stock-alert__qty {
  color: #d8494b;
  font-weight: 600;
  font-variant-numeric: tabular-nums;

  em {
    margin-left: 2px;
    font-style: normal;
    font-weight: 400;
    color: #9a9da1;
  }
}

.stock-healthy {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 14px;
  font-size: 12px;
  color: #3ba05c;
}

.overview-strip {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 1px;
  margin-top: 16px;
  overflow: hidden;
  border-radius: 12px;
  background: rgba(22, 22, 24, 0.06);
}

.overview-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 13px 16px;
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
  .metric-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .overview-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .workbench-head {
    align-items: flex-start;
  }

  .head-actions {
    width: 100%;
  }

  .panel-total {
    display: none;
  }
}
</style>
