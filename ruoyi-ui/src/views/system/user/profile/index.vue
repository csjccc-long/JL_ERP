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
            <section class="stage-deck" aria-label="我的卡片">
              <header class="stage-deck__head">
                <div class="stage-deck__intro">
                  <p class="stage-deck__eyebrow">{{ deckHeader.eyebrow }}</p>
                  <h3 class="stage-deck__title">{{ deckHeader.title }}</h3>
                </div>
                <div class="stage-deck__side">
                  <span class="stage-deck__spec">{{ deckHeader.spec }}</span>
                  <button v-if="focusedKey && !sheetOpen" type="button" class="deck-back" @click="resetDeck">
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <rect x="8" y="3" width="12" height="9" rx="2" />
                      <rect x="5" y="7" width="12" height="9" rx="2" />
                      <rect x="2" y="11" width="12" height="9" rx="2" />
                    </svg>
                    <span>返回堆叠</span>
                  </button>
                </div>
              </header>

              <CardDeck :active="focusedKey" :order="deckOrder" @select="focusDeckCard" @promote="promoteDeckCard">
                <template #badge>
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
                    :interactive="focusedKey === 'badge' && !sheetOpen"
                    :sheet-open="sheetOpen"
                    @toggle-sheet="toggleSheet"
                  />
                </template>
                <template #dept>
                  <DeptPass v-if="state.user.dept" :dept="deptCard" />
                </template>
                <template #role>
                  <RolePass v-if="primaryRole" :role="primaryRole" :index="0" :total="roleCards.length" />
                </template>
              </CardDeck>

              <p class="stage-deck__hint">{{ deckHint }}</p>

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
            </section>
          </div>
        </section>
      </el-col>
    </el-row>

    <!-- 打卡足迹：卡片堆下方的留白处 -->
    <checkin-panel />

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
import DeptPass from "./badge/DeptPass.vue"
import RolePass from "./badge/RolePass.vue"
import CardDeck from "./badge/CardDeck.vue"
import userInfo from "./userInfo"
import resetPwd from "./resetPwd"
import { DEFAULT_PALETTE, paletteOf } from "./badge/badgeArt"
import CheckinPanel from "./checkin/CheckinPanel.vue"
import { getUserProfile } from "@/api/system/user"
import { getDept } from "@/api/system/dept"
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

/** 卡片堆：空字符串表示堆叠，否则是展开查看的卡片 key */
const focusedKey = ref("")
const deckOrder = ref(["badge"])
/** 正在按 CardSwap 的 goTo 节奏逐张前移，期间不再响应新的点击 */
const promoting = ref(false)
let promoteTimer = 0

const stopPromote = () => {
  window.clearTimeout(promoteTimer)
  promoteTimer = 0
  promoting.value = false
}

const openSheet = name => {
  // 打开表单时把员工工卡翻到最前面，表单始终接在它下面
  stopPromote()
  focusedKey.value = "badge"
  deckOrder.value = ["badge", ...deckOrder.value.filter(key => key !== "badge")]
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

/** 从堆叠里选一张：它回到正常居中的状态，其余卡片收进后面 */
const focusDeckCard = key => {
  if (!key || focusedKey.value === key) return
  stopPromote()
  if (key !== "badge") closeSheet()
  focusedKey.value = key
  deckOrder.value = [key, ...deckOrder.value.filter(item => item !== key)]
}

/** 点击后方卡片：按 CardSwap 的 goTo 节奏逐张前移，点最前面的那张才进入单卡查看 */
const promoteDeckCard = key => {
  if (!key || focusedKey.value || promoting.value) return
  const position = deckOrder.value.indexOf(key)
  if (position <= 0) return
  promoting.value = true
  let remaining = position
  const step = () => {
    deckOrder.value = [...deckOrder.value.slice(1), deckOrder.value[0]]
    remaining -= 1
    if (remaining > 0) {
      promoteTimer = window.setTimeout(step, 140)
    } else {
      promoteTimer = window.setTimeout(() => {
        promoting.value = false
      }, 650)
    }
  }
  step()
}

/** 返回堆叠；当前选中的卡仍保留在最前面 */
const resetDeck = () => {
  stopPromote()
  focusedKey.value = ""
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

onBeforeUnmount(() => {
  window.clearTimeout(sheetTimer)
  stopPromote()
})

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

/** 当前登录用户自己的部门卡：只取 state.user.dept，不再列全公司部门 */
const deptParentName = ref("")
let deptParentToken = 0
const canReadDept = computed(() => {
  const permissions = userStore.permissions || []
  return permissions.includes("*:*:*") || permissions.includes("system:dept:list")
})
const deptCard = computed(() => ({
  ...(state.user.dept || {}),
  parentName: deptParentName.value || "—"
}))

/** 当前登录用户的角色卡：优先用 profile 接口返回的 roles 数组 */
const roleCards = computed(() => {
  const roles = Array.isArray(state.user.roles)
    ? state.user.roles.filter(role => role && (role.roleName || role.roleKey))
    : []
  if (roles.length) return roles
  return String(state.roleGroup || "")
    .split(",")
    .map(name => name.trim())
    .filter(Boolean)
    .map((name, index) => ({ roleId: 9000 + index, roleName: name, roleKey: "", status: "0" }))
})

/** 卡片堆里只放当前账号自己的角色；多角色时先展示第一个，标题里保留总数 */
const primaryRole = computed(() => roleCards.value[0] || null)

/** 当前可用的卡片 key：员工工卡始终有，部门和角色按数据出现 */
const deckCards = computed(() => {
  const cards = ["badge"]
  if (state.user.dept) cards.push("dept")
  if (roleCards.value.length) cards.push("role")
  return cards
})

watch(
  deckCards,
  cards => {
    deckOrder.value = deckOrder.value.filter(key => cards.includes(key))
    cards.forEach(key => {
      if (!deckOrder.value.includes(key)) deckOrder.value.push(key)
    })
    if (focusedKey.value && !cards.includes(focusedKey.value)) focusedKey.value = ""
  },
  { immediate: true }
)

const deckHeader = computed(() => {
  if (focusedKey.value === "dept") {
    return { eyebrow: "MY DEPARTMENT", title: "我的部门", spec: "85.6 × 53.98 mm" }
  }
  if (focusedKey.value === "role") {
    return {
      eyebrow: "MY ROLE",
      title: "我的角色",
      spec: `${roleCards.value.length} 张角色卡 · 85.6 × 53.98 mm`
    }
  }
  if (focusedKey.value === "badge") {
    return { eyebrow: "EMPLOYEE BADGE", title: "员工工卡", spec: "85.6 × 53.98 mm" }
  }
  return { eyebrow: "MY PASSES", title: "我的卡片", spec: `${deckCards.value.length} 张 · 85.6 × 53.98 mm` }
})

const deckHint = computed(() => {
  if (sheetOpen.value) {
    if (mode.value === "resetPwd") return "密码只在输入框中显示，不会写在卡片上"
    return "编辑资料时，左侧工卡会实时预览"
  }
  if (!focusedKey.value) return "移动指针感受景深 · 点击后方卡片换到最前 · 点击最前卡片查看"
  if (focusedKey.value === "badge") return "按住拖动可任意旋转 · 轻点翻面 · 点顶部卡槽展开资料"
  return "点击右上角「返回堆叠」回到卡片堆"
})

/** 部门详情接口本身没有上级名称；有权限时补一次，没权限就留一条破折号 */
watch(
  [() => state.user.dept, canReadDept],
  async ([dept]) => {
    const token = ++deptParentToken
    deptParentName.value = (dept && dept.parentName) || ""
    if (!dept || dept.parentName || !dept.parentId || !canReadDept.value) return
    try {
      const response = await getDept(dept.parentId)
      if (token === deptParentToken) deptParentName.value = (response.data && response.data.deptName) || ""
    } catch (e) {
      if (token === deptParentToken) deptParentName.value = ""
    }
  },
  { immediate: true }
)

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
  /* 右侧整组卡片（含标题、堆叠、提示）靠顶部，与左侧人事卡齐平 */
  min-height: clamp(420px, calc(100vh - 136px), 1040px);
}

/* 表单展开时仍保持一屏：页签移到上方，卡片在左、表单在右 */
.profile-work.is-open {
  flex-direction: column;
  align-items: stretch;
  justify-content: flex-start;
  gap: 14px;
}

.profile-work.is-open .work-rail {
  flex-direction: row;
  align-items: center;
  width: 100%;
  padding-top: 0;
  position: static;
}

.profile-work.is-open .work-switch__btn {
  width: auto;
  padding: 9px 16px;
  border-radius: 999px;
  text-align: center;
}

.profile-work.is-open .work-switch__btn:hover {
  transform: none;
}

.profile-work.is-open .work-rail .work-tag {
  align-self: center;
  margin-top: 0;
  margin-left: auto;
}

.profile-work.is-open .work-main {
  flex: none;
  width: 100%;
  max-width: 880px;
  margin: 0 auto;
  align-self: stretch;
}

/* 左侧竖排的页签 + 账号状态 */
.work-rail {
  flex: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 104px;
  padding-top: 6px;
  /* 跟着页面一起滚，不吸顶 */
  align-self: flex-start;
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
  align-self: flex-start;
  min-width: 0;
  max-width: 560px;
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* ---------- 卡片堆：工卡 / 部门 / 角色三张叠在一起，指针带一点景深 ---------- */
.stage-deck {
  position: relative;
  z-index: 2;
  width: 100%;
}

.stage-deck__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

.stage-deck__eyebrow {
  margin: 0 0 4px;
  color: var(--pf-faint);
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.22em;
}

.stage-deck__title {
  margin: 0;
  color: var(--pf-ink);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.stage-deck__side {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stage-deck__spec {
  color: var(--pf-faint);
  font-size: 10px;
  letter-spacing: 0.08em;
  white-space: nowrap;
}

.deck-back {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 34px;
  padding: 0 14px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.05);
  color: var(--pf-ink);
  font: inherit;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.02em;
  cursor: pointer;
  backdrop-filter: blur(10px);
  transition: border-color 0.2s ease, background 0.2s ease, color 0.2s ease;
}

.deck-back svg {
  width: 14px;
  height: 14px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.5;
}

.deck-back:hover {
  border-color: rgba(0, 190, 180, 0.5);
  background: rgba(0, 190, 180, 0.12);
  color: #8ff5ef;
}

.stage-deck__hint {
  margin: 16px 0 0;
  color: rgba(244, 244, 245, 0.32);
  font-size: 12px;
  letter-spacing: 0.05em;
  text-align: center;
  pointer-events: none;
}

@media (max-height: 820px) and (min-width: 901px) {
  .stage-deck__head {
    margin-bottom: 10px;
  }

  .stage-deck__hint {
    margin-top: 8px;
  }
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

/* 编辑态：左边“标题 + 卡片 + 提示”，右边表单，互不叠加、不增高页面 */
.profile-work.is-open .stage-deck {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 0.9fr);
  grid-template-rows: auto auto auto;
  grid-template-areas:
    "head sheet"
    "deck sheet"
    "hint sheet";
  align-content: start;
  align-items: start;
  column-gap: clamp(16px, 1.8vw, 26px);
  row-gap: 10px;
}

.profile-work.is-open .stage-deck__head {
  grid-area: head;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  margin-bottom: 0;
}

.profile-work.is-open .stage-deck__side {
  width: 100%;
  justify-content: flex-start;
  gap: 8px;
}

.profile-work.is-open .stage-deck__spec {
  white-space: normal;
}

.profile-work.is-open .card-deck {
  grid-area: deck;
  container-type: inline-size;
}

.profile-work.is-open .stage-deck__hint {
  grid-area: hint;
  margin-top: 0;
  text-align: left;
}

.profile-work.is-open .stage-sheet {
  grid-area: sheet;
  align-self: start;
  margin-top: 0;
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

@media (max-width: 560px) {
  .stage-deck__head {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
  }

  .stage-deck__side {
    width: 100%;
    justify-content: space-between;
    gap: 8px;
  }

  .stage-deck__spec {
    white-space: normal;
  }

  .deck-back {
    height: 32px;
    padding: 0 12px;
    font-size: 11px;
  }
}

@media (max-width: 1100px) {
  .profile-work {
    flex-direction: column;
    align-items: stretch;
    gap: 14px;
    min-height: 0;
  }

  .profile-work.is-open .work-main {
    max-width: 100%;
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
    align-self: stretch;
  }
}

/* 编辑态卡片只作为预览，字号随左栏收一档，避免手机号行被裁掉 */
.profile-work.is-open :deep(.badge__hero-label),
.profile-work.is-open :deep(.badge__digits) {
  font-size: clamp(10.5px, 5cqw, 20px);
}

/* 表单变窄后配色块换到第二行，避免右侧被裁掉 */
.profile-work.is-open :deep(.palette-field) {
  grid-template-columns: auto auto;
  justify-content: space-between;
}

.profile-work.is-open :deep(.palette-field__label) {
  grid-column: 1;
  grid-row: 1;
}

.profile-work.is-open :deep(.palette-field__now) {
  grid-column: 2;
  grid-row: 1;
  justify-self: end;
}

.profile-work.is-open :deep(.palette-field__row) {
  grid-column: 1 / -1;
  grid-row: 2;
  flex-wrap: wrap;
  gap: 4px;
}

.profile-work.is-open :deep(.dot) {
  width: 22px;
  height: 22px;
}

/* 窄屏不再强行左右分栏，恢复上下排布，页面自然滚动 */
@media (max-width: 920px) {
  .profile-work.is-open .stage-deck {
    display: block;
  }

  .profile-work.is-open .stage-deck__head {
    margin-bottom: 12px;
  }

  .profile-work.is-open .stage-sheet {
    margin-top: 12px;
  }

  .profile-work.is-open .stage-deck__hint {
    margin-top: 12px;
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
