<template>
  <div class="app-container profile-page">
    <el-row :gutter="18">
      <el-col :span="6" :md="8" :sm="10" :xs="24">
        <profileCard
          class="profile-page__card"
          :name="state.user.nickName || state.user.userName"
          :title="heroTitle"
          :handle="state.user.userName"
          :status="state.roleGroup"
          :mini-avatar-url="userStore.avatar"
          contact-text="个人信息"
          @contact="infoOpen = true"
        >
          <userAvatar />
        </profileCard>
      </el-col>

      <el-col :span="18" :md="16" :sm="14" :xs="24">
        <section class="profile-work" :class="{ 'is-open': sheetOpen }">
          <nav class="work-rail" role="tablist" aria-orientation="vertical" aria-label="账号设置">
            <button
              v-for="item in modes"
              :key="item.name"
              type="button"
              role="tab"
              class="work-switch__btn"
              :class="{ 'is-active': mode === item.name && sheetOpen }"
              :aria-selected="mode === item.name && sheetOpen"
              @click="toggleMode(item.name)"
            >
              {{ item.label }}
            </button>
            <span class="work-tag">
              <i class="work-tag__dot" />
              账号已启用
            </span>
          </nav>

          <div class="work-main">
            <div class="stage-card">
              <badgeCard
                :palette="palette"
                :name="draft.nickName"
                :phone="draft.phonenumber"
                :email="draft.email"
                :sex="draft.sex"
                :flipped="mode === 'resetPwd'"
                :active-field="activeField"
                :secret-label="secretLabel"
                :secret-length="secretLength"
                :interactive="!sheetOpen"
                :sheet-open="sheetOpen"
                @toggle-sheet="toggleSheet"
              />
              <transition name="hint">
                <p v-if="!sheetOpen" class="stage-hint">按住拖动可任意旋转 · 轻点翻面 · 点顶部卡槽展开资料</p>
              </transition>
            </div>

            <div class="stage-sheet" :class="{ 'is-open': sheetOpen }">
              <div class="sheet-clip">
                <div class="sheet-body">
                  <userInfo
                    v-if="mode === 'userinfo'"
                    v-model:palette="palette"
                    :user="state.user"
                    @active-field="activeField = $event"
                    @draft="draft = $event"
                    @close="closeSheet"
                  />
                  <resetPwd v-else-if="mode === 'resetPwd'" @active-field="activeField = $event" @secret="onSecret" @close="closeSheet" />
                </div>
              </div>
            </div>
          </div>
        </section>
      </el-col>
    </el-row>

    <el-dialog
      v-model="infoOpen"
      title="个人信息"
      width="460px"
      append-to-body
      class="profile-info-dialog"
    >
      <ul class="info-list">
        <li v-for="row in infoRows" :key="row.label" class="info-row">
          <span class="info-row__icon">
            <svg-icon :icon-class="row.icon" />
          </span>
          <span class="info-row__label">{{ row.label }}</span>
          <span class="info-row__value" :title="row.value">{{ row.value || "—" }}</span>
        </li>
      </ul>
      <template #footer>
        <el-button type="primary" @click="infoOpen = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Profile">
import profileCard from "./profileCard"
import userAvatar from "./userAvatar"
import badgeCard from "./badge/badgeCard"
import userInfo from "./userInfo"
import resetPwd from "./resetPwd"
import { DEFAULT_PALETTE, paletteOf } from "./badge/badgeArt"
import { getUserProfile } from "@/api/system/user"
import useUserStore from "@/store/modules/user"

const route = useRoute()
const userStore = useUserStore()
const infoOpen = ref(false)

/** 基本资料 / 修改密码——同一张工卡，翻面切换 */
const modes = [
  { name: "userinfo", label: "基本资料" },
  { name: "resetPwd", label: "修改密码" }
]
/** 默认不展开任何表单：页面上只有一张可以随手转着看的工卡 */
const mode = ref(null)
/** 表单卡片从工卡下面滑出来的状态 */
const sheetOpen = ref(false)
let sheetTimer = 0

const openSheet = name => {
  window.clearTimeout(sheetTimer)
  if (mode.value !== name) {
    if (name !== "resetPwd") {
      secretLabel.value = "新密码"
      secretLength.value = 0
    }
    activeField.value = ""
    mode.value = name
  }
  nextTick(() => {
    sheetOpen.value = true
  })
}

const closeSheet = () => {
  window.clearTimeout(sheetTimer)
  sheetOpen.value = false
  activeField.value = ""
  /* 等卡片落回、面板收完再把表单卸掉，动画期间内容要一直在 */
  sheetTimer = window.setTimeout(() => {
    mode.value = null
  }, 520)
}

/** 卡片上的展开/收起：没展开时按当前页签（默认基本资料）拉出表单 */
const toggleSheet = () => {
  if (sheetOpen.value) {
    closeSheet()
    return
  }
  openSheet(mode.value || "userinfo")
}

const toggleMode = name => {
  if (mode.value === name && sheetOpen.value) {
    closeSheet()
    return
  }
  openSheet(name)
}

onBeforeUnmount(() => window.clearTimeout(sheetTimer))

/** 卡片上弹的提示框跟着哪个字段 */
const activeField = ref("")
/** 卡背圆点：记当前输入的是哪个密码、多少位，永远不记内容 */
const secretLabel = ref("新密码")
const secretLength = ref(0)

const onSecret = payload => {
  if (!payload) return
  secretLabel.value = payload.label || "新密码"
  secretLength.value = Number(payload.length) || 0
}

const state = reactive({
  user: {},
  roleGroup: "",
  postGroup: ""
})

/** 卡片实时预览的数据，跟着表单走 */
const draft = ref({ nickName: "", phonenumber: "", email: "", sex: "" })

const PALETTE_KEY = "erp.profile.badge.palette"
const palette = ref(DEFAULT_PALETTE)

try {
  const saved = window.localStorage.getItem(PALETTE_KEY)
  if (saved) palette.value = paletteOf(saved).key
} catch (e) {
  palette.value = DEFAULT_PALETTE
}

watch(palette, value => {
  try {
    window.localStorage.setItem(PALETTE_KEY, value)
  } catch (e) {
    // 隐私模式下写不进去，不影响当前会话
  }
})

const heroTitle = computed(() => {
  const dept = state.user.dept && state.user.dept.deptName
  return [dept, state.postGroup].filter(Boolean).join(" · ")
})

const infoRows = computed(() => {
  const user = state.user || {}
  return [
    { icon: "user", label: "用户名称", value: user.userName },
    { icon: "phone", label: "手机号码", value: user.phonenumber },
    { icon: "email", label: "用户邮箱", value: user.email },
    {
      icon: "tree",
      label: "所属部门",
      value: user.dept ? user.dept.deptName + " / " + state.postGroup : state.postGroup
    },
    { icon: "peoples", label: "所属角色", value: state.roleGroup },
    { icon: "date", label: "创建日期", value: user.createTime }
  ]
})

function getUser() {
  getUserProfile().then(response => {
    state.user = response.data
    state.roleGroup = response.roleGroup
    state.postGroup = response.postGroup
    draft.value = {
      nickName: state.user.nickName,
      phonenumber: state.user.phonenumber,
      email: state.user.email,
      sex: state.user.sex
    }
  })
}

onMounted(() => {
  const activeTab = route.params && route.params.activeTab
  if (activeTab === "resetPwd") openSheet("resetPwd")
  getUser()
})
</script>

<style lang="scss" scoped>
/* ---------- 页面：与卡片一体的暗色场景 ---------- */
#app .app-container.profile-page {
  --pf-ink: #f4f4f5;
  --pf-muted: rgba(244, 244, 245, 0.55);
  --pf-faint: rgba(244, 244, 245, 0.38);
  --pf-edge: rgba(255, 255, 255, 0.08);
  --pf-line: rgba(255, 255, 255, 0.07);
  --pf-accent: #00beb4;
  --pf-accent-glow: #7ef3ea;

  margin: 0;
  padding: 24px 24px 28px;
  min-height: calc(100vh - 84px);
  color: var(--pf-ink);
  background:
    radial-gradient(110% 70% at 4% -16%, rgba(0, 190, 180, 0.18) 0%, transparent 58%),
    radial-gradient(85% 65% at 98% -6%, rgba(124, 140, 255, 0.2) 0%, transparent 62%),
    radial-gradient(70% 60% at 60% 110%, rgba(255, 106, 193, 0.1) 0%, transparent 60%),
    linear-gradient(168deg, #1a1823 0%, #100e16 56%, #15131c 100%);
}

.profile-page__card {
  margin-bottom: 18px;
}

/* ---------- 右侧：不再套整块面板，卡片与表单各自独立 ---------- */
.profile-work {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  gap: 18px;
  /* 只有右侧这张卡跟着视口上下居中，左边的人事卡保持贴顶 */
  min-height: calc(100vh - 152px);
}

/* 左侧竖排的页签 + 账号状态 */
.work-rail {
  flex: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 104px;
  padding-top: 6px;
  position: sticky;
  top: 16px;
}

.work-switch__btn {
  width: 100%;
  padding: 11px 12px;
  border: 1px solid var(--pf-edge);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.035);
  color: var(--pf-muted);
  font: inherit;
  font-size: 13px;
  font-weight: 500;
  text-align: left;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.work-switch__btn:hover {
  color: var(--pf-ink);
  background: rgba(255, 255, 255, 0.07);
  transform: translateX(2px);
}

.work-switch__btn.is-active {
  color: var(--pf-accent-glow);
  background: rgba(0, 190, 180, 0.16);
  box-shadow: inset 0 0 0 1px rgba(0, 190, 180, 0.34);
  font-weight: 600;
}

/* 卡片、表单共用右侧这一列，宽度一致 */
.work-rail .work-tag {
  align-self: flex-start;
  margin-top: 2px;
  padding: 4px 9px;
  font-size: 11px;
}

.work-main {
  flex: 1;
  align-self: center;
  min-width: 0;
  max-width: 560px;
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* ---------- 工卡：默认就一张卡立在中间，可以随手转着看 ---------- */
.stage-card {
  position: relative;
  z-index: 2;
  width: 100%;
  transition: transform 0.62s cubic-bezier(0.2, 0.78, 0.2, 1);
}

/* 点开表单时卡片整体上浮，让出下面的空间 */
.profile-work.is-open .stage-card {
  transform: translateY(-18px);
}

.stage-hint {
  margin: 16px 0 0;
  color: rgba(244, 244, 245, 0.32);
  font-size: 12px;
  letter-spacing: 0.05em;
  text-align: center;
  pointer-events: none;
}

.hint-enter-active,
.hint-leave-active {
  transition: opacity 0.24s ease;
}

.hint-enter-from,
.hint-leave-to {
  opacity: 0;
}

/* ---------- 输入卡片：从工卡下面滑出来 ---------- */
.stage-sheet {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-rows: 0fr;
  margin-top: -20px;
  transition:
    grid-template-rows 0.58s cubic-bezier(0.2, 0.78, 0.2, 1),
    margin-top 0.58s cubic-bezier(0.2, 0.78, 0.2, 1);
}

.stage-sheet.is-open {
  grid-template-rows: 1fr;
  margin-top: 12px;
}

.sheet-clip {
  overflow: hidden;
}

.sheet-body {
  opacity: 0;
  transform: translateY(-24px) scaleY(0.97);
  transform-origin: 50% 0;
  transition:
    opacity 0.36s ease,
    transform 0.58s cubic-bezier(0.2, 0.78, 0.2, 1);
}

.stage-sheet.is-open .sheet-body {
  opacity: 1;
  transform: none;
}

.work-main > * {
  width: 100%;
}

@media (max-width: 1100px) {
  .profile-work {
    flex-direction: column;
    gap: 14px;
  }

  .work-rail {
    position: static;
    flex-direction: row;
    align-items: center;
    width: 100%;
    padding-top: 0;
  }

  .work-switch__btn {
    width: auto;
    padding: 9px 16px;
    border-radius: 999px;
    text-align: center;
  }

  .work-switch__btn:hover {
    transform: none;
  }

  .work-tag {
    margin-left: auto;
  }

  .work-main {
    max-width: 560px;
    width: 100%;
    margin: 0 auto;
  }
}

.work-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex: none;
  padding: 4px 11px;
  border: 1px solid rgba(34, 197, 94, 0.24);
  border-radius: 999px;
  background: rgba(34, 197, 94, 0.12);
  color: #7ee2a8;
  font-size: 12px;
  font-weight: 600;
}

.work-tag__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 8px rgba(34, 197, 94, 0.9);
}

/* ---------- 表单皮肤：两块表单共用一套下划线样式 ---------- */
#app .profile-page .profile-work {
  :deep(.el-form-item__label) {
    padding-bottom: 2px;
    font-size: 10.5px;
    font-weight: 600;
    letter-spacing: 0.16em;
    line-height: 1.4;
    text-transform: uppercase;
    color: rgba(244, 244, 245, 0.42);
  }

  :deep(.el-input__wrapper) {
    height: 42px;
    padding: 0;
    border-radius: 0 !important;
    background: transparent !important;
    box-shadow: none !important;
    border-bottom: 1px solid rgba(255, 255, 255, 0.14);
    transition: border-color 0.22s ease;
  }

  :deep(.el-input__wrapper:hover) {
    border-bottom-color: rgba(255, 255, 255, 0.28);
    box-shadow: none !important;
  }

  :deep(.el-input__wrapper.is-focus) {
    border-bottom-color: #00beb4;
    box-shadow: 0 1px 0 0 #00beb4 !important;
  }

  :deep(.el-input__inner) {
    height: 40px;
    font-size: 14.5px;
    color: #f4f4f5;
  }

  :deep(.el-input__inner::placeholder) {
    color: rgba(244, 244, 245, 0.24);
  }

  :deep(.el-input__password),
  :deep(.el-input__suffix-inner) {
    color: rgba(244, 244, 245, 0.55);
  }

  /* 错误提示参与排版，避免压到下一个字段的标签 */
  :deep(.el-form-item__error) {
    position: static;
    padding-top: 5px;
    font-size: 11.5px;
    line-height: 1.4;
    color: #ff8a80;
  }

  :deep(.el-button) {
    height: 42px;
    padding: 0 26px;
    border-radius: 11px;
    font-weight: 600;
    transition: filter 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
  }

  :deep(.el-button--primary) {
    --el-button-bg-color: #00c9bd;
    --el-button-border-color: #00c9bd;
    --el-button-text-color: #04211f;
    --el-button-hover-bg-color: #1adccf;
    --el-button-hover-border-color: #1adccf;
    --el-button-hover-text-color: #04211f;
    --el-button-active-bg-color: #00b0a6;
    --el-button-active-border-color: #00b0a6;
    --el-button-active-text-color: #04211f;
    box-shadow: 0 8px 18px -10px rgba(0, 201, 189, 0.9);
  }

  :deep(.el-button--danger) {
    --el-button-bg-color: rgba(255, 255, 255, 0.05);
    --el-button-border-color: rgba(255, 255, 255, 0.14);
    --el-button-text-color: #f6a5a5;
  }
}

/* ---------- 个人信息弹窗内容 ---------- */
.info-list {
  display: flex;
  flex-direction: column;
  margin: 0;
  padding: 0;
  list-style: none;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 4px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);

  &:last-child {
    border-bottom: none;
  }
}

.info-row__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: rgba(0, 190, 180, 0.14);
  color: #4fe0d4;
  font-size: 14px;
}

.info-row__label {
  flex: none;
  color: rgba(244, 244, 245, 0.55);
  font-size: 13px;
}

.info-row__value {
  flex: 1;
  overflow: hidden;
  color: #f4f4f5;
  font-size: 13.5px;
  font-weight: 600;
  text-align: right;
  white-space: nowrap;
  text-overflow: ellipsis;
}

@media (max-width: 992px) {
  #app .app-container.profile-page {
    margin: 0;
    padding: 16px 14px 20px;
  }
}
</style>

<!-- 弹窗被 teleport 到 body，样式需脱离 scoped 作用域 -->
<style lang="scss">
.profile-info-dialog.el-dialog,
.profile-info-dialog .el-dialog {
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 18px;
  background: #1b1922;
  box-shadow:
    0 30px 70px -24px rgba(0, 0, 0, 0.9),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
  overflow: hidden;

  .el-dialog__header {
    padding: 18px 22px 14px;
    margin-right: 0;
    border-bottom: 1px solid rgba(255, 255, 255, 0.07);
  }

  .el-dialog__title {
    font-size: 15px;
    font-weight: 600;
    color: #f4f4f5;
  }

  .el-dialog__headerbtn .el-dialog__close {
    color: rgba(244, 244, 245, 0.5);
  }

  .el-dialog__headerbtn:hover .el-dialog__close {
    color: #f4f4f5;
  }

  .el-dialog__body {
    padding: 8px 22px 6px;
  }

  .el-dialog__footer {
    padding: 12px 22px 18px;
  }

  .el-button--primary {
    --el-button-bg-color: #00c9bd;
    --el-button-border-color: #00c9bd;
    --el-button-text-color: #04211f;
    --el-button-hover-bg-color: #1adccf;
    --el-button-hover-border-color: #1adccf;
    --el-button-hover-text-color: #04211f;
    --el-button-active-bg-color: #00b0a6;
    --el-button-active-border-color: #00b0a6;
    --el-button-active-text-color: #04211f;
    height: 34px;
    padding: 0 18px;
    border-radius: 10px;
    font-weight: 600;
  }
}
</style>
