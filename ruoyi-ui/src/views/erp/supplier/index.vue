<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="供应商编码" prop="supplierCode">
        <el-input v-model="queryParams.supplierCode" placeholder="请输入供应商编码" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="供应商名称" prop="supplierName">
        <el-input v-model="queryParams.supplierName" placeholder="请输入供应商名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="准入状态" prop="admissionStatus">
        <el-select v-model="queryParams.admissionStatus" placeholder="全部" clearable style="width: 160px">
          <el-option v-for="dict in erp_supplier_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="风险等级" prop="riskLevel">
        <el-select v-model="queryParams.riskLevel" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="dict in erp_supplier_risk" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['erp:supplier:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['erp:supplier:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['erp:supplier:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['erp:supplier:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="supplierList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="供应商编码" align="center" prop="supplierCode" width="130" />
      <el-table-column label="供应商名称" align="center" prop="supplierName" min-width="180" show-overflow-tooltip />
      <el-table-column label="统一社会信用代码" align="center" prop="creditCode" width="180" show-overflow-tooltip />
      <el-table-column label="分类" align="center" prop="supplierCategory" width="110" />
      <el-table-column label="联系人" align="center" prop="contactName" width="100" />
      <el-table-column label="联系电话" align="center" prop="contactPhone" width="130" />
      <el-table-column label="准入状态" align="center" prop="admissionStatus" width="110">
        <template #default="scope">
          <dict-tag :options="erp_supplier_status" :value="scope.row.admissionStatus" />
        </template>
      </el-table-column>
      <el-table-column label="风险等级" align="center" prop="riskLevel" width="100">
        <template #default="scope">
          <dict-tag :options="erp_supplier_risk" :value="scope.row.riskLevel" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['erp:supplier:edit']">修改</el-button>
          <el-button link type="primary" icon="Checked" @click="handleAudit(scope.row)" v-hasPermi="['erp:supplier:approve']">准入</el-button>
          <el-button link type="primary" icon="Document" @click="handleKnowledge(scope.row)">知识</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['erp:supplier:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="720px" append-to-body>
      <el-form ref="supplierRef" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="供应商编码" prop="supplierCode">
              <el-input v-model="form.supplierCode" placeholder="留空自动生成" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商名称" prop="supplierName">
              <el-input v-model="form.supplierName" placeholder="请输入供应商名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="信用代码" prop="creditCode">
              <el-input v-model="form.creditCode" placeholder="请输入统一社会信用代码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商分类" prop="supplierCategory">
              <el-input v-model="form.supplierCategory" placeholder="如：原材料、设备、服务" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人" prop="contactName">
              <el-input v-model="form.contactName" placeholder="请输入联系人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="税号" prop="taxNo">
              <el-input v-model="form.taxNo" placeholder="请输入税号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="地址" prop="address">
              <el-input v-model="form.address" placeholder="请输入地址" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开户银行" prop="bankName">
              <el-input v-model="form.bankName" placeholder="请输入开户银行" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="银行账号" prop="bankAccount">
              <el-input v-model="form.bankAccount" placeholder="请输入银行账号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="风险等级" prop="riskLevel">
              <el-select v-model="form.riskLevel" placeholder="请选择风险等级" style="width: 100%">
                <el-option v-for="dict in erp_supplier_risk" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
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

    <el-dialog title="供应商准入审核" v-model="auditOpen" width="520px" append-to-body>
      <el-form ref="auditRef" :model="auditForm" :rules="auditRules" label-width="100px">
        <el-form-item label="准入结论" prop="admissionStatus">
          <el-radio-group v-model="auditForm.admissionStatus">
            <el-radio value="APPROVED">准入通过</el-radio>
            <el-radio value="REJECTED">拒绝准入</el-radio>
            <el-radio value="SUSPENDED">暂停合作</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="风险等级" prop="riskLevel">
          <el-select v-model="auditForm.riskLevel" placeholder="请选择风险等级" style="width: 100%">
            <el-option v-for="dict in erp_supplier_risk" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="审核意见" prop="opinion">
          <el-input v-model="auditForm.opinion" type="textarea" :rows="3" placeholder="请输入审核意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitAudit">确 定</el-button>
          <el-button @click="auditOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="knowledgeOpen" title="供应商知识库" size="45%">
      <AssistantPanel context-type="SUPPLIER" :context-id="currentBusinessId" :context-name="currentBusinessName" />
    </el-drawer>
  </div>
</template>

<script setup name="ErpSupplier">
import { listSupplier, getSupplier, addSupplier, updateSupplier, delSupplier, auditSupplier } from "@/api/erp/supplier"
import AssistantPanel from "@/components/AssistantPanel/index.vue"

const { proxy } = getCurrentInstance()
const { erp_supplier_status, erp_supplier_risk } = useDict("erp_supplier_status", "erp_supplier_risk")

const supplierList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const auditOpen = ref(false)
const knowledgeOpen = ref(false)
const currentBusinessId = ref(undefined)
const currentBusinessName = ref("")

const data = reactive({
  form: {},
  auditForm: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    supplierCode: undefined,
    supplierName: undefined,
    admissionStatus: undefined,
    riskLevel: undefined
  },
  rules: {
    supplierName: [{ required: true, message: "供应商名称不能为空", trigger: "blur" }],
    contactPhone: [{ pattern: /^$|^[0-9+\-() ]{6,20}$/, message: "联系电话格式不正确", trigger: "blur" }],
    email: [{ pattern: /^$|^[\w.-]+@[\w-]+\.[\w.]+$/, message: "邮箱格式不正确", trigger: "blur" }]
  },
  auditRules: {
    admissionStatus: [{ required: true, message: "请选择准入结论", trigger: "change" }]
  }
})

const { form, auditForm, queryParams, rules, auditRules } = toRefs(data)

function getList() {
  loading.value = true
  listSupplier(queryParams.value).then(response => {
    supplierList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.value = {
    id: undefined,
    supplierCode: undefined,
    supplierName: undefined,
    creditCode: undefined,
    supplierCategory: undefined,
    contactName: undefined,
    contactPhone: undefined,
    email: undefined,
    address: undefined,
    bankName: undefined,
    bankAccount: undefined,
    taxNo: undefined,
    riskLevel: "LOW",
    remark: undefined
  }
  proxy.resetForm("supplierRef")
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
  title.value = "新增供应商"
}

function handleUpdate(row) {
  reset()
  const id = row.id || ids.value[0]
  getSupplier(id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改供应商"
  })
}

function submitForm() {
  proxy.$refs["supplierRef"].validate(valid => {
    if (!valid) return
    if (form.value.id != undefined) {
      updateSupplier(form.value).then(() => {
        proxy.$modal.msgSuccess("修改成功")
        open.value = false
        getList()
      })
    } else {
      addSupplier(form.value).then(() => {
        proxy.$modal.msgSuccess("新增成功")
        open.value = false
        getList()
      })
    }
  })
}

function handleAudit(row) {
  auditForm.value = {
    id: row.id,
    admissionStatus: "APPROVED",
    riskLevel: row.riskLevel || "LOW",
    opinion: undefined
  }
  auditOpen.value = true
}

function submitAudit() {
  proxy.$refs["auditRef"].validate(valid => {
    if (!valid) return
    auditSupplier(auditForm.value.id, {
      admissionStatus: auditForm.value.admissionStatus,
      riskLevel: auditForm.value.riskLevel,
      opinion: auditForm.value.opinion
    }).then(() => {
      proxy.$modal.msgSuccess("准入审核已提交")
      auditOpen.value = false
      getList()
    })
  })
}

function handleKnowledge(row) {
  currentBusinessId.value = row.id
  currentBusinessName.value = row.supplierName
  knowledgeOpen.value = true
}

function handleDelete(row) {
  const delIds = row.id || ids.value
  proxy.$modal.confirm('是否确认删除所选供应商数据？删除前请确认无关联合同与订单。').then(() => {
    return delSupplier(delIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

function handleExport() {
  proxy.download("erp/supplier/export", { ...queryParams.value }, `supplier_${new Date().getTime()}.xlsx`)
}

getList()
</script>
