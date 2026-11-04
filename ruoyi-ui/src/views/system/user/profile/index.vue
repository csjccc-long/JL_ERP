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
        <section class="profile-panel">
          <header class="panel-head">
            <div class="panel-head__text">
              <p class="panel-head__title">基本资料</p>
              <p class="panel-head__sub">维护昵称、联系方式与登录密码</p>
            </div>
            <span class="panel-head__tag">
              <i class="panel-head__dot" />
              账号已启用
            </span>
          </header>

          <div class="panel-body">
            <el-tabs v-model="selectedTab" class="panel-tabs">
              <el-tab-pane label="基本资料" name="userinfo">
                <userInfo :user="state.user" />
              </el-tab-pane>
              <el-tab-pane label="修改密码" name="resetPwd">
                <resetPwd />
              </el-tab-pane>
            </el-tabs>
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
import userInfo from "./userInfo"
import resetPwd from "./resetPwd"
import { getUserProfile } from "@/api/system/user"
import useUserStore from "@/store/modules/user"

const route = useRoute()
const userStore = useUserStore()
const selectedTab = ref("userinfo")
const infoOpen = ref(false)
const state = reactive({
  user: {},
  roleGroup: "",
  postGroup: ""
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
  })
}

onMounted(() => {
  const activeTab = route.params && route.params.activeTab
  if (activeTab) {
    selectedTab.value = activeTab
  }
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

/* ---------- 右侧资料面板：与卡片同款玻璃质感 ---------- */
.profile-panel {
  position: relative;
  border: 1px solid var(--pf-edge);
  border-radius: 24px;
  background: rgba(38, 35, 46, 0.66);
  box-shadow:
    0 22px 44px -20px rgba(0, 0, 0, 0.8),
    0 6px 16px -8px rgba(0, 0, 0, 0.5),
    inset 0 1px 0 rgba(255, 255, 255, 0.07);
  -webkit-backdrop-filter: blur(20px) saturate(1.6);
  backdrop-filter: blur(20px) saturate(1.6);
  overflow: hidden;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 24px 14px;
  border-bottom: 1px solid var(--pf-line);
}

.panel-head__text {
  min-width: 0;
}

.panel-head__title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--pf-ink);
}

.panel-head__sub {
  margin: 3px 0 0;
  font-size: 12.5px;
  line-height: 1.4;
  color: var(--pf-faint);
}

.panel-head__tag {
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

.panel-head__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 8px rgba(34, 197, 94, 0.9);
}

.panel-body {
  padding: 18px 24px 26px;
}

/* ---------- 页签 ---------- */
#app .profile-page .panel-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 22px;
  }

  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }

  :deep(.el-tabs__active-bar) {
    display: none;
  }

  :deep(.el-tabs__item) {
    height: 34px;
    line-height: 34px;
    padding: 0 16px;
    margin-right: 6px;
    border-radius: 9px;
    color: var(--pf-muted);
    font-size: 13.5px;
    font-weight: 500;
    transition: background 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
  }

  :deep(.el-tabs__item:hover) {
    color: var(--pf-ink);
    background: rgba(255, 255, 255, 0.06);
  }

  :deep(.el-tabs__item.is-active) {
    color: var(--pf-accent-glow);
    background: rgba(0, 190, 180, 0.14);
    box-shadow: inset 0 0 0 1px rgba(0, 190, 180, 0.24);
    font-weight: 600;
  }
}

/* ---------- 表单：暗色输入与按钮 ---------- */
#app .profile-page .panel-body {
  :deep(.el-form-item__label) {
    color: rgba(244, 244, 245, 0.62);
  }

  :deep(.el-input__wrapper),
  :deep(.el-textarea__inner) {
    background-color: rgba(255, 255, 255, 0.045);
    box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1) inset;
  }

  :deep(.el-input__wrapper:hover),
  :deep(.el-textarea__inner:hover) {
    box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.2) inset;
  }

  :deep(.el-input__wrapper.is-focus),
  :deep(.el-textarea__inner:focus) {
    box-shadow:
      0 0 0 1px rgba(0, 190, 180, 0.85) inset,
      0 0 0 3px rgba(0, 190, 180, 0.16) !important;
  }

  :deep(.el-input__inner) {
    color: var(--pf-ink);
  }

  :deep(.el-input__inner::placeholder) {
    color: rgba(244, 244, 245, 0.32);
  }

  :deep(.el-input__password),
  :deep(.el-input__suffix-inner) {
    color: var(--pf-muted);
  }

  :deep(.el-radio__label) {
    color: rgba(244, 244, 245, 0.72);
  }

  :deep(.el-radio__inner) {
    border-color: rgba(255, 255, 255, 0.26);
    background: rgba(255, 255, 255, 0.06);
  }

  :deep(.el-radio.is-checked .el-radio__inner) {
    border-color: var(--pf-accent);
    background: var(--pf-accent);
  }

  :deep(.el-radio.is-checked .el-radio__label) {
    color: var(--pf-accent-glow);
  }

  :deep(.el-form-item__error) {
    color: #ff9b9b;
  }

  :deep(.el-button) {
    height: 34px;
    padding: 0 18px;
    border-radius: 10px;
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
    --el-button-hover-bg-color: rgba(239, 68, 68, 0.16);
    --el-button-hover-border-color: rgba(239, 68, 68, 0.36);
    --el-button-hover-text-color: #ffc4c4;
    --el-button-active-bg-color: rgba(239, 68, 68, 0.22);
    --el-button-active-border-color: rgba(239, 68, 68, 0.42);
    --el-button-active-text-color: #ffd7d7;
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
