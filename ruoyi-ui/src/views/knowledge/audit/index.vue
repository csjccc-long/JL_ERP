<template>
  <div class="app-container">
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
import { listAudit, getAudit, auditOverview } from "@/api/knowledge/audit"

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
</script>
