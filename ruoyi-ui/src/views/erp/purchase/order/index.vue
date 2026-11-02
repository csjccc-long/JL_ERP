<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="订单编号" prop="orderNo">
        <el-input v-model="queryParams.orderNo" placeholder="请输入订单编号" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="供应商" prop="supplierId">
        <el-select v-model="queryParams.supplierId" placeholder="全部" clearable filterable style="width: 200px">
          <el-option v-for="item in supplierOptions" :key="item.id" :label="item.supplierName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="订单状态" prop="orderStatus">
        <el-select v-model="queryParams.orderStatus" placeholder="全部" clearable style="width: 170px">
          <el-option v-for="dict in erp_order_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['erp:order:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['erp:order:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['erp:order:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['erp:order:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="orderList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="订单编号" align="center" prop="orderNo" width="150" />
      <el-table-column label="供应商" align="center" prop="supplierName" min-width="180" show-overflow-tooltip />
      <el-table-column label="关联合同" align="center" prop="contractNo" width="150" show-overflow-tooltip />
      <el-table-column label="关联申请" align="center" prop="requestNo" width="150" show-overflow-tooltip />
      <el-table-column label="订单日期" align="center" prop="orderDate" width="120" />
      <el-table-column label="期望到货" align="center" prop="expectedDate" width="120" />
      <el-table-column label="订单金额" align="center" prop="totalAmount" width="140">
        <template #default="scope">
          <span>{{ formatAmount(scope.row.totalAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="orderStatus" width="140">
        <template #default="scope">
          <dict-tag :options="erp_order_status" :value="scope.row.orderStatus" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="320" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button v-if="scope.row.orderStatus === 'DRAFT'" link type="primary" icon="Promotion" @click="handleSubmit(scope.row)" v-hasPermi="['erp:order:submit']">提交</el-button>
          <el-button v-if="scope.row.orderStatus === 'PENDING_APPROVAL'" link type="primary" icon="Checked" @click="handleApprove(scope.row)" v-hasPermi="['erp:order:approve']">审批</el-button>
          <el-button v-if="canCancel(scope.row)" link type="primary" icon="CircleClose" @click="handleCancel(scope.row)" v-hasPermi="['erp:order:edit']">取消</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['erp:order:edit']">修改</el-button>
          <el-button link type="primary" icon="Document" @click="handleKnowledge(scope.row)">知识</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['erp:order:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="orderRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="订单编号" prop="orderNo">
              <el-input v-model="form.orderNo" placeholder="留空自动生成" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商" prop="supplierId">
              <el-select v-model="form.supplierId" placeholder="请选择供应商" filterable style="width: 100%" @change="handleSupplierChange">
                <el-option v-for="item in supplierOptions" :key="item.id" :label="item.supplierName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联合同" prop="contractId">
              <el-select v-model="form.contractId" placeholder="可不选" clearable filterable style="width: 100%" @change="handleContractChange">
                <el-option v-for="item in contractOptions" :key="item.id" :label="item.contractNo + ' - ' + item.contractName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联申请" prop="requestId">
              <el-select v-model="form.requestId" placeholder="可不选" clearable filterable style="width: 100%" @change="handleRequestChange">
                <el-option v-for="item in requestOptions" :key="item.id" :label="item.requestNo + ' - ' + item.title" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="订单日期" prop="orderDate">
              <el-date-picker v-model="form.orderDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择订单日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="期望到货" prop="expectedDate">
              <el-date-picker v-model="form.expectedDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择期望到货日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">订单明细</el-divider>
        <el-row class="mb8">
          <el-button type="primary" plain icon="Plus" size="small" @click="handleAddItem">添加明细</el-button>
        </el-row>
        <el-table :data="form.items" border size="small">
          <el-table-column label="物料" width="220">
            <template #default="scope">
              <el-select v-model="scope.row.materialId" placeholder="请选择物料" filterable style="width: 100%" @change="val => handleMaterialChange(scope.row, val)">
                <el-option v-for="item in materialOptions" :key="item.id" :label="item.materialName" :value="item.id" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="规格型号" prop="specification" width="140">
            <template #default="scope">
              <el-input v-model="scope.row.specification" placeholder="规格" />
            </template>
          </el-table-column>
          <el-table-column label="单位" prop="unit" width="80">
            <template #default="scope">
              <el-input v-model="scope.row.unit" placeholder="单位" />
            </template>
          </el-table-column>
          <el-table-column label="数量" width="130">
            <template #default="scope">
              <el-input-number v-model="scope.row.quantity" :precision="4" :min="0" controls-position="right" style="width: 100%" @change="calcAmount(scope.row)" />
            </template>
          </el-table-column>
          <el-table-column label="已入库" width="90" align="center">
            <template #default="scope">
              <span>{{ scope.row.receivedQuantity || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="140">
            <template #default="scope">
              <el-input-number v-model="scope.row.unitPrice" :precision="2" :min="0" controls-position="right" style="width: 100%" @change="calcAmount(scope.row)" />
            </template>
          </el-table-column>
          <el-table-column label="金额" width="130">
            <template #default="scope">
              <span>{{ formatAmount(scope.row.amount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center">
            <template #default="scope">
              <el-button link type="danger" icon="Delete" @click="form.items.splice(scope.$index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-row justify="end" style="margin-top: 8px">
          <span style="font-weight: 600">订单总金额：{{ formatAmount(form.totalAmount) }}</span>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="采购订单审批" v-model="approveOpen" width="520px" append-to-body>
      <el-form ref="approveRef" :model="approveForm" :rules="approveRules" label-width="90px">
        <el-form-item label="审批结论" prop="pass">
          <el-radio-group v-model="approveForm.pass">
            <el-radio :value="true">通过</el-radio>
            <el-radio :value="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见" prop="opinion">
          <el-input v-model="approveForm.opinion" type="textarea" :rows="3" placeholder="请输入审批意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitApprove">确 定</el-button>
          <el-button @click="approveOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="knowledgeOpen" title="采购订单知识库" size="45%">
      <AssistantPanel context-type="PURCHASE_ORDER" :context-id="currentBusinessId" :context-name="currentBusinessName" />
    </el-drawer>
  </div>
</template>

<script setup name="ErpPurchaseOrder">
import { listOrder, getOrder, addOrder, updateOrder, delOrder } from "@/api/erp/order"
import { listSupplier } from "@/api/erp/supplier"
import { listContract } from "@/api/erp/contract"
import { listRequest } from "@/api/erp/request"
import { listMaterial } from "@/api/erp/material"
import { submitOrder, approveOrder, cancelOrder } from "@/api/erp/workflow"
import AssistantPanel from "@/components/AssistantPanel/index.vue"

const { proxy } = getCurrentInstance()
const { erp_order_status } = useDict("erp_order_status")

const orderList = ref([])
const supplierOptions = ref([])
const contractOptions = ref([])
const requestOptions = ref([])
const materialOptions = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const approveOpen = ref(false)
const knowledgeOpen = ref(false)
const currentBusinessId = ref(undefined)
const currentBusinessName = ref("")

const data = reactive({
  form: { items: [] },
  approveForm: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    orderNo: undefined,
    supplierId: undefined,
    orderStatus: undefined
  },
  rules: {
    supplierId: [{ required: true, message: "请选择供应商", trigger: "change" }],
    orderDate: [{ required: true, message: "订单日期不能为空", trigger: "blur" }]
  },
  approveRules: {
    pass: [{ required: true, message: "请选择审批结论", trigger: "change" }]
  }
})

const { form, approveForm, queryParams, rules, approveRules } = toRefs(data)

function formatAmount(value) {
  if (value === null || value === undefined || value === "") return "-"
  return Number(value).toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function canCancel(row) {
  return ["DRAFT", "PENDING_APPROVAL", "APPROVED"].includes(row.orderStatus)
}

function getList() {
  loading.value = true
  listOrder(queryParams.value).then(response => {
    orderList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function getOptions() {
  listSupplier({ pageNum: 1, pageSize: 200 }).then(response => {
    supplierOptions.value = response.rows || []
  })
  listContract({ pageNum: 1, pageSize: 200 }).then(response => {
    contractOptions.value = response.rows || []
  })
  listRequest({ pageNum: 1, pageSize: 200 }).then(response => {
    requestOptions.value = response.rows || []
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
    orderNo: undefined,
    requestId: undefined,
    requestNo: undefined,
    supplierId: undefined,
    supplierName: undefined,
    contractId: undefined,
    contractNo: undefined,
    orderDate: undefined,
    expectedDate: undefined,
    totalAmount: 0,
    remark: undefined,
    items: []
  }
  proxy.resetForm("orderRef")
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

function handleSupplierChange(value) {
  const target = supplierOptions.value.find(item => item.id === value)
  form.value.supplierName = target ? target.supplierName : undefined
}

function handleContractChange(value) {
  const target = contractOptions.value.find(item => item.id === value)
  form.value.contractNo = target ? target.contractNo : undefined
}

function handleRequestChange(value) {
  const target = requestOptions.value.find(item => item.id === value)
  form.value.requestNo = target ? target.requestNo : undefined
}

function handleAdd() {
  reset()
  open.value = true
  title.value = "新增采购订单"
}

function handleUpdate(row) {
  reset()
  const id = row.id || ids.value[0]
  getOrder(id).then(response => {
    form.value = response.data
    form.value.items = response.data.items || []
    open.value = true
    title.value = "修改采购订单"
  })
}

function handleAddItem() {
  form.value.items.push({ materialId: undefined, materialName: undefined, specification: undefined, unit: undefined, quantity: 1, receivedQuantity: 0, unitPrice: 0, amount: 0 })
}

function handleMaterialChange(row, value) {
  const target = materialOptions.value.find(item => item.id === value)
  if (target) {
    row.materialCode = target.materialCode
    row.materialName = target.materialName
    row.specification = target.specification
    row.unit = target.unit
  }
}

function calcAmount(row) {
  const quantity = Number(row.quantity || 0)
  const price = Number(row.unitPrice || 0)
  row.amount = Number((quantity * price).toFixed(2))
  let total = 0
  form.value.items.forEach(item => {
    total += Number(item.amount || 0)
  })
  form.value.totalAmount = Number(total.toFixed(2))
}

function submitForm() {
  proxy.$refs["orderRef"].validate(valid => {
    if (!valid) return
    if (!form.value.items || form.value.items.length === 0) {
      proxy.$modal.msgWarning("请至少添加一条订单明细")
      return
    }
    if (form.value.id != undefined) {
      updateOrder(form.value).then(() => {
        proxy.$modal.msgSuccess("修改成功")
        open.value = false
        getList()
      })
    } else {
      addOrder(form.value).then(() => {
        proxy.$modal.msgSuccess("新增成功")
        open.value = false
        getList()
      })
    }
  })
}

function handleSubmit(row) {
  proxy.$modal.confirm('是否确认提交订单"' + row.orderNo + '"进入审批？').then(() => {
    return submitOrder(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess("提交成功")
    getList()
  }).catch(() => {})
}

function handleApprove(row) {
  approveForm.value = { id: row.id, pass: true, opinion: undefined }
  approveOpen.value = true
}

function submitApprove() {
  proxy.$refs["approveRef"].validate(valid => {
    if (!valid) return
    approveOrder(approveForm.value.id, approveForm.value.pass, approveForm.value.opinion).then(() => {
      proxy.$modal.msgSuccess("审批完成")
      approveOpen.value = false
      getList()
    })
  })
}

function handleCancel(row) {
  proxy.$modal.prompt('请输入取消订单"' + row.orderNo + '"的原因').then(({ value }) => {
    return cancelOrder(row.id, value)
  }).then(() => {
    proxy.$modal.msgSuccess("订单已取消")
    getList()
  }).catch(() => {})
}

function handleKnowledge(row) {
  currentBusinessId.value = row.id
  currentBusinessName.value = row.orderNo
  knowledgeOpen.value = true
}

function handleDelete(row) {
  const delIds = row.id || ids.value
  proxy.$modal.confirm("是否确认删除所选采购订单数据？").then(() => {
    return delOrder(delIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

function handleExport() {
  proxy.download("erp/order/export", { ...queryParams.value }, `purchase_order_${new Date().getTime()}.xlsx`)
}

getList()
getOptions()
</script>
