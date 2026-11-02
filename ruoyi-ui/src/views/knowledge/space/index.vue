<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="空间名称" prop="spaceName">
        <el-input v-model="queryParams.spaceName" placeholder="请输入知识空间名称" clearable style="width: 200px" @keyup.enter="getList" />
      </el-form-item>
      <el-form-item label="空间类型" prop="spaceType">
        <el-select v-model="queryParams.spaceType" placeholder="全部" clearable style="width: 160px">
          <el-option v-for="dict in erp_space_type" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="spaceStatus">
        <el-select v-model="queryParams.spaceStatus" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="dict in erp_space_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['knowledge:space:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['knowledge:space:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['knowledge:space:remove']">停用</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="spaceList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="空间名称" align="center" prop="spaceName" min-width="200" show-overflow-tooltip />
      <el-table-column label="空间类型" align="center" prop="spaceType" width="140">
        <template #default="scope">
          <dict-tag :options="erp_space_type" :value="scope.row.spaceType" />
        </template>
      </el-table-column>
      <el-table-column label="数据集ID" align="center" prop="ragDatasetId" width="200" show-overflow-tooltip />
      <el-table-column label="描述" align="center" prop="description" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" align="center" prop="spaceStatus" width="110">
        <template #default="scope">
          <dict-tag :options="erp_space_status" :value="scope.row.spaceStatus" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" width="280" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['knowledge:space:edit']">修改</el-button>
          <el-button link type="primary" icon="Lock" @click="handleAcl(scope.row)" v-hasPermi="['knowledge:space:edit']">授权</el-button>
          <el-button link type="primary" icon="Document" @click="handleDocuments(scope.row)">文档</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['knowledge:space:remove']">停用</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="620px" append-to-body>
      <el-form ref="spaceRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="空间名称" prop="spaceName">
          <el-input v-model="form.spaceName" placeholder="请输入知识空间名称" />
        </el-form-item>
        <el-form-item label="空间类型" prop="spaceType">
          <el-select v-model="form.spaceType" placeholder="请选择空间类型" style="width: 100%">
            <el-option v-for="dict in erp_space_type" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人ID" prop="ownerId">
          <el-input-number v-model="form.ownerId" :min="1" controls-position="right" style="width: 100%" placeholder="留空默认当前用户" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-alert v-if="!form.id" type="info" :closable="false" show-icon title="保存时将自动在 RAGFlow 创建对应知识库数据集" style="margin-bottom: 10px" />
        <el-form-item label="数据集ID" prop="ragDatasetId" v-if="form.id">
          <el-input v-model="form.ragDatasetId" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="open = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="aclOpen" :title="'空间授权 - ' + currentSpaceName" size="50%">
      <el-form :model="aclForm" ref="aclRef" :inline="true" :rules="aclRules">
        <el-form-item label="授权对象" prop="subjectType">
          <el-select v-model="aclForm.subjectType" style="width: 130px">
            <el-option v-for="dict in erp_acl_subject_type" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="对象ID" prop="subjectId">
          <el-input-number v-model="aclForm.subjectId" :min="1" controls-position="right" style="width: 140px" />
        </el-form-item>
        <el-form-item label="权限" prop="permissionLevel">
          <el-select v-model="aclForm.permissionLevel" style="width: 120px">
            <el-option v-for="dict in erp_acl_permission" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Plus" @click="submitAcl">添加授权</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="aclLoading" :data="aclList" size="small">
        <el-table-column label="授权对象类型" align="center" prop="subjectType" width="140">
          <template #default="scope">
            <dict-tag :options="erp_acl_subject_type" :value="scope.row.subjectType" />
          </template>
        </el-table-column>
        <el-table-column label="对象ID" align="center" prop="subjectId" width="110" />
        <el-table-column label="权限级别" align="center" prop="permissionLevel" width="120">
          <template #default="scope">
            <dict-tag :options="erp_acl_permission" :value="scope.row.permissionLevel" />
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="90">
          <template #default="scope">
            <el-button link type="danger" icon="Delete" @click="handleDelAcl(scope.row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!aclLoading && aclList.length === 0" description="暂无授权记录，仅负责人可访问" :image-size="70" />
    </el-drawer>
  </div>
</template>

<script setup name="KbSpace">
import { useRouter } from "vue-router"
import { listSpace, getSpace, addSpace, updateSpace, delSpace, listAcl, addAcl, delAcl } from "@/api/knowledge/space"

const { proxy } = getCurrentInstance()
const router = useRouter()
const { erp_space_type, erp_space_status, erp_acl_subject_type, erp_acl_permission } = useDict("erp_space_type", "erp_space_status", "erp_acl_subject_type", "erp_acl_permission")

const spaceList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const aclOpen = ref(false)
const aclLoading = ref(false)
const aclList = ref([])
const currentSpaceId = ref(undefined)
const currentSpaceName = ref("")

const data = reactive({
  form: {},
  aclForm: { subjectType: "ROLE", subjectId: undefined, permissionLevel: "READ" },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    spaceName: undefined,
    spaceType: undefined,
    spaceStatus: undefined
  },
  rules: {
    spaceName: [{ required: true, message: "知识空间名称不能为空", trigger: "blur" }],
    spaceType: [{ required: true, message: "请选择空间类型", trigger: "change" }]
  },
  aclRules: {
    subjectType: [{ required: true, message: "请选择授权对象类型", trigger: "change" }],
    subjectId: [{ required: true, message: "请输入授权对象ID", trigger: "blur" }],
    permissionLevel: [{ required: true, message: "请选择权限级别", trigger: "change" }]
  }
})

const { form, aclForm, queryParams, rules, aclRules } = toRefs(data)

function getList() {
  loading.value = true
  listSpace(queryParams.value).then(response => {
    spaceList.value = response.rows || []
    total.value = response.total || 0
    loading.value = false
  })
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

function reset() {
  form.value = { id: undefined, spaceName: undefined, spaceType: "POLICY", ownerId: undefined, description: undefined, ragDatasetId: undefined }
  proxy.resetForm("spaceRef")
}

function handleAdd() {
  reset()
  open.value = true
  title.value = "新增知识空间"
}

function handleUpdate(row) {
  reset()
  const id = row.id || ids.value[0]
  getSpace(id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改知识空间"
  })
}

function submitForm() {
  proxy.$refs["spaceRef"].validate(valid => {
    if (!valid) return
    if (form.value.id != undefined) {
      updateSpace(form.value).then(() => {
        proxy.$modal.msgSuccess("修改成功")
        open.value = false
        getList()
      })
    } else {
      addSpace(form.value).then(() => {
        proxy.$modal.msgSuccess("新增成功")
        open.value = false
        getList()
      })
    }
  })
}

function handleDelete(row) {
  const delIds = row.id || ids.value
  proxy.$modal.confirm("停用后该空间文档将不再参与检索，是否确认停用？").then(() => {
    return delSpace(delIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("已停用")
  }).catch(() => {})
}

function handleAcl(row) {
  currentSpaceId.value = row.id
  currentSpaceName.value = row.spaceName
  aclOpen.value = true
  aclForm.value = { subjectType: "ROLE", subjectId: undefined, permissionLevel: "READ" }
  loadAcl()
}

function loadAcl() {
  aclLoading.value = true
  listAcl(currentSpaceId.value).then(response => {
    aclList.value = response.data || []
    aclLoading.value = false
  }).catch(() => {
    aclLoading.value = false
  })
}

function submitAcl() {
  proxy.$refs["aclRef"].validate(valid => {
    if (!valid) return
    addAcl(currentSpaceId.value, aclForm.value).then(() => {
      proxy.$modal.msgSuccess("授权成功")
      loadAcl()
    })
  })
}

function handleDelAcl(row) {
  proxy.$modal.confirm("是否确认移除该授权？").then(() => {
    return delAcl(currentSpaceId.value, row.id)
  }).then(() => {
    loadAcl()
    proxy.$modal.msgSuccess("已移除")
  }).catch(() => {})
}

function handleDocuments(row) {
  router.push({ path: "/knowledge/document", query: { spaceId: row.id } })
}

getList()
</script>
