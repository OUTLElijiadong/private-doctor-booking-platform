<template>
  <div class="page-card">
    <div class="page-header">
      <span class="title">科室管理</span>
      <el-button type="primary" :icon="Plus" @click="openEdit(null)">新增科室</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="科室名称" width="140" />
      <el-table-column prop="description" label="科室介绍" min-width="200" show-overflow-tooltip />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除该科室？" @confirm="onDelete(row)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="editVisible" :title="form.id ? '编辑科室' : '新增科室'" width="440px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="科室名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="科室介绍"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="排序号"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
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
import { Plus } from '@element-plus/icons-vue'
import { departmentApi } from '@/api'

const list = ref([])
const loading = ref(false)
const submitting = ref(false)
const editVisible = ref(false)
const emptyForm = { id: null, name: '', description: '', sort: 0, status: 1 }
const form = reactive({ ...emptyForm })

async function load() {
  loading.value = true
  try {
    const res = await departmentApi.list()
    list.value = res.data
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  Object.assign(form, row ? { ...row } : { ...emptyForm })
  editVisible.value = true
}

async function onSave() {
  if (!form.name) {
    ElMessage.warning('请填写科室名称')
    return
  }
  submitting.value = true
  try {
    if (form.id) {
      await departmentApi.update(form)
    } else {
      await departmentApi.create(form)
    }
    ElMessage.success('保存成功')
    editVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function onDelete(row) {
  try {
    await departmentApi.remove(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) { /* 错误提示由拦截器统一弹出 */ }
}

onMounted(load)
</script>
