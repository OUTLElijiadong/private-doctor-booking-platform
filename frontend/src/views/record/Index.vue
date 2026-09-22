<template>
  <div class="page-card">
    <div class="page-header">
      <span class="title">就诊病历</span>
      <el-text type="info" size="small">病历永久归档，可随时查阅</el-text>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="recordNo" label="病历编号" width="170" />
      <el-table-column prop="patientName" label="患者" width="100" />
      <el-table-column prop="doctorName" label="接诊医生" width="100" />
      <el-table-column prop="chiefComplaint" label="主诉" min-width="150" show-overflow-tooltip />
      <el-table-column prop="diagnosis" label="诊断结果" min-width="150" show-overflow-tooltip />
      <el-table-column prop="visitTime" label="就诊时间" width="170">
        <template #default="{ row }">{{ formatTime(row.visitTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="onDetail(row)">详情</el-button>
          <el-button v-if="store.role === 'DOCTOR'" link type="success" @click="openPrescription(row)">开处方</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="table-footer">
      <el-pagination background layout="total, prev, pager, next" :total="total"
        v-model:current-page="query.pageNum" :page-size="query.pageSize" @current-change="load" />
    </div>

    <!-- 病历详情 -->
    <el-drawer v-model="detailVisible" title="病历详情" size="440px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="病历编号">{{ current.recordNo }}</el-descriptions-item>
        <el-descriptions-item label="患者">{{ current.patientName }}</el-descriptions-item>
        <el-descriptions-item label="接诊医生">{{ current.doctorName }}</el-descriptions-item>
        <el-descriptions-item label="就诊时间">{{ formatTime(current.visitTime) }}</el-descriptions-item>
        <el-descriptions-item label="主诉">{{ current.chiefComplaint }}</el-descriptions-item>
        <el-descriptions-item label="诊断结果">{{ current.diagnosis }}</el-descriptions-item>
        <el-descriptions-item label="治疗方案">{{ current.treatment || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <!-- 开具处方（抽屉式，字段含义清晰） -->
    <el-drawer v-model="prescriptionVisible" title="开具处方" size="680px">
      <div class="rx-form">
        <el-form label-width="90px">
          <el-form-item label="临床诊断">
            <el-input v-model="prescriptionForm.diagnosis" placeholder="本次就诊的诊断结论" />
          </el-form-item>
          <el-form-item label="医嘱">
            <el-input v-model="prescriptionForm.advice" placeholder="如：注意休息，清淡饮食，三天后复诊" />
          </el-form-item>
          <el-form-item label="药品明细" required>
            <div class="rx-items">
              <div class="rx-head">
                <span class="col-med">药品</span>
                <span class="col-dose">单次剂量</span>
                <span class="col-usage">用法用量</span>
                <span class="col-qty">数量</span>
                <span class="col-op"></span>
              </div>
              <div v-for="(item, idx) in prescriptionForm.items" :key="idx" class="rx-item">
                <el-select v-model="item.medicationId" placeholder="选择药品" filterable class="col-med">
                  <el-option v-for="m in medications" :key="m.id" :label="`${m.name}（¥${m.price}/${m.unit}，库存${m.stock}）`" :value="m.id" />
                </el-select>
                <el-input v-model="item.dosage" placeholder="如：1片" class="col-dose" />
                <el-input v-model="item.usageNote" placeholder="如：每日3次" class="col-usage" />
                <el-input-number v-model="item.quantity" :min="1" :max="99" controls-position="right" class="col-qty" />
                <el-button link type="danger" :icon="Delete" class="col-op" @click="prescriptionForm.items.splice(idx, 1)" />
              </div>
              <el-button size="small" :icon="Plus" plain @click="prescriptionForm.items.push({ medicationId: null, dosage: '', usageNote: '', quantity: 1 })">
                添加药品
              </el-button>
              <div class="rx-total">预估合计：<span>¥{{ estimatedTotal }}</span></div>
            </div>
          </el-form-item>
        </el-form>
        <div class="drawer-footer">
          <el-button @click="prescriptionVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="onPrescription">开具处方</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { recordApi, prescriptionApi, medicationApi } from '@/api'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const list = ref([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const medications = ref([])
const query = reactive({ pageNum: 1, pageSize: 10 })

const detailVisible = ref(false)
const prescriptionVisible = ref(false)
const current = ref({})
const prescriptionForm = reactive({ recordId: null, diagnosis: '', advice: '', items: [] })

/** 实时预估合计金额 */
const estimatedTotal = computed(() => {
  return prescriptionForm.items.reduce((sum, item) => {
    const med = medications.value.find(m => m.id === item.medicationId)
    return sum + (med ? med.price * (item.quantity || 0) : 0)
  }, 0).toFixed(2)
})

const formatTime = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '—')

async function load() {
  loading.value = true
  try {
    const res = await recordApi.page(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function onDetail(row) {
  const res = await recordApi.detail(row.id)
  current.value = res.data
  detailVisible.value = true
}

async function openPrescription(row) {
  prescriptionForm.recordId = row.id
  prescriptionForm.diagnosis = row.diagnosis
  prescriptionForm.advice = ''
  prescriptionForm.items = [{ medicationId: null, dosage: '', usageNote: '', quantity: 1 }]
  if (medications.value.length === 0) {
    const res = await medicationApi.onSale()
    medications.value = res.data
  }
  prescriptionVisible.value = true
}

async function onPrescription() {
  if (prescriptionForm.items.length === 0 || prescriptionForm.items.some(i => !i.medicationId)) {
    ElMessage.warning('请完善药品明细')
    return
  }
  submitting.value = true
  try {
    await prescriptionApi.create(prescriptionForm)
    ElMessage.success('处方开具成功，等待患者缴费')
    prescriptionVisible.value = false
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.rx-form {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.rx-items {
  width: 100%;
  border: 1px solid var(--gray-200);
  border-radius: 10px;
  padding: 12px;
}

.rx-head {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 600;
  color: var(--gray-500);
  padding: 0 2px;
}

.rx-item {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}

.col-med {
  width: 200px;
  flex: none;
}

.col-dose {
  width: 76px;
  flex: none;
}

.col-usage {
  flex: 1;
  min-width: 110px;
}

.col-qty {
  width: 84px;
  flex: none;
}

.col-op {
  width: 28px;
  flex: none;
}

.rx-total {
  margin-top: 10px;
  text-align: right;
  font-size: 14px;
  color: var(--gray-700);
}

.rx-total span {
  color: #ef4444;
  font-weight: 700;
  font-size: 17px;
}

.drawer-footer {
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid var(--gray-100);
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
