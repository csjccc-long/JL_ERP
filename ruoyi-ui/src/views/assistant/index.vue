<template>
  <div class="app-container assistant-page">
    <el-row :gutter="16">
      <el-col :xs="24" :sm="7" :md="6">
        <el-card shadow="never" class="session-card">
          <template #header>
            <div class="session-header">
              <span>会话列表</span>
              <el-button link type="primary" icon="Plus" @click="handleCreate">新建</el-button>
            </div>
          </template>
          <div v-loading="loadingSessions" class="session-list">
            <div v-for="item in sessions" :key="item.id" :class="['session-item', item.id === currentSessionId ? 'active' : '']" @click="handleSelect(item)">
              <div class="session-title">{{ item.title || "新会话" }}</div>
              <div class="session-meta">
                <span>{{ item.createTime || "" }}</span>
                <el-icon class="session-delete" @click.stop="handleDelete(item)"><Delete /></el-icon>
              </div>
            </div>
            <el-empty v-if="!loadingSessions && sessions.length === 0" description="暂无会话" :image-size="70" />
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="17" :md="18">
        <el-card shadow="never" class="chat-card">
          <AssistantPanel v-model:session-id="currentSessionId" @created="handleCreated" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="AssistantIndex">
import { listSessions, delSession } from "@/api/knowledge/assistant"
import AssistantPanel from "@/components/AssistantPanel/index.vue"

const { proxy } = getCurrentInstance()

const sessions = ref([])
const loadingSessions = ref(false)
const currentSessionId = ref(undefined)

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
.assistant-page .session-card,
.assistant-page .chat-card {
  height: calc(100vh - 130px);
  overflow: hidden;
}
.session-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.session-list {
  overflow-y: auto;
  max-height: calc(100vh - 210px);
}
.session-item {
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 6px;
  border: 1px solid transparent;
}
.session-item:hover {
  background: #f5f7fa;
}
.session-item.active {
  background: #ecf5ff;
  border-color: #b3d8ff;
}
.session-title {
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.session-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #c0c4cc;
  font-size: 12px;
  margin-top: 4px;
}
.session-delete:hover {
  color: #f56c6c;
}
</style>
