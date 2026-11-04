<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="库存余额" name="balance">
        <el-form :model="balanceQuery" ref="balanceQueryRef" :inline="true" v-show="showSearch">
          <el-form-item label="仓库" prop="warehouseId">
            <el-select v-model="balanceQuery.warehouseId" placeholder="全部" clearable style="width: 180px">
              <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.warehouseName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="物料编码" prop="materialCode">
            <el-input v-model="balanceQuery.materialCode" placeholder="请输入物料编码" clearable style="width: 180px" @keyup.enter="getBalanceList" />
          </el-form-item>
          <el-form-item label="物料名称" prop="materialName">
            <el-input v-model="balanceQuery.materialName" placeholder="请输入物料名称" clearable style="width: 180px" @keyup.enter="getBalanceList" />
          </el-form-item>
          <el-form-item label="仅看低库存">
            <el-switch v-model="lowStockOnly" @change="getBalanceList" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="getBalanceList">搜索</el-button>
            <el-button icon="Refresh" @click="resetBalanceQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button plain icon="Download" @click="exportBalance" v-hasPermi="['erp:inventory:export']">导出</el-button>
          </el-col>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getBalanceList" />
        </el-row>

        <el-table v-loading="balanceLoading" :data="filteredBalanceList">
          <el-table-column label="仓库" align="center" prop="warehouseName" width="150" show-overflow-tooltip />
          <el-table-column label="物料编码" align="center" prop="materialCode" width="150" />
          <el-table-column label="物料名称" align="center" prop="materialName" min-width="180" show-overflow-tooltip />
          <el-table-column label="规格型号" align="center" prop="specification" width="150" show-overflow-tooltip />
          <el-table-column label="单位" align="center" prop="unit" width="80" />
          <el-table-column label="可用数量" align="center" prop="availableQuantity" width="140">
            <template #default="scope">
              <span :style="{ color: scope.row.lowStock ? '#f56c6c' : '', fontWeight: scope.row.lowStock ? 600 : 400 }">{{ formatQuantity(scope.row.availableQuantity) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="平均成本" align="center" prop="averageCost" width="130">
            <template #default="scope">
              <span>{{ formatAmount(scope.row.averageCost) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="库存金额" align="center" prop="inventoryAmount" width="150">
            <template #default="scope">
              <span>{{ formatAmount(scope.row.inventoryAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="库存预警" align="center" prop="lowStock" width="100">
            <template #default="scope">
              <el-tag v-if="scope.row.lowStock" type="danger" size="small">低库存</el-tag>
              <el-tag v-else type="success" size="small">正常</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="最后业务时间" align="center" prop="lastBusinessTime" width="160" />
        </el-table>

        <pagination v-show="balanceTotal > 0" :total="balanceTotal" v-model:page="balanceQuery.pageNum" v-model:limit="balanceQuery.pageSize" @pagination="getBalanceList" />
      </el-tab-pane>

      <el-tab-pane label="库存流水" name="ledger">
        <el-form :model="ledgerQuery" ref="ledgerQueryRef" :inline="true" v-show="showSearch">
          <el-form-item label="仓库" prop="warehouseId">
            <el-select v-model="ledgerQuery.warehouseId" placeholder="全部" clearable style="width: 180px">
              <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.warehouseName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="物料编码" prop="materialCode">
            <el-input v-model="ledgerQuery.materialCode" placeholder="请输入物料编码" clearable style="width: 180px" @keyup.enter="getLedgerList" />
          </el-form-item>
          <el-form-item label="业务类型" prop="businessType">
            <el-select v-model="ledgerQuery.businessType" placeholder="全部" clearable style="width: 160px">
              <el-option v-for="dict in erp_inventory_business" :key="dict.value" :label="dict.label" :value="dict.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="getLedgerList">搜索</el-button>
            <el-button icon="Refresh" @click="resetLedgerQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button plain icon="Download" @click="exportLedger" v-hasPermi="['erp:inventory:export']">导出</el-button>
          </el-col>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getLedgerList" />
        </el-row>

        <el-table v-loading="ledgerLoading" :data="ledgerList">
          <el-table-column label="发生时间" align="center" prop="occurredTime" width="160" />
          <el-table-column label="仓库" align="center" prop="warehouseName" width="140" show-overflow-tooltip />
          <el-table-column label="物料编码" align="center" prop="materialCode" width="150" />
          <el-table-column label="物料名称" align="center" prop="materialName" min-width="170" show-overflow-tooltip />
          <el-table-column label="业务类型" align="center" prop="businessType" width="110">
            <template #default="scope">
              <dict-tag :options="erp_inventory_business" :value="scope.row.businessType" />
            </template>
          </el-table-column>
          <el-table-column label="变动数量" align="center" prop="changeQuantity" width="130">
            <template #default="scope">
              <span :style="{ color: Number(scope.row.changeQuantity) < 0 ? '#f56c6c' : '#67c23a' }">{{ Number(scope.row.changeQuantity) > 0 ? '+' : '' }}{{ formatQuantity(scope.row.changeQuantity) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="成本单价" align="center" prop="unitCost" width="120">
            <template #default="scope">
              <span>{{ formatAmount(scope.row.unitCost) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="结存数量" align="center" prop="balanceQuantity" width="130">
            <template #default="scope">
              <span>{{ formatQuantity(scope.row.balanceQuantity) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="来源单据" align="center" prop="sourceId" width="100" />
        </el-table>

        <pagination v-show="ledgerTotal > 0" :total="ledgerTotal" v-model:page="ledgerQuery.pageNum" v-model:limit="ledgerQuery.pageSize" @pagination="getLedgerList" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup name="ErpInventory">
import { listBalance, listLedger } from "@/api/erp/inventory"
import { listWarehouse } from "@/api/erp/warehouse"

const { proxy } = getCurrentInstance()
const { erp_inventory_business } = useDict("erp_inventory_business")

const activeTab = ref("balance")
const showSearch = ref(true)
const lowStockOnly = ref(false)
const warehouseOptions = ref([])

const balanceList = ref([])
const balanceLoading = ref(true)
const balanceTotal = ref(0)
const ledgerList = ref([])
const ledgerLoading = ref(false)
const ledgerTotal = ref(0)

const data = reactive({
  balanceQuery: {
    pageNum: 1,
    pageSize: 10,
    warehouseId: undefined,
    materialCode: undefined,
    materialName: undefined
  },
  ledgerQuery: {
    pageNum: 1,
    pageSize: 10,
    warehouseId: undefined,
    materialCode: undefined,
    businessType: undefined
  }
})

const { balanceQuery, ledgerQuery } = toRefs(data)

function formatAmount(value) {
  if (value === null || value === undefined || value === "") return "-"
  return Number(value).toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatQuantity(value) {
  if (value === null || value === undefined || value === "") return "-"
  return Number(value).toLocaleString("zh-CN", { maximumFractionDigits: 4 })
}

const filteredBalanceList = computed(() => {
  if (!lowStockOnly.value) return balanceList.value
  return balanceList.value.filter(item => item.lowStock)
})

function getBalanceList() {
  balanceLoading.value = true
  listBalance(balanceQuery.value).then(response => {
    balanceList.value = response.rows || []
    balanceTotal.value = response.total || 0
    balanceLoading.value = false
  })
}

function getLedgerList() {
  ledgerLoading.value = true
  listLedger(ledgerQuery.value).then(response => {
    ledgerList.value = response.rows || []
    ledgerTotal.value = response.total || 0
    ledgerLoading.value = false
  })
}

function handleTabChange(name) {
  if (name === "ledger" && ledgerList.value.length === 0) {
    getLedgerList()
  }
}

function resetBalanceQuery() {
  proxy.resetForm("balanceQueryRef")
  lowStockOnly.value = false
  balanceQuery.value.pageNum = 1
  getBalanceList()
}

function resetLedgerQuery() {
  proxy.resetForm("ledgerQueryRef")
  ledgerQuery.value.pageNum = 1
  getLedgerList()
}

function exportBalance() {
  proxy.download("erp/inventory/export", { ...balanceQuery.value }, `inventory_balance_${new Date().getTime()}.xlsx`)
}

function exportLedger() {
  proxy.download("erp/inventory/ledger/export", { ...ledgerQuery.value }, `inventory_ledger_${new Date().getTime()}.xlsx`)
}

function getWarehouses() {
  listWarehouse({ pageNum: 1, pageSize: 200 }).then(response => {
    warehouseOptions.value = response.rows || []
  })
}

getWarehouses()
getBalanceList()
</script>
