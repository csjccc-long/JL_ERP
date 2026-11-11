<template>
  <div class="assistant-panel">
    <header class="assistant-header">
      <div class="assistant-brand">
        <span class="brand-avatar">
          <el-icon><MagicStick /></el-icon>
        </span>
        <div class="brand-text">
          <div class="brand-title">
            <span>智能助手</span>
            <el-tag v-if="contextName" size="small" effect="plain" class="context-tag">{{ contextName }}</el-tag>
          </div>
          <div class="brand-sub">
            <span class="health-dot" :class="'is-' + healthType"></span>
            <span class="health-text">{{ healthText }}</span>
            <span class="brand-sep">·</span>
            <span>回答基于你可访问的企业文档与业务数据</span>
          </div>
        </div>
      </div>
      <div class="assistant-actions">
        <el-button class="new-chat" size="small" plain @click="handleNewSession">
          <el-icon><Plus /></el-icon>
          <span>新会话</span>
        </el-button>
      </div>
    </header>

    <div class="assistant-body">
      <div
        class="assistant-messages"
        ref="messagesRef"
        v-loading="loadingMessages"
        element-loading-background="rgba(255, 255, 255, 0.7)"
        aria-live="polite"
        @scroll.passive="handleScroll"
      >
        <div v-if="messages.length === 0" class="chat-empty">
          <span class="empty-mark">
            <el-icon><MagicStick /></el-icon>
          </span>
          <h3 class="empty-title">今天想了解点什么？</h3>
          <p class="empty-desc">直接提问采购、合同、库存与制度相关的问题，答案会标注引用来源，便于你回查原文。</p>
          <div class="empty-hints">
            <button v-for="hint in suggestions" :key="hint" type="button" class="hint-card" @click="askSuggestion(hint)">
              <span>{{ hint }}</span>
              <el-icon class="hint-arrow"><TopRight /></el-icon>
            </button>
          </div>
        </div>

        <div
          v-for="(item, index) in messages"
          :key="index"
          class="message-row"
          :class="[item.role === 'USER' ? 'is-user' : 'is-assistant', { 'is-streaming': streamingIndex === index }]"
        >
          <span class="msg-avatar" :class="item.role === 'USER' ? 'is-user' : 'is-ai'">
            <el-icon v-if="item.role === 'USER'"><User /></el-icon>
            <el-icon v-else><MagicStick /></el-icon>
          </span>

          <div class="msg-main">
            <div class="msg-meta">
              <span class="msg-name">{{ item.role === "USER" ? "我" : "智能助手" }}</span>
              <span v-if="item.createdAt" class="msg-time">{{ formatTime(item.createdAt) }}</span>
              <button v-if="item.traceId" type="button" class="trace" @click="copyTrace(item.traceId)">
                trace · {{ item.traceId.slice(0, 8) }}
              </button>
            </div>

            <div class="bubble" :class="item.role === 'USER' ? 'bubble-user' : 'bubble-ai'">
              <template v-if="item.role === 'USER'">
                <div class="plain-text">{{ item.content }}</div>
              </template>
              <template v-else>
                <div v-if="streamingIndex === index && !item.content" class="typing">
                  <i></i><i></i><i></i>
                  <span>正在检索企业知识库…</span>
                </div>
                <div v-else class="markdown-body" v-html="renderMarkdown(item.content)"></div>
              </template>

              <div v-if="item.citations && item.citations.length" class="citations">
                <div class="citations-title">
                  <el-icon><Collection /></el-icon>
                  <span>引用来源</span>
                  <span class="citations-count">{{ item.citations.length }}</span>
                </div>
                <div v-for="citation in item.citations" :key="citation.order" class="citation">
                  <span class="citation-index">[{{ citation.order }}]</span>
                  <span class="citation-name" :title="citation.documentName">{{ citation.documentName }}</span>
                  <span v-if="citation.pageNumber" class="citation-page">第 {{ citation.pageNumber }} 页</span>
                  <button type="button" class="citation-link" @click="openDocument(citation)">查看原文</button>
                </div>
              </div>

              <div v-if="item.statusText" class="status" :class="'status-' + (item.statusType || 'warn')">
                <el-icon><WarningFilled /></el-icon>
                <span>{{ item.statusText }}</span>
              </div>
            </div>

            <div v-if="item.role === 'ASSISTANT' && item.content" class="msg-tools">
              <span v-if="streamingIndex === index" class="streaming">
                <i></i>
                <span>正在生成</span>
              </span>
              <button v-else type="button" class="tool" @click="copyAnswer(item)">
                <el-icon><CopyDocument /></el-icon>
                <span>复制</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <transition name="scroll-btn">
        <button v-show="!atBottom" type="button" class="scroll-bottom" title="回到最新" @click="scrollToBottom(true)">
          <el-icon><ArrowDown /></el-icon>
        </button>
      </transition>
    </div>

    <file-preview v-model="previewOpen" :document-id="previewDocId" :document-name="previewDocName" />

    <footer class="composer" :class="{ 'is-asking': asking }">
      <el-input
        ref="composerRef"
        v-model="question"
        type="textarea"
        :autosize="{ minRows: 1, maxRows: 5 }"
        resize="none"
        class="composer-input"
        placeholder="输入你的问题，Enter 发送，Shift + Enter 换行"
        @keydown.enter="handleEnter"
        @keydown.esc="handleEscape"
      />
      <div class="composer-bar">
        <span class="composer-hint">回答仅供业务参考，写操作请在业务页面完成</span>
        <div class="composer-actions">
          <el-button v-if="asking" class="btn-stop" size="small" @click="handleAbort">
            <el-icon><VideoPause /></el-icon>
            <span>停止</span>
          </el-button>
          <el-button class="btn-send" type="primary" size="small" :disabled="!canSend" @click="handleAsk">
            <el-icon><Promotion /></el-icon>
            <span>{{ asking ? "生成中" : "发送" }}</span>
          </el-button>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup name="AssistantPanel">
import { listMessages, askStream, assistantHealth } from "@/api/knowledge/assistant"
import FilePreview from "@/components/FilePreview"

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

const SUGGESTIONS = {
  DEFAULT: [
    "采购订单从提交到入库要经过哪些环节？",
    "合同金额超过 50 万需要谁参与会签？",
    "安全库存阈值是如何计算的？",
    "库存低于多少需要发起补货？"
  ],
  SUPPLIER: [
    "这家供应商的准入状态如何？",
    "该供应商有哪些历史采购订单？",
    "供应商风险等级如何评定？",
    "供应商资质到期会提醒吗？"
  ],
  CONTRACT: [
    "这份合同的付款条件是什么？",
    "合同金额超过 50 万需要谁参与会签？",
    "合同到期前多久会提醒？",
    "合同变更需要走什么流程？"
  ],
  PURCHASE_ORDER: [
    "这张订单当前进行到哪个环节？",
    "采购订单从提交到入库要经过哪些环节？",
    "部分到货后如何继续入库？",
    "订单逾期未到货如何处理？"
  ]
}

const { proxy } = getCurrentInstance()

const question = ref("")
const asking = ref(false)
const messages = ref([])
const loadingMessages = ref(false)
const messagesRef = ref(null)
const composerRef = ref(null)
const currentSessionId = ref(props.sessionId)
const health = ref(null)
const previewOpen = ref(false)
const previewDocId = ref(undefined)
const previewDocName = ref("")
const atBottom = ref(true)
let abortHandler = null

const healthType = computed(() => {
  if (!health.value) return "info"
  if (!health.value.ragflowConfigured) return "info"
  return health.value.ragflowAvailable ? "success" : "danger"
})

const healthText = computed(() => {
  if (!health.value) return "检测中"
  if (!health.value.ragflowConfigured) return "未配置知识服务"
  if (!health.value.ragflowAvailable) return "知识服务不可用"
  return "知识服务正常"
})

const suggestions = computed(() => SUGGESTIONS[props.contextType] || SUGGESTIONS.DEFAULT)

const canSend = computed(() => !!question.value.trim() && !asking.value)

const streamingIndex = computed(() => (asking.value ? messages.value.length - 1 : -1))

const escapeHtml = text => {
  return String(text || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;")
}

// 仅渲染受控 Markdown：先转义 HTML，再处理代码块、标题、加粗、行内代码、列表与换行
function renderMarkdown(text) {
  if (!text) return ""
  const blocks = []
  let html = escapeHtml(text)
  html = html.replace(/```([\s\S]*?)```/g, (match, code) => {
    blocks.push('<pre class="md-code"><code>' + String(code).replace(/^\n+/, "") + "</code></pre>")
    return "\u0000" + (blocks.length - 1) + "\u0000"
  })
  html = html.replace(/^#{1,6}\s+(.+)$/gm, '<div class="md-heading">$1</div>')
  html = html.replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>")
  html = html.replace(/`([^`]+)`/g, "<code>$1</code>")
  html = html.replace(/^\s*[-*]\s+(.*)$/gm, '<div class="md-li">$1</div>')
  html = html.replace(/^\s*(\d+)[.)]\s+(.*)$/gm, '<div class="md-li md-li-num"><span class="md-num">$1</span>$2</div>')
  html = html.replace(/\n{2,}/g, '<div class="md-gap"></div>')
  html = html.replace(/\n/g, "<br/>")
  html = html.replace(/<br\/>\s*(?=<(?:div|pre) class="md-)/g, "")
  html = html.replace(/(<\/(?:div|pre)>)\s*<br\/>/g, "$1")
  html = html.replace(/\u0000(\d+)\u0000/g, (match, i) => blocks[Number(i)] || "")
  return html
}

function formatTime(value) {
  if (!value) return ""
  const date = typeof value === "number" ? new Date(value) : new Date(String(value).replace(" ", "T"))
  if (Number.isNaN(date.getTime())) return ""
  const hh = String(date.getHours()).padStart(2, "0")
  const mm = String(date.getMinutes()).padStart(2, "0")
  return hh + ":" + mm
}

function handleScroll() {
  const el = messagesRef.value
  if (!el) return
  atBottom.value = el.scrollHeight - el.scrollTop - el.clientHeight < 80
}

function scrollToBottom(force = false) {
  nextTick(() => {
    const el = messagesRef.value
    if (!el) return
    if (force || atBottom.value) {
      el.scrollTop = el.scrollHeight
      atBottom.value = true
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
      createdAt: item.createdTime || item.createTime,
      citations: []
    }))
    loadingMessages.value = false
    scrollToBottom(true)
  }).catch(() => {
    loadingMessages.value = false
  })
}

function handleNewSession() {
  currentSessionId.value = undefined
  emit("update:sessionId", undefined)
  messages.value = []
  question.value = ""
  atBottom.value = true
}

function handleEnter(event) {
  if (event.shiftKey || event.isComposing) return
  event.preventDefault()
  handleAsk()
}

function handleEscape() {
  if (asking.value) {
    handleAbort()
  }
}

function askSuggestion(text) {
  if (asking.value) return
  question.value = text
  handleAsk()
}

function handleAsk() {
  const text = question.value.trim()
  if (!text || asking.value) return
  messages.value.push({ role: "USER", content: text, createdAt: Date.now() })
  const assistantMessage = {
    role: "ASSISTANT",
    content: "",
    citations: [],
    statusText: "",
    statusType: "warn",
    traceId: "",
    createdAt: Date.now()
  }
  messages.value.push(assistantMessage)
  const index = messages.value.length - 1
  question.value = ""
  asking.value = true
  scrollToBottom(true)
  nextTick(() => {
    if (composerRef.value) composerRef.value.focus()
  })

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
        if (!messages.value[index].content) {
          messages.value[index].content = "未在企业知识库中检索到与该问题相关的充分证据，可以补充相关文档，或换一种问法再试。"
        }
        messages.value[index].statusText = "依据不足：知识库中未检索到充分证据"
        messages.value[index].statusType = "warn"
      } else if (status === "RAG_UNAVAILABLE") {
        if (!messages.value[index].content) {
          messages.value[index].content = "知识服务暂时不可用，请稍后重试；ERP 业务操作不受影响。"
        }
        messages.value[index].statusText = "知识服务暂不可用，ERP 业务操作不受影响"
        messages.value[index].statusType = "warn"
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
      messages.value[index].statusType = "error"
    }
  })
}

function handleAbort() {
  if (abortHandler) {
    abortHandler()
    abortHandler = null
  }
  if (asking.value) {
    const last = messages.value[messages.value.length - 1]
    if (last && last.role === "ASSISTANT" && !last.statusText) {
      last.statusText = "已停止生成"
      last.statusType = "info"
    }
  }
  asking.value = false
}

function copyTrace(traceId) {
  proxy.$modal.msgSuccess("追踪号：" + traceId)
}

function copyAnswer(item) {
  const text = item && item.content ? item.content : ""
  if (!text) return
  const done = () => proxy.$modal.msgSuccess("回答已复制")
  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(text).then(done).catch(() => fallbackCopy(text, done))
  } else {
    fallbackCopy(text, done)
  }
}

function fallbackCopy(text, done) {
  const area = document.createElement("textarea")
  area.value = text
  area.setAttribute("readonly", "")
  area.style.position = "fixed"
  area.style.left = "-9999px"
  area.style.top = "0"
  document.body.appendChild(area)
  area.select()
  try {
    document.execCommand("copy")
    done()
  } catch (e) {
    proxy.$modal.msgError("复制失败，请手动选择文本")
  }
  document.body.removeChild(area)
}

function openDocument(citation) {
  if (!citation || !citation.documentId) return
  previewDocId.value = citation.documentId
  previewDocName.value = citation.documentName || ""
  previewOpen.value = true
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
  min-height: 480px;
  height: 100%;
  background: #fff;
}

/* ---------------- 头部 ---------------- */
.assistant-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 20px;
  border-bottom: 1px solid rgba(22, 22, 24, 0.07);
  background: linear-gradient(180deg, #fbfdfd 0%, #ffffff 100%);
}

.assistant-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.brand-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex: none;
  border-radius: 12px;
  color: #fff;
  font-size: 17px;
  background: linear-gradient(135deg, #19ded2 0%, #00a89f 100%);
  box-shadow: 0 8px 18px -10px rgba(0, 190, 180, 0.9);
}

.brand-text {
  min-width: 0;
}

.brand-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #161618;
  line-height: 1.3;
}

.context-tag {
  border-radius: 999px;
}

.brand-sub {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 3px;
  font-size: 12px;
  color: #8a8f96;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.health-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex: none;
  background: #b2b5b7;
}

.health-dot.is-success {
  background: #3ba05c;
  box-shadow: 0 0 0 3px rgba(59, 160, 92, 0.14);
}

.health-dot.is-danger {
  background: #d8494b;
  box-shadow: 0 0 0 3px rgba(216, 73, 75, 0.14);
}

.brand-sep {
  color: #cfd3d6;
}

.new-chat {
  border-radius: 10px;
  border-color: rgba(0, 190, 180, 0.28);
  color: #009d95;
  background: rgba(0, 190, 180, 0.06);
}

.new-chat:hover {
  color: #fff;
  background: #00beb4;
  border-color: #00beb4;
}

/* ---------------- 消息区 ---------------- */
.assistant-body {
  position: relative;
  flex: 1;
  min-height: 0;
  display: flex;
}

.assistant-messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 20px 20px 6px;
  background: linear-gradient(180deg, #fcfdfd 0%, #ffffff 180px);
}

.assistant-messages::-webkit-scrollbar {
  width: 8px;
}

.assistant-messages::-webkit-scrollbar-thumb {
  background-color: #dcdfe3;
  border-radius: 999px;
}

.assistant-messages::-webkit-scrollbar-thumb:hover {
  background-color: #c5cacf;
}

.chat-empty {
  max-width: 560px;
  margin: 6vh auto 0;
  text-align: center;
  padding: 0 8px;
}

.empty-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 18px;
  font-size: 26px;
  color: #00a89f;
  background: rgba(0, 190, 180, 0.1);
  box-shadow: inset 0 0 0 1px rgba(0, 190, 180, 0.16);
}

.empty-title {
  margin: 16px 0 6px;
  font-size: 17px;
  font-weight: 600;
  color: #161618;
}

.empty-desc {
  margin: 0 auto;
  max-width: 440px;
  font-size: 13px;
  line-height: 1.7;
  color: #8a8f96;
}

.empty-hints {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 20px;
}

.hint-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 11px 14px;
  border-radius: 12px;
  border: 1px solid rgba(22, 22, 24, 0.08);
  background: #fff;
  font-size: 13px;
  line-height: 1.5;
  color: #3b3d40;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.hint-card:hover {
  border-color: rgba(0, 190, 180, 0.45);
  box-shadow: 0 10px 22px -18px rgba(0, 190, 180, 0.9);
  transform: translateY(-1px);
  color: #009d95;
}

.hint-arrow {
  flex: none;
  font-size: 13px;
  color: #c0c4cc;
}

.hint-card:hover .hint-arrow {
  color: #00beb4;
}

/* 消息行 */
.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 22px;
  animation: msg-in 0.28s cubic-bezier(0.22, 0.75, 0.28, 1) both;
}

.message-row.is-user {
  flex-direction: row-reverse;
}

.msg-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  flex: none;
  border-radius: 10px;
  font-size: 15px;
  margin-top: 2px;
}

.msg-avatar.is-ai {
  color: #009d95;
  background: rgba(0, 190, 180, 0.12);
  box-shadow: inset 0 0 0 1px rgba(0, 190, 180, 0.18);
}

.msg-avatar.is-user {
  color: #5b6067;
  background: #eef1f3;
}

.msg-main {
  min-width: 0;
  max-width: min(680px, 84%);
  display: flex;
  flex-direction: column;
}

.is-user .msg-main {
  align-items: flex-end;
}

.msg-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 12px;
  color: #a8adb3;
}

.is-user .msg-meta {
  flex-direction: row-reverse;
}

.msg-name {
  font-weight: 600;
  color: #6b7075;
}

.msg-time {
  font-variant-numeric: tabular-nums;
}

.trace {
  border: 0;
  background: rgba(22, 22, 24, 0.04);
  color: #8a8f96;
  font-family: "SFMono-Regular", Consolas, monospace;
  font-size: 11px;
  line-height: 18px;
  padding: 0 7px;
  border-radius: 999px;
  cursor: pointer;
  transition: background-color 0.18s ease, color 0.18s ease;
}

.trace:hover {
  background: rgba(0, 190, 180, 0.12);
  color: #009d95;
}

.bubble {
  position: relative;
  padding: 11px 14px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}

.bubble-ai {
  color: #2b2f34;
  background: #fff;
  border: 1px solid rgba(22, 22, 24, 0.08);
  border-top-left-radius: 6px;
  box-shadow: 0 10px 24px -22px rgba(16, 24, 40, 0.9);
}

.bubble-user {
  color: #fff;
  background: linear-gradient(135deg, #14ccc1 0%, #00a89f 100%);
  border-top-right-radius: 6px;
  box-shadow: 0 12px 24px -18px rgba(0, 190, 180, 0.95);
}

.plain-text {
  white-space: pre-wrap;
}

/* 打字指示 */
.typing {
  display: flex;
  align-items: center;
  gap: 4px;
  min-height: 24px;
  font-size: 12px;
  color: #8a8f96;
}

.typing i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #00beb4;
  animation: typing-bounce 1s infinite ease-in-out;
}

.typing i:nth-child(2) {
  animation-delay: 0.14s;
}

.typing i:nth-child(3) {
  animation-delay: 0.28s;
}

.typing span {
  margin-left: 6px;
}

.streaming {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #a8adb3;
}

.streaming i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #00beb4;
  animation: typing-bounce 1s infinite ease-in-out;
}

/* markdown */
.markdown-body :deep(.md-heading) {
  margin: 10px 0 6px;
  font-weight: 600;
  color: #161618;
}

.markdown-body :deep(.md-li) {
  display: flex;
  gap: 6px;
  margin: 2px 0;
}

.markdown-body :deep(.md-li::before) {
  content: "";
  width: 5px;
  height: 5px;
  margin-top: 8px;
  flex: none;
  border-radius: 50%;
  background: #00beb4;
}

.markdown-body :deep(.md-li-num::before) {
  display: none;
}

.markdown-body :deep(.md-num) {
  color: #00a89f;
  font-weight: 600;
}

.markdown-body :deep(.md-gap) {
  height: 8px;
}

.markdown-body :deep(code) {
  padding: 1px 5px;
  border-radius: 5px;
  background: rgba(0, 190, 180, 0.1);
  color: #00857e;
  font-size: 13px;
}

.markdown-body :deep(.md-code) {
  margin: 8px 0;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f5f7fa;
  overflow-x: auto;
}

.markdown-body :deep(.md-code code) {
  padding: 0;
  background: transparent;
  color: #3b3d40;
}

/* 引用 */
.citations {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed rgba(22, 22, 24, 0.1);
}

.citations-title {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-bottom: 8px;
  font-size: 12px;
  color: #8a8f96;
}

.citations-count {
  padding: 0 6px;
  border-radius: 999px;
  background: rgba(0, 190, 180, 0.12);
  color: #009d95;
  font-size: 11px;
  line-height: 16px;
}

.citation {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  margin: 0 -8px;
  border-radius: 8px;
  font-size: 13px;
  transition: background-color 0.16s ease;
}

.citation:hover {
  background: rgba(0, 190, 180, 0.06);
}

.citation-index {
  flex: none;
  color: #00a89f;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.citation-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #3b3d40;
}

.citation-page {
  flex: none;
  font-size: 12px;
  color: #a8adb3;
}

.citation-link {
  flex: none;
  border: 0;
  background: transparent;
  color: #009d95;
  font-size: 12px;
  cursor: pointer;
  padding: 0 2px;
}

.citation-link:hover {
  text-decoration: underline;
}

/* 状态提示 */
.status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1.6;
}

.status-warn {
  color: #b8770f;
  background: #fff7e6;
}

.status-error {
  color: #c24345;
  background: #fdecec;
}

.status-info {
  color: #6b7075;
  background: #f2f4f5;
}

/* 工具条 */
.msg-tools {
  display: flex;
  gap: 4px;
  margin-top: 4px;
  opacity: 0;
  transition: opacity 0.18s ease;
}

.message-row.is-assistant:hover .msg-tools,
.message-row.is-streaming .msg-tools {
  opacity: 1;
}

.tool {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 0;
  background: transparent;
  color: #a8adb3;
  font-size: 12px;
  padding: 2px 4px;
  cursor: pointer;
}

.tool:hover {
  color: #009d95;
}

/* 回到底部 */
.scroll-bottom {
  position: absolute;
  right: 22px;
  bottom: 14px;
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  border: 1px solid rgba(22, 22, 24, 0.08);
  background: #fff;
  color: #6b7075;
  cursor: pointer;
  box-shadow: 0 10px 24px -14px rgba(16, 24, 40, 0.6);
  transition: color 0.18s ease, border-color 0.18s ease, transform 0.18s ease;
}

.scroll-bottom:hover {
  color: #00a89f;
  border-color: rgba(0, 190, 180, 0.45);
  transform: translateY(-1px);
}

.scroll-btn-enter-active,
.scroll-btn-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.scroll-btn-enter-from,
.scroll-btn-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

/* ---------------- 输入区 ---------------- */
.composer {
  margin: 0 16px 16px;
  padding: 10px 12px 8px;
  border: 1px solid rgba(22, 22, 24, 0.1);
  border-radius: 14px;
  background: #fff;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.composer:focus-within {
  border-color: rgba(0, 190, 180, 0.55);
  box-shadow: 0 0 0 4px rgba(0, 190, 180, 0.12);
}

.composer.is-asking {
  border-color: rgba(0, 190, 180, 0.28);
}

.composer-input :deep(.el-textarea__inner) {
  padding: 4px 2px;
  border: 0;
  box-shadow: none;
  background: transparent;
  font-size: 14px;
  line-height: 1.65;
  color: #2b2f34;
  min-height: 26px;
}

.composer-input :deep(.el-textarea__inner:focus) {
  box-shadow: none;
}

.composer-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 6px;
}

.composer-hint {
  font-size: 11px;
  color: #a8adb3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.composer-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: none;
}

.btn-send {
  height: 32px;
  padding: 0 14px;
  border: 0;
  border-radius: 10px;
  font-weight: 500;
  background: linear-gradient(135deg, #14ccc1 0%, #00a89f 100%);
  box-shadow: 0 10px 20px -14px rgba(0, 190, 180, 0.95);
}

.btn-send:hover,
.btn-send:focus {
  background: linear-gradient(135deg, #19ded2 0%, #00b3a9 100%);
}

.btn-send.is-disabled,
.btn-send.is-disabled:hover {
  background: #dfe4e6;
  box-shadow: none;
}

.btn-stop {
  height: 32px;
  padding: 0 12px;
  border-radius: 10px;
  border-color: rgba(216, 73, 75, 0.35);
  color: #c24345;
  background: rgba(216, 73, 75, 0.06);
}

/* 动效 */
@keyframes msg-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes typing-bounce {
  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.45;
  }
  30% {
    transform: translateY(-4px);
    opacity: 1;
  }
}

@media (hover: none) {
  .msg-tools {
    opacity: 1;
  }
}

@media (max-width: 768px) {
  .assistant-header {
    padding: 12px 14px;
  }

  .assistant-messages {
    padding: 16px 14px 4px;
  }

  .msg-main {
    max-width: 88%;
  }

  .empty-hints {
    grid-template-columns: 1fr;
  }

  .composer {
    margin: 0 10px 10px;
  }

  .composer-hint {
    display: none;
  }

  .brand-sub .brand-sep,
  .brand-sub span:last-child {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .message-row,
  .typing i,
  .streaming i {
    animation: none;
  }
}
</style>
