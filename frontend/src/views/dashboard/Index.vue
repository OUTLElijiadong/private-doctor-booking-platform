<template>
  <div class="dashboard">
    <!-- 欢迎条 -->
    <div class="welcome-bar">
      <div>
        <h2>{{ greeting }}，{{ store.realName }}</h2>
        <p>{{ today }}</p>
      </div>
      <el-button type="primary" :icon="Refresh" circle @click="loadAll" />
    </div>

    <!-- ==================== 管理员视图 ==================== -->
    <template v-if="store.role === 'ADMIN'">
      <el-row :gutter="16" class="stat-row">
        <el-col :xs="12" :sm="8" :md="4" v-for="card in adminCards" :key="card.label">
          <div class="stat-card">
            <div class="stat-icon" :style="{ background: card.bg }">
              <el-icon :size="20" color="#fff"><component :is="card.icon" /></el-icon>
            </div>
            <div class="stat-meta">
              <div class="value">{{ card.display }}</div>
              <div class="label">{{ card.label }}</div>
            </div>
          </div>
        </el-col>
      </el-row>
      <el-row :gutter="16" style="margin-top: 16px">
        <el-col :xs="24" :md="14">
          <div class="page-card chart-card">
            <div class="chart-head"><span class="chart-title">近 7 天预约趋势</span></div>
            <div ref="trendRef" class="chart"></div>
          </div>
        </el-col>
        <el-col :xs="24" :md="10">
          <div class="page-card chart-card">
            <div class="chart-head"><span class="chart-title">科室预约分布</span></div>
            <div ref="deptRef" class="chart"></div>
          </div>
        </el-col>
      </el-row>
      <el-row :gutter="16" style="margin-top: 16px">
        <el-col :xs="24" :md="12">
          <div class="page-card chart-card">
            <div class="chart-head"><span class="chart-title">医生接诊量 TOP10</span></div>
            <div ref="workloadRef" class="chart"></div>
          </div>
        </el-col>
        <el-col :xs="24" :md="12">
          <div class="page-card chart-card">
            <div class="chart-head"><span class="chart-title">药品消耗 TOP10</span></div>
            <div ref="medRef" class="chart"></div>
          </div>
        </el-col>
      </el-row>
    </template>

    <!-- ==================== 医生视图 ==================== -->
    <template v-else-if="store.role === 'DOCTOR'">
      <!-- 资质审核提示：未通过审核前不能排班接诊 -->
      <el-alert v-if="audit.status !== null && audit.status !== 2" :type="audit.status === 3 ? 'error' : 'warning'"
        :closable="false" show-icon style="margin-bottom: 16px">
        <template #title>
          {{ { 0: '执业资质尚未提交审核，暂不能排班接诊', 1: '执业资质审核中，请耐心等待管理员审核', 3: '执业资质审核被驳回，请修改后重新提交' }[audit.status] || '执业资质未通过审核，暂不能排班接诊' }}
          <span v-if="audit.status === 3 && audit.remark" style="font-size: 12px; opacity: .8">（驳回原因：{{ audit.remark }}）</span>
        </template>
        <el-button size="small" type="primary" plain style="margin-top: 6px" @click="$router.push('/reviews')">
          {{ audit.status === 1 ? '查看审核进度' : '前往提交资质' }}
        </el-button>
      </el-alert>
      <el-row :gutter="16" class="stat-row">
        <el-col :xs="12" :sm="6" v-for="card in doctorCards" :key="card.label">
          <div class="stat-card">
            <div class="stat-icon" :style="{ background: card.bg }">
              <el-icon :size="20" color="#fff"><component :is="card.icon" /></el-icon>
            </div>
            <div class="stat-meta">
              <div class="value">{{ card.display }}<span v-if="card.suffix" class="suffix">{{ card.suffix }}</span></div>
              <div class="label">{{ card.label }}</div>
            </div>
          </div>
        </el-col>
      </el-row>
      <el-row :gutter="16" style="margin-top: 16px">
        <el-col :xs="24" :md="14">
          <div class="page-card chart-card">
            <div class="chart-head"><span class="chart-title">近 7 天接诊安排</span></div>
            <div ref="doctorTrendRef" class="chart"></div>
          </div>
        </el-col>
        <el-col :xs="24" :md="10">
          <div class="page-card chart-card">
            <div class="chart-head"><span class="chart-title">快捷操作</span></div>
            <div class="quick-actions">
              <div class="quick-item" @click="$router.push('/appointments')">
                <el-icon :size="22"><Calendar /></el-icon>
                <div><div class="q-title">接诊预约</div><div class="q-sub">查看并处理我的预约单</div></div>
              </div>
              <div class="quick-item" @click="$router.push('/schedules')">
                <el-icon :size="22"><Clock /></el-icon>
                <div><div class="q-title">排班管理</div><div class="q-sub">规划我的出诊时间</div></div>
              </div>
              <div class="quick-item" @click="$router.push('/records')">
                <el-icon :size="22"><Document /></el-icon>
                <div><div class="q-title">就诊病历</div><div class="q-sub">填写病历、开具处方</div></div>
              </div>
              <div class="quick-item" @click="$router.push('/evaluations')">
                <el-icon :size="22"><Star /></el-icon>
                <div><div class="q-title">患者评价</div><div class="q-sub">查看并回复患者评价</div></div>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </template>

    <!-- ==================== 患者视图 ==================== -->
    <template v-else>
      <!-- 最近一次就诊提醒 -->
      <div v-if="nextAppointment" class="next-visit">
        <div class="nv-icon"><el-icon :size="24" color="#fff"><AlarmClock /></el-icon></div>
        <div class="nv-info">
          <div class="nv-title">您有一次即将到来的就诊</div>
          <div class="nv-meta">
            {{ nextAppointment.appointDate }} {{ nextAppointment.timeSlot }} · {{ nextAppointment.departmentName }}
          </div>
        </div>
        <el-button round @click="$router.push('/appointments')">查看详情</el-button>
      </div>

      <el-row :gutter="16" class="stat-row">
        <el-col :xs="12" :sm="6" v-for="card in patientCards" :key="card.label">
          <div class="stat-card">
            <div class="stat-icon" :style="{ background: card.bg }">
              <el-icon :size="20" color="#fff"><component :is="card.icon" /></el-icon>
            </div>
            <div class="stat-meta">
              <div class="value">{{ card.display }}</div>
              <div class="label">{{ card.label }}</div>
            </div>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="16" style="margin-top: 16px">
        <el-col :span="24">
          <div class="page-card chart-card">
            <div class="chart-head"><span class="chart-title">常用服务</span></div>
            <div class="quick-actions patient">
              <div class="quick-item highlight" @click="$router.push('/doctors')">
                <el-icon :size="24"><FirstAidKit /></el-icon>
                <div><div class="q-title">预约挂号</div><div class="q-sub">选择科室医生，在线预约就诊</div></div>
              </div>
              <div class="quick-item" @click="$router.push('/prescriptions')">
                <el-icon :size="24"><Tickets /></el-icon>
                <div><div class="q-title">电子处方</div><div class="q-sub">查看处方、在线缴费</div></div>
              </div>
              <div class="quick-item" @click="$router.push('/records')">
                <el-icon :size="24"><Document /></el-icon>
                <div><div class="q-title">就诊病历</div><div class="q-sub">查看历史就诊记录</div></div>
              </div>
              <div class="quick-item" @click="$router.push('/evaluations')">
                <el-icon :size="24"><Star /></el-icon>
                <div><div class="q-title">医德评价</div><div class="q-sub">评价就诊过的医生</div></div>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, shallowRef } from 'vue'
import * as echarts from 'echarts'
import { Refresh, Calendar, Clock, Document, Star, FirstAidKit, Tickets, AlarmClock } from '@element-plus/icons-vue'
import { statsApi } from '@/api'
import { useUserStore } from '@/store/user'
import dayjs from 'dayjs'

const store = useUserStore()
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})
const today = dayjs().format('YYYY年MM月DD日') + ' ' + ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][new Date().getDay()]

const trendRef = ref()
const deptRef = ref()
const workloadRef = ref()
const medRef = ref()
const doctorTrendRef = ref()
const charts = shallowRef([])

const adminCards = ref([
  { label: '注册患者', value: 0, display: '0', icon: 'User', bg: 'linear-gradient(135deg,#6366f1,#8b5cf6)' },
  { label: '在册医生', value: 0, display: '0', icon: 'FirstAidKit', bg: 'linear-gradient(135deg,#10b981,#34d399)' },
  { label: '累计预约', value: 0, display: '0', icon: 'Calendar', bg: 'linear-gradient(135deg,#3b82f6,#60a5fa)' },
  { label: '今日预约', value: 0, display: '0', icon: 'AlarmClock', bg: 'linear-gradient(135deg,#f59e0b,#fbbf24)' },
  { label: '待派单', value: 0, display: '0', icon: 'Bell', bg: 'linear-gradient(135deg,#ef4444,#f87171)' },
  { label: '库存预警', value: 0, display: '0', icon: 'Box', bg: 'linear-gradient(135deg,#8b5cf6,#a78bfa)' }
])

const doctorCards = ref([
  { label: '今日待接诊', value: 0, display: '0', icon: 'AlarmClock', bg: 'linear-gradient(135deg,#f59e0b,#fbbf24)' },
  { label: '待就诊总数', value: 0, display: '0', icon: 'Calendar', bg: 'linear-gradient(135deg,#3b82f6,#60a5fa)' },
  { label: '累计完成接诊', value: 0, display: '0', icon: 'CircleCheck', bg: 'linear-gradient(135deg,#10b981,#34d399)' },
  { label: '患者评分', value: 0, display: '—', icon: 'Star', bg: 'linear-gradient(135deg,#6366f1,#8b5cf6)', suffix: '' }
])

const patientCards = ref([
  { label: '我的预约', value: 0, display: '0', icon: 'Calendar', bg: 'linear-gradient(135deg,#6366f1,#8b5cf6)' },
  { label: '已完成就诊', value: 0, display: '0', icon: 'CircleCheck', bg: 'linear-gradient(135deg,#10b981,#34d399)' },
  { label: '待缴费处方', value: 0, display: '0', icon: 'Tickets', bg: 'linear-gradient(135deg,#f59e0b,#fbbf24)' },
  { label: '我的病历', value: 0, display: '0', icon: 'Document', bg: 'linear-gradient(135deg,#3b82f6,#60a5fa)' }
])

const nextAppointment = ref(null)

// 医生资质状态（0未提交 1待审核 2已通过 3已驳回），用于出诊提示
const audit = reactive({ status: null, remark: '' })

function animateNumber(card) {
  const target = card.value
  const duration = 800
  const start = performance.now()
  // 用 setTimeout 而非 requestAnimationFrame：后台标签页 rAF 被暂停会导致数字永远停在 0
  const step = () => {
    const p = Math.min((performance.now() - start) / duration, 1)
    card.display = String(Math.round(target * (1 - Math.pow(1 - p, 3))))
    if (p < 1) setTimeout(step, 16)
  }
  setTimeout(step, 16)
}

function makeChart(el, option) {
  if (!el) return
  const chart = echarts.init(el)
  chart.setOption(option)
  charts.value.push(chart)
}

const axisStyle = {
  axisLine: { lineStyle: { color: '#e2e8f0' } },
  axisLabel: { color: '#94a3b8', fontSize: 11 },
  splitLine: { lineStyle: { color: '#f1f5f9' } }
}

function lineOption(data, color) {
  return {
    tooltip: { trigger: 'axis', borderRadius: 8 },
    grid: { left: 44, right: 16, top: 24, bottom: 28 },
    xAxis: { type: 'category', data: data.map(i => String(i.label).slice(5)), ...axisStyle },
    yAxis: { type: 'value', minInterval: 1, ...axisStyle },
    series: [{
      type: 'line', smooth: true, data: data.map(i => i.value),
      symbol: 'circle', symbolSize: 7,
      lineStyle: { width: 3, color },
      itemStyle: { color, borderColor: '#fff', borderWidth: 2 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: color + '40' },
          { offset: 1, color: color + '05' }
        ])
      }
    }]
  }
}

async function loadAdmin() {
  const ov = await statsApi.overview()
  const d = ov.data || {}
  const values = [d.patientCount, d.doctorCount, d.appointmentTotal, d.appointmentToday, d.pendingAppointment, d.lowStockMedication]
  adminCards.value.forEach((c, i) => { c.value = values[i] ?? 0; animateNumber(c) })

  const trend = await statsApi.appointmentTrend()
  makeChart(trendRef.value, lineOption(trend.data || [], '#6366f1'))

  const dept = await statsApi.departmentDistribution()
  makeChart(deptRef.value, {
    tooltip: { trigger: 'item', borderRadius: 8 },
    legend: { bottom: 0, icon: 'circle', itemWidth: 8, textStyle: { color: '#64748b', fontSize: 11 } },
    series: [{
      type: 'pie', radius: ['48%', '70%'], center: ['50%', '44%'],
      data: (dept.data || []).map(i => ({ name: i.label, value: i.value })),
      label: { show: false },
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      emphasis: { scaleSize: 6 }
    }]
  })

  const workload = await statsApi.doctorWorkload()
  const wl = workload.data || []
  makeChart(workloadRef.value, {
    tooltip: { trigger: 'axis', borderRadius: 8 },
    grid: { left: 60, right: 16, top: 24, bottom: 28 },
    xAxis: { type: 'category', data: wl.map(i => i.label), ...axisStyle },
    yAxis: { type: 'value', minInterval: 1, ...axisStyle },
    series: [{
      type: 'bar', data: wl.map(i => i.value), barMaxWidth: 26,
      itemStyle: {
        borderRadius: [6, 6, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#34d399' }, { offset: 1, color: '#10b981' }
        ])
      }
    }]
  })

  const med = await statsApi.medicationConsumption()
  const md = med.data || []
  makeChart(medRef.value, {
    tooltip: { trigger: 'axis', borderRadius: 8 },
    grid: { left: 100, right: 30, top: 16, bottom: 28 },
    xAxis: { type: 'value', minInterval: 1, ...axisStyle },
    yAxis: { type: 'category', data: md.map(i => i.label).reverse(), ...axisStyle },
    series: [{
      type: 'bar', data: md.map(i => i.value).reverse(), barMaxWidth: 16,
      itemStyle: {
        borderRadius: [0, 6, 6, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#a78bfa' }, { offset: 1, color: '#8b5cf6' }
        ])
      }
    }]
  })
}

async function loadDoctor() {
  const ov = await statsApi.doctorOverview()
  const d = ov.data || {}
  audit.status = d.auditStatus ?? 0
  audit.remark = d.auditRemark || ''
  doctorCards.value[0].value = d.todayAppointments ?? 0
  doctorCards.value[1].value = d.upcomingAppointments ?? 0
  doctorCards.value[2].value = d.completedTotal ?? 0
  doctorCards.value.slice(0, 3).forEach(animateNumber)
  // 评分卡
  if ((d.evaluationCount ?? 0) > 0) {
    doctorCards.value[3].display = `${d.avgScore}`
    doctorCards.value[3].suffix = ` 分（${d.evaluationCount}条）`
  } else {
    doctorCards.value[3].display = '暂无评价'
  }
  const trend = await statsApi.doctorTrend()
  makeChart(doctorTrendRef.value, lineOption(trend.data || [], '#10b981'))
}

async function loadPatient() {
  const ov = await statsApi.patientOverview()
  const d = ov.data || {}
  const values = [d.appointmentTotal, d.completedVisits, d.unpaidPrescriptions, d.recordTotal]
  patientCards.value.forEach((c, i) => { c.value = values[i] ?? 0; animateNumber(c) })
  nextAppointment.value = d.nextAppointment || null
}

function loadAll() {
  charts.value.forEach(c => c.dispose())
  charts.value = []
  if (store.role === 'ADMIN') loadAdmin()
  else if (store.role === 'DOCTOR') loadDoctor()
  else loadPatient()
}

function resize() {
  charts.value.forEach(c => c.resize())
}

onMounted(() => {
  loadAll()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  charts.value.forEach(c => c.dispose())
})
</script>

<style scoped>
.welcome-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 4px 4px 0;
}

.welcome-bar h2 {
  font-size: 22px;
  font-weight: 700;
  color: var(--gray-900);
  letter-spacing: -0.01em;
}

.welcome-bar p {
  font-size: 13px;
  color: var(--gray-500);
  margin-top: 4px;
}

.stat-card {
  background: var(--surface);
  border-radius: var(--radius-lg);
  padding: 18px;
  box-shadow: var(--shadow-md);
  border: 1px solid rgba(226, 232, 240, 0.7);
  display: flex;
  align-items: center;
  gap: 14px;
  transition: transform var(--duration) var(--ease), box-shadow var(--duration) var(--ease);
  margin-bottom: 12px;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-lg);
}

.stat-icon {
  width: 44px;
  height: 44px;
  min-width: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-meta .value {
  font-size: 26px;
  font-weight: 800;
  color: var(--gray-900);
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}

.stat-meta .value .suffix {
  font-size: 12px;
  font-weight: 500;
  color: var(--gray-500);
}

.stat-meta .label {
  font-size: 12px;
  color: var(--gray-500);
  margin-top: 2px;
}

.chart-card {
  margin-bottom: 12px;
}

.chart-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.chart-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--gray-800);
}

.chart {
  height: 300px;
}

/* 快捷操作 */
.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 6px 0;
}

.quick-actions.patient {
  flex-direction: row;
  flex-wrap: wrap;
}

.quick-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  border-radius: 12px;
  border: 1px solid var(--gray-200);
  cursor: pointer;
  transition: all var(--duration) var(--ease);
  color: var(--gray-600);
  flex: 1;
  min-width: 220px;
}

.quick-item:hover {
  border-color: var(--brand-500);
  background: var(--brand-50);
  color: var(--brand-600);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.quick-item.highlight {
  border-color: var(--brand-200);
  background: linear-gradient(135deg, var(--brand-50), #f5f3ff);
  color: var(--brand-600);
}

.q-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--gray-900);
}

.quick-item:hover .q-title,
.quick-item.highlight .q-title {
  color: var(--brand-700);
}

.q-sub {
  font-size: 12px;
  color: var(--gray-500);
  margin-top: 3px;
}

/* 最近就诊提醒 */
.next-visit {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 22px;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  margin-bottom: 16px;
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.3);
}

.nv-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.nv-info {
  flex: 1;
}

.nv-title {
  font-size: 16px;
  font-weight: 700;
}

.nv-meta {
  font-size: 13px;
  opacity: 0.85;
  margin-top: 4px;
}
</style>
