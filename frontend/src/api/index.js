import request from './request'

// ---------------- 公开数据（无需登录） ----------------
export const publicApi = {
  platformStats: () => request.get('/api/public/platform-stats')
}

// ---------------- 认证 ----------------
export const authApi = {
  login: (data) => request.post('/api/auth/login', data),
  register: (data) => request.post('/api/auth/register', data),
  resetPassword: (data) => request.post('/api/auth/password/reset', data),
  profile: () => request.get('/api/auth/profile'),
  updateProfile: (data) => request.put('/api/auth/profile', data),
  changePassword: (data) => request.put('/api/auth/password', data)
}

// ---------------- 用户管理 ----------------
export const userApi = {
  page: (params) => request.get('/api/users', { params }),
  create: (data) => request.post('/api/users', data),
  update: (data) => request.put('/api/users', data),
  changeStatus: (id, status) => request.put(`/api/users/${id}/status`, { status }),
  resetPassword: (id) => request.put(`/api/users/${id}/password/reset`),
  remove: (id) => request.delete(`/api/users/${id}`),
  // 患者下拉（线下登记用，医生/管理员均可访问）
  patientOptions: () => request.get('/api/users/patients')
}

// ---------------- 科室 ----------------
export const departmentApi = {
  enabled: () => request.get('/api/departments/enabled'),
  list: () => request.get('/api/departments'),
  create: (data) => request.post('/api/departments', data),
  update: (data) => request.put('/api/departments', data),
  remove: (id) => request.delete(`/api/departments/${id}`)
}

// ---------------- 医生 ----------------
export const doctorApi = {
  page: (params) => request.get('/api/doctors', { params }),
  detail: (id) => request.get(`/api/doctors/${id}`),
  mine: () => request.get('/api/doctors/mine'),
  updateProfile: (data) => request.put('/api/doctors/profile', data)
}

// ---------------- 资质审核 ----------------
export const reviewApi = {
  submit: (data) => request.post('/api/doctor-reviews', data),
  audit: (data) => request.post('/api/doctor-reviews/audit', data),
  page: (params) => request.get('/api/doctor-reviews', { params })
}

// ---------------- 排班 ----------------
export const scheduleApi = {
  page: (params) => request.get('/api/schedules', { params }),
  batch: (data) => request.post('/api/schedules/batch', data),
  available: (params) => request.get('/api/schedules/available', { params }),
  changeStatus: (id, status, remark) => request.put(`/api/schedules/${id}/status`, { status, remark }),
  remove: (id) => request.delete(`/api/schedules/${id}`)
}

// ---------------- 预约 ----------------
export const appointmentApi = {
  page: (params) => request.get('/api/appointments', { params }),
  online: (data) => request.post('/api/appointments/online', data),
  offline: (data) => request.post('/api/appointments/offline', data),
  assign: (data) => request.post('/api/appointments/assign', data),
  cancel: (id, reason) => request.put(`/api/appointments/${id}/cancel`, { reason }),
  complete: (id) => request.put(`/api/appointments/${id}/complete`),
  detail: (id) => request.get(`/api/appointments/${id}`)
}

// ---------------- 病历 ----------------
export const recordApi = {
  page: (params) => request.get('/api/records', { params }),
  create: (data) => request.post('/api/records', data),
  detail: (id) => request.get(`/api/records/${id}`)
}

// ---------------- 处方 ----------------
export const prescriptionApi = {
  page: (params) => request.get('/api/prescriptions', { params }),
  create: (data) => request.post('/api/prescriptions', data),
  pay: (id) => request.put(`/api/prescriptions/${id}/pay`),
  dispense: (id) => request.put(`/api/prescriptions/${id}/dispense`),
  void: (id) => request.put(`/api/prescriptions/${id}/void`),
  detail: (id) => request.get(`/api/prescriptions/${id}`)
}

// ---------------- 药品 ----------------
export const medicationApi = {
  page: (params) => request.get('/api/medications', { params }),
  onSale: () => request.get('/api/medications/on-sale'),
  create: (data) => request.post('/api/medications', data),
  update: (data) => request.put('/api/medications', data),
  remove: (id) => request.delete(`/api/medications/${id}`),
  changeStock: (id, quantity, remark) => request.post(`/api/medications/${id}/stock`, { quantity, remark }),
  stockLogs: (params) => request.get('/api/medications/stock-logs', { params }),
  categories: () => request.get('/api/medications/categories'),
  createCategory: (data) => request.post('/api/medications/categories', data),
  updateCategory: (data) => request.put('/api/medications/categories', data),
  removeCategory: (id) => request.delete(`/api/medications/categories/${id}`)
}

// ---------------- 供应商 ----------------
export const supplierApi = {
  page: (params) => request.get('/api/suppliers', { params }),
  options: () => request.get('/api/suppliers/options'),
  create: (data) => request.post('/api/suppliers', data),
  update: (data) => request.put('/api/suppliers', data),
  remove: (id) => request.delete(`/api/suppliers/${id}`)
}

// ---------------- 评价 ----------------
export const evaluationApi = {
  page: (params) => request.get('/api/evaluations', { params }),
  create: (data) => request.post('/api/evaluations', data),
  reply: (id, reply) => request.put(`/api/evaluations/${id}/reply`, { reply })
}

// ---------------- 统计分析 ----------------
export const statsApi = {
  overview: () => request.get('/api/stats/overview'),
  appointmentTrend: () => request.get('/api/stats/appointment-trend'),
  departmentDistribution: () => request.get('/api/stats/department-distribution'),
  doctorWorkload: () => request.get('/api/stats/doctor-workload'),
  medicationConsumption: () => request.get('/api/stats/medication-consumption'),
  doctorOverview: () => request.get('/api/stats/doctor-overview'),
  doctorTrend: () => request.get('/api/stats/doctor-trend'),
  patientOverview: () => request.get('/api/stats/patient-overview')
}

// ---------------- 日志 ----------------
export const logApi = {
  page: (params) => request.get('/api/logs', { params })
}
