<template>
  <div>
    <el-row :gutter="16">
      <!-- 个人资料 -->
      <el-col :span="10">
        <div class="page-card">
          <div class="page-header"><span class="title">个人资料</span></div>
          <el-form :model="form" label-width="90px">
            <el-form-item label="账号">
              <el-input :model-value="form.username" disabled />
            </el-form-item>
            <el-form-item label="角色">
              <el-tag :type="{ ADMIN: 'danger', DOCTOR: 'success', PATIENT: 'primary' }[store.role]">
                {{ { ADMIN: '管理员', DOCTOR: '医生', PATIENT: '患者' }[store.role] }}
              </el-tag>
            </el-form-item>
            <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="form.gender">
                <el-radio value="男">男</el-radio>
                <el-radio value="女">女</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
            <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="onSaveProfile">保存资料</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-col>

      <!-- 修改密码 -->
      <el-col :span="7">
        <div class="page-card">
          <div class="page-header"><span class="title">修改密码</span></div>
          <el-form :model="pwdForm" label-width="90px">
            <el-form-item label="原密码"><el-input v-model="pwdForm.oldPassword" type="password" show-password /></el-form-item>
            <el-form-item label="新密码"><el-input v-model="pwdForm.newPassword" type="password" show-password /></el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="onChangePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-col>

      <!-- 医生执业信息 -->
      <el-col :span="7" v-if="store.role === 'DOCTOR'">
        <div class="page-card">
          <div class="page-header">
            <span class="title">执业信息</span>
            <el-tag size="small" :type="['info', 'warning', 'success', 'danger'][doctorInfo.auditStatus ?? 0]">
              {{ ['未提交审核', '待审核', '已通过', '已驳回'][doctorInfo.auditStatus ?? 0] }}
            </el-tag>
          </div>
          <el-form :model="doctorInfo" label-width="90px">
            <el-form-item label="所属科室">
              <el-select v-model="doctorInfo.departmentId" style="width: 100%">
                <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="职称">
              <el-select v-model="doctorInfo.title" style="width: 100%">
                <el-option v-for="t in ['主任医师', '副主任医师', '主治医师', '住院医师']" :key="t" :label="t" :value="t" />
              </el-select>
            </el-form-item>
            <el-form-item label="从业年限"><el-input-number v-model="doctorInfo.years" :min="0" :max="60" /></el-form-item>
            <el-form-item label="擅长领域"><el-input v-model="doctorInfo.specialty" /></el-form-item>
            <el-form-item label="个人简介"><el-input v-model="doctorInfo.introduction" type="textarea" :rows="3" /></el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="onSaveDoctor">保存执业信息</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { authApi, doctorApi, departmentApi } from '@/api'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const saving = ref(false)
const form = reactive({ username: '', realName: '', gender: '男', phone: '', email: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '' })
const doctorInfo = reactive({ departmentId: null, title: '', specialty: '', introduction: '', years: 0, auditStatus: 0 })
const departments = ref([])

async function loadProfile() {
  const res = await authApi.profile()
  Object.assign(form, res.data)
}

async function onSaveProfile() {
  saving.value = true
  try {
    await authApi.updateProfile(form)
    Object.assign(store.userInfo, form)
    localStorage.setItem('userInfo', JSON.stringify(store.userInfo))
    ElMessage.success('资料已保存')
  } finally {
    saving.value = false
  }
}

async function onChangePassword() {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    ElMessage.warning('请填写原密码与新密码')
    return
  }
  saving.value = true
  try {
    await authApi.changePassword(pwdForm)
    ElMessage.success('密码修改成功，请重新登录')
    store.logout()
    window.location.href = '/login'
  } finally {
    saving.value = false
  }
}

async function loadDoctorInfo() {
  const [mineRes, deptRes] = await Promise.all([doctorApi.mine(), departmentApi.enabled()])
  if (mineRes.data) {
    Object.assign(doctorInfo, mineRes.data)
  }
  departments.value = deptRes.data
}

async function onSaveDoctor() {
  saving.value = true
  try {
    await doctorApi.updateProfile(doctorInfo)
    ElMessage.success('执业信息已保存')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadProfile()
  if (store.role === 'DOCTOR') {
    loadDoctorInfo()
  }
})
</script>
