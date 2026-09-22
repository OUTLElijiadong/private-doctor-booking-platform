<template>
  <div class="page-card">
    <div class="page-header">
      <span class="title">排班管理</span>
      <div class="search-bar">
        <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD"
          start-placeholder="开始日期" end-placeholder="结束日期" style="width: 260px" @change="load" />
        <el-tooltip :disabled="auditPassed" content="执业资质审核通过后才能排班出诊" placement="top">
          <span>
            <el-button type="primary" :icon="Plus" :disabled="!auditPassed" @click="openBatch">批量排班</el-button>
          </span>
        </el-tooltip>
      </div>
    </div>

    <el-alert v-if="store.role === 'DOCTOR' && !auditPassed && auditLoaded" type="warning" :closable="false" show-icon
      style="margin-bottom: 14px" title="执业资质尚未通过审核，暂不能排班出诊；请先在「资质审核」页提交材料，由管理员审核。" />

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="workDate" label="出诊日期" width="120" />
      <el-table-column prop="timeSlot" label="时段" width="160" />
      <el-table-column prop="doctorName" label="医生" width="100" />
      <el-table-column prop="departmentName" label="科室" width="100" />
      <el-table-column label="号源" width="120">
        <template #default="{ row }">
          <el-progress :percentage="Math.round(row.bookedCount / row.maxCount * 100)" :stroke-width="8"
            :format="() => `${row.bookedCount}/${row.maxCount}`" />
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '停诊' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 1" link type="danger" @click="onStop(row)">停诊</el-button>
          <el-button v-else link type="success" @click="onResume(row)">恢复出诊</el-button>
          <el-popconfirm v-if="row.bookedCount === 0" title="确定删除该排班？" @confirm="onDelete(row)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <div class="table-footer">
      <el-pagination background layout="total, prev, pager, next" :total="total"
        v-model:current-page="query.pageNum" :page-size="query.pageSize" @current-change="load" />
    </div>

    <!-- 批量排班 -->
    <el-dialog v-model="batchVisible" title="批量出诊规划" width="480px">
      <el-form :model="batchForm" label-width="90px">
        <el-form-item v-if="store.role === 'ADMIN'" label="医生" required>
          <el-select v-model="batchForm.doctorId" style="width: 100%">
            <el-option v-for="d in doctors" :key="d.userId" :label="`${d.realName}（${d.departmentName || '未分科室'}）`" :value="d.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围" required>
          <el-date-picker v-model="batchRange" type="daterange" value-format="YYYY-MM-DD"
            start-placeholder="开始" end-placeholder="结束" style="width: 100%" />
        </el-form-item>
        <el-form-item label="出诊时段" required>
          <el-checkbox-group v-model="batchForm.timeSlots">
            <el-checkbox value="上午 08:00-12:00">上午 08:00-12:00</el-checkbox>
            <el-checkbox value="下午 14:00-17:00">下午 14:00-17:00</el-checkbox>
            <el-checkbox value="晚上 18:00-21:00">晚上 18:00-21:00</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="每时段号源">
          <el-input-number v-model="batchForm.maxCount" :min="1" :max="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onBatch">生成排班</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { scheduleApi, doctorApi } from '@/api'
import { useUserStore } from '@/store/user'

const store = useUserStore()

// 医生资质状态：未通过审核时禁止排班出诊
const auditStatus = ref(null)
const auditLoaded = ref(false)
const auditPassed = computed(() => store.role === 'ADMIN' || auditStatus.value === 2)
const list = ref([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const doctors = ref([])
const dateRange = ref(null)
const query = reactive({ pageNum: 1, pageSize: 10 })

const batchVisible = ref(false)
const batchRange = ref([])
const batchForm = reactive({ doctorId: null, timeSlots: ['上午 08:00-12:00', '下午 14:00-17:00'], maxCount: 20 })

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await scheduleApi.page(params)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function openBatch() {
  batchForm.doctorId = null
  batchRange.value = []
  batchVisible.value = true
}

async function onBatch() {
  if (!batchRange.value || batchRange.value.length !== 2) {
    ElMessage.warning('请选择日期范围')
    return
  }
  if (batchForm.timeSlots.length === 0) {
    ElMessage.warning('请选择出诊时段')
    return
  }
  submitting.value = true
  try {
    const res = await scheduleApi.batch({
      doctorId: batchForm.doctorId,
      startDate: batchRange.value[0],
      endDate: batchRange.value[1],
      timeSlots: batchForm.timeSlots,
      maxCount: batchForm.maxCount
    })
    ElMessage.success(res.data)
    batchVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function onStop(row) {
  try {
    const { value } = await ElMessageBox.prompt('停诊将自动取消该时段未完成的预约，请输入停诊原因', '停诊确认', { type: 'warning' })
    const res = await scheduleApi.changeStatus(row.id, 0, value)
    ElMessage.success(res.data > 0 ? `已停诊，联动取消 ${res.data} 笔预约` : '已停诊')
    load()
  } catch (e) { /* 用户取消输入或业务错误（提示已弹出） */ }
}

async function onResume(row) {
  try {
    await scheduleApi.changeStatus(row.id, 1, '')
    ElMessage.success('已恢复出诊')
    load()
  } catch (e) { /* 错误提示由拦截器统一弹出 */ }
}

async function onDelete(row) {
  try {
    await scheduleApi.remove(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) { /* 错误提示由拦截器统一弹出 */ }
}

onMounted(async () => {
  load()
  if (store.role === 'ADMIN') {
    const res = await doctorApi.page({ pageNum: 1, pageSize: 100 })
    // 派单/排班只面向资质已通过的医生（与后端校验一致）
    doctors.value = res.data.records.filter(d => d.auditStatus === 2)
  } else if (store.role === 'DOCTOR') {
    const mine = await doctorApi.mine()
    auditStatus.value = mine.data?.auditStatus ?? 0
    auditLoaded.value = true
  }
})
</script>
