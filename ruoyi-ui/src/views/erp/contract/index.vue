<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="合同编号" prop="contractNo">
        <el-input v-model="queryParams.contractNo" placeholder="请输入合同编号" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="合同名称" prop="contractName">
        <el-input v-model="queryParams.contractName" placeholder="请输入合同名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="供应商" prop="supplierId">
        <el-select v-model="queryParams.supplierId" placeholder="全部" clearable filterable style="width: 200px">
          <el-option v-for="item in supplierOptions" :key="item.id" :label="item.supplierName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="合同状态" prop="contractStatus">
        <el-select v-model="queryParams.contractStatus" placeholder="全部" clearable style="width: 160px">
          <el-option v-for="dict in erp_contract_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['erp:contract:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['erp:contract:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['erp:contract:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['erp:contract:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="contractList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="合同编号" align="center" prop="contractNo" width="150" />
      <el-table-column label="合同名称" align="center" prop="contractName" min-width="180" show-overflow-tooltip />
      <el-table-column label="供应商" align="center" prop="supplierName" min-width="160" show-overflow-tooltip />
      <el-table-column label="合同金额" align="center" prop="contractAmount" width="140">
        <template #default="scope">
          <span>{{ formatAmount(scope.row.contractAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="币种" align="center" prop="currency" width="80" />
      <el-table-column label="生效日期" align="center" prop="startDate" width="120" />
      <el-table-column label="到期日期" align="center" prop="endDate" width="120" />
      <el-table-column label="合同状态" align="center" prop="contractStatus" width="110">
        <template #default="scope">
          <dict-tag :options="erp_contract_status" :value="scope.row.contractStatus" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button v-if="scope.row.contractStatus === 'DRAFT'" link type="primary" icon="Promotion" @click="handleSubmit(scope.row)" v-hasPermi="['erp:contract:submit']">提交</el-button>
          <el-button v-if="scope.row.contractStatus === 'PENDING_APPROVAL'" link type="primary" icon="Checked" @click="handleApprove(scope.row)" v-hasPermi="['erp:contract:approve']">审批</el-button>
          <el-button v-if="scope.row.contractStatus === 'ACTIVE'" link type="primary" icon="CircleClose" @click="handleTerminate(scope.row)" v-hasPermi="['erp:contract:terminate']">终止</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['erp:contract:edit']">修改</el-button>
          <el-button link type="primary" icon="Document" @click="handleKnowledge(scope.row)">知识</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['erp:contract:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="760px" append-to-body>
      <el-form ref="contractRef" :model="form" :rules="rules" label-width="110px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="合同编号" prop="contractNo">
              <el-input v-model="form.contractNo" placeholder="留空自动生成" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="合同名称" prop="contractName">
              <el-input v-model="form.contractName" placeholder="请输入合同名称" />
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
            <el-form-item label="合同金额" prop="contractAmount">
              <el-input-number v-model="form.contractAmount" :precision="2" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="币种" prop="currency">
              <el-input v-model="form.currency" placeholder="CNY" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="签订日期" prop="signedDate">
              <el-date-picker v-model="form.signedDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择签订日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生效日期" prop="startDate">
              <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择生效日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="到期日期" prop="endDate">
              <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择到期日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="付款条件" prop="paymentTerms">
              <el-input v-model="form.paymentTerms" type="textarea" :rows="2" placeholder="请输入付款条件" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="合同审批" v-model="approveOpen" width="520px" append-to-body>
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

    <el-drawer v-model="knowledgeOpen" title="合同知识库" size="45%">
      <AssistantPanel context-type="CONTRACT" :context-id="currentBusinessId" :context-name="currentBusinessName" />
    </el-drawer>
  </div>
</template>

<script setup name="ErpContract">
import { listContract, getContract, addContract, updateContract, delContract } from "@/api/erp/contract"
import { listSupplier } from "@/api/erp/supplier"
import { submitContract, approveContract, terminateContract } from "@/api/erp/workflow"
import AssistantPanel from "@/components/AssistantPanel/index.vue"

const { proxy } = getCurrentInstance()
const { erp_contract_status } = useDict("erp_contract_status")

const contractList = ref([])
const supplierOptions = ref([])
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
  form: {},
  approveForm: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    contractNo: undefined,
    contractName: undefined,
    supplierId: undefined,
    contractStatus: undefined
  },
  rules: {
    contractName: [{ required: true, message: "合同名称不能为空", trigger: "blur" }],
    supplierId: [{ required: true, message: "请选择供应商", trigger: "change" }],
    contractAmount: [{ required: true, message: "合同金额不能为空", trigger: "blur" }],
    endDate: [{ required: true, message: "到期日期不能为空", trigger: "blur" }]
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
  listContract(queryParams.value).then(response => {
    contractList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function getSuppliers() {
  listSupplier({ pageNum: 1, pageSize: 200 }).then(response => {
    supplierOptions.value = response.rows || []
  })
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.value = {
    id: undefined,
    contractNo: undefined,
    contractName: undefined,
    supplierId: undefined,
    supplierName: undefined,
    startDate: undefined,
    endDate: undefined,
    contractAmount: undefined,
    currency: "CNY",
    paymentTerms: undefined,
    signedDate: undefined,
    remark: undefined
  }
  proxy.resetForm("contractRef")
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

function handleAdd() {
  reset()
  open.value = true
  title.value = "新增合同"
}

function handleUpdate(row) {
  reset()
  const id = row.id || ids.value[0]
  getContract(id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改合同"
  })
}

function submitForm() {
  proxy.$refs["contractRef"].validate(valid => {
    if (!valid) return
    if (form.value.id != undefined) {
      updateContract(form.value).then(() => {
        proxy.$modal.msgSuccess("修改成功")
        open.value = false
        getList()
      })
    } else {
      addContract(form.value).then(() => {
        proxy.$modal.msgSuccess("新增成功")
        open.value = false
        getList()
      })
    }
  })
}

function handleSubmit(row) {
  proxy.$modal.confirm('是否确认提交合同"' + row.contractName + '"进入审批？').then(() => {
    return submitContract(row.id)
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
    approveContract(approveForm.value.id, approveForm.value.pass, approveForm.value.opinion).then(() => {
      proxy.$modal.msgSuccess("审批完成")
      approveOpen.value = false
      getList()
    })
  })
}

function handleTerminate(row) {
  proxy.$modal.prompt('请输入终止合同"' + row.contractName + '"的原因', "合同终止").then(({ value }) => {
    return terminateContract(row.id, value)
  }).then(() => {
    proxy.$modal.msgSuccess("合同已终止")
    getList()
  }).catch(() => {})
}

function handleKnowledge(row) {
  currentBusinessId.value = row.id
  currentBusinessName.value = row.contractName
  knowledgeOpen.value = true
}

function handleDelete(row) {
  const delIds = row.id || ids.value
  proxy.$modal.confirm('是否确认删除所选合同数据？').then(() => {
    return delContract(delIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

function handleExport() {
  proxy.download("erp/contract/export", { ...queryParams.value }, `contract_${new Date().getTime()}.xlsx`)
}

getList()
getSuppliers()
</script>
