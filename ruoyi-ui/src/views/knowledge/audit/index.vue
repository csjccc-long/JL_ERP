<template>
  <div class="app-container audit-page">
    <!-- 数据可视化概览 -->
    <el-row :gutter="12" class="mb8 audit-charts">
      <el-col :xs="24" :sm="24" :lg="7">
        <el-card shadow="never" class="chart-card" v-loading="statsLoading">
          <template #header>
            <div class="card-header">
              <span>问答状态构成</span>
              <span class="card-extra">共 {{ statsTotal }} 次</span>
            </div>
          </template>
          <div v-if="statusSegments.length" class="donut-body">
            <DonutChart
              :data="statusSegments"
              :size="166"
              :stroke-width="22"
              :animation-duration="1"
              :animation-delay-per-segment="0.06"
              :active-label="hoveredStatusLabel"
              aria-label="问答状态构成"
              @segment-hover="handleStatusHover"
            >
              <template #center="{ hovered, percent }">
                <Transition name="audit-center" mode="out-in">
                  <div class="donut-center" :key="hovered ? hovered.label : 'total'">
                    <p class="donut-center__label">{{ hovered ? hovered.label : '问答总数' }}</p>
                    <p class="donut-center__value">{{ hovered ? hovered.value : statsTotal }}</p>
                    <p class="donut-center__percent">{{ hovered ? percent.toFixed(0) + '%' : '全部记录' }}</p>
                  </div>
                </Transition>
              </template>
            </DonutChart>
            <DonutChartLegend
              class="donut-legend"
              :data="statusSegments"
              :total="statsTotal"
              :active-label="hoveredStatusLabel"
              @hover="handleStatusHover"
            />
          </div>
          <el-empty v-else-if="!statsLoading" description="暂无问答记录" :image-size="70" />
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :lg="10">
        <el-card shadow="never" class="chart-card" v-loading="statsLoading">
          <template #header>
            <div class="card-header">
              <span>问答趋势</span>
              <div class="header-tools">
                <span class="card-extra">共 {{ trendTotal }} 条</span>
                <chart-mode-switch v-model="trendMode" />
                <el-radio-group v-model="trendDays" size="small" @change="renderTrend">
                  <el-radio-button :value="7">7 天</el-radio-button>
                  <el-radio-button :value="14">14 天</el-radio-button>
                  <el-radio-button :value="30">30 天</el-radio-button>
                </el-radio-group>
              </div>
            </div>
          </template>
          <div class="chart-box">
            <div ref="trendChartRef" class="chart-wrap" v-show="trendMode === '2d'"></div>
            <ridge-chart
              v-if="trendMode === '3d' && trendTotal > 0"
              class="chart-ridge"
              :labels="trendLabels"
              :series="trendSeries"
            />
            <div v-if="!statsLoading && trendTotal === 0" class="chart-empty">
              <el-empty description="所选区间暂无问答记录" :image-size="60" />
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :lg="7">
        <el-card shadow="never" class="chart-card" v-loading="statsLoading">
          <template #header>
            <div class="card-header">
              <span>响应耗时分布</span>
              <span class="card-extra">平均 {{ avgLatencyText }}</span>
            </div>
          </template>
          <div ref="latencyChartRef" class="chart-wrap chart-wrap--bar">
            <div v-if="!statsLoading && statsTotal === 0" class="chart-empty">
              <el-empty description="暂无耗时数据" :image-size="60" />
            </div>
          </div>
          <div v-if="statsTotal" class="chart-foot">
            <span>慢请求（≥3s）{{ slowCount }} 次 · 占 {{ slowShare }}%</span>
            <span>P95 {{ p95LatencyText }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" class="mb8">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>近 7 天高频问题</span></template>
          <el-table :data="overview.topQuestions" size="small" v-loading="overviewLoading">
            <el-table-column type="index" label="#" width="50" align="center" />
            <el-table-column label="问题" prop="question" show-overflow-tooltip />
            <el-table-column label="次数" prop="total" width="80" align="center" />
          </el-table>
          <el-empty v-if="!overviewLoading && (!overview.topQuestions || overview.topQuestions.length === 0)" description="暂无数据" :image-size="60" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>最近失败问答</span></template>
          <el-table :data="overview.recentFailures" size="small" v-loading="overviewLoading">
            <el-table-column label="问题" prop="question" show-overflow-tooltip />
            <el-table-column label="状态" prop="status" width="150" align="center">
              <template #default="scope">
                <dict-tag :options="erp_audit_status" :value="scope.row.status" />
              </template>
            </el-table-column>
            <el-table-column label="时间" prop="createdTime" width="160" align="center" />
          </el-table>
          <el-empty v-if="!overviewLoading && (!overview.recentFailures || overview.recentFailures.length === 0)" description="暂无失败记录" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>

    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="追踪ID" prop="traceId">
        <el-input v-model="queryParams.traceId" placeholder="请输入追踪ID" clearable style="width: 200px" @keyup.enter="getList" />
      </el-form-item>
      <el-form-item label="问题" prop="question">
        <el-input v-model="queryParams.question" placeholder="请输入问题关键字" clearable style="width: 200px" @keyup.enter="getList" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 170px">
          <el-option v-for="dict in erp_audit_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="上下文类型" prop="contextType">
        <el-select v-model="queryParams.contextType" placeholder="全部" clearable style="width: 160px">
          <el-option v-for="dict in erp_business_type" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="时间范围">
        <el-date-picker
          v-model="dateRange"
          value-format="YYYY-MM-DD"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="auditList">
      <el-table-column label="追踪ID" align="center" prop="traceId" width="230" show-overflow-tooltip />
      <el-table-column label="问题" align="center" prop="question" min-width="240" show-overflow-tooltip />
      <el-table-column label="上下文类型" align="center" prop="contextType" width="140">
        <template #default="scope">
          <dict-tag v-if="scope.row.contextType" :options="erp_business_type" :value="scope.row.contextType" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="160">
        <template #default="scope">
          <dict-tag :options="erp_audit_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="耗时" align="center" prop="latencyMs" width="100">
        <template #default="scope">{{ scope.row.latencyMs ? scope.row.latencyMs + ' ms' : '-' }}</template>
      </el-table-column>
      <el-table-column label="用户ID" align="center" prop="userId" width="90" />
      <el-table-column label="时间" align="center" prop="createdTime" width="160" />
      <el-table-column label="操作" align="center" width="90" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="问答审计详情" v-model="detailOpen" width="720px" append-to-body>
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="追踪ID">{{ detail.traceId }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <dict-tag :options="erp_audit_status" :value="detail.status" />
        </el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ detail.userId }}</el-descriptions-item>
        <el-descriptions-item label="部门ID">{{ detail.deptId }}</el-descriptions-item>
        <el-descriptions-item label="上下文类型">{{ detail.contextType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="上下文ID">{{ detail.contextId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="模型">{{ detail.modelName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detail.latencyMs ? detail.latencyMs + ' ms' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="时间" :span="2">{{ detail.createdTime }}</el-descriptions-item>
        <el-descriptions-item label="问题" :span="2">{{ detail.question }}</el-descriptions-item>
        <el-descriptions-item label="检索空间范围" :span="2">{{ formatScope(detail.spaceIdsJson) }}</el-descriptions-item>
        <el-descriptions-item label="召回文档范围" :span="2">{{ formatScope(detail.documentIdsJson) }}</el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2">
          <span style="color: var(--el-color-danger)">{{ detail.errorMessage || '-' }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="KbAudit">
import * as echarts from "echarts"
import { listAudit, getAudit, auditOverview } from "@/api/knowledge/audit"
import DonutChart from "@/components/DonutChart/index.vue"
import DonutChartLegend from "@/components/DonutChart/Legend.vue"
import RidgeChart from "@/components/RidgeChart/index.vue"
import ChartModeSwitch from "@/components/ChartModeSwitch/index.vue"

const { proxy } = getCurrentInstance()
const { erp_audit_status, erp_business_type } = useDict("erp_audit_status", "erp_business_type")

const auditList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const dateRange = ref([])
const detailOpen = ref(false)
const detail = ref(null)
const overview = ref({ topQuestions: [], recentFailures: [] })
const overviewLoading = ref(false)

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    traceId: undefined,
    question: undefined,
    status: undefined,
    contextType: undefined
  }
})

const { queryParams } = toRefs(data)

/* ------------------------------ 数据可视化 ------------------------------ */

const statsRows = ref([])
const statsLoading = ref(false)
const statsTotal = ref(0)
const trendDays = ref(14)
const trendMode = ref("2d")
const hoveredStatus = ref(null)
const trendChartRef = ref(null)
const latencyChartRef = ref(null)
let trendChart = null
let latencyChart = null

/** 审计状态配色（按 erp_audit_status 字典语义） */
const STATUS_COLORS = {
  SUCCESS: "#3ba05c",
  INSUFFICIENT_EVIDENCE: "#faad14",
  RAG_UNAVAILABLE: "#d8494b",
  FORBIDDEN: "#9270ca",
  FAILED: "#b23a3c"
}
const STATUS_FALLBACK_COLORS = ["#5b8ff9", "#9270ca", "#ff9d4d", "#269a99", "#66b2ff"]

const statusSegments = computed(() => {
  const dictOrder = erp_audit_status.value.map(item => item.value)
  const dictLabels = {}
  erp_audit_status.value.forEach(item => {
    dictLabels[item.value] = item.label
  })
  const counts = new Map()
  statsRows.value.forEach(row => {
    const key = row.status || "UNKNOWN"
    counts.set(key, (counts.get(key) || 0) + 1)
  })
  const list = Array.from(counts.entries()).map(([key, value]) => ({
    key,
    label: dictLabels[key] || key,
    value
  }))
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

const hoveredStatusLabel = computed(() => hoveredStatus.value?.label ?? null)

function handleStatusHover(segment) {
  hoveredStatus.value = segment || null
}

function pad2(value) {
  return String(value).padStart(2, "0")
}

function dayKey(date) {
  return date.getFullYear() + "-" + pad2(date.getMonth() + 1) + "-" + pad2(date.getDate())
}

/** 按天归集的问答量 / 失败量（近 trendDays 天） */
const trendBuckets = computed(() => {
  const days = []
  const index = new Map()
  const today = new Date()
  for (let i = trendDays.value - 1; i >= 0; i--) {
    const date = new Date(today.getFullYear(), today.getMonth(), today.getDate() - i)
    const bucket = {
      label: pad2(date.getMonth() + 1) + "-" + pad2(date.getDate()),
      total: 0,
      failed: 0
    }
    days.push(bucket)
    index.set(dayKey(date), bucket)
  }
  statsRows.value.forEach(row => {
    const bucket = index.get(String(row.createdTime || "").slice(0, 10))
    if (!bucket) return
    bucket.total += 1
    if (row.status !== "SUCCESS") bucket.failed += 1
  })
  return days
})

const trendTotal = computed(() => trendBuckets.value.reduce((sum, item) => sum + item.total, 0))

/** 三维山形视图：问答量、失败量各一道山脊 */
const trendLabels = computed(() => trendBuckets.value.map(item => item.label))
const trendSeries = computed(() => [
  { name: "问答量", color: "#00beb4", values: trendBuckets.value.map(item => item.total) },
  { name: "失败量", color: "#f7863b", values: trendBuckets.value.map(item => item.failed) }
])

watch(trendMode, mode => {
  if (mode !== "2d") return
  nextTick(() => {
    trendChart && trendChart.resize()
  })
})

/** 耗时分档：<0.5s / 0.5-1s / 1-2s / 2-3s / >=3s */
const LATENCY_BUCKETS = [
  { label: "< 0.5s", max: 500, color: "#00beb4" },
  { label: "0.5-1s", max: 1000, color: "#5b8ff9" },
  { label: "1-2s", max: 2000, color: "#9270ca" },
  { label: "2-3s", max: 3000, color: "#f7863b" },
  { label: "≥ 3s", max: Infinity, color: "#d8494b" }
]

const latencyCounts = computed(() => {
  const counts = LATENCY_BUCKETS.map(() => 0)
  statsRows.value.forEach(row => {
    const value = Number(row.latencyMs || 0)
    const idx = LATENCY_BUCKETS.findIndex(bucket => value < bucket.max)
    if (idx >= 0) counts[idx] += 1
  })
  return counts
})

const slowCount = computed(() => latencyCounts.value[LATENCY_BUCKETS.length - 1] || 0)

const slowShare = computed(() => (statsTotal.value ? ((slowCount.value / statsTotal.value) * 100).toFixed(0) : 0))

const avgLatency = computed(() => {
  if (!statsRows.value.length) return 0
  const sum = statsRows.value.reduce((acc, row) => acc + Number(row.latencyMs || 0), 0)
  return sum / statsRows.value.length
})

const p95Latency = computed(() => {
  const list = statsRows.value.map(row => Number(row.latencyMs || 0)).sort((a, b) => a - b)
  if (!list.length) return 0
  return list[Math.max(0, Math.ceil(list.length * 0.95) - 1)]
})

function formatMs(value) {
  const num = Number(value || 0)
  if (num >= 1000) {
    return (num / 1000).toFixed(num >= 10000 ? 0 : 1) + " s"
  }
  return Math.round(num) + " ms"
}

const avgLatencyText = computed(() => formatMs(avgLatency.value))
const p95LatencyText = computed(() => formatMs(p95Latency.value))

function renderTrend() {
  if (!trendChartRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  const days = trendBuckets.value
  trendChart.setOption({
    tooltip: { trigger: "axis", confine: true },
    legend: {
      right: 0,
      top: 0,
      icon: "circle",
      itemWidth: 8,
      itemHeight: 8,
      itemGap: 14,
      textStyle: { color: "#75787a", fontSize: 12 }
    },
    grid: { left: 4, right: 18, top: 34, bottom: 0, containLabel: true },
    xAxis: {
      type: "category",
      boundaryGap: false,
      data: days.map(item => item.label),
      axisTick: { show: false },
      axisLine: { lineStyle: { color: "#e7e9eb" } },
      axisLabel: { color: "#909399", fontSize: 11 }
    },
    yAxis: {
      type: "value",
      minInterval: 1,
      splitLine: { lineStyle: { color: "#f0f1f3" } },
      axisLabel: { color: "#909399", fontSize: 11 }
    },
    series: [
      {
        name: "问答量",
        type: "line",
        smooth: true,
        symbolSize: 6,
        data: days.map(item => item.total),
        lineStyle: { width: 2, color: "#00beb4" },
        itemStyle: { color: "#00beb4" },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: "rgba(0,190,180,0.22)" },
            { offset: 1, color: "rgba(0,190,180,0)" }
          ])
        }
      },
      {
        name: "失败量",
        type: "line",
        smooth: true,
        symbolSize: 6,
        data: days.map(item => item.failed),
        lineStyle: { width: 2, color: "#f7863b" },
        itemStyle: { color: "#f7863b" }
      }
    ]
  }, true)
}

function renderLatency() {
  if (!latencyChartRef.value) return
  if (!latencyChart) {
    latencyChart = echarts.init(latencyChartRef.value)
  }
  latencyChart.setOption({
    tooltip: { trigger: "axis", axisPointer: { type: "shadow" }, confine: true },
    grid: { left: 4, right: 10, top: 24, bottom: 0, containLabel: true },
    xAxis: {
      type: "category",
      data: LATENCY_BUCKETS.map(bucket => bucket.label),
      axisTick: { show: false },
      axisLine: { lineStyle: { color: "#e7e9eb" } },
      axisLabel: { color: "#909399", fontSize: 11 }
    },
    yAxis: {
      type: "value",
      minInterval: 1,
      splitLine: { lineStyle: { color: "#f0f1f3" } },
      axisLabel: { color: "#909399", fontSize: 11 }
    },
    series: [
      {
        type: "bar",
        barWidth: 20,
        data: latencyCounts.value.map((value, index) => ({
          value,
          itemStyle: { color: LATENCY_BUCKETS[index].color, borderRadius: [6, 6, 0, 0] }
        })),
        label: { show: true, position: "top", color: "#75787a", fontSize: 11 }
      }
    ]
  }, true)
}

function resizeCharts() {
  trendChart && trendChart.resize()
  latencyChart && latencyChart.resize()
}

/** 图表统计取全量审计记录（列表分页不影响统计口径） */
function loadStats() {
  statsLoading.value = true
  listAudit({ pageNum: 1, pageSize: 500 }).then(response => {
    statsRows.value = response.rows || []
    statsTotal.value = response.total != null ? response.total : statsRows.value.length
    statsLoading.value = false
    nextTick(() => {
      renderTrend()
      renderLatency()
    })
  }).catch(() => {
    statsLoading.value = false
  })
}

function getList() {
  loading.value = true
  listAudit(proxy.addDateRange(queryParams.value, dateRange.value)).then(response => {
    auditList.value = response.rows || []
    total.value = response.total || 0
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

function loadOverview() {
  overviewLoading.value = true
  auditOverview().then(response => {
    overview.value = response.data || { topQuestions: [], recentFailures: [] }
    overviewLoading.value = false
  }).catch(() => {
    overviewLoading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm("queryRef")
  dateRange.value = []
  handleQuery()
}

function handleDetail(row) {
  getAudit(row.traceId).then(response => {
    detail.value = response.data
    detailOpen.value = true
  })
}

function formatScope(json) {
  if (!json) return "-"
  try {
    const arr = JSON.parse(json)
    return Array.isArray(arr) && arr.length ? arr.join(", ") : "-"
  } catch (e) {
    return json
  }
}

loadOverview()
getList()

let chartsResizeObserver = null

onMounted(() => {
  loadStats()
  window.addEventListener("resize", resizeCharts)
  if (typeof ResizeObserver !== "undefined") {
    chartsResizeObserver = new ResizeObserver(() => resizeCharts())
    trendChartRef.value && chartsResizeObserver.observe(trendChartRef.value)
    latencyChartRef.value && chartsResizeObserver.observe(latencyChartRef.value)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", resizeCharts)
  chartsResizeObserver && chartsResizeObserver.disconnect()
  chartsResizeObserver = null
  trendChart && trendChart.dispose()
  latencyChart && latencyChart.dispose()
  trendChart = null
  latencyChart = null
})
</script>

<style scoped>
.audit-page .chart-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.audit-page .chart-card :deep(.el-card__body) {
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  padding: 16px 18px;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.card-extra {
  color: #909399;
  font-size: 12px;
  font-weight: 400;
}
.header-tools {
  display: flex;
  align-items: center;
  gap: 10px;
}
.donut-body {
  flex: 1 1 auto;
  min-height: 214px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  flex-wrap: wrap;
}
.donut-legend {
  flex: 1 1 128px;
  max-width: 190px;
}
.donut-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  max-width: 118px;
}
.donut-center__label {
  max-width: 100%;
  margin: 0;
  font-size: 12px;
  line-height: 1.4;
  color: #75787a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.donut-center__value {
  margin: 2px 0 0;
  font-size: 26px;
  font-weight: 600;
  line-height: 1.1;
  color: #161618;
  font-variant-numeric: tabular-nums;
}
.donut-center__percent {
  margin: 2px 0 0;
  font-size: 12px;
  color: #00a89f;
}
.chart-box {
  position: relative;
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-height: 206px;
}
.chart-wrap {
  position: relative;
  flex: 1 1 auto;
  min-height: 206px;
}
.chart-ridge {
  flex: 1 1 auto;
  min-height: 0;
}
.chart-wrap--bar {
  min-height: 186px;
}
.chart-empty {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
}
.chart-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #ebedf0;
  color: #9a9da1;
  font-size: 12px;
}
.audit-center-enter-active,
.audit-center-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}
.audit-center-enter-from,
.audit-center-leave-to {
  opacity: 0;
  transform: scale(0.92);
}
</style>
