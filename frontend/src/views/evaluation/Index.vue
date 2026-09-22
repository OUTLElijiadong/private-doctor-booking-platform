<template>
  <div>
    <!-- 待评价就诊（患者可见） -->
    <div v-if="store.role === 'PATIENT'" class="page-card pending-card">
      <div class="page-header">
        <span class="title">待评价就诊</span>
        <el-text type="info" size="small" v-if="pendingList.length > 0">{{ pendingList.length }} 次就诊待评价</el-text>
      </div>
      <div v-if="pendingList.length === 0" class="empty-pending">
        <el-icon :size="32" color="#cbd5e1"><CircleCheck /></el-icon>
        <p>暂无待评价的就诊记录</p>
        <p class="sub">就诊完成后即可对医生进行评价</p>
      </div>
      <div v-else class="pending-grid">
        <div v-for="a in pendingList" :key="a.id" class="pending-item">
          <div class="p-info">
            <div class="p-doctor">{{ a.doctorName || '待分配医生' }}</div>
            <div class="p-meta">{{ a.departmentName }} · {{ a.appointDate }} {{ a.timeSlot }}</div>
          </div>
          <el-button type="primary" round size="small" @click="openEvaluate(a)">立即评价</el-button>
        </div>
      </div>
    </div>

    <!-- 评价列表 -->
    <div class="page-card" style="margin-top: 16px">
      <div class="page-header">
        <span class="title">{{ store.role === 'PATIENT' ? '我的评价' : store.role === 'DOCTOR' ? '收到的评价' : '医德评价' }}</span>
      </div>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="patientName" label="患者" width="100" />
        <el-table-column prop="doctorName" label="医生" width="100" />
        <el-table-column label="评分" width="160">
          <template #default="{ row }">
            <el-rate :model-value="row.score" disabled />
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评价内容" min-width="180" show-overflow-tooltip />
        <el-table-column prop="reply" label="回复" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reply || '—' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="评价时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column v-if="store.role !== 'PATIENT'" label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openReply(row)">回复</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination background layout="total, prev, pager, next" :total="total"
          v-model:current-page="query.pageNum" :page-size="query.pageSize" @current-change="load" />
      </div>
    </div>

    <!-- 评价对话框 -->
    <el-dialog v-model="evaluateVisible" :title="`评价 - ${evaluateForm.doctorName || ''}`" width="440px">
      <div style="text-align: center; margin-bottom: 16px">
        <el-rate v-model="evaluateForm.score" :max="5" show-text />
      </div>
      <el-input v-model="evaluateForm.content" type="textarea" :rows="4" maxlength="500" show-word-limit
        placeholder="请评价医生的诊疗服务与医德医风" />
      <template #footer>
        <el-button @click="evaluateVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onEvaluate">提交评价</el-button>
      </template>
    </el-dialog>

    <!-- 回复对话框 -->
    <el-dialog v-model="replyVisible" title="回复评价" width="440px">
      <el-input v-model="replyContent" type="textarea" :rows="4" placeholder="请输入回复内容" />
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onReply">提交回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { evaluationApi, appointmentApi } from '@/api'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const list = ref([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const pendingList = ref([])
const query = reactive({ pageNum: 1, pageSize: 10 })

const evaluateVisible = ref(false)
const replyVisible = ref(false)
const replyContent = ref('')
const currentId = ref(null)
const evaluateForm = reactive({ appointmentId: null, doctorName: '', score: 5, content: '' })

const formatTime = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '—')

async function load() {
  loading.value = true
  try {
    const res = await evaluationApi.page(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

/** 患者：找出「已完成但未评价」的就诊 */
async function loadPending() {
  if (store.role !== 'PATIENT') return
  const [apptRes, evalRes] = await Promise.all([
    appointmentApi.page({ pageNum: 1, pageSize: 100, status: 2 }),
    evaluationApi.page({ pageNum: 1, pageSize: 100 })
  ])
  const evaluatedIds = new Set(evalRes.data.records.map(e => e.appointmentId))
  pendingList.value = apptRes.data.records.filter(a => !evaluatedIds.has(a.id))
}

function openEvaluate(row) {
  evaluateForm.appointmentId = row.id
  evaluateForm.doctorName = row.doctorName
  evaluateForm.score = 5
  evaluateForm.content = ''
  evaluateVisible.value = true
}

async function onEvaluate() {
  submitting.value = true
  try {
    await evaluationApi.create({
      appointmentId: evaluateForm.appointmentId,
      score: evaluateForm.score,
      content: evaluateForm.content
    })
    ElMessage.success('评价成功，感谢反馈')
    evaluateVisible.value = false
    load()
    loadPending()
  } finally {
    submitting.value = false
  }
}

function openReply(row) {
  currentId.value = row.id
  replyContent.value = row.reply || ''
  replyVisible.value = true
}

async function onReply() {
  submitting.value = true
  try {
    await evaluationApi.reply(currentId.value, replyContent.value)
    ElMessage.success('回复成功')
    replyVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  load()
  loadPending()
})
</script>

<style scoped>
.pending-card {
  border-left: 4px solid var(--brand-500);
}

.empty-pending {
  text-align: center;
  padding: 28px 0 20px;
  color: var(--gray-400);
}

.empty-pending p {
  margin-top: 10px;
  font-size: 14px;
  color: var(--gray-600);
}

.empty-pending .sub {
  margin-top: 4px;
  font-size: 12px;
  color: var(--gray-400);
}

.pending-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 12px;
}

.pending-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-radius: 10px;
  background: var(--gray-50);
  border: 1px solid var(--gray-200);
  transition: all var(--duration) var(--ease);
}

.pending-item:hover {
  border-color: var(--brand-200);
  background: var(--brand-50);
}

.p-doctor {
  font-size: 15px;
  font-weight: 700;
  color: var(--gray-900);
}

.p-meta {
  font-size: 12px;
  color: var(--gray-500);
  margin-top: 4px;
}
</style>
