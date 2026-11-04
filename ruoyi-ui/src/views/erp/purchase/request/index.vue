<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="申请单号" prop="requestNo">
        <el-input v-model="queryParams.requestNo" placeholder="请输入申请单号" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="申请标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入申请标题" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="申请状态" prop="requestStatus">
        <el-select v-model="queryParams.requestStatus" placeholder="全部" clearable style="width: 160px">
          <el-option v-for="dict in erp_request_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['erp:request:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['erp:request:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['erp:request:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button plain icon="Download" @click="handleExport" v-hasPermi="['erp:request:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="requestList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="申请单号" align="center" prop="requestNo" width="150" />
      <el-table-column label="申请标题" align="center" prop="title" min-width="180" show-overflow-tooltip />
      <el-table-column label="申请人" align="center" prop="applicantName" width="110" />
      <el-table-column label="需求日期" align="center" prop="requiredDate" width="120" />
      <el-table-column label="目标仓库" align="center" prop="targetWarehouseName" width="130" show-overflow-tooltip />
      <el-table-column label="预估金额" align="center" prop="estimatedAmount" width="130">
        <template #default="scope">
          <span>{{ formatAmount(scope.row.estimatedAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="requestStatus" width="110">
        <template #default="scope">
          <dict-tag :options="erp_request_status" :value="scope.row.requestStatus" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button v-if="scope.row.requestStatus === 'DRAFT'" link type="primary" icon="Promotion" @click="handleSubmit(scope.row)" v-hasPermi="['erp:request:submit']">提交</el-button>
          <el-button v-if="scope.row.requestStatus === 'PENDING_APPROVAL'" link type="primary" icon="Checked" @click="handleApprove(scope.row)" v-hasPermi="['erp:request:approve']">审批</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['erp:request:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['erp:request:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="900px" append-to-body>
      <el-form ref="requestRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="申请单号" prop="requestNo">
              <el-input v-model="form.requestNo" placeholder="留空自动生成" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="申请标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入申请标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="需求日期" prop="requiredDate">
              <el-date-picker v-model="form.requiredDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择需求日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标仓库" prop="targetWarehouseId">
              <el-select v-model="form.targetWarehouseId" placeholder="请选择目标仓库" style="width: 100%" @change="handleWarehouseChange">
                <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.warehouseName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="申请理由" prop="requestReason">
              <el-input v-model="form.requestReason" type="textarea" :rows="2" placeholder="请输入申请理由" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">申请明细</el-divider>
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
          <el-table-column label="规格型号" prop="specification" width="150">
            <template #default="scope">
              <el-input v-model="scope.row.specification" placeholder="规格" />
            </template>
          </el-table-column>
          <el-table-column label="单位" prop="unit" width="90">
            <template #default="scope">
              <el-input v-model="scope.row.unit" placeholder="单位" />
            </template>
          </el-table-column>
          <el-table-column label="数量" width="140">
            <template #default="scope">
              <el-input-number v-model="scope.row.quantity" :precision="4" :min="0" controls-position="right" style="width: 100%" @change="calcAmount(scope.row)" />
            </template>
          </el-table-column>
          <el-table-column label="预估单价" width="150">
            <template #default="scope">
              <el-input-number v-model="scope.row.estimatedPrice" :precision="2" :min="0" controls-position="right" style="width: 100%" @change="calcAmount(scope.row)" />
            </template>
          </el-table-column>
          <el-table-column label="预估金额" width="130">
            <template #default="scope">
              <span>{{ formatAmount(scope.row.estimatedAmount) }}</span>
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

    <el-dialog title="采购申请审批" v-model="approveOpen" width="520px" append-to-body>
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
  </div>
</template>

<script setup name="ErpPurchaseRequest">
import { listRequest, getRequest, addRequest, updateRequest, delRequest } from "@/api/erp/request"
import { listWarehouse } from "@/api/erp/warehouse"
import { listMaterial } from "@/api/erp/material"
import { submitRequest, approveRequest } from "@/api/erp/workflow"

const { proxy } = getCurrentInstance()
const { erp_request_status } = useDict("erp_request_status")

const requestList = ref([])
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
const approveOpen = ref(false)

const data = reactive({
  form: { items: [] },
  approveForm: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    requestNo: undefined,
    title: undefined,
    requestStatus: undefined
  },
  rules: {
    title: [{ required: true, message: "申请标题不能为空", trigger: "blur" }],
    targetWarehouseId: [{ required: true, message: "请选择目标仓库", trigger: "change" }]
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

function getList() {
  loading.value = true
  listRequest(queryParams.value).then(response => {
    requestList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function getOptions() {
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
    requestNo: undefined,
    title: undefined,
    requiredDate: undefined,
    targetWarehouseId: undefined,
    targetWarehouseName: undefined,
    requestReason: undefined,
    items: []
  }
  proxy.resetForm("requestRef")
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
  title.value = "新增采购申请"
}

function handleUpdate(row) {
  reset()
  const id = row.id || ids.value[0]
  getRequest(id).then(response => {
    form.value = response.data
    form.value.items = response.data.items || []
    open.value = true
    title.value = "修改采购申请"
  })
}

function handleAddItem() {
  form.value.items.push({ materialId: undefined, materialName: undefined, specification: undefined, unit: undefined, quantity: 1, estimatedPrice: 0, estimatedAmount: 0 })
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

function handleWarehouseChange(value) {
  const target = warehouseOptions.value.find(item => item.id === value)
  form.value.targetWarehouseName = target ? target.warehouseName : undefined
}

function calcAmount(row) {
  const quantity = Number(row.quantity || 0)
  const price = Number(row.estimatedPrice || 0)
  row.estimatedAmount = Number((quantity * price).toFixed(2))
}

function submitForm() {
  proxy.$refs["requestRef"].validate(valid => {
    if (!valid) return
    if (!form.value.items || form.value.items.length === 0) {
      proxy.$modal.msgWarning("请至少添加一条申请明细")
      return
    }
    let totalAmount = 0
    form.value.items.forEach(item => {
      totalAmount += Number(item.estimatedAmount || 0)
    })
    form.value.estimatedAmount = Number(totalAmount.toFixed(2))
    if (form.value.id != undefined) {
      updateRequest(form.value).then(() => {
        proxy.$modal.msgSuccess("修改成功")
        open.value = false
        getList()
      })
    } else {
      addRequest(form.value).then(() => {
        proxy.$modal.msgSuccess("新增成功")
        open.value = false
        getList()
      })
    }
  })
}

function handleSubmit(row) {
  proxy.$modal.confirm('是否确认提交申请"' + row.title + '"进入审批？').then(() => {
    return submitRequest(row.id)
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
    approveRequest(approveForm.value.id, approveForm.value.pass, approveForm.value.opinion).then(() => {
      proxy.$modal.msgSuccess("审批完成")
      approveOpen.value = false
      getList()
    })
  })
}

function handleDelete(row) {
  const delIds = row.id || ids.value
  proxy.$modal.confirm("是否确认删除所选采购申请数据？").then(() => {
    return delRequest(delIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

function handleExport() {
  proxy.download("erp/request/export", { ...queryParams.value }, `purchase_request_${new Date().getTime()}.xlsx`)
}

getList()
getOptions()
</script>
