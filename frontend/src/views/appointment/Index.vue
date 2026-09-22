<template>
  <div class="page-card">
    <div class="page-header">
      <span class="title">预约管理</span>
      <div class="search-bar">
        <el-select v-model="query.departmentId" placeholder="全部科室" clearable style="width: 130px" @change="load">
          <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" />
        </el-select>
        <el-button v-if="store.role !== 'PATIENT'" type="primary" :icon="Plus" @click="offlineVisible = true">线下登记</el-button>
      </div>
    </div>

    <!-- 状态标签页 -->
    <el-tabs v-model="activeTab" class="status-tabs" @tab-change="onTabChange">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane v-for="(s, i) in statusList" :key="i" :name="String(i)">
        <template #label>
          <el-badge v-if="i === 0 && pendingCount > 0" :value="pendingCount" :offset="[8, 0]">{{ s }}</el-badge>
          <template v-else>{{ s }}</template>
        </template>
      </el-tab-pane>
    </el-tabs>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="appointmentNo" label="预约编号" width="170" />
      <el-table-column prop="patientName" label="患者" width="90" />
      <el-table-column prop="departmentName" label="科室" width="90" />
      <el-table-column prop="doctorName" label="接诊医生" width="100">
        <template #default="{ row }">{{ row.doctorName || '待派单' }}</template>
      </el-table-column>
      <el-table-column prop="appointDate" label="就诊日期" width="110" />
      <el-table-column prop="timeSlot" label="时段" width="150" />
      <el-table-column prop="source" label="来源" width="80">
        <template #default="{ row }">
          <el-tag size="small" :type="row.source === 'ONLINE' ? 'primary' : 'warning'">
            {{ row.source === 'ONLINE' ? '线上' : '线下' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType[row.status]">{{ statusList[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="symptoms" label="症状描述" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="canAssign(row)" link type="primary" @click="openAssign(row)">派单</el-button>
          <el-button v-if="store.role === 'DOCTOR' && row.status === 1" link type="success" @click="openRecord(row)">填写病历</el-button>
          <el-button v-if="row.status <= 1" link type="danger" @click="onCancel(row)">取消</el-button>
          <el-button v-if="store.role === 'PATIENT' && row.status === 2" link type="warning" @click="openEvaluate(row)">评价</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="table-footer">
      <el-pagination background layout="total, prev, pager, next" :total="total"
        v-model:current-page="query.pageNum" :page-size="query.pageSize" @current-change="load" />
    </div>

    <!-- 派单 -->
    <el-dialog v-model="assignVisible" title="预约派单" width="440px">
      <el-form :model="assignForm" label-width="90px">
        <el-form-item label="接诊医生" required>
          <el-select v-model="assignForm.doctorId" style="width: 100%">
            <el-option v-for="d in doctors" :key="d.userId" :label="`${d.realName}（${d.departmentName || '未分科室'}）`" :value="d.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="就诊日期" required>
          <el-date-picker v-model="assignForm.appointDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="就诊时段" required>
          <el-select v-model="assignForm.timeSlot" style="width: 100%">
            <el-option label="上午 08:00-12:00" value="上午 08:00-12:00" />
            <el-option label="下午 14:00-17:00" value="下午 14:00-17:00" />
            <el-option label="晚上 18:00-21:00" value="晚上 18:00-21:00" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onAssign">确认派单</el-button>
      </template>
    </el-dialog>

    <!-- 线下登记 -->
    <el-dialog v-model="offlineVisible" title="线下预约登记" width="520px">
      <el-form :model="offlineForm" label-width="90px">
        <el-form-item label="患者" required>
          <el-select v-model="offlineForm.patientId" filterable placeholder="选择患者" style="width: 100%">
            <el-option v-for="p in patients" :key="p.id" :label="`${p.realName}（${p.phone || p.username}）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="科室" required>
          <el-select v-model="offlineForm.departmentId" style="width: 100%">
            <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="就诊日期" required>
          <el-date-picker v-model="offlineForm.appointDate" type="date" value-format="YYYY-MM-DD"
            :disabled-date="(d) => d.getTime() < Date.now() - 86400000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="就诊时段">
          <el-select v-model="offlineForm.timeSlot" style="width: 100%">
            <el-option label="上午 08:00-12:00" value="上午 08:00-12:00" />
            <el-option label="下午 14:00-17:00" value="下午 14:00-17:00" />
            <el-option label="晚上 18:00-21:00" value="晚上 18:00-21:00" />
          </el-select>
        </el-form-item>
        <el-form-item label="症状描述">
          <el-input v-model="offlineForm.symptoms" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="offlineVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onOffline">确认登记</el-button>
      </template>
    </el-dialog>

    <!-- 填写病历 -->
    <el-dialog v-model="recordVisible" title="填写病历" width="560px">
      <el-form :model="recordForm" label-width="90px">
        <el-form-item label="主诉" required>
          <el-input v-model="recordForm.chiefComplaint" type="textarea" :rows="2" placeholder="患者主要症状与持续时间" />
        </el-form-item>
        <el-form-item label="诊断结果" required>
          <el-input v-model="recordForm.diagnosis" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="治疗方案">
          <el-input v-model="recordForm.treatment" type="textarea" :rows="3" placeholder="治疗方案与医嘱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recordVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onRecord">保存并归档</el-button>
      </template>
    </el-dialog>

    <!-- 评价 -->
    <el-dialog v-model="evaluateVisible" title="医德评价" width="440px">
      <div style="text-align: center; margin-bottom: 16px">
        <el-rate v-model="evaluateForm.score" :max="5" show-text />
      </div>
      <el-input v-model="evaluateForm.content" type="textarea" :rows="4" placeholder="请评价医生的诊疗服务与医德医风" />
      <template #footer>
        <el-button @click="evaluateVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onEvaluate">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { appointmentApi, departmentApi, doctorApi, userApi, recordApi, evaluationApi } from '@/api'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const statusList = ['待派单', '已派单', '已完成', '已取消']
const statusType = ['warning', 'primary', 'success', 'info']

const list = ref([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const departments = ref([])
const doctors = ref([])
const patients = ref([])
const activeTab = ref('all')
const pendingCount = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, status: null, departmentId: null })

function onTabChange(name) {
  query.status = name === 'all' ? null : Number(name)
  query.pageNum = 1
  load()
}

const assignVisible = ref(false)
const offlineVisible = ref(false)
const recordVisible = ref(false)
const evaluateVisible = ref(false)
const assignForm = reactive({ appointmentId: null, doctorId: null, appointDate: '', timeSlot: '' })
const offlineForm = reactive({ patientId: null, departmentId: null, appointDate: '', timeSlot: '上午 08:00-12:00', symptoms: '' })
const recordForm = reactive({ appointmentId: null, chiefComplaint: '', diagnosis: '', treatment: '' })
const evaluateForm = reactive({ appointmentId: null, score: 5, content: '' })

const canAssign = (row) => row.status === 0 && (store.role === 'ADMIN' || store.role === 'DOCTOR')

async function load() {
  loading.value = true
  try {
    const res = await appointmentApi.page(query)
    list.value = res.data.records
    total.value = res.data.total
    // 待派单角标
    if (store.role !== 'PATIENT') {
      const pending = await appointmentApi.page({ pageNum: 1, pageSize: 1, status: 0 })
      pendingCount.value = pending.data.total
    }
  } finally {
    loading.value = false
  }
}

function openAssign(row) {
  assignForm.appointmentId = row.id
  assignForm.doctorId = null
  assignForm.appointDate = row.appointDate
  assignForm.timeSlot = row.timeSlot || '上午 08:00-12:00'
  assignVisible.value = true
}

async function onAssign() {
  if (!assignForm.doctorId) {
    ElMessage.warning('请选择接诊医生')
    return
  }
  submitting.value = true
  try {
    await appointmentApi.assign(assignForm)
    ElMessage.success('派单成功')
    assignVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function onOffline() {
  if (!offlineForm.patientId || !offlineForm.departmentId || !offlineForm.appointDate) {
    ElMessage.warning('请完整填写患者、科室与就诊日期')
    return
  }
  submitting.value = true
  try {
    await appointmentApi.offline(offlineForm)
    ElMessage.success('登记成功，等待派单')
    offlineVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function onCancel(row) {
  try {
    const { value } = await ElMessageBox.prompt('请输入取消原因', '取消预约', { type: 'warning' })
    await appointmentApi.cancel(row.id, value)
    ElMessage.success('已取消')
    load()
  } catch (e) { /* 用户取消输入或业务错误（提示已弹出） */ }
}

function openRecord(row) {
  recordForm.appointmentId = row.id
  recordForm.chiefComplaint = row.symptoms || ''
  recordForm.diagnosis = ''
  recordForm.treatment = ''
  recordVisible.value = true
}

async function onRecord() {
  if (!recordForm.chiefComplaint || !recordForm.diagnosis) {
    ElMessage.warning('请填写主诉与诊断结果')
    return
  }
  submitting.value = true
  try {
    await recordApi.create(recordForm)
    ElMessage.success('病历已归档，预约已完结')
    recordVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

function openEvaluate(row) {
  evaluateForm.appointmentId = row.id
  evaluateForm.score = 5
  evaluateForm.content = ''
  evaluateVisible.value = true
}

async function onEvaluate() {
  submitting.value = true
  try {
    await evaluationApi.create(evaluateForm)
    ElMessage.success('评价成功')
    evaluateVisible.value = false
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  load()
  const deptRes = await departmentApi.enabled()
  departments.value = deptRes.data
  if (store.role !== 'PATIENT') {
    const [doctorRes, patientRes] = await Promise.all([
      doctorApi.page({ pageNum: 1, pageSize: 100 }),
      userApi.patientOptions()
    ])
    // 派单只面向资质已通过的医生（与后端校验一致）
    doctors.value = doctorRes.data.records.filter(d => d.auditStatus === 2)
    patients.value = patientRes.data
  }
})
</script>

<style scoped>
.status-tabs {
  margin-bottom: 4px;
}
.status-tabs :deep(.el-tabs__item) {
  font-weight: 500;
}
.status-tabs :deep(.el-tabs__item.is-active) {
  color: var(--brand-600);
  font-weight: 600;
}
.status-tabs :deep(.el-tabs__active-bar) {
  background: linear-gradient(90deg, var(--brand-500), #8b5cf6);
  height: 3px;
  border-radius: 2px;
}
</style>
