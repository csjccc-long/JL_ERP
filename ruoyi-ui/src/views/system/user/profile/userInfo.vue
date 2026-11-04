<template>
  <div class="badge-section">
    <el-form ref="userRef" :model="form" :rules="rules" label-position="top" class="badge-form">
      <el-form-item label="手机号码" prop="phonenumber" class="badge-form__phone">
        <el-input
          v-model="form.phonenumber"
          maxlength="11"
          placeholder="138 0013 8000"
          @focus="focusOn('phone')"
          @blur="focusOff"
        />
      </el-form-item>

      <div class="badge-form__row">
        <el-form-item label="用户昵称" prop="nickName">
          <el-input v-model="form.nickName" maxlength="30" placeholder="持卡人姓名" @focus="focusOn('holder')" @blur="focusOff" />
        </el-form-item>
        <el-form-item label="性别" class="badge-form__sex">
          <div class="sex-field" @focusin="focusOn('sex')" @focusout="focusOff">
            <el-radio-group v-model="form.sex">
              <el-radio value="0">男</el-radio>
              <el-radio value="1">女</el-radio>
            </el-radio-group>
          </div>
        </el-form-item>
      </div>

      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" maxlength="50" placeholder="name@company.com" @focus="focusOn('email')" @blur="focusOff" />
      </el-form-item>

      <badgePalette
        :model-value="palette"
        class="badge-form__palette"
        @update:modelValue="value => emit('update:palette', value)"
      />

      <div class="badge-form__foot">
        <el-button type="primary" @click="submit">保存</el-button>
        <el-button @click="close">关闭</el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup name="UserInfo">
import badgePalette from "./badge/badgePalette"
import { updateUserProfile } from "@/api/system/user"

const props = defineProps({
  user: {
    type: Object
  },
  palette: {
    type: String,
    default: "aurora"
  }
})

const emit = defineEmits(["update:palette", "active-field", "draft"])

const { proxy } = getCurrentInstance()

const form = ref({})
const rules = ref({
  nickName: [{ required: true, message: "用户昵称不能为空", trigger: "blur" }],
  email: [{ required: true, message: "邮箱地址不能为空", trigger: "blur" }, { type: "email", message: "请输入正确的邮箱地址", trigger: ["blur", "change"] }],
  phonenumber: [{ required: true, message: "手机号码不能为空", trigger: "blur" }, { pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: "请输入正确的手机号码", trigger: "blur" }],
})

/** 聚焦哪个字段，卡片上的提示框就跟到哪一块 */
const focusOn = field => emit("active-field", field)
const focusOff = () => emit("active-field", "")

/** 提交按钮 */
function submit() {
  proxy.$refs.userRef.validate(valid => {
    if (valid) {
      updateUserProfile(form.value).then(() => {
        proxy.$modal.msgSuccess("修改成功")
        props.user.phonenumber = form.value.phonenumber
        props.user.email = form.value.email
      })
    }
  })
}

/** 关闭按钮 */
function close() {
  proxy.$tab.closePage()
}

// 回显当前登录用户信息
watch(() => props.user, user => {
  if (user) {
    form.value = { nickName: user.nickName, phonenumber: user.phonenumber, email: user.email, sex: user.sex }
  }
}, { immediate: true })

// 卡片实时跟着表单内容走
watch(form, value => emit("draft", { ...value }), { deep: true, immediate: true })
</script>

<style lang="scss" scoped>
.badge-section {
  width: 100%;
  display: flex;
  justify-content: center;
}

/* 输入卡片：与上面的工卡完全分开，各自独立成块 */
.badge-form {
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

.badge-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

/* 手机号是这张卡的"卡号"，字号和字距都放大 */
#app .badge-section .badge-form__phone :deep(.el-input__wrapper) {
  height: 54px;
}

#app .badge-section .badge-form__phone :deep(.el-input__inner) {
  height: 52px;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 0.09em;
  font-variant-numeric: tabular-nums;
}

.badge-form__row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 24px;
}

.sex-field {
  display: flex;
  align-items: center;
  height: 42px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.14);
  transition: border-color 0.22s ease;
}

.sex-field:focus-within {
  border-bottom-color: #00beb4;
  box-shadow: 0 1px 0 0 #00beb4;
}

.sex-field :deep(.el-radio) {
  height: auto;
  margin-right: 18px;
}

.sex-field :deep(.el-radio__label) {
  font-size: 13.5px;
  color: rgba(244, 244, 245, 0.66);
}

.sex-field :deep(.el-radio.is-checked .el-radio__label) {
  color: #f4f4f5;
}

.badge-form__palette {
  margin-top: 2px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.07);
}

.badge-form__foot {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 18px;
}

.badge-form__foot :deep(.el-button) {
  height: 42px;
  padding: 0 26px;
  border-radius: 11px;
  font-weight: 600;
}

@media (max-width: 640px) {
  .badge-form {
    padding: 22px 18px 18px;
  }

  .badge-form__row {
    grid-template-columns: 1fr;
  }
}
</style>
