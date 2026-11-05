<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="所属空间" prop="spaceId">
        <el-select v-model="queryParams.spaceId" placeholder="全部空间" clearable filterable style="width: 200px">
          <el-option v-for="item in spaceOptionList" :key="item.id" :label="item.spaceName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="文档名称" prop="documentName">
        <el-input v-model="queryParams.documentName" placeholder="请输入文档名称" clearable style="width: 200px" @keyup.enter="getList" />
      </el-form-item>
      <el-form-item label="解析状态" prop="parseStatus">
        <el-select v-model="queryParams.parseStatus" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="dict in erp_document_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="doc-toolbar">
      <div class="doc-toolbar__left">
        <el-button type="primary" icon="Upload" @click="handleUpload" v-hasPermi="['knowledge:document:upload']">上传文档</el-button>
        <span class="doc-toolbar__hint">共 {{ total }} 篇文档</span>
      </div>
      <div class="doc-toolbar__right">
        <el-radio-group v-model="viewMode" size="small" class="view-switch">
          <el-radio-button value="folder">文件夹</el-radio-button>
          <el-radio-button value="list">列表</el-radio-button>
        </el-radio-group>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
      </div>
    </div>

    <div v-if="viewMode === 'folder'" v-loading="loading" class="doc-grid">
      <doc-folder
        v-for="doc in documentList"
        :key="doc.id"
        :doc="doc"
        :space-name="spaceNameOf(doc.spaceId)"
        :status-label="statusLabelOf(doc.parseStatus)"
        :status-type="statusTypeOf(doc.parseStatus)"
        @open="openDetail"
        @preview="handlePreview"
        @download="handleDownload"
        @link="handleLinks"
        @disable="handleFolderDisable"
        @enable="handleEnable"
      />
      <el-empty v-if="!loading && !documentList.length" class="doc-grid__empty" description="暂无知识文档" :image-size="86" />
    </div>

    <el-table v-if="viewMode === 'list'" v-loading="loading" :data="documentList">
      <el-table-column label="文档名称" align="center" prop="documentName" min-width="200" show-overflow-tooltip />
      <el-table-column label="所属空间" align="center" prop="spaceName" width="160" show-overflow-tooltip />
      <el-table-column label="版本" align="center" prop="documentVersion" width="90">
        <template #default="scope">
          <span>v{{ scope.row.documentVersion }}</span>
          <el-tag v-if="scope.row.isCurrent == 1" type="success" size="small" style="margin-left: 4px">当前</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="解析状态" align="center" prop="parseStatus" width="110">
        <template #default="scope">
          <dict-tag :options="erp_document_status" :value="scope.row.parseStatus" />
        </template>
      </el-table-column>
      <el-table-column label="分片数" align="center" prop="chunkCount" width="90" />
      <el-table-column label="文件大小" align="center" prop="fileSize" width="110">
        <template #default="scope">{{ formatSize(scope.row.fileSize) }}</template>
      </el-table-column>
      <el-table-column label="业务关联" align="center" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.links && scope.row.links.length" size="small" type="info">{{ scope.row.links.length }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="失败原因" align="center" prop="errorMessage" min-width="160" show-overflow-tooltip />
      <el-table-column label="上传时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" width="280" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handlePreview(scope.row)" v-if="scope.row.parseStatus !== 'DISABLED'">预览</el-button>
          <el-button link type="primary" icon="Download" @click="handleDownload(scope.row)">下载</el-button>
          <el-button link type="primary" icon="RefreshRight" @click="handleRetry(scope.row)" v-if="scope.row.parseStatus === 'FAILED'" v-hasPermi="['knowledge:document:upload']">重试</el-button>
          <el-button link type="primary" icon="Link" @click="handleLinks(scope.row)" v-hasPermi="['knowledge:document:upload']">关联</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDisable(scope.row)" v-if="scope.row.parseStatus !== 'DISABLED'" v-hasPermi="['knowledge:document:remove']">停用</el-button>
          <el-button link type="success" icon="RefreshLeft" @click="handleEnable(scope.row)" v-if="scope.row.parseStatus === 'DISABLED'" v-hasPermi="['knowledge:document:upload', 'knowledge:document:remove']">启用</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="上传知识文档" v-model="uploadOpen" width="640px" append-to-body :close-on-click-modal="false">
      <el-form ref="uploadRef" :model="uploadForm" :rules="uploadRules" label-width="100px">
        <el-form-item label="所属空间" prop="spaceId">
          <el-select v-model="uploadForm.spaceId" placeholder="请选择知识空间" filterable style="width: 100%">
            <el-option v-for="item in spaceOptionList" :key="item.id" :label="item.spaceName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择文件" prop="file">
          <el-upload
            ref="fileRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :file-list="fileList"
            drag
            style="width: 100%"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">将文件拖到此处，或<em>点击选择</em></div>
            <template #tip>
              <div class="el-upload__tip">支持 pdf / doc / docx / xls / xlsx / txt / png / jpg / jpeg，单个文件不超过 {{ maxUploadLabel }}</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="稳定键" prop="stableKey">
          <el-input v-model="uploadForm.stableKey" placeholder="留空默认取文件名（用于版本管理）" />
        </el-form-item>
        <el-divider content-position="left">业务关联（可选）</el-divider>
        <el-form-item label="业务类型" prop="businessType">
          <el-select v-model="uploadForm.businessType" placeholder="不关联业务对象" clearable style="width: 100%">
            <el-option v-for="dict in erp_business_type" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务对象ID" prop="businessId" v-if="uploadForm.businessType">
          <el-input-number v-model="uploadForm.businessId" :min="1" controls-position="right" style="width: 100%" placeholder="请输入业务对象主键ID" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="uploading" @click="submitUpload">上 传</el-button>
          <el-button @click="uploadOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="detailOpen" title="文档详情" size="440px" class="doc-detail-drawer">
      <template v-if="detailDoc">
        <div class="detail-head">
          <span class="detail-ext">{{ extOf(detailDoc) }}</span>
          <div class="detail-head__text">
            <div class="detail-name">{{ detailDoc.documentName }}</div>
            <div class="detail-sub">{{ spaceNameOf(detailDoc.spaceId) }}</div>
          </div>
        </div>
        <el-descriptions :column="1" border size="small" class="detail-desc">
          <el-descriptions-item label="解析状态">
            <dict-tag :options="erp_document_status" :value="detailDoc.parseStatus" />
          </el-descriptions-item>
          <el-descriptions-item label="版本">
            <span>v{{ detailDoc.documentVersion }}</span>
            <el-tag v-if="detailDoc.isCurrent == 1" type="success" size="small" style="margin-left: 6px">当前</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="所属空间">{{ spaceNameOf(detailDoc.spaceId) }}</el-descriptions-item>
          <el-descriptions-item label="分片数">{{ detailDoc.chunkCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="文件大小">{{ formatSize(detailDoc.fileSize) }}</el-descriptions-item>
          <el-descriptions-item label="业务关联">{{ detailDoc.links && detailDoc.links.length ? detailDoc.links.length + ' 条' : '未关联' }}</el-descriptions-item>
          <el-descriptions-item label="上传时间">{{ detailDoc.createTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="就绪时间">{{ detailDoc.readyTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="稳定键">{{ detailDoc.stableKey || '-' }}</el-descriptions-item>
          <el-descriptions-item label="文件类型">{{ detailDoc.mimeType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="RAG 文档 ID">{{ detailDoc.ragDocumentId || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="detailDoc.errorMessage" label="失败原因">
            <span class="detail-error">{{ detailDoc.errorMessage }}</span>
          </el-descriptions-item>
        </el-descriptions>
        <div class="detail-actions">
          <el-button v-if="detailDoc.parseStatus !== 'DISABLED'" icon="View" @click="handlePreview(detailDoc)">预览</el-button>
          <el-button icon="Download" @click="handleDownload(detailDoc)">下载</el-button>
          <el-button v-if="detailDoc.parseStatus === 'FAILED'" icon="RefreshRight" @click="handleRetry(detailDoc)">重试</el-button>
          <el-button icon="Link" @click="handleLinks(detailDoc)" v-hasPermi="['knowledge:document:upload']">关联</el-button>
          <el-button v-if="detailDoc.parseStatus !== 'DISABLED'" type="danger" plain icon="Delete" @click="handleDisable(detailDoc)" v-hasPermi="['knowledge:document:remove']">停用</el-button>
          <el-button v-else type="success" plain icon="RefreshLeft" @click="handleEnable(detailDoc)" v-hasPermi="['knowledge:document:upload', 'knowledge:document:remove']">启用</el-button>
        </div>
      </template>
    </el-drawer>

    <el-drawer v-model="linkOpen" :title="'业务关联 - ' + currentDocName" size="50%">
      <el-form :model="linkForm" ref="linkRef" :inline="true" :rules="linkRules">
        <el-form-item label="业务类型" prop="businessType">
          <el-select v-model="linkForm.businessType" style="width: 160px">
            <el-option v-for="dict in erp_business_type" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务对象ID" prop="businessId">
          <el-input-number v-model="linkForm.businessId" :min="1" controls-position="right" style="width: 140px" />
        </el-form-item>
        <el-form-item label="关联类型" prop="relationType">
          <el-input v-model="linkForm.relationType" style="width: 130px" placeholder="如 ATTACHMENT" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Plus" @click="submitLink">添加</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="linkLoading" :data="linkList" size="small">
        <el-table-column label="业务类型" align="center" prop="businessType" width="160">
          <template #default="scope">
            <dict-tag :options="erp_business_type" :value="scope.row.businessType" />
          </template>
        </el-table-column>
        <el-table-column label="业务对象ID" align="center" prop="businessId" width="120" />
        <el-table-column label="关联类型" align="center" prop="relationType" />
        <el-table-column label="操作" align="center" width="90">
          <template #default="scope">
            <el-button link type="danger" icon="Delete" @click="handleDelLink(scope.row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <file-preview v-model="previewOpen" :document-id="previewDocId" :document-name="previewDocName" />
  </div>
</template>

<script setup name="KbDocument">
import { useRoute } from "vue-router"
import { listDocument, uploadDocument, retryDocument, disableDocument, enableDocument, listLinks, addLink, delLink, downloadDocument } from "@/api/knowledge/document"
import { spaceOptions } from "@/api/knowledge/space"
import FilePreview from "@/components/FilePreview"
import DocFolder from "./components/DocFolder"

const { proxy } = getCurrentInstance()
const route = useRoute()
const { erp_document_status, erp_business_type } = useDict("erp_document_status", "erp_business_type")

const documentList = ref([])
const spaceOptionList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const uploadOpen = ref(false)
const uploading = ref(false)
const fileList = ref([])
const previewOpen = ref(false)
const previewDocId = ref(undefined)
const previewDocName = ref("")
const linkOpen = ref(false)
const linkLoading = ref(false)
const linkList = ref([])
const currentDocId = ref(undefined)
const currentDocName = ref("")
const maxUpload = ref(52428800)
const detailOpen = ref(false)
const detailDoc = ref(null)
const viewMode = ref(localStorage.getItem("kb-doc-view") === "list" ? "list" : "folder")

watch(viewMode, value => {
  localStorage.setItem("kb-doc-view", value)
})

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    spaceId: route.query.spaceId ? Number(route.query.spaceId) : undefined,
    documentName: undefined,
    parseStatus: undefined
  },
  uploadForm: {
    spaceId: route.query.spaceId ? Number(route.query.spaceId) : undefined,
    stableKey: undefined,
    businessType: undefined,
    businessId: undefined,
    file: undefined
  },
  linkForm: { businessType: "SUPPLIER", businessId: undefined, relationType: "ATTACHMENT" },
  uploadRules: {
    spaceId: [{ required: true, message: "请选择知识空间", trigger: "change" }],
    file: [{ required: true, message: "请选择上传文件", trigger: "change" }]
  },
  linkRules: {
    businessType: [{ required: true, message: "请选择业务类型", trigger: "change" }],
    businessId: [{ required: true, message: "请输入业务对象ID", trigger: "blur" }]
  }
})

const { queryParams, uploadForm, linkForm, uploadRules, linkRules } = toRefs(data)

const maxUploadLabel = computed(() => {
  const mb = maxUpload.value / 1024 / 1024
  return mb >= 1 ? Math.round(mb) + "MB" : maxUpload.value + "字节"
})

function formatSize(size) {
  if (!size && size !== 0) return "-"
  if (size < 1024) return size + " B"
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + " KB"
  return (size / 1024 / 1024).toFixed(2) + " MB"
}

function extOf(doc) {
  const name = (doc && doc.documentName) || ""
  const raw = name.includes(".") ? name.split(".").pop() : ""
  const value = (raw || "").toUpperCase()
  return value && value.length <= 5 ? value : "FILE"
}

function spaceNameOf(spaceId) {
  const hit = spaceOptionList.value.find(item => item.id === spaceId)
  return hit ? hit.spaceName : "未归属空间"
}

function statusLabelOf(value) {
  const hit = (erp_document_status.value || []).find(item => item.value === value)
  return hit ? hit.label : value || "-"
}

function statusTypeOf(value) {
  const hit = (erp_document_status.value || []).find(item => item.value === value)
  return (hit && hit.elTagType) || "info"
}

function openDetail(doc) {
  detailDoc.value = doc
  detailOpen.value = true
}

function handleEnable(doc) {
  proxy.$modal.loading("正在启用并重新解析…")
  enableDocument(doc.id).then(response => {
    proxy.$modal.closeLoading()
    const data = response.data || {}
    if (data.parseStatus === "UPLOADED") {
      proxy.$modal.msgWarning("已启用，但原始文件已丢失，请重新上传后再解析")
    } else {
      proxy.$modal.msgSuccess("已启用，正在重新解析")
    }
    if (detailDoc.value && detailDoc.value.id === doc.id) {
      detailDoc.value = { ...detailDoc.value, ...data }
    }
    getList()
  }).catch(() => {
    proxy.$modal.closeLoading()
  })
}

function handleFolderDisable(doc) {
  disableDocument(doc.id).then(() => {
    proxy.$modal.msgSuccess("已停用")
    getList()
  }).catch(() => {})
}

function getList() {
  loading.value = true
  listDocument(queryParams.value).then(response => {
    documentList.value = response.rows || []
    total.value = response.total || 0
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

function loadSpaceOptions() {
  spaceOptions().then(response => {
    spaceOptionList.value = response.data || []
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm("queryRef")
  queryParams.value.spaceId = undefined
  handleQuery()
}

function handleUpload() {
  uploadForm.value = { spaceId: queryParams.value.spaceId, stableKey: undefined, businessType: undefined, businessId: undefined, file: undefined }
  fileList.value = []
  uploadOpen.value = true
}

function handleFileChange(file, files) {
  fileList.value = files.slice(-1)
  uploadForm.value.file = file.raw
}

function handleFileRemove() {
  fileList.value = []
  uploadForm.value.file = undefined
}

function submitUpload() {
  proxy.$refs["uploadRef"].validate(valid => {
    if (!valid) return
    const file = uploadForm.value.file
    if (!file) {
      proxy.$modal.msgWarning("请选择上传文件")
      return
    }
    if (file.size > maxUpload.value) {
      proxy.$modal.msgWarning("文件大小超过限制（" + maxUploadLabel.value + "）")
      return
    }
    const formData = new FormData()
    formData.append("file", file)
    formData.append("spaceId", uploadForm.value.spaceId)
    if (uploadForm.value.stableKey) formData.append("stableKey", uploadForm.value.stableKey)
    if (uploadForm.value.businessType && uploadForm.value.businessId) {
      formData.append("businessType", uploadForm.value.businessType)
      formData.append("businessId", uploadForm.value.businessId)
      formData.append("relationType", "ATTACHMENT")
    }
    uploading.value = true
    uploadDocument(formData).then(() => {
      proxy.$modal.msgSuccess("上传成功，正在提交解析")
      uploading.value = false
      uploadOpen.value = false
      getList()
    }).catch(() => {
      uploading.value = false
    })
  })
}

function handleRetry(row) {
  retryDocument(row.id).then(() => {
    proxy.$modal.msgSuccess("已提交重新解析")
    getList()
  })
}

function handleDisable(row) {
  proxy.$modal.confirm("停用后该文档不再参与检索，是否确认停用？").then(() => {
    return disableDocument(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("已停用")
  }).catch(() => {})
}

function openBlob(blob, fileName) {
  // 重建 Blob 时必须带上原始 MIME，否则浏览器会把文档当纯文本渲染成乱码
  const url = window.URL.createObjectURL(new Blob([blob], { type: blob.type || "application/octet-stream" }))
  const link = document.createElement("a")
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

function handlePreview(row) {
  previewDocId.value = row.id
  previewDocName.value = row.documentName
  previewOpen.value = true
}

function handleDownload(row) {
  downloadDocument(row.id).then(response => {
    openBlob(response, row.documentName)
  }).catch(() => {
    proxy.$modal.msgError("下载失败")
  })
}

function handleLinks(row) {
  currentDocId.value = row.id
  currentDocName.value = row.documentName
  linkForm.value = { businessType: "SUPPLIER", businessId: undefined, relationType: "ATTACHMENT" }
  linkOpen.value = true
  loadLinks()
}

function loadLinks() {
  linkLoading.value = true
  listLinks(currentDocId.value).then(response => {
    linkList.value = response.data || []
    linkLoading.value = false
  }).catch(() => {
    linkLoading.value = false
  })
}

function submitLink() {
  proxy.$refs["linkRef"].validate(valid => {
    if (!valid) return
    addLink(currentDocId.value, linkForm.value).then(() => {
      proxy.$modal.msgSuccess("关联成功")
      loadLinks()
      getList()
    })
  })
}

function handleDelLink(row) {
  proxy.$modal.confirm("是否确认移除该业务关联？").then(() => {
    return delLink(currentDocId.value, row.id)
  }).then(() => {
    loadLinks()
    getList()
    proxy.$modal.msgSuccess("已移除")
  }).catch(() => {})
}

loadSpaceOptions()
getList()
</script>

<style scoped lang="scss">
.doc-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.doc-toolbar__left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.doc-toolbar__hint {
  font-size: 12px;
  color: #9a9ea1;
}

.doc-toolbar__right {
  display: flex;
  align-items: center;
  gap: 10px;

  :deep(.right-toolbar) {
    float: none;
    margin-left: 0;
  }
}

.view-switch {
  :deep(.el-radio-button__inner) {
    padding: 6px 14px;
    font-size: 12px;
  }
}

.doc-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, 300px);
  justify-content: start;
  gap: 8px 10px;
  padding: 8px 2px 22px;
  min-height: 220px;
}

.doc-grid__empty {
  grid-column: 1 / -1;
  width: 100%;
}


.detail-head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(22, 22, 24, 0.06);
}

.detail-ext {
  flex: none;
  width: 48px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px 12px 12px 8px;
  background: linear-gradient(180deg, #a9ece7 0%, #4fc1ba 100%);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
  box-shadow: 0 10px 18px -14px rgba(6, 72, 68, 0.8);
}

.detail-head__text {
  min-width: 0;
}

.detail-name {
  font-size: 15px;
  font-weight: 600;
  line-height: 21px;
  color: #1b1c1e;
  word-break: break-all;
}

.detail-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #8b8f92;
}

.detail-desc {
  margin-top: 18px;

  :deep(.el-descriptions__label) {
    width: 96px;
    color: #75787a;
  }

  :deep(.el-descriptions__content) {
    color: #1b1c1e;
    word-break: break-all;
  }
}

.detail-error {
  color: #c0453f;
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid rgba(22, 22, 24, 0.06);

  :deep(.el-button + .el-button) {
    margin-left: 0;
  }
}

@media (max-width: 768px) {
  .doc-grid {
    grid-template-columns: repeat(auto-fill, minmax(168px, 1fr));
    justify-content: stretch;
  }
}
</style>
