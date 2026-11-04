<template>
  <div class="pwd-section">
    <el-form ref="pwdRef" :model="user" :rules="rules" label-position="top" class="pwd-form">
      <el-form-item label="旧密码" prop="oldPassword">
        <el-input
          v-model="user.oldPassword"
          placeholder="请输入旧密码"
          type="password"
          show-password
          @focus="focusOn('oldPassword')"
          @blur="focusOff"
        />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword" :rules="infoPwdValidator">
        <el-input
          v-model="user.newPassword"
          placeholder="请输入新密码"
          type="password"
          show-password
          @focus="focusOn('newPassword')"
          @blur="focusOff"
        />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
          v-model="user.confirmPassword"
          placeholder="请确认新密码"
          type="password"
          show-password
          @focus="focusOn('confirmPassword')"
          @blur="focusOff"
        />
      </el-form-item>

      <div class="pwd-form__foot">
        <el-button type="primary" @click="submit">保存</el-button>
        <el-button @click="close">关闭</el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup name="ResetPwd">
import { usePasswordRule } from "@/utils/passwordRule"
import { updateUserPwd } from "@/api/system/user"

const { proxy } = getCurrentInstance()
const { infoPwdValidator } = usePasswordRule()

const emit = defineEmits(["active-field", "secret"])

const PWD_LABELS = { oldPassword: "旧密码", newPassword: "新密码", confirmPassword: "确认密码" }
/** 卡背显示当前正在输入的那个密码框 */
const activeKey = ref("newPassword")

const user = reactive({
  oldPassword: undefined,
  newPassword: undefined,
  confirmPassword: undefined
})

const equalToPassword = (rule, value, callback) => {
  if (user.newPassword !== value) {
    callback(new Error("两次输入的密码不一致"))
  } else {
    callback()
  }
}

const rules = ref({
  oldPassword: [{ required: true, message: "旧密码不能为空", trigger: "blur" }],
  confirmPassword: [{ required: true, message: "确认密码不能为空", trigger: "blur" }, { required: true, validator: equalToPassword, trigger: "blur" }]
})

/** 卡背只显示圆点数量，绝不回显任何一位密码 */
const emitSecret = () => {
  const key = activeKey.value
  emit("secret", { label: PWD_LABELS[key], length: String(user[key] || "").length })
}

const focusOn = key => {
  activeKey.value = key
  emit("active-field", "secret")
  emitSecret()
}

const focusOff = () => emit("active-field", "")

watch(() => [user.oldPassword, user.newPassword, user.confirmPassword], emitSecret, { immediate: true })

/** 提交按钮 */
function submit() {
  proxy.$refs.pwdRef.validate(valid => {
    if (valid) {
      updateUserPwd(user.oldPassword, user.newPassword).then(() => {
        proxy.$modal.msgSuccess("修改成功")
      })
    }
  })
}

/** 关闭按钮 */
function close() {
  proxy.$tab.closePage()
}
</script>

<style lang="scss" scoped>
.pwd-section {
  width: 100%;
  display: flex;
  justify-content: center;
}

/* 与基本资料同一张输入卡片，只换里面的字段 */
.pwd-form {
  width: 100%;
  max-width: 560px;
  margin: 0 auto;
  padding: 26px 26px 22px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background:
    radial-gradient(120% 90% at 0% 0%, rgba(255, 255, 255, 0.055), transparent 62%),
    rgba(255, 255, 255, 0.026);
  box-shadow: 0 30px 60px -46px #000;
}

.pwd-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.pwd-form__foot {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 18px;
}

.pwd-form__foot :deep(.el-button) {
  height: 42px;
  padding: 0 26px;
  border-radius: 11px;
  font-weight: 600;
}

@media (max-width: 640px) {
  .pwd-form {
    padding: 22px 18px 18px;
  }
}
</style>
