<template>
  <div class="page-card">
    <div class="page-header">
      <span class="title">用户管理</span>
      <div class="search-bar">
        <el-input v-model="query.keyword" placeholder="账号/姓名/手机号" clearable style="width: 170px" @keyup.enter="load" />
        <el-select v-model="query.role" placeholder="全部角色" clearable style="width: 120px" @change="load">
          <el-option label="管理员" value="ADMIN" />
          <el-option label="医生" value="DOCTOR" />
          <el-option label="患者" value="PATIENT" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="load">搜索</el-button>
        <el-button type="success" :icon="Plus" @click="openEdit(null)">新增用户</el-button>
      </div>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="username" label="账号" width="120" />
      <el-table-column prop="realName" label="姓名" width="100" />
      <el-table-column prop="role" label="角色" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="{ ADMIN: 'danger', DOCTOR: 'success', PATIENT: 'primary' }[row.role]">
            {{ { ADMIN: '管理员', DOCTOR: '医生', PATIENT: '患者' }[row.role] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="gender" label="性别" width="70" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="email" label="邮箱" min-width="150" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '冻结' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastLoginAt" label="最后登录" width="170">
        <template #default="{ row }">{{ formatTime(row.lastLoginAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="onToggle(row)">
            {{ row.status === 1 ? '冻结' : '解冻' }}
          </el-button>
          <el-popconfirm title="将密码重置为123456？" @confirm="onReset(row)">
            <template #reference><el-button link type="warning">重置密码</el-button></template>
          </el-popconfirm>
          <el-popconfirm title="确定删除该用户？" @confirm="onDelete(row)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <div class="table-footer">
      <el-pagination background layout="total, prev, pager, next" :total="total"
        v-model:current-page="query.pageNum" :page-size="query.pageSize" @current-change="load" />
    </div>

    <el-dialog v-model="editVisible" :title="form.id ? '编辑用户' : '新增用户'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="账号" required>
          <el-input v-model="form.username" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="初始密码">
          <el-input v-model="form.password" placeholder="默认 123456" show-password />
        </el-form-item>
        <el-form-item label="姓名" required><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="角色">
          <el-radio-group v-model="form.role" :disabled="!!form.id">
            <el-radio value="PATIENT">患者</el-radio>
            <el-radio value="DOCTOR">医生</el-radio>
            <el-radio value="ADMIN">管理员</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { userApi } from '@/api'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const editVisible = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', role: null })
const emptyForm = { id: null, username: '', password: '', realName: '', role: 'PATIENT', gender: '男', phone: '', email: '' }
const form = reactive({ ...emptyForm })

const formatTime = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '—')

async function load() {
  loading.value = true
  try {
    const res = await userApi.page(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  Object.assign(form, row ? { ...row, password: '' } : { ...emptyForm })
  editVisible.value = true
}

async function onSave() {
  if (!form.username || !form.realName) {
    ElMessage.warning('请填写账号与姓名')
    return
  }
  submitting.value = true
  try {
    if (form.id) {
      await userApi.update(form)
    } else {
      await userApi.create(form)
    }
    ElMessage.success('保存成功')
    editVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function onToggle(row) {
  try {
    await userApi.changeStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success(row.status === 1 ? '已冻结' : '已解冻')
    load()
  } catch (e) { /* 错误提示由拦截器统一弹出 */ }
}

async function onReset(row) {
  try {
    await userApi.resetPassword(row.id)
    ElMessage.success('密码已重置为 123456')
  } catch (e) { /* 错误提示由拦截器统一弹出 */ }
}

async function onDelete(row) {
  try {
    await userApi.remove(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) { /* 错误提示由拦截器统一弹出 */ }
}

onMounted(load)
</script>
