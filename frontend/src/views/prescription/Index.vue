<template>
  <div class="page-card">
    <div class="page-header">
      <span class="title">电子处方</span>
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="load">
        <el-option v-for="(s, i) in statusList" :key="i" :label="s" :value="i" />
      </el-select>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="prescriptionNo" label="处方编号" width="170" />
      <el-table-column prop="patientName" label="患者" width="90" />
      <el-table-column prop="doctorName" label="开方医生" width="90" />
      <el-table-column prop="diagnosis" label="临床诊断" min-width="140" show-overflow-tooltip />
      <el-table-column prop="totalAmount" label="金额" width="100">
        <template #default="{ row }">¥{{ row.totalAmount }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType[row.status]">{{ statusList[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="开立时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="onDetail(row)">详情</el-button>
          <el-button v-if="store.role === 'PATIENT' && row.status === 0" link type="success" @click="onPay(row)">缴费</el-button>
          <el-button v-if="(store.role === 'ADMIN' || store.role === 'DOCTOR') && row.status === 1" link type="warning" @click="onDispense(row)">发药</el-button>
          <el-button v-if="store.role !== 'PATIENT' && row.status <= 1" link type="danger" @click="onVoid(row)">作废</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="table-footer">
      <el-pagination background layout="total, prev, pager, next" :total="total"
        v-model:current-page="query.pageNum" :page-size="query.pageSize" @current-change="load" />
    </div>

    <!-- 处方详情 -->
    <el-drawer v-model="detailVisible" title="处方详情" size="520px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="处方编号" :span="2">{{ current.prescriptionNo }}</el-descriptions-item>
        <el-descriptions-item label="患者">{{ current.patientName }}</el-descriptions-item>
        <el-descriptions-item label="开方医生">{{ current.doctorName }}</el-descriptions-item>
        <el-descriptions-item label="临床诊断" :span="2">{{ current.diagnosis }}</el-descriptions-item>
        <el-descriptions-item label="医嘱" :span="2">{{ current.advice || '—' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="current.items || []" border size="small" style="margin-top: 16px">
        <el-table-column prop="medicationName" label="药品" min-width="110" />
        <el-table-column prop="dosage" label="剂量" width="80" />
        <el-table-column prop="usageNote" label="用法" min-width="120" />
        <el-table-column prop="quantity" label="数量" width="60" />
        <el-table-column prop="subtotal" label="小计" width="80">
          <template #default="{ row }">¥{{ row.subtotal }}</template>
        </el-table-column>
      </el-table>
      <div style="text-align: right; margin-top: 12px; font-size: 16px">
        合计：<span style="color: #f56c6c; font-weight: 600">¥{{ current.totalAmount }}</span>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { prescriptionApi } from '@/api'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const statusList = ['待缴费', '已缴费', '已发药', '已作废']
const statusType = ['warning', 'primary', 'success', 'info']

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, status: null })
const detailVisible = ref(false)
const current = ref({})

const formatTime = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '—')

async function load() {
  loading.value = true
  try {
    const res = await prescriptionApi.page(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function onDetail(row) {
  const res = await prescriptionApi.detail(row.id)
  current.value = res.data
  detailVisible.value = true
}

async function onPay(row) {
  try {
    await ElMessageBox.confirm(`确认支付 ¥${row.totalAmount} 吗？`, '处方缴费', { type: 'info' })
    await prescriptionApi.pay(row.id)
    ElMessage.success('缴费成功')
    load()
  } catch (e) { /* 用户取消确认或业务错误（提示已弹出） */ }
}

async function onDispense(row) {
  try {
    await ElMessageBox.confirm('确认发药？发药将扣减对应药品库存。', '发药确认', { type: 'warning' })
    await prescriptionApi.dispense(row.id)
    ElMessage.success('发药完成，库存已扣减')
    load()
  } catch (e) { /* 用户取消确认或业务错误（提示已弹出） */ }
}

async function onVoid(row) {
  try {
    await ElMessageBox.confirm('确定作废该处方吗？', '作废确认', { type: 'warning' })
    await prescriptionApi.void(row.id)
    ElMessage.success('已作废')
    load()
  } catch (e) { /* 用户取消确认或业务错误（提示已弹出） */ }
}

onMounted(load)
</script>
