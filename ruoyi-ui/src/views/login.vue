<template>
  <div class="login">
    <div class="login-aura login-aura--one"></div>
    <div class="login-aura login-aura--two"></div>

    <header class="login-header">
      <img src="@/assets/logo/logo.png" alt="金流智能 ERP" class="brand-logo" />
      <span class="brand-name">金流智能 ERP</span>
    </header>

    <main class="login-main">
      <h1 class="headline">让业务流与知识一起流动</h1>
      <p class="subhead">采购、合同、库存与企业知识库，在一个工作台里完成</p>

      <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
        <div class="form-title">登录到您的工作台</div>

        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            type="text"
            size="large"
            auto-complete="off"
            placeholder="请输入账号"
          >
            <template #prefix><svg-icon icon-class="user" class="input-icon" /></template>
          </el-input>
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            size="large"
            auto-complete="off"
            placeholder="请输入密码"
            @keyup.enter="handleLogin"
          >
            <template #prefix><svg-icon icon-class="password" class="input-icon" /></template>
          </el-input>
        </el-form-item>

        <el-form-item prop="code" v-if="captchaEnabled">
          <div class="captcha-row">
            <el-input
              v-model="loginForm.code"
              size="large"
              auto-complete="off"
              placeholder="请输入验证码"
              @keyup.enter="handleLogin"
            >
              <template #prefix><svg-icon icon-class="validCode" class="input-icon" /></template>
            </el-input>
            <img :src="codeUrl" @click="getCode" class="login-code-img" alt="验证码" />
          </div>
        </el-form-item>

        <div class="form-options">
          <el-checkbox v-model="loginForm.rememberMe">记住密码</el-checkbox>
          <router-link v-if="register" class="link-type" to="/register">立即注册</router-link>
        </div>

        <el-button
          :loading="loading"
          size="large"
          class="login-submit"
          @click.prevent="handleLogin"
        >
          <span v-if="!loading">登 录</span>
          <span v-else>登 录 中...</span>
        </el-button>
      </el-form>
    </main>

    <footer class="login-footer">
      <span>{{ footerContent }}</span>
    </footer>
  </div>
</template>

<script setup>
import { getCodeImg } from "@/api/login"
import Cookies from "js-cookie"
import { encrypt, decrypt } from "@/utils/jsencrypt"
import useUserStore from '@/store/modules/user'
import defaultSettings from '@/settings'

const footerContent = defaultSettings.footerContent
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const loginForm = ref({
  username: "admin",
  password: "admin123",
  rememberMe: false,
  code: "",
  uuid: ""
})

const loginRules = {
  username: [{ required: true, trigger: "blur", message: "请输入您的账号" }],
  password: [{ required: true, trigger: "blur", message: "请输入您的密码" }],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }]
}

const codeUrl = ref("")
const loading = ref(false)
// 验证码开关
const captchaEnabled = ref(true)
// 注册开关
const register = ref(false)
const redirect = ref(undefined)

watch(route, (newRoute) => {
    redirect.value = newRoute.query && newRoute.query.redirect
}, { immediate: true })

function handleLogin() {
  proxy.$refs.loginRef.validate(valid => {
    if (valid) {
      loading.value = true
      // 勾选了需要记住密码设置在 cookie 中设置记住用户名和密码
      if (loginForm.value.rememberMe) {
        Cookies.set("username", loginForm.value.username, { expires: 30 })
        Cookies.set("password", encrypt(loginForm.value.password), { expires: 30 })
        Cookies.set("rememberMe", loginForm.value.rememberMe, { expires: 30 })
      } else {
        // 否则移除
        Cookies.remove("username")
        Cookies.remove("password")
        Cookies.remove("rememberMe")
      }
      // 调用action的登录方法
      userStore.login(loginForm.value).then(() => {
        const query = route.query
        const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
          if (cur !== "redirect") {
            acc[cur] = query[cur]
          }
          return acc
        }, {})
        router.push({ path: redirect.value || "/", query: otherQueryParams })
      }).catch(() => {
        loading.value = false
        // 重新获取验证码
        if (captchaEnabled.value) {
          getCode()
        }
      })
    }
  })
}

function getCode() {
  getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = "data:image/gif;base64," + res.img
      loginForm.value.uuid = res.uuid
    }
  })
}

function getCookie() {
  const username = Cookies.get("username")
  const password = Cookies.get("password")
  const rememberMe = Cookies.get("rememberMe")
  loginForm.value = {
    username: username === undefined ? loginForm.value.username : username,
    password: password === undefined ? loginForm.value.password : decrypt(password),
    rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
  }
}

getCode()
getCookie()
</script>

<style lang='scss' scoped>
.login {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  overflow: hidden;
  background-color: #0b0a12;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.022) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.022) 1px, transparent 1px);
  background-size: 72px 72px, 72px 72px;
  background-position: center;
  color: #f4f4f5;
}

.login::before {
  content: "";
  position: absolute;
  inset: 0;
  background: radial-gradient(120% 80% at 50% 0%, rgba(0, 190, 180, 0.12) 0%, transparent 55%);
  pointer-events: none;
}

.login-aura {
  position: absolute;
  border-radius: 50%;
  filter: blur(120px);
  pointer-events: none;
}

.login-aura--one {
  width: 520px;
  height: 520px;
  top: -220px;
  left: -140px;
  background: rgba(0, 190, 180, 0.16);
}

.login-aura--two {
  width: 460px;
  height: 460px;
  right: -160px;
  bottom: -200px;
  background: rgba(52, 92, 255, 0.12);
}

.login-header {
  position: absolute;
  top: 26px;
  left: 32px;
  display: flex;
  align-items: center;
  gap: 10px;
  z-index: 2;
}

.brand-logo {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.08), 0 6px 18px rgba(0, 0, 0, 0.45);
}

.brand-name {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.3px;
  color: #f4f4f5;
}

.login-main {
  position: relative;
  z-index: 2;
  width: 420px;
  max-width: calc(100vw - 32px);
  text-align: center;
}

.headline {
  margin: 0 0 10px;
  font-size: 30px;
  line-height: 1.3;
  font-weight: 600;
  letter-spacing: 0.4px;
  color: #f7f7f8;
}

.subhead {
  margin: 0 0 30px;
  font-size: 13px;
  color: #8b8e93;
  letter-spacing: 0.2px;
}

.login-form {
  padding: 26px 26px 24px;
  text-align: left;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(26, 26, 30, 0.84);
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(18px);

  .form-title {
    margin-bottom: 20px;
    font-size: 14px;
    font-weight: 600;
    color: #ededee;
  }

  :deep(.el-form-item) {
    margin-bottom: 16px;
  }

  :deep(.el-input__wrapper) {
    height: 44px;
    padding: 1px 12px;
    border-radius: 9px;
    background-color: rgba(255, 255, 255, 0.05) !important;
    box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.12) inset !important;
  }

  :deep(.el-input__wrapper:hover) {
    background-color: rgba(255, 255, 255, 0.07) !important;
    box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.22) inset !important;
  }

  :deep(.el-input__wrapper.is-focus) {
    background-color: rgba(255, 255, 255, 0.07) !important;
    box-shadow: 0 0 0 1px #00beb4 inset, 0 0 0 3px rgba(0, 190, 180, 0.18) !important;
  }

  :deep(.el-input__inner) {
    height: 44px;
    color: #f2f2f3 !important;
    -webkit-text-fill-color: #f2f2f3;
    font-size: 14px;
    caret-color: #00beb4;

    &::placeholder {
      color: #6f7276 !important;
      -webkit-text-fill-color: #6f7276;
    }
  }

  :deep(.el-form-item__error) {
    padding-top: 3px;
    font-size: 12px;
    color: #f0a3a4;
  }

  .input-icon {
    width: 15px;
    height: 15px;
    color: #85888b;
  }
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 2px 0 20px;

  :deep(.el-checkbox__label) {
    color: #9a9da1;
    font-size: 13px;
  }

  :deep(.el-checkbox__inner) {
    background-color: transparent;
    border-color: rgba(255, 255, 255, 0.24);
    border-radius: 4px;
  }

  :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
    background-color: #00beb4;
    border-color: #00beb4;
  }

  .link-type {
    font-size: 13px;
    color: #00beb4;
  }
}

.login-submit {
  width: 100%;
  height: 44px;
  border: none;
  border-radius: 9px;
  background: #f4f4f5;
  color: #101013;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.5px;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.35);

  &:hover,
  &:focus {
    background: #ffffff;
    color: #101013;
  }

  :deep(.el-icon.is-loading) {
    color: #101013;
  }
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;

  .el-input {
    flex: 1;
  }
}

.login-code-img {
  height: 44px;
  width: 118px;
  border-radius: 9px;
  cursor: pointer;
  object-fit: cover;
  background: rgba(255, 255, 255, 0.06);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1) inset;
}

.login-footer {
  position: absolute;
  bottom: 22px;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  letter-spacing: 0.3px;
  color: #5c5f63;
}

@media (max-width: 640px) {
  .login-header {
    top: 20px;
    left: 20px;
  }

  .headline {
    font-size: 24px;
  }

  .login-main {
    width: 100%;
    padding: 0 16px;
  }

  .login-form {
    padding: 22px 18px;
  }
}
</style>
