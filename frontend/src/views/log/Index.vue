<template>
  <div class="page-card">
    <div class="page-header">
      <span class="title">运行日志</span>
      <div class="search-bar">
        <el-input v-model="query.username" placeholder="操作人" clearable style="width: 130px" @keyup.enter="load" />
        <el-input v-model="query.module" placeholder="所属模块" clearable style="width: 130px" @keyup.enter="load" />
        <el-button type="primary" :icon="Search" @click="load">搜索</el-button>
      </div>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="username" label="操作人" width="100" />
      <el-table-column prop="module" label="模块" width="100" />
      <el-table-column prop="action" label="操作" width="130" />
      <el-table-column prop="method" label="请求" min-width="180" show-overflow-tooltip />
      <el-table-column prop="ip" label="IP" width="120" />
      <el-table-column prop="status" label="结果" width="80">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '成功' : '失败' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="costMs" label="耗时" width="90">
        <template #default="{ row }">{{ row.costMs }} ms</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="详情" width="80" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="onDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="table-footer">
      <el-pagination background layout="total, prev, pager, next" :total="total"
        v-model:current-page="query.pageNum" :page-size="query.pageSize" @current-change="load" />
    </div>

    <el-drawer v-model="detailVisible" title="日志详情" size="480px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="操作人">{{ current.username }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ current.module }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ current.action }}</el-descriptions-item>
        <el-descriptions-item label="请求">{{ current.method }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ current.ip }}</el-descriptions-item>
        <el-descriptions-item label="参数摘要">{{ current.params || '—' }}</el-descriptions-item>
        <el-descriptions-item label="错误信息">{{ current.errorMsg || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { logApi } from '@/api'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, username: '', module: '' })
const detailVisible = ref(false)
const current = ref({})

const formatTime = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm:ss') : '—')

async function load() {
  loading.value = true
  try {
    const res = await logApi.page(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function onDetail(row) {
  current.value = row
  detailVisible.value = true
}

onMounted(load)
</script>
