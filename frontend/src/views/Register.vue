<template>
  <div class="auth-page">
    <div class="auth-brand">
      <div class="brand-content">
        <div class="badge">
          <el-icon><FirstAidKit /></el-icon>
          三分钟完成注册 · 即刻在线预约
        </div>
        <h1>让就医<br />更简单一点</h1>
        <p>注册后即可浏览科室医生、查看出诊排班、在线预约挂号，并随时查阅自己的就诊病历与电子处方。</p>
        <div class="brand-stats">
          <div class="item"><div class="num">在线</div><div class="txt">预约挂号</div></div>
          <div class="item"><div class="num">电子</div><div class="txt">病历处方</div></div>
          <div class="item"><div class="num">永久</div><div class="txt">档案留存</div></div>
        </div>
      </div>
    </div>

    <div class="auth-panel">
      <div class="auth-card">
        <div class="logo">
          <h1>创建账号</h1>
          <p>注册患者账号，开始在线预约</p>
        </div>
        <el-form ref="formRef" :model="form" :rules="rules" size="large">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="账号（4-20位）" :prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="realName">
            <el-input v-model="form.realName" placeholder="真实姓名" :prefix-icon="Postcard" />
          </el-form-item>
          <el-form-item prop="phone">
            <el-input v-model="form.phone" placeholder="手机号" :prefix-icon="Iphone" />
          </el-form-item>
          <el-form-item prop="gender">
            <el-radio-group v-model="form.gender" style="width: 100%">
              <el-radio-button value="男" style="width: 50%">男</el-radio-button>
              <el-radio-button value="女" style="width: 50%">女</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item>
            <el-radio-group v-model="form.role" style="width: 100%">
              <el-radio-button value="PATIENT" style="width: 50%">患者注册</el-radio-button>
              <el-radio-button value="DOCTOR" style="width: 50%">医生注册</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-alert v-if="form.role === 'DOCTOR'" type="warning" :closable="false" show-icon style="margin-bottom: 18px">
            医生账号注册后，请登录平台在「资质审核」页提交执业资质材料，管理员审核通过后方可排班接诊。
          </el-alert>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码（6-20位）" show-password :prefix-icon="Lock" />
          </el-form-item>
          <el-form-item prop="confirm">
            <el-input v-model="form.confirm" type="password" placeholder="确认密码" show-password :prefix-icon="Lock" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="onRegister">
              注 册
            </el-button>
          </el-form-item>
        </el-form>
        <div style="text-align: center">
          <el-link type="primary" @click="$router.push('/login')">已有账号？去登录</el-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Postcard, Iphone, FirstAidKit } from '@element-plus/icons-vue'
import { authApi } from '@/api'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', realName: '', phone: '', gender: '男', password: '', confirm: '', role: 'PATIENT' })

const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, max: 20, message: '账号长度需为4-20位', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需为6-20位', trigger: 'blur' }
  ],
  confirm: [{
    validator: (r, v, cb) => cb(v === form.password ? undefined : new Error('两次密码不一致')),
    trigger: 'blur'
  }]
}

async function onRegister() {
  await formRef.value.validate()
  loading.value = true
  try {
    await authApi.register(form)
    ElMessage.success(form.role === 'DOCTOR'
      ? '注册成功，请登录后在「资质审核」页提交执业资质，审核通过后方可出诊'
      : '注册成功，请登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>
