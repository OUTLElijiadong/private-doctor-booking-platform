<template>
  <div class="booking-page">
    <!-- 顶部筛选 -->
    <div class="page-card filter-card">
      <div class="page-header" style="margin-bottom: 0">
        <span class="title">预约挂号</span>
        <div class="search-bar">
          <el-select v-model="query.departmentId" placeholder="全部科室" clearable style="width: 150px" @change="load">
            <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
          <el-input v-model="query.keyword" placeholder="搜索医生姓名" clearable style="width: 180px" :prefix-icon="Search" @keyup.enter="load" />
          <el-button type="primary" @click="load">搜索</el-button>
        </div>
      </div>
    </div>

    <!-- 医生卡片 -->
    <div v-loading="loading" class="doctor-grid">
      <transition-group name="card">
        <div v-for="doctor in list" :key="doctor.id" class="doctor-card">
          <div class="doctor-top">
            <div class="avatar-ring">
              <el-avatar :size="60" :src="doctor.avatar">{{ (doctor.realName || '').charAt(0) }}</el-avatar>
            </div>
            <div class="head-info">
              <div class="name-row">
                <span class="name">{{ doctor.realName }}</span>
                <span class="title-tag">{{ doctor.title || '医师' }}</span>
              </div>
              <div class="dept">{{ doctor.departmentName || '未分科室' }} · 从业 {{ doctor.years || 0 }} 年</div>
              <div class="score">
                <el-rate v-if="doctor.avgScore" :model-value="doctor.avgScore" disabled size="small" />
                <span class="score-num">{{ doctor.avgScore ? doctor.avgScore + ' 分' : '暂无评分' }}</span>
              </div>
            </div>
          </div>
          <div class="specialty">{{ doctor.specialty || '暂无擅长介绍' }}</div>
          <el-button type="primary" round style="width: 100%" @click="openBook(doctor)">
            <el-icon style="margin-right: 4px"><Calendar /></el-icon>立即预约
          </el-button>
        </div>
      </transition-group>
      <el-empty v-if="!loading && list.length === 0" description="暂无符合条件的医生" style="grid-column: 1 / -1" />
    </div>

    <div class="table-footer">
      <el-pagination background layout="total, prev, pager, next" :total="total"
        v-model:current-page="query.pageNum" :page-size="query.pageSize" @current-change="load" />
    </div>

    <!-- 预约抽屉 -->
    <el-drawer v-model="bookVisible" size="440px" :with-header="false">
      <div class="drawer-body">
        <div class="drawer-doctor">
          <div class="avatar-ring large">
            <el-avatar :size="64" :src="current.avatar">{{ (current.realName || '').charAt(0) }}</el-avatar>
          </div>
          <div>
            <div class="dname">{{ current.realName }} <span class="title-tag">{{ current.title }}</span></div>
            <div class="ddept">{{ current.departmentName }}</div>
          </div>
        </div>

        <div class="step-title"><span class="step-num">1</span>选择就诊日期</div>
        <el-date-picker v-model="bookForm.date" type="date" value-format="YYYY-MM-DD" style="width: 100%"
          :disabled-date="(d) => d.getTime() < Date.now() - 86400000" @change="loadSlots" />

        <div class="step-title"><span class="step-num">2</span>选择就诊时段</div>
        <div v-loading="slotLoading" class="slot-list">
          <div
            v-for="s in slots" :key="s.id"
            class="slot-item"
            :class="{ active: bookForm.scheduleId === s.id, disabled: s.maxCount - s.bookedCount <= 0 }"
            @click="s.maxCount - s.bookedCount > 0 && (bookForm.scheduleId = s.id)"
          >
            <div class="slot-time">{{ s.timeSlot }}</div>
            <div class="slot-remain" :class="{ zero: s.maxCount - s.bookedCount <= 0 }">
              {{ s.maxCount - s.bookedCount > 0 ? `余 ${s.maxCount - s.bookedCount} 号` : '已约满' }}
            </div>
          </div>
          <el-text v-if="!slotLoading && bookForm.date && slots.length === 0" type="info">当日暂无排班，请换日期</el-text>
          <el-text v-if="!bookForm.date" type="info">请先选择就诊日期</el-text>
        </div>

        <div class="step-title"><span class="step-num">3</span>描述症状（选填）</div>
        <el-input v-model="bookForm.symptoms" type="textarea" :rows="3" maxlength="500" show-word-limit
          placeholder="请简要描述症状，便于医生提前了解病情" />

        <el-button type="primary" size="large" round style="width: 100%; margin-top: 24px"
          :loading="booking" :disabled="!bookForm.scheduleId" @click="onBook">
          确认预约
        </el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Calendar } from '@element-plus/icons-vue'
import { departmentApi, doctorApi, scheduleApi, appointmentApi } from '@/api'

const departments = ref([])
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 9, departmentId: null, keyword: '' })

const bookVisible = ref(false)
const booking = ref(false)
const slotLoading = ref(false)
const slots = ref([])
const current = ref({})
const bookForm = reactive({ date: '', scheduleId: null, symptoms: '' })

async function load() {
  loading.value = true
  try {
    const res = await doctorApi.page(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function openBook(doctor) {
  current.value = doctor
  bookForm.date = ''
  bookForm.scheduleId = null
  bookForm.symptoms = ''
  slots.value = []
  bookVisible.value = true
}

async function loadSlots() {
  if (!bookForm.date) return
  slotLoading.value = true
  bookForm.scheduleId = null
  try {
    const res = await scheduleApi.available({ doctorId: current.value.userId, date: bookForm.date })
    slots.value = res.data
  } finally {
    slotLoading.value = false
  }
}

async function onBook() {
  booking.value = true
  try {
    await appointmentApi.online({
      departmentId: current.value.departmentId,
      doctorId: current.value.userId,
      scheduleId: bookForm.scheduleId,
      appointDate: bookForm.date,
      symptoms: bookForm.symptoms
    })
    ElMessage.success('预约成功，可在「预约管理」中查看')
    bookVisible.value = false
  } finally {
    booking.value = false
  }
}

onMounted(async () => {
  const res = await departmentApi.enabled()
  departments.value = res.data
  load()
})
</script>

<style scoped>
.filter-card {
  margin-bottom: 16px;
}

.doctor-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  min-height: 200px;
}

.doctor-card {
  background: var(--surface);
  border-radius: var(--radius-lg);
  padding: 22px;
  box-shadow: var(--shadow-md);
  border: 1px solid rgba(226, 232, 240, 0.7);
  transition: all var(--duration) var(--ease);
}

.doctor-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: var(--brand-200);
}

.doctor-top {
  display: flex;
  gap: 14px;
  margin-bottom: 14px;
}

.avatar-ring {
  padding: 3px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--brand-500), #06b6d4);
  display: inline-flex;
}
.avatar-ring.large {
  padding: 4px;
}

.head-info .name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.head-info .name {
  font-size: 17px;
  font-weight: 700;
  color: var(--gray-900);
}

.title-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 6px;
  background: var(--brand-50);
  color: var(--brand-600);
  font-weight: 600;
}

.head-info .dept {
  font-size: 12px;
  color: var(--gray-500);
  margin: 5px 0;
}

.head-info .score {
  display: flex;
  align-items: center;
  gap: 6px;
}

.score-num {
  font-size: 12px;
  color: #f59e0b;
  font-weight: 700;
}

.specialty {
  font-size: 13px;
  color: var(--gray-600);
  line-height: 1.6;
  min-height: 42px;
  margin-bottom: 14px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* 抽屉 */
.drawer-body {
  padding: 8px 4px;
}

.drawer-doctor {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--gray-100);
  margin-bottom: 18px;
}

.dname {
  font-size: 18px;
  font-weight: 700;
  color: var(--gray-900);
}

.ddept {
  font-size: 13px;
  color: var(--gray-500);
  margin-top: 4px;
}

.step-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--gray-800);
  margin: 18px 0 10px;
}

.step-num {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--brand-500), #8b5cf6);
  color: #fff;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.slot-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 40px;
}

.slot-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-radius: 10px;
  border: 1.5px solid var(--gray-200);
  cursor: pointer;
  transition: all var(--duration) var(--ease);
}

.slot-item:hover:not(.disabled) {
  border-color: var(--brand-200);
  background: var(--brand-50);
}

.slot-item.active {
  border-color: var(--brand-500);
  background: var(--brand-50);
  box-shadow: 0 0 0 3px var(--brand-100);
}

.slot-item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.slot-time {
  font-size: 14px;
  font-weight: 600;
  color: var(--gray-800);
}

.slot-remain {
  font-size: 12px;
  color: var(--success);
  font-weight: 600;
}

.slot-remain.zero {
  color: var(--gray-400);
}

/* 卡片入场 */
.card-enter-active {
  transition: all 0.35s var(--ease);
}
.card-enter-from {
  opacity: 0;
  transform: translateY(16px);
}
</style>
