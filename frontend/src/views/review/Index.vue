<template>
  <div class="page-card">
    <div class="page-header">
      <span class="title">执业资格审核</span>
      <el-button v-if="store.role === 'DOCTOR'" type="primary" :icon="Plus" @click="submitVisible = true">提交资质材料</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="doctorName" label="医生" width="100" />
      <el-table-column prop="certName" label="证书名称" width="150" />
      <el-table-column prop="certNo" label="证书编号" width="160" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="['warning', 'success', 'danger'][row.status]">{{ ['待审核', '已通过', '已驳回'][row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="审核意见" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ row.remark || '—' }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="提交时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column v-if="store.role === 'ADMIN'" label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 0">
            <el-button link type="success" @click="onAudit(row, 1)">通过</el-button>
            <el-button link type="danger" @click="onAudit(row, 2)">驳回</el-button>
          </template>
          <el-text v-else type="info" size="small">已审核</el-text>
        </template>
      </el-table-column>
    </el-table>
    <div class="table-footer">
      <el-pagination background layout="total, prev, pager, next" :total="total"
        v-model:current-page="query.pageNum" :page-size="query.pageSize" @current-change="load" />
    </div>

    <!-- 提交资质 -->
    <el-dialog v-model="submitVisible" title="提交执业资质材料" width="480px">
      <el-form :model="submitForm" label-width="90px">
        <el-form-item label="证书名称" required>
          <el-input v-model="submitForm.certName" placeholder="如：医师执业证书" />
        </el-form-item>
        <el-form-item label="证书编号" required>
          <el-input v-model="submitForm.certNo" placeholder="请输入证书编号" />
        </el-form-item>
        <el-form-item label="材料链接">
          <el-input v-model="submitForm.materialUrl" placeholder="材料扫描件地址（可多个，逗号分隔）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">提交审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { reviewApi } from '@/api'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const list = ref([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10 })

const submitVisible = ref(false)
const submitForm = reactive({ certName: '', certNo: '', materialUrl: '' })

const formatTime = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '—')

async function load() {
  loading.value = true
  try {
    const res = await reviewApi.page(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  if (!submitForm.certName || !submitForm.certNo) {
    ElMessage.warning('请填写证书名称与编号')
    return
  }
  submitting.value = true
  try {
    await reviewApi.submit(submitForm)
    ElMessage.success('提交成功，等待管理员审核')
    submitVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function onAudit(row, status) {
  try {
    const { value } = await ElMessageBox.prompt('请输入审核意见', status === 1 ? '通过审核' : '驳回审核', {
      type: status === 1 ? 'success' : 'warning',
      inputValue: status === 1 ? '资质真实有效，审核通过' : ''
    })
    await reviewApi.audit({ id: row.id, status, remark: value })
    ElMessage.success('审核完成')
    load()
  } catch (e) { /* 用户取消输入或业务错误（提示已弹出） */ }
}

onMounted(load)
</script>
