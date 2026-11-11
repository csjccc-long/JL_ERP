<template>
  <div class="app-container assistant-page">
    <div class="chat-shell">
      <aside class="session-pane">
        <div class="session-head">
          <div class="session-head-text">
            <div class="session-head-title">会话记录</div>
            <div class="session-head-sub">{{ sessions.length }} 个会话</div>
          </div>
          <el-button class="session-new" type="primary" size="small" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            <span>新建</span>
          </el-button>
        </div>

        <div class="session-search">
          <el-input v-model="keyword" size="small" clearable placeholder="搜索会话">
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <div v-loading="loadingSessions" class="session-list">
          <template v-for="group in groups" :key="group.key">
            <div class="session-group">{{ group.label }}</div>
            <div
              v-for="item in group.items"
              :key="item.id"
              class="session-item"
              :class="{ active: item.id === currentSessionId }"
              role="button"
              tabindex="0"
              @click="handleSelect(item)"
              @keydown.enter.prevent="handleSelect(item)"
            >
              <span class="session-dot"></span>
              <div class="session-main">
                <div class="session-name">{{ item.title || "新会话" }}</div>
                <div class="session-meta">{{ formatSessionTime(item.createTime) }}</div>
              </div>
              <el-icon class="session-delete" title="删除会话" @click.stop="handleDelete(item)"><Delete /></el-icon>
            </div>
          </template>

          <div v-if="!loadingSessions && sessions.length === 0" class="session-empty">
            <el-icon><ChatLineSquare /></el-icon>
            <p>还没有会话</p>
            <span>点击「新建」开始第一次提问</span>
          </div>
          <div v-else-if="!loadingSessions && keyword && groups.length === 0" class="session-empty session-empty--plain">
            <p>没有匹配的会话</p>
          </div>
        </div>

        <div class="session-foot">会话记录仅本人可见</div>
      </aside>

      <section class="chat-pane">
        <AssistantPanel v-model:session-id="currentSessionId" @created="handleCreated" />
      </section>
    </div>
  </div>
</template>

<script setup name="AssistantIndex">
import { listSessions, delSession } from "@/api/knowledge/assistant"
import AssistantPanel from "@/components/AssistantPanel/index.vue"

const { proxy } = getCurrentInstance()

const sessions = ref([])
const loadingSessions = ref(false)
const keyword = ref("")
const currentSessionId = ref(undefined)

function toDate(value) {
  if (!value) return null
  const date = new Date(String(value).replace(" ", "T"))
  return Number.isNaN(date.getTime()) ? null : date
}

function dayKey(date) {
  if (!date) return ""
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const day = String(date.getDate()).padStart(2, "0")
  return date.getFullYear() + "-" + month + "-" + day
}

function startOfDay(offset) {
  const date = new Date()
  date.setHours(0, 0, 0, 0)
  date.setDate(date.getDate() - offset)
  return date
}

const groups = computed(() => {
  const todayKey = dayKey(startOfDay(0))
  const yesterdayKey = dayKey(startOfDay(1))
  const buckets = { today: [], yesterday: [], earlier: [] }
  const search = keyword.value.trim().toLowerCase()

  sessions.value.forEach(item => {
    const title = item.title || "新会话"
    if (search && !title.toLowerCase().includes(search)) return
    const key = dayKey(toDate(item.createTime))
    if (key === todayKey) {
      buckets.today.push(item)
    } else if (key === yesterdayKey) {
      buckets.yesterday.push(item)
    } else {
      buckets.earlier.push(item)
    }
  })

  return [
    { key: "today", label: "今天", items: buckets.today },
    { key: "yesterday", label: "昨天", items: buckets.yesterday },
    { key: "earlier", label: "更早", items: buckets.earlier }
  ].filter(group => group.items.length > 0)
})

function formatSessionTime(value) {
  const date = toDate(value)
  if (!date) return ""
  const todayKey = dayKey(startOfDay(0))
  const yesterdayKey = dayKey(startOfDay(1))
  const key = dayKey(date)
  if (key === todayKey) {
    return String(date.getHours()).padStart(2, "0") + ":" + String(date.getMinutes()).padStart(2, "0")
  }
  if (key === yesterdayKey) return "昨天"
  return String(date.getMonth() + 1).padStart(2, "0") + "-" + String(date.getDate()).padStart(2, "0")
}

function loadSessions() {
  loadingSessions.value = true
  listSessions().then(response => {
    sessions.value = response.data || []
    loadingSessions.value = false
  }).catch(() => {
    loadingSessions.value = false
  })
}

function handleSelect(item) {
  currentSessionId.value = item.id
}

function handleCreate() {
  currentSessionId.value = undefined
}

function handleCreated() {
  loadSessions()
}

function handleDelete(item) {
  proxy.$modal.confirm('是否确认删除会话"' + (item.title || "新会话") + '"？').then(() => {
    return delSession(item.id)
  }).then(() => {
    if (currentSessionId.value === item.id) {
      currentSessionId.value = undefined
    }
    loadSessions()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

onMounted(() => {
  loadSessions()
})
</script>

<style scoped>
.assistant-page {
  padding-bottom: 0;
}

.chat-shell {
  display: flex;
  height: calc(100vh - 130px);
  min-height: 560px;
  overflow: hidden;
  border-radius: 14px;
  border: 1px solid rgba(22, 22, 24, 0.07);
  background: #fff;
  box-shadow: 0 18px 44px -34px rgba(16, 24, 40, 0.55);
}

/* ---------------- 会话栏 ---------------- */
.session-pane {
  width: 268px;
  flex: none;
  display: flex;
  flex-direction: column;
  min-height: 0;
  border-right: 1px solid rgba(22, 22, 24, 0.07);
  background: linear-gradient(180deg, #fbfcfc 0%, #f6f8f8 100%);
}

.session-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 16px 16px 10px;
}

.session-head-title {
  font-size: 14px;
  font-weight: 600;
  color: #161618;
  line-height: 1.3;
}

.session-head-sub {
  margin-top: 3px;
  font-size: 12px;
  color: #a8adb3;
}

.session-new {
  height: 30px;
  padding: 0 12px;
  border: 0;
  border-radius: 9px;
  font-weight: 500;
  background: linear-gradient(135deg, #14ccc1 0%, #00a89f 100%);
  box-shadow: 0 10px 18px -14px rgba(0, 190, 180, 0.95);
}

.session-new:hover,
.session-new:focus {
  background: linear-gradient(135deg, #19ded2 0%, #00b3a9 100%);
}

.session-search {
  padding: 0 16px 10px;
}

.session-search :deep(.el-input__wrapper) {
  border-radius: 9px;
  background: #fff;
  box-shadow: 0 0 0 1px rgba(22, 22, 24, 0.08) inset;
}

.session-search :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px rgba(0, 190, 180, 0.55) inset;
}

.session-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 0 10px 10px;
}

.session-list::-webkit-scrollbar {
  width: 6px;
}

.session-list::-webkit-scrollbar-thumb {
  background-color: #dcdfe3;
  border-radius: 999px;
}

.session-group {
  padding: 10px 6px 6px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: #b3b8bd;
}

.session-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
  margin-bottom: 2px;
  border-radius: 10px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: background-color 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.session-item:hover {
  background: rgba(255, 255, 255, 0.9);
  border-color: rgba(22, 22, 24, 0.06);
}

.session-item:focus-visible {
  outline: 2px solid rgba(0, 190, 180, 0.55);
  outline-offset: 1px;
}

.session-item.active {
  background: #fff;
  border-color: rgba(0, 190, 180, 0.28);
  box-shadow: 0 10px 22px -22px rgba(0, 190, 180, 0.9);
}

.session-item.active::before {
  content: "";
  position: absolute;
  left: -4px;
  top: 12px;
  bottom: 12px;
  width: 3px;
  border-radius: 3px;
  background: linear-gradient(180deg, #19ded2, #00a89f);
}

.session-dot {
  width: 6px;
  height: 6px;
  flex: none;
  border-radius: 50%;
  background: #d5d9dc;
  transition: background-color 0.18s ease, box-shadow 0.18s ease;
}

.session-item.active .session-dot {
  background: #00beb4;
  box-shadow: 0 0 0 3px rgba(0, 190, 180, 0.14);
}

.session-main {
  min-width: 0;
  flex: 1;
}

.session-name {
  font-size: 13px;
  line-height: 1.5;
  color: #3b3d40;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-item.active .session-name {
  color: #161618;
  font-weight: 500;
}

.session-meta {
  margin-top: 1px;
  font-size: 11px;
  color: #a8adb3;
}

.session-delete {
  flex: none;
  font-size: 13px;
  color: #b3b8bd;
  opacity: 0;
  transition: opacity 0.18s ease, color 0.18s ease;
}

.session-item:hover .session-delete,
.session-item.active .session-delete {
  opacity: 1;
}

.session-delete:hover {
  color: #d8494b;
}

.session-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 40px 12px;
  text-align: center;
  color: #a8adb3;
}

.session-empty .el-icon {
  font-size: 26px;
  color: #cfd3d6;
}

.session-empty p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #6b7075;
}

.session-empty span {
  font-size: 12px;
}

.session-empty--plain {
  padding: 26px 12px;
}

.session-foot {
  padding: 10px 16px;
  border-top: 1px solid rgba(22, 22, 24, 0.06);
  font-size: 11px;
  color: #b3b8bd;
}

/* ---------------- 对话区 ---------------- */
.chat-pane {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chat-pane :deep(.assistant-panel) {
  flex: 1;
  min-height: 0;
}

@media (hover: none) {
  .session-delete {
    opacity: 1;
  }
}

@media (max-width: 991px) {
  .chat-shell {
    flex-direction: column;
    height: auto;
    min-height: 0;
  }

  .session-pane {
    width: auto;
    max-height: 240px;
    border-right: 0;
    border-bottom: 1px solid rgba(22, 22, 24, 0.07);
  }

  .session-list {
    padding-bottom: 6px;
  }

  .session-foot {
    display: none;
  }

  .chat-pane {
    height: calc(100vh - 200px);
    min-height: 520px;
  }
}
</style>
