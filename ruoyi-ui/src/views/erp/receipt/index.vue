<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="入库单号" prop="receiptNo">
        <el-input v-model="queryParams.receiptNo" placeholder="请输入入库单号" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="采购订单号" prop="orderNo">
        <el-input v-model="queryParams.orderNo" placeholder="请输入采购订单号" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="仓库" prop="warehouseId">
        <el-select v-model="queryParams.warehouseId" placeholder="全部" clearable style="width: 180px">
          <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.warehouseName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="入库状态" prop="receiptStatus">
        <el-select v-model="queryParams.receiptStatus" placeholder="全部" clearable style="width: 150px">
          <el-option v-for="dict in erp_receipt_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['erp:receipt:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['erp:receipt:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['erp:receipt:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['erp:receipt:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="receiptList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="入库单号" align="center" prop="receiptNo" width="150" />
      <el-table-column label="采购订单号" align="center" prop="orderNo" width="150" />
      <el-table-column label="供应商" align="center" prop="supplierName" min-width="160" show-overflow-tooltip />
      <el-table-column label="仓库" align="center" prop="warehouseName" width="130" show-overflow-tooltip />
      <el-table-column label="入库日期" align="center" prop="receiptDate" width="120" />
      <el-table-column label="质检说明" align="center" prop="qualityNote" min-width="150" show-overflow-tooltip />
      <el-table-column label="状态" align="center" prop="receiptStatus" width="100">
        <template #default="scope">
          <dict-tag :options="erp_receipt_status" :value="scope.row.receiptStatus" />
        </template>
      </el-table-column>
      <el-table-column label="确认时间" align="center" prop="confirmedTime" width="160" />
      <el-table-column label="操作" width="280" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button v-if="scope.row.receiptStatus === 'DRAFT'" link type="primary" icon="Checked" @click="handleConfirm(scope.row)" v-hasPermi="['erp:receipt:edit']">确认入库</el-button>
          <el-button v-if="scope.row.receiptStatus === 'CONFIRMED'" link type="primary" icon="RefreshLeft" @click="handleReverse(scope.row)" v-hasPermi="['erp:receipt:edit']">冲销</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['erp:receipt:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['erp:receipt:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="receiptRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="入库单号" prop="receiptNo">
              <el-input v-model="form.receiptNo" placeholder="留空自动生成" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="采购订单" prop="orderId">
              <el-select v-model="form.orderId" placeholder="请选择采购订单" filterable style="width: 100%" @change="handleOrderChange">
                <el-option v-for="item in orderOptions" :key="item.id" :label="item.orderNo + ' - ' + (item.supplierName || '')" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入库仓库" prop="warehouseId">
              <el-select v-model="form.warehouseId" placeholder="请选择入库仓库" style="width: 100%" @change="handleWarehouseChange">
                <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.warehouseName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入库日期" prop="receiptDate">
              <el-date-picker v-model="form.receiptDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择入库日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="质检说明" prop="qualityNote">
              <el-input v-model="form.qualityNote" type="textarea" :rows="2" placeholder="请输入质检说明" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">入库明细</el-divider>
        <el-row class="mb8">
          <el-button type="primary" plain icon="Plus" size="small" @click="handleAddItem">添加明细</el-button>
          <span style="margin-left: 12px; color: #909399">选择采购订单后自动带出订单明细，可按实际到货调整数量</span>
        </el-row>
        <el-table :data="form.items" border size="small">
          <el-table-column label="物料" width="200">
            <template #default="scope">
              <el-select v-model="scope.row.materialId" placeholder="请选择物料" filterable style="width: 100%" @change="val => handleMaterialChange(scope.row, val)">
                <el-option v-for="item in materialOptions" :key="item.id" :label="item.materialName" :value="item.id" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="单位" prop="unit" width="80">
            <template #default="scope">
              <el-input v-model="scope.row.unit" placeholder="单位" />
            </template>
          </el-table-column>
          <el-table-column label="到货数量" width="130">
            <template #default="scope">
              <el-input-number v-model="scope.row.receivedQuantity" :precision="4" :min="0" controls-position="right" style="width: 100%" @change="calcQualified(scope.row)" />
            </template>
          </el-table-column>
          <el-table-column label="合格数量" width="130">
            <template #default="scope">
              <el-input-number v-model="scope.row.qualifiedQuantity" :precision="4" :min="0" controls-position="right" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="不合格数量" width="130">
            <template #default="scope">
              <el-input-number v-model="scope.row.rejectedQuantity" :precision="4" :min="0" controls-position="right" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="成本单价" width="140">
            <template #default="scope">
              <el-input-number v-model="scope.row.unitCost" :precision="2" :min="0" controls-position="right" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center">
            <template #default="scope">
              <el-button link type="danger" icon="Delete" @click="form.items.splice(scope.$index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ErpReceipt">
import { listReceipt, getReceipt, addReceipt, updateReceipt, delReceipt } from "@/api/erp/receipt"
import { listOrder, getOrder } from "@/api/erp/order"
import { listWarehouse } from "@/api/erp/warehouse"
import { listMaterial } from "@/api/erp/material"
import { confirmReceipt, reverseReceipt } from "@/api/erp/workflow"

const { proxy } = getCurrentInstance()
const { erp_receipt_status } = useDict("erp_receipt_status")

const receiptList = ref([])
const orderOptions = ref([])
const warehouseOptions = ref([])
const materialOptions = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

const data = reactive({
  form: { items: [] },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    receiptNo: undefined,
    orderNo: undefined,
    warehouseId: undefined,
    receiptStatus: undefined
  },
  rules: {
    orderId: [{ required: true, message: "请选择采购订单", trigger: "change" }],
    warehouseId: [{ required: true, message: "请选择入库仓库", trigger: "change" }],
    receiptDate: [{ required: true, message: "入库日期不能为空", trigger: "blur" }]
  }
})

const { form, queryParams, rules } = toRefs(data)

function getList() {
  loading.value = true
  listReceipt(queryParams.value).then(response => {
    receiptList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function getOptions() {
  listOrder({ pageNum: 1, pageSize: 200 }).then(response => {
    orderOptions.value = response.rows || []
  })
  listWarehouse({ pageNum: 1, pageSize: 200 }).then(response => {
    warehouseOptions.value = response.rows || []
  })
  listMaterial({ pageNum: 1, pageSize: 500 }).then(response => {
    materialOptions.value = response.rows || []
  })
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.value = {
    id: undefined,
    receiptNo: undefined,
    orderId: undefined,
    orderNo: undefined,
    supplierName: undefined,
    warehouseId: undefined,
    warehouseName: undefined,
    receiptDate: undefined,
    qualityNote: undefined,
    items: []
  }
  proxy.resetForm("receiptRef")
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function handleAdd() {
  reset()
  open.value = true
  title.value = "新增入库单"
}

function handleUpdate(row) {
  reset()
  const id = row.id || ids.value[0]
  getReceipt(id).then(response => {
    form.value = response.data
    form.value.items = response.data.items || []
    open.value = true
    title.value = "修改入库单"
  })
}

function handleOrderChange(value) {
  const target = orderOptions.value.find(item => item.id === value)
  if (!target) return
  form.value.orderNo = target.orderNo
  form.value.supplierName = target.supplierName
  getOrder(value).then(response => {
    const order = response.data || {}
    form.value.items = (order.items || []).map(item => {
      const quantity = Number(item.quantity || 0)
      const received = Number(item.receivedQuantity || 0)
      const remain = quantity - received > 0 ? quantity - received : 0
      return {
        orderItemId: item.id,
        materialId: item.materialId,
        materialCode: item.materialCode,
        materialName: item.materialName,
        unit: item.unit,
        receivedQuantity: remain,
        qualifiedQuantity: remain,
        rejectedQuantity: 0,
        unitCost: item.unitPrice
      }
    })
  })
}

function handleWarehouseChange(value) {
  const target = warehouseOptions.value.find(item => item.id === value)
  form.value.warehouseName = target ? target.warehouseName : undefined
}

function handleAddItem() {
  form.value.items.push({ orderItemId: undefined, materialId: undefined, materialName: undefined, unit: undefined, receivedQuantity: 0, qualifiedQuantity: 0, rejectedQuantity: 0, unitCost: 0 })
}

function handleMaterialChange(row, value) {
  const target = materialOptions.value.find(item => item.id === value)
  if (target) {
    row.materialCode = target.materialCode
    row.materialName = target.materialName
    row.unit = target.unit
  }
}

function calcQualified(row) {
  const received = Number(row.receivedQuantity || 0)
  const rejected = Number(row.rejectedQuantity || 0)
  row.qualifiedQuantity = received - rejected > 0 ? Number((received - rejected).toFixed(4)) : 0
}

function submitForm() {
  proxy.$refs["receiptRef"].validate(valid => {
    if (!valid) return
    if (!form.value.items || form.value.items.length === 0) {
      proxy.$modal.msgWarning("请至少添加一条入库明细")
      return
    }
    if (form.value.id != undefined) {
      updateReceipt(form.value).then(() => {
        proxy.$modal.msgSuccess("修改成功")
        open.value = false
        getList()
      })
    } else {
      addReceipt(form.value).then(() => {
        proxy.$modal.msgSuccess("新增成功")
        open.value = false
        getList()
      })
    }
  })
}

function handleConfirm(row) {
  proxy.$modal.confirm('确认入库后将增加库存并更新订单到货进度，是否确认入库单"' + row.receiptNo + '"？').then(() => {
    return confirmReceipt(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess("入库确认成功")
    getList()
  }).catch(() => {})
}

function handleReverse(row) {
  proxy.$modal.prompt('请输入冲销入库单"' + row.receiptNo + '"的原因').then(({ value }) => {
    return reverseReceipt(row.id, value)
  }).then(() => {
    proxy.$modal.msgSuccess("入库单已冲销")
    getList()
  }).catch(() => {})
}

function handleDelete(row) {
  const delIds = row.id || ids.value
  proxy.$modal.confirm("是否确认删除所选入库单数据？已确认的入库单不允许删除。").then(() => {
    return delReceipt(delIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

function handleExport() {
  proxy.download("erp/receipt/export", { ...queryParams.value }, `receipt_${new Date().getTime()}.xlsx`)
}

getList()
getOptions()
</script>
