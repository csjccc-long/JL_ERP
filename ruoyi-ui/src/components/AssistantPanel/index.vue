<template>
  <div class="assistant-panel">
    <div class="assistant-header">
      <div class="assistant-title">
        <el-icon><ChatDotRound /></el-icon>
        <span>知识助手</span>
        <el-tag v-if="contextName" size="small" type="info" class="context-tag">{{ contextName }}</el-tag>
      </div>
      <div class="assistant-actions">
        <el-tooltip content="知识服务状态" placement="bottom">
          <el-tag :type="healthType" size="small" effect="plain">{{ healthText }}</el-tag>
        </el-tooltip>
        <el-button link type="primary" icon="Plus" @click="handleNewSession">新会话</el-button>
      </div>
    </div>

    <div class="assistant-messages" ref="messagesRef" v-loading="loadingMessages">
      <el-empty v-if="messages.length === 0" description="向知识助手提问，答案将仅基于你可访问的企业文档与业务数据" :image-size="80" />
      <div v-for="(item, index) in messages" :key="index" :class="['message-row', item.role === 'USER' ? 'from-user' : 'from-assistant']">
        <div class="message-bubble">
          <div class="message-role">
            <span>{{ item.role === 'USER' ? "我" : "知识助手" }}</span>
            <span v-if="item.traceId" class="trace" @click="copyTrace(item.traceId)">trace: {{ item.traceId.slice(0, 8) }}</span>
          </div>
          <div class="message-content" v-if="item.role === 'USER'">{{ item.content }}</div>
          <div class="message-content markdown-body" v-else v-html="renderMarkdown(item.content)"></div>
          <div v-if="item.citations && item.citations.length" class="citation-list">
            <div class="citation-title">引用来源</div>
            <div v-for="citation in item.citations" :key="citation.order" class="citation-item">
              <el-tag size="small" type="primary" effect="plain">[{{ citation.order }}] {{ citation.documentName }}</el-tag>
              <span v-if="citation.pageNumber" class="citation-page">第 {{ citation.pageNumber }} 页</span>
              <el-button link type="primary" size="small" @click="openDocument(citation.documentId)">查看原文</el-button>
            </div>
          </div>
          <div v-if="item.statusText" class="status-text">{{ item.statusText }}</div>
        </div>
      </div>
    </div>

    <div class="assistant-input">
      <el-input v-model="question" type="textarea" :rows="3" resize="none" placeholder="请输入问题，Enter 发送，Shift + Enter 换行" @keydown.enter="handleEnter" />
      <div class="input-actions">
        <span class="hint">回答仅供业务参考，涉及写操作请在业务页面完成</span>
        <div>
          <el-button v-if="asking" type="danger" plain size="small" @click="handleAbort">停止</el-button>
          <el-button type="primary" size="small" :loading="asking" :disabled="!question.trim()" @click="handleAsk">发送</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup name="AssistantPanel">
import { listMessages, askStream, assistantHealth } from "@/api/knowledge/assistant"
import request from "@/utils/request"

const props = defineProps({
  contextType: {
    type: String,
    default: undefined
  },
  contextId: {
    type: [Number, String],
    default: undefined
  },
  contextName: {
    type: String,
    default: ""
  },
  sessionId: {
    type: [Number, String],
    default: undefined
  }
})

const emit = defineEmits(["update:sessionId", "created"])

const { proxy } = getCurrentInstance()

const question = ref("")
const asking = ref(false)
const messages = ref([])
const loadingMessages = ref(false)
const messagesRef = ref(null)
const currentSessionId = ref(props.sessionId)
const health = ref(null)
let abortHandler = null

const healthType = computed(() => {
  if (!health.value) return "info"
  if (!health.value.ragflowConfigured) return "info"
  return health.value.ragflowAvailable ? "success" : "danger"
})

const healthText = computed(() => {
  if (!health.value) return "检测中"
  if (!health.value.ragflowConfigured) return "未配置"
  if (!health.value.ragflowAvailable) return "不可用"
  return "服务正常"
})

const escapeHtml = text => {
  return String(text || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;")
}

// 仅渲染受控 Markdown：先转义 HTML，再处理加粗、行内代码、列表、换行
function renderMarkdown(text) {
  if (!text) return ""
  let html = escapeHtml(text)
  html = html.replace(/```([\s\S]*?)```/g, (match, code) => '<pre class="code-block">' + code + "</pre>")
  html = html.replace(/`([^`]+)`/g, "<code>$1</code>")
  html = html.replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>")
  html = html.replace(/^\s*[-*]\s+(.*)$/gm, "<div class=\"list-item\">• $1</div>")
  html = html.replace(/\n/g, "<br/>")
  return html
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

function loadHealth() {
  assistantHealth().then(response => {
    health.value = response.data || {}
  }).catch(() => {
    health.value = { ragflowConfigured: false, ragflowAvailable: false }
  })
}

function loadMessages(sessionId) {
  if (!sessionId) {
    messages.value = []
    return
  }
  loadingMessages.value = true
  listMessages(sessionId).then(response => {
    const list = response.data || []
    messages.value = list.map(item => ({
      role: item.role,
      content: item.content,
      traceId: item.traceId,
      citations: []
    }))
    loadingMessages.value = false
    scrollToBottom()
  }).catch(() => {
    loadingMessages.value = false
  })
}

function handleNewSession() {
  currentSessionId.value = undefined
  emit("update:sessionId", undefined)
  messages.value = []
  question.value = ""
}

function handleEnter(event) {
  if (event.shiftKey) return
  event.preventDefault()
  handleAsk()
}

function handleAsk() {
  const text = question.value.trim()
  if (!text || asking.value) return
  messages.value.push({ role: "USER", content: text })
  const assistantMessage = { role: "ASSISTANT", content: "", citations: [], statusText: "", traceId: "" }
  messages.value.push(assistantMessage)
  const index = messages.value.length - 1
  question.value = ""
  asking.value = true
  scrollToBottom()

  abortHandler = askStream({
    question: text,
    sessionId: currentSessionId.value || undefined,
    contextType: props.contextType || undefined,
    contextId: props.contextId || undefined
  }, {
    onMeta: data => {
      if (data && data.sessionId) {
        currentSessionId.value = data.sessionId
        emit("update:sessionId", data.sessionId)
        emit("created", data.sessionId)
      }
      if (data && data.traceId) {
        messages.value[index].traceId = data.traceId
      }
    },
    onDelta: data => {
      const content = data && typeof data === "object" ? data.content : data
      messages.value[index].content += content || ""
      scrollToBottom()
    },
    onCitation: data => {
      if (!data) return
      messages.value[index].citations.push(data)
    },
    onDone: data => {
      asking.value = false
      abortHandler = null
      const status = data && data.status
      if (status === "INSUFFICIENT_EVIDENCE") {
        messages.value[index].statusText = "依据不足：知识库中未检索到充分证据"
      } else if (status === "RAG_UNAVAILABLE") {
        messages.value[index].statusText = "知识服务暂不可用，ERP 业务操作不受影响"
      }
    },
    onError: data => {
      asking.value = false
      abortHandler = null
      const message = data && data.message ? data.message : "问答失败，请稍后重试"
      if (!messages.value[index].content) {
        messages.value[index].content = message
      }
      messages.value[index].statusText = message
    }
  })
}

function handleAbort() {
  if (abortHandler) {
    abortHandler()
    abortHandler = null
  }
  asking.value = false
}

function copyTrace(traceId) {
  proxy.$modal.msgSuccess("追踪号：" + traceId)
}

function openDocument(documentId) {
  if (!documentId) return
  request({
    url: "/knowledge/document/preview/" + documentId,
    method: "get",
    responseType: "blob"
  }).then(blob => {
    const url = window.URL.createObjectURL(new Blob([blob], { type: blob.type || "application/pdf" }))
    window.open(url, "_blank")
    setTimeout(() => window.URL.revokeObjectURL(url), 60000)
  }).catch(() => {
    proxy.$modal.msgError("原文打开失败，请确认文档是否存在或是否有权访问")
  })
}

watch(() => props.sessionId, value => {
  if (value !== currentSessionId.value) {
    currentSessionId.value = value
    loadMessages(value)
  }
})

onMounted(() => {
  loadHealth()
  loadMessages(currentSessionId.value)
})

onBeforeUnmount(() => {
  handleAbort()
})

defineExpose({ handleNewSession, loadMessages })
</script>

<style scoped>
.assistant-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 480px;
}
.assistant-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}
.assistant-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}
.context-tag {
  margin-left: 8px;
}
.assistant-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.assistant-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px 4px;
  max-height: 60vh;
}
.message-row {
  display: flex;
  margin-bottom: 12px;
}
.from-user {
  justify-content: flex-end;
}
.message-bubble {
  max-width: 82%;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f4f4f5;
  font-size: 14px;
  line-height: 1.6;
}
.from-user .message-bubble {
  background: #ecf5ff;
}
.message-role {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #909399;
  font-size: 12px;
  margin-bottom: 4px;
}
.trace {
  cursor: pointer;
}
.message-content {
  white-space: pre-wrap;
  word-break: break-word;
}
.markdown-body :deep(.code-block) {
  background: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
  overflow-x: auto;
}
.citation-list {
  margin-top: 8px;
  border-top: 1px dashed #dcdfe6;
  padding-top: 6px;
}
.citation-title {
  color: #909399;
  font-size: 12px;
  margin-bottom: 4px;
}
.citation-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  flex-wrap: wrap;
}
.citation-page {
  color: #909399;
  font-size: 12px;
}
.status-text {
  color: #e6a23c;
  font-size: 12px;
  margin-top: 6px;
}
.assistant-input {
  border-top: 1px solid #ebeef5;
  padding-top: 8px;
}
.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 6px;
}
.hint {
  color: #c0c4cc;
  font-size: 12px;
}
</style>
