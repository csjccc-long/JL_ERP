<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="960px"
    top="5vh"
    append-to-body
    destroy-on-close
    class="file-preview-dialog"
    @closed="handleClosed"
  >
    <div class="file-preview" v-loading="loading" element-loading-text="正在加载文档…">
      <div v-if="errorMessage" class="file-preview-tip">
        <el-icon class="tip-icon"><WarningFilled /></el-icon>
        <p class="tip-text">{{ errorMessage }}</p>
        <el-button type="primary" plain @click="handleDownload">下载后查看</el-button>
      </div>
      <template v-else-if="!loading">
        <iframe v-if="kind === 'pdf'" :src="objectUrl" class="preview-frame" title="PDF 预览"></iframe>
        <img v-else-if="kind === 'image'" :src="objectUrl" class="preview-image" alt="图片预览" />
        <pre v-else-if="kind === 'text'" class="preview-text">{{ textContent }}</pre>
        <div v-else-if="kind === 'docx'" ref="docxContainer" class="preview-docx"></div>
        <div v-else-if="kind === 'sheet'" class="preview-sheet">
          <el-tabs v-if="sheets.length > 1" v-model="activeSheet" class="sheet-tabs">
            <el-tab-pane v-for="(sheet, index) in sheets" :key="sheet.name" :label="sheet.name" :name="String(index)" />
          </el-tabs>
          <div class="preview-sheet-body" v-html="currentSheetHtml"></div>
        </div>
        <div v-else class="file-preview-tip">
          <el-icon class="tip-icon"><Document /></el-icon>
          <p class="tip-text">「{{ fileExtension.toUpperCase() || "未知" }}」格式暂不支持在线预览，请下载后查看。</p>
          <el-button type="primary" plain @click="handleDownload">下载文档</el-button>
        </div>
      </template>
    </div>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button type="primary" plain :disabled="loading" @click="handleDownload">下载</el-button>
    </template>
  </el-dialog>
</template>

<script setup name="FilePreview">
import { computed, nextTick, ref, watch } from "vue"
import { ElMessage } from "element-plus"
import { saveAs } from "file-saver"
import { previewDocument, downloadDocument } from "@/api/knowledge/document"

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  documentId: {
    type: [Number, String],
    default: undefined
  },
  documentName: {
    type: String,
    default: ""
  }
})

const emit = defineEmits(["update:modelValue"])

const TEXT_EXTENSIONS = ["txt", "md", "markdown", "log", "csv", "tsv", "json", "xml", "yml", "yaml", "html", "htm", "ini", "properties", "sql", "java", "js", "ts", "py"]
const IMAGE_MIME = { png: "image/png", jpg: "image/jpeg", jpeg: "image/jpeg", gif: "image/gif", bmp: "image/bmp", webp: "image/webp", svg: "image/svg+xml" }

const visible = computed({
  get: () => props.modelValue,
  set: value => emit("update:modelValue", value)
})

const loading = ref(false)
const errorMessage = ref("")
const kind = ref("unsupported")
const objectUrl = ref("")
const textContent = ref("")
const sheets = ref([])
const activeSheet = ref("0")
const docxContainer = ref(null)

const dialogTitle = computed(() => props.documentName ? "文档预览 - " + props.documentName : "文档预览")
const fileExtension = computed(() => {
  const name = props.documentName || ""
  const index = name.lastIndexOf(".")
  return index === -1 ? "" : name.slice(index + 1).toLowerCase()
})
const currentSheetHtml = computed(() => {
  const sheet = sheets.value[Number(activeSheet.value)]
  return sheet ? sheet.html : ""
})

watch(() => props.modelValue, value => {
  if (value) {
    loadDocument()
  }
})

function resetState() {
  if (objectUrl.value) {
    window.URL.revokeObjectURL(objectUrl.value)
    objectUrl.value = ""
  }
  errorMessage.value = ""
  kind.value = "unsupported"
  textContent.value = ""
  sheets.value = []
  activeSheet.value = "0"
}

function resolveKind(extension) {
  if (extension === "pdf") return "pdf"
  if (IMAGE_MIME[extension]) return "image"
  if (TEXT_EXTENSIONS.includes(extension)) return "text"
  if (extension === "docx") return "docx"
  if (extension === "xlsx" || extension === "xls") return "sheet"
  return "unsupported"
}

async function readErrorMessage(blob) {
  try {
    const data = JSON.parse(await blob.text())
    return data.msg || data.message || "文档读取失败，请下载后查看"
  } catch (e) {
    return "文档读取失败，请下载后查看"
  }
}

async function decodeText(blob) {
  const buffer = await blob.arrayBuffer()
  const bytes = new Uint8Array(buffer)
  let text = new TextDecoder("utf-8").decode(bytes)
  if (text.includes("\uFFFD")) {
    try {
      text = new TextDecoder("gbk").decode(bytes)
    } catch (e) {
      // 浏览器不支持 gbk 时保留 utf-8 结果
    }
  }
  return text
}

async function renderDocx(blob) {
  await nextTick()
  const container = docxContainer.value
  if (!container) return
  const { renderAsync } = await import("docx-preview")
  container.innerHTML = ""
  await renderAsync(blob, container, null, {
    inWrapper: true,
    ignoreWidth: true,
    ignoreHeight: true,
    breakPages: false,
    renderHeaders: false,
    renderFooters: false
  })
}

async function renderSheet(blob) {
  const XLSX = await import("xlsx")
  const workbook = XLSX.read(await blob.arrayBuffer(), { type: "array" })
  sheets.value = workbook.SheetNames.map(name => ({
    name,
    html: XLSX.utils.sheet_to_html(workbook.Sheets[name])
  }))
  activeSheet.value = "0"
}

async function loadDocument() {
  resetState()
  if (!props.documentId) {
    errorMessage.value = "缺少文档标识，无法预览"
    return
  }
  loading.value = true
  try {
    const blob = await previewDocument(props.documentId)
    if (!(blob instanceof Blob)) {
      errorMessage.value = "文档读取失败，请下载后查看"
      return
    }
    if ((blob.type || "").toLowerCase().includes("json")) {
      errorMessage.value = await readErrorMessage(blob)
      return
    }
    const extension = fileExtension.value
    kind.value = resolveKind(extension)
    if (kind.value === "pdf") {
      objectUrl.value = window.URL.createObjectURL(new Blob([blob], { type: "application/pdf" }))
    } else if (kind.value === "image") {
      objectUrl.value = window.URL.createObjectURL(new Blob([blob], { type: IMAGE_MIME[extension] }))
    } else if (kind.value === "text") {
      textContent.value = await decodeText(blob)
    } else if (kind.value === "docx") {
      loading.value = false
      await renderDocx(blob)
    } else if (kind.value === "sheet") {
      loading.value = false
      await renderSheet(blob)
    }
  } catch (error) {
    errorMessage.value = "文档加载失败，请稍后重试或下载后查看"
  } finally {
    loading.value = false
  }
}

function handleDownload() {
  downloadDocument(props.documentId).then(blob => {
    if (!(blob instanceof Blob)) {
      ElMessage.error("下载失败")
      return
    }
    saveAs(new Blob([blob], { type: blob.type || "application/octet-stream" }), props.documentName || "document")
  }).catch(() => {
    ElMessage.error("下载失败")
  })
}

function handleClosed() {
  resetState()
}
</script>

<style scoped>
.file-preview {
  min-height: 420px;
  max-height: 74vh;
  overflow: auto;
}
.preview-frame {
  width: 100%;
  height: 74vh;
  border: 0;
}
.preview-image {
  display: block;
  max-width: 100%;
  margin: 0 auto;
}
.preview-text {
  margin: 0;
  padding: 16px;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 13px;
  line-height: 1.7;
  color: var(--el-text-color-primary);
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
}
.preview-docx {
  min-height: 420px;
}
.preview-sheet-body :deep(table) {
  border-collapse: collapse;
  font-size: 13px;
}
.preview-sheet-body :deep(td),
.preview-sheet-body :deep(th) {
  border: 1px solid var(--el-border-color);
  padding: 4px 8px;
  white-space: pre-wrap;
}
.file-preview-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: 420px;
  color: var(--el-text-color-secondary);
}
.tip-icon {
  font-size: 40px;
  color: var(--el-color-warning);
}
.tip-text {
  margin: 0;
  font-size: 14px;
  text-align: center;
}
</style>
