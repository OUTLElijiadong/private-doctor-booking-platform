<template>
  <div class="auth-page">
    <!-- 品牌区 -->
    <div class="auth-brand">
      <div class="brand-content">
        <h1>私人医生<br />预约服务平台</h1>
        <p>打通预约挂号、出诊排班、电子病历、处方流转与药品库存的全流程诊疗业务闭环，为医患双方提供一站式数字化医疗服务。</p>
        <div v-if="platformStats" class="brand-stats">
          <div class="item"><div class="num">{{ platformStats.doctorCount }}</div><div class="txt">在册医生</div></div>
          <div class="item"><div class="num">{{ platformStats.departmentCount }}</div><div class="txt">开诊科室</div></div>
          <div class="item"><div class="num">{{ platformStats.appointmentTotal }}</div><div class="txt">累计预约</div></div>
        </div>
      </div>
    </div>

    <!-- 表单区 -->
    <div class="auth-panel">
      <div class="auth-card">
        <div class="logo">
          <h1>欢迎回来</h1>
          <p>登录以继续使用平台服务</p>
        </div>
        <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="onLogin">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入账号" :prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password :prefix-icon="Lock" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="onLogin">
              登 录
            </el-button>
          </el-form-item>
        </el-form>
        <div class="links">
          <el-link type="primary" @click="$router.push('/register')">注册账号</el-link>
          <el-link type="info" @click="resetVisible = true">忘记密码</el-link>
        </div>
      </div>
    </div>

    <!-- 找回密码 -->
    <el-dialog v-model="resetVisible" title="找回密码" width="420px">
      <el-form :model="resetForm" label-width="80px">
        <el-form-item label="账号"><el-input v-model="resetForm.username" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="resetForm.phone" placeholder="注册时预留的手机号" /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="resetForm.newPassword" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetLoading" @click="onReset">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { authApi, publicApi } from '@/api'
import { useUserStore } from '@/store/user'

const router = useRouter()
const store = useUserStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: ['blur', 'change'] }],
  password: [{ required: true, message: '请输入密码', trigger: ['blur', 'change'] }]
}

// 品牌区实时平台统计（真实数据库数据，接口不可用时隐藏）
const platformStats = ref(null)
onMounted(async () => {
  try {
    const res = await publicApi.platformStats()
    platformStats.value = res.data || null
  } catch {
    platformStats.value = null
  }
})

async function onLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await authApi.login(form)
    store.setLogin(res.data.token, res.data.userInfo)
    ElMessage.success('登录成功，欢迎回来')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}

const resetVisible = ref(false)
const resetLoading = ref(false)
const resetForm = reactive({ username: '', phone: '', newPassword: '' })

async function onReset() {
  resetLoading.value = true
  try {
    await authApi.resetPassword(resetForm)
    ElMessage.success('密码重置成功，请使用新密码登录')
    resetVisible.value = false
  } finally {
    resetLoading.value = false
  }
}
</script>

<style scoped>
.links {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}
</style>
