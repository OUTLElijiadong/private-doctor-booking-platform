<template>
  <div class="page-card">
    <div class="page-header">
      <span class="title">供应商管理</span>
      <div class="search-bar">
        <el-input v-model="query.keyword" placeholder="供应商名称" clearable style="width: 160px" @keyup.enter="load" />
        <el-button type="primary" :icon="Search" @click="load">搜索</el-button>
        <el-button type="success" :icon="Plus" @click="openEdit(null)">新增供应商</el-button>
      </div>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="name" label="供应商名称" min-width="160" />
      <el-table-column prop="contact" label="联系人" width="90" />
      <el-table-column prop="phone" label="联系电话" width="130" />
      <el-table-column prop="licenseNo" label="经营许可证号" width="150" />
      <el-table-column prop="qualification" label="资质/经营范围" min-width="180" show-overflow-tooltip />
      <el-table-column prop="address" label="地址" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除该供应商？" @confirm="onDelete(row)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <div class="table-footer">
      <el-pagination background layout="total, prev, pager, next" :total="total"
        v-model:current-page="query.pageNum" :page-size="query.pageSize" @current-change="load" />
    </div>

    <el-dialog v-model="editVisible" :title="form.id ? '编辑供应商' : '新增供应商'" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="供应商名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contact" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="许可证号"><el-input v-model="form.licenseNo" placeholder="药品经营许可证号" /></el-form-item>
        <el-form-item label="资质说明"><el-input v-model="form.qualification" type="textarea" :rows="2" placeholder="经营范围、质检资质等" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
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
import { supplierApi } from '@/api'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const editVisible = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
const emptyForm = { id: null, name: '', contact: '', phone: '', licenseNo: '', qualification: '', address: '', remark: '' }
const form = reactive({ ...emptyForm })

async function load() {
  loading.value = true
  try {
    const res = await supplierApi.page(query)
    list.value = res.data.records
    total.value = res.data.total
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
    ElMessage.warning('请填写供应商名称')
    return
  }
  submitting.value = true
  try {
    if (form.id) {
      await supplierApi.update(form)
    } else {
      await supplierApi.create(form)
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
    await supplierApi.remove(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) { /* 错误提示由拦截器统一弹出 */ }
}

onMounted(load)
</script>
