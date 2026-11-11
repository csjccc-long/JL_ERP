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
          <div class="upload-card">
            <div
              class="upload-card__header"
              :class="{ 'is-dragover': dragOver }"
              @click="pickFile"
              @dragover.prevent="dragOver = true"
              @dragenter.prevent="dragOver = true"
              @dragleave.prevent="dragOver = false"
              @drop.prevent="onFileDrop"
            >
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                <path
                  d="M7 10V9C7 6.23858 9.23858 4 12 4C14.7614 4 17 6.23858 17 9V10C19.2091 10 21 11.7909 21 14C21 15.4806 20.1956 16.8084 19 17.5M7 10C4.79086 10 3 11.7909 3 14C3 15.4806 3.8044 16.8084 5 17.5M7 10C7.43285 10 7.84965 10.0688 8.24006 10.1959M12 12V21M12 12L15 15M12 12L9 15"
                  stroke="currentColor"
                  stroke-width="1.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
              <p>{{ uploadForm.file ? '重新选择文件' : '点击选择文件，或拖到此处' }}</p>
            </div>
            <label class="upload-card__footer" for="kb-upload-input">
              <svg class="footer-file" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                <path d="M15.331 6H8.5v20h15V14.154h-8.169z" />
                <path d="M18.153 6h-.009v5.342H23.5v-.002z" />
              </svg>
              <p :class="{ 'is-empty': !uploadForm.file }">{{ uploadForm.file ? uploadForm.file.name : '尚未选择文件' }}</p>
              <button v-if="uploadForm.file" type="button" class="footer-clear" title="移除文件" @click.stop.prevent="clearFile">
                <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                  <path d="M5.16565 10.1534C5.07629 8.99181 5.99473 8 7.15975 8H16.8402C18.0053 8 18.9237 8.9918 18.8344 10.1534L18.142 19.1534C18.0619 20.1954 17.193 21 16.1479 21H7.85206C6.80699 21 5.93811 20.1954 5.85795 19.1534L5.16565 10.1534Z" stroke="currentColor" stroke-width="2" />
                  <path d="M19.5 5H4.5" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
                  <path d="M10 3C10 2.44772 10.4477 2 11 2H13C13.5523 2 14 2.44772 14 3V5H10V3Z" stroke="currentColor" stroke-width="2" />
                </svg>
              </button>
            </label>
            <input
              id="kb-upload-input"
              ref="fileInputRef"
              class="upload-card__input"
              type="file"
              :accept="uploadAccept"
              @change="onFilePicked"
            />
            <div v-if="uploadForm.file && stageVisible" class="upload-stage">
              <image-gen-loader
                :progress="stageProgress"
                :text="stageText"
                :colors="stageColors"
                :cell-size="3"
                :gap="1"
                :band-height="42"
                :wave-amplitude="9"
                :overlay-opacity="0"
              />
            </div>
          </div>
          <div class="upload-tip">支持 pdf / doc / docx / xls / xlsx / txt / png / jpg / jpeg，单个文件不超过 {{ maxUploadLabel }}</div>
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
import ImageGenLoader from "@/components/ImageGenLoader/index.vue"
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
/** 上传波浪：ready 选完文件即显形，uploading 跟真实字节走，done/error 收尾 */
const uploadPhase = ref("ready")
const uploadPercent = ref(0)
const stageVisible = ref(false)
const stageProgress = computed(() => {
  if (uploadPhase.value === "done") return 100
  if (uploadPhase.value === "uploading") return Math.min(99, uploadPercent.value)
  return null
})
const stageText = computed(() => {
  const map = { ready: "准备上传", uploading: "上传中", done: "上传完成", error: "上传失败" }
  return map[uploadPhase.value] || "准备上传"
})
const stageColors = computed(() => {
  if (uploadPhase.value === "error") return ["#fda4af", "#9f1239"]
  if (uploadPhase.value === "done") return ["#5eead4", "#0f766e"]
  return ["#22d3ee", "#1e40af"]
})
const fileInputRef = ref(null)
const dragOver = ref(false)
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
  if (fileInputRef.value) fileInputRef.value.value = ""
  stageVisible.value = false
  uploadPhase.value = "ready"
  uploadPercent.value = 0
  uploadOpen.value = true
}

const UPLOAD_EXT = ["pdf", "doc", "docx", "xls", "xlsx", "txt", "png", "jpg", "jpeg"]
const uploadAccept = UPLOAD_EXT.map(ext => "." + ext).join(",")

function pickFile() {
  if (fileInputRef.value) fileInputRef.value.click()
}

function applyFile(file) {
  if (!file) return
  const ext = (file.name.split(".").pop() || "").toLowerCase()
  if (!UPLOAD_EXT.includes(ext)) {
    proxy.$modal.msgWarning("不支持的文件类型：" + ext)
    return
  }
  if (file.size > maxUpload.value) {
    proxy.$modal.msgWarning("文件大小超过限制（" + maxUploadLabel.value + "）")
    return
  }
  uploadForm.value.file = file
  uploadPhase.value = "ready"
  uploadPercent.value = 0
  stageVisible.value = true
}

function onFilePicked(event) {
  const file = event.target.files && event.target.files[0]
  applyFile(file)
  event.target.value = ""
}

function onFileDrop(event) {
  dragOver.value = false
  const file = event.dataTransfer && event.dataTransfer.files && event.dataTransfer.files[0]
  applyFile(file)
}

function clearFile() {
  uploadForm.value.file = undefined
  stageVisible.value = false
  uploadPhase.value = "ready"
  uploadPercent.value = 0
  if (fileInputRef.value) fileInputRef.value.value = ""
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
    uploadPhase.value = "uploading"
    uploadPercent.value = 0
    uploadDocument(formData, event => {
      const total = (event && event.total) || file.size || 0
      const loaded = (event && event.loaded) || 0
      if (total > 0) {
        const percent = Math.min(99, Math.round((loaded / total) * 100))
        if (percent > uploadPercent.value) uploadPercent.value = percent
      }
    }).then(() => {
      // 文件已传完，稍等后端入库并提交解析
      uploadPercent.value = 100
      uploadPhase.value = "done"
      return new Promise(resolve => setTimeout(resolve, 850))
    }).then(() => {
      proxy.$modal.msgSuccess("上传成功，正在提交解析")
      uploading.value = false
      uploadOpen.value = false
      stageVisible.value = false
      uploadPhase.value = "ready"
      getList()
    }).catch(() => {
      uploading.value = false
      uploadPhase.value = "error"
      setTimeout(() => {
        if (uploadPhase.value === "error") uploadPhase.value = "ready"
      }, 1800)
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

.upload-card {
  position: relative;
  width: 100%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  border-radius: 12px;
  background: rgba(0, 190, 180, 0.045);
  box-shadow: 4px 4px 24px rgba(20, 60, 58, 0.08);
}

.upload-stage {
  position: absolute;
  inset: 0 0 52px 0;
  z-index: 3;
  overflow: hidden;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 6px 22px rgba(20, 60, 58, 0.12);
  pointer-events: none;
  animation: upload-stage-in 0.22s ease-out;
}

@keyframes upload-stage-in {
  from {
    opacity: 0;
    transform: scale(0.98);
  }

  to {
    opacity: 1;
    transform: none;
  }
}

.upload-card__header {
  width: 100%;
  height: 150px;
  box-sizing: border-box;
  border: 2px dashed rgba(0, 190, 180, 0.5);
  border-radius: 10px;
  background: rgba(0, 190, 180, 0.03);
  color: #00beb4;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;

  svg {
    width: 62px;
    height: 62px;
    transition: transform 0.25s ease;
  }

  p {
    margin: 0;
    font-size: 13px;
    color: #51706c;
  }

  &:hover,
  &.is-dragover {
    border-color: var(--el-color-primary, #00beb4);
    background: rgba(0, 190, 180, 0.1);
    box-shadow: inset 0 0 0 1px rgba(0, 190, 180, 0.16);

    svg {
      transform: translateY(-2px);
    }
  }
}

.upload-card__footer {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  height: 42px;
  box-sizing: border-box;
  padding: 0 10px;
  border-radius: 10px;
  background: rgba(0, 190, 180, 0.09);
  cursor: pointer;
  transition: background 0.2s ease;

  &:hover {
    background: rgba(0, 190, 180, 0.16);
  }

  .footer-file {
    width: 18px;
    height: 18px;
    flex: none;
    fill: var(--el-color-primary, #00beb4);
  }

  p {
    flex: 1;
    min-width: 0;
    margin: 0;
    font-size: 12.5px;
    text-align: center;
    color: #16423c;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  p.is-empty {
    color: #94a5a2;
  }
}

.footer-clear {
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  color: #7c8d8a;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 1px 6px rgba(20, 60, 58, 0.14);
  transition: color 0.2s ease, background 0.2s ease;

  svg {
    width: 14px;
    height: 14px;
  }

  &:hover {
    color: #d9534f;
    background: #fff;
  }
}

.upload-card__input {
  display: none;
}

.upload-tip {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.5;
  color: #9a9ea1;
}

@media (max-width: 768px) {
  .doc-grid {
    grid-template-columns: repeat(auto-fill, minmax(168px, 1fr));
    justify-content: stretch;
  }

  .upload-card__header {
    height: 120px;

    svg {
      width: 52px;
      height: 52px;
    }
  }
}
</style>
