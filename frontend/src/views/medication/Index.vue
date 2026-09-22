<template>
  <div class="page-card">
    <div class="page-header">
      <span class="title">药品管理</span>
      <div class="search-bar">
        <el-input v-model="query.keyword" placeholder="药品名称" clearable style="width: 150px" @keyup.enter="load" />
        <el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width: 130px" @change="load">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="load">搜索</el-button>
        <el-button type="success" :icon="Plus" @click="openEdit(null)">新增药品</el-button>
        <el-button :icon="Collection" @click="categoryVisible = true">分类管理</el-button>
        <el-button :icon="Tickets" @click="openStockLogs">出入库流水</el-button>
      </div>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="name" label="药品名称" min-width="130" />
      <el-table-column prop="categoryName" label="分类" width="100" />
      <el-table-column prop="spec" label="规格" width="130" />
      <el-table-column prop="price" label="单价" width="90">
        <template #default="{ row }">¥{{ row.price }}</template>
      </el-table-column>
      <el-table-column label="库存" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="row.stock <= row.warningStock ? 'danger' : 'success'">{{ row.stock }}</el-tag>
          <el-tooltip v-if="row.stock <= row.warningStock" content="库存低于预警线"><el-icon color="#f56c6c"><Warning /></el-icon></el-tooltip>
        </template>
      </el-table-column>
      <el-table-column prop="supplierName" label="供应商" min-width="150" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '在售' : '下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="success" @click="openStock(row, 1)">入库</el-button>
          <el-button link type="warning" @click="openStock(row, -1)">调整</el-button>
          <el-popconfirm title="确定删除该药品？" @confirm="onDelete(row)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <div class="table-footer">
      <el-pagination background layout="total, prev, pager, next" :total="total"
        v-model:current-page="query.pageNum" :page-size="query.pageSize" @current-change="load" />
    </div>

    <!-- 药品编辑 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑药品' : '新增药品'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="药品名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="规格"><el-input v-model="form.spec" placeholder="如：0.25g*24片/盒" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="form.unit" placeholder="盒/瓶/支" /></el-form-item>
        <el-form-item label="生产厂家"><el-input v-model="form.manufacturer" /></el-form-item>
        <el-form-item label="供应商">
          <el-select v-model="form.supplierId" style="width: 100%">
            <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="单价"><el-input-number v-model="form.price" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="预警库存"><el-input-number v-model="form.warningStock" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">在售</el-radio>
            <el-radio :value="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="药品说明"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 入库/调整 -->
    <el-dialog v-model="stockVisible" :title="stockDirection > 0 ? '药品入库' : '库存调整（出库）'" width="440px">
      <el-form label-width="90px">
        <el-form-item label="药品">
          <el-input :model-value="stockForm.name" disabled />
        </el-form-item>
        <el-form-item label="变动数量">
          <el-input-number v-model="stockForm.quantity" :min="1" />
          <el-text v-if="stockDirection < 0" type="warning" size="small" style="margin-left: 8px">将作为出库扣减</el-text>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="stockForm.remark" placeholder="如：采购入库 / 盘点调整" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onStock">确认</el-button>
      </template>
    </el-dialog>

    <!-- 分类管理 -->
    <el-dialog v-model="categoryVisible" title="药品分类管理" width="480px">
      <div style="display: flex; gap: 8px; margin-bottom: 12px">
        <el-input v-model="newCategory" placeholder="新分类名称" />
        <el-button type="primary" @click="onAddCategory">添加</el-button>
      </div>
      <el-table :data="categories" border size="small">
        <el-table-column prop="name" label="分类名称" />
        <el-table-column prop="remark" label="备注" />
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-popconfirm title="删除该分类？" @confirm="onDeleteCategory(row)">
              <template #reference><el-button link type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 出入库流水 -->
    <el-drawer v-model="logVisible" title="药品出入库流水" size="640px">
      <el-table :data="stockLogs" border size="small" v-loading="logLoading">
        <el-table-column prop="medicationName" label="药品" min-width="110" />
        <el-table-column prop="type" label="类型" width="70">
          <template #default="{ row }">
            <el-tag size="small" :type="row.type === 'IN' ? 'success' : 'danger'">{{ row.type === 'IN' ? '入库' : '出库' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="60" />
        <el-table-column label="库存变化" width="100">
          <template #default="{ row }">{{ row.beforeStock }} → {{ row.afterStock }}</template>
        </el-table-column>
        <el-table-column prop="refType" label="来源" width="100">
          <template #default="{ row }">{{ { PURCHASE: '采购入库', PRESCRIPTION: '处方发药', ADJUST: '人工调整' }[row.refType] || row.refType }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="130" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus, Collection, Tickets, Warning } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { medicationApi, supplierApi } from '@/api'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const categories = ref([])
const suppliers = ref([])
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', categoryId: null })

const editVisible = ref(false)
const stockVisible = ref(false)
const categoryVisible = ref(false)
const logVisible = ref(false)
const stockDirection = ref(1)
const newCategory = ref('')
const stockLogs = ref([])
const logLoading = ref(false)

const emptyForm = { id: null, name: '', categoryId: null, spec: '', unit: '盒', manufacturer: '', supplierId: null, price: 0, warningStock: 10, status: 1, description: '' }
const form = reactive({ ...emptyForm })
const stockForm = reactive({ id: null, name: '', quantity: 1, remark: '' })

const formatTime = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '—')

async function load() {
  loading.value = true
  try {
    const res = await medicationApi.page(query)
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
    ElMessage.warning('请填写药品名称')
    return
  }
  submitting.value = true
  try {
    if (form.id) {
      await medicationApi.update(form)
    } else {
      await medicationApi.create(form)
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
    await medicationApi.remove(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) { /* 错误提示由拦截器统一弹出 */ }
}

function openStock(row, direction) {
  stockDirection.value = direction
  stockForm.id = row.id
  stockForm.name = row.name
  stockForm.quantity = 1
  stockForm.remark = ''
  stockVisible.value = true
}

async function onStock() {
  submitting.value = true
  try {
    await medicationApi.changeStock(stockForm.id, stockForm.quantity * stockDirection.value, stockForm.remark)
    ElMessage.success('库存已更新')
    stockVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function onAddCategory() {
  if (!newCategory.value) return
  await medicationApi.createCategory({ name: newCategory.value })
  ElMessage.success('已添加')
  newCategory.value = ''
  loadCategories()
}

async function onDeleteCategory(row) {
  try {
    await medicationApi.removeCategory(row.id)
    ElMessage.success('已删除')
    loadCategories()
  } catch (e) { /* 错误提示由拦截器统一弹出 */ }
}

async function openStockLogs() {
  logVisible.value = true
  logLoading.value = true
  try {
    const res = await medicationApi.stockLogs({ pageNum: 1, pageSize: 50 })
    stockLogs.value = res.data.records
  } finally {
    logLoading.value = false
  }
}

async function loadCategories() {
  const res = await medicationApi.categories()
  categories.value = res.data
}

onMounted(async () => {
  load()
  loadCategories()
  const res = await supplierApi.options()
  suppliers.value = res.data
})
</script>
