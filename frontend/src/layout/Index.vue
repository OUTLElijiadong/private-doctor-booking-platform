<template>
  <el-container class="layout">
    <!-- 侧边栏 -->
    <el-aside :width="collapse ? '72px' : '232px'" class="aside">
      <div class="logo" @click="$router.push('/dashboard')">
        <div class="logo-icon">
          <el-icon :size="22" color="#fff"><FirstAidKit /></el-icon>
        </div>
        <transition name="fade">
          <div v-show="!collapse" class="logo-text">
            <span class="name">私人医生平台</span>
            <span class="sub">Appointment Cloud</span>
          </div>
        </transition>
      </div>

      <el-scrollbar class="menu-scroll">
        <div v-for="group in menuGroups" :key="group.label" class="menu-group">
          <div v-show="!collapse && group.items.some(i => visible(i))" class="group-label">{{ group.label }}</div>
          <div
            v-for="item in group.items"
            :key="item.path"
            v-show="visible(item)"
            class="menu-item"
            :class="{ active: $route.path === item.path }"
            @click="$router.push(item.path)"
          >
            <el-icon :size="18"><component :is="item.icon" /></el-icon>
            <transition name="fade">
              <span v-show="!collapse">{{ item.title }}</span>
            </transition>
          </div>
        </div>
      </el-scrollbar>

      <div class="aside-footer" v-show="!collapse">
        <div class="role-card">
          <div class="role-dot" :class="store.role.toLowerCase()"></div>
          <div class="role-name">{{ roleName }}</div>
        </div>
      </div>
    </el-aside>

    <!-- 主区域 -->
    <el-container class="main-wrap">
      <el-header class="header">
        <div class="left">
          <el-icon class="collapse-btn" @click="collapse = !collapse">
            <Fold v-if="!collapse" /><Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>工作台</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="right">
          <el-tag :type="roleTagType" size="small" effect="light" round>{{ roleName }}</el-tag>
          <el-dropdown @command="onCommand">
            <div class="user-info">
              <el-avatar :size="32" :src="store.userInfo.avatar" class="avatar">
                {{ store.realName.charAt(0) }}
              </el-avatar>
              <div class="meta">
                <span class="name">{{ store.realName }}</span>
                <span class="account">@{{ store.userInfo.username }}</span>
              </div>
              <el-icon size="12" color="#94a3b8"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><Setting /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const router = useRouter()
const collapse = ref(false)

const menuGroups = [
  {
    label: '总览',
    items: [{ path: '/dashboard', title: '数据看板', icon: 'Odometer' }]
  },
  {
    label: '诊疗服务',
    items: [
      { path: '/doctors', title: '预约挂号', icon: 'FirstAidKit', roles: ['PATIENT'] },
      { path: '/appointments', title: '预约管理', icon: 'Calendar' },
      { path: '/records', title: '就诊病历', icon: 'Document' },
      { path: '/prescriptions', title: '电子处方', icon: 'Tickets' },
      { path: '/evaluations', title: '医德评价', icon: 'Star' }
    ]
  },
  {
    label: '医生工作',
    items: [
      { path: '/schedules', title: '排班管理', icon: 'Clock', roles: ['DOCTOR', 'ADMIN'] },
      { path: '/reviews', title: '资质审核', icon: 'Stamp', roles: ['DOCTOR', 'ADMIN'] }
    ]
  },
  {
    label: '平台管理',
    items: [
      { path: '/departments', title: '科室管理', icon: 'OfficeBuilding', roles: ['ADMIN'] },
      { path: '/medications', title: '药品管理', icon: 'Box', roles: ['ADMIN'] },
      { path: '/suppliers', title: '供应商管理', icon: 'Van', roles: ['ADMIN'] },
      { path: '/users', title: '用户管理', icon: 'User', roles: ['ADMIN'] },
      { path: '/logs', title: '运行日志', icon: 'Memo', roles: ['ADMIN'] }
    ]
  },
  {
    label: '我的',
    items: [{ path: '/profile', title: '个人中心', icon: 'Setting' }]
  }
]

const visible = (item) => !item.roles || item.roles.includes(store.role)
const roleName = computed(() => ({ ADMIN: '管理员', DOCTOR: '医生', PATIENT: '患者' }[store.role] || '用户'))
const roleTagType = computed(() => ({ ADMIN: 'danger', DOCTOR: 'success', PATIENT: 'primary' }[store.role] || 'info'))

function onCommand(cmd) {
  if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' }).then(() => {
      store.logout()
      router.push('/login')
    }).catch(() => {})
  }
}
</script>

<style scoped>
.layout {
  height: 100%;
}

/* ---------- 侧边栏 ---------- */
.aside {
  background: var(--surface);
  border-right: 1px solid var(--gray-200);
  transition: width 0.25s var(--ease);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 18px;
  cursor: pointer;
  border-bottom: 1px solid var(--gray-100);
}

.logo-icon {
  width: 36px;
  height: 36px;
  min-width: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--brand-500), #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.35);
}

.logo-text {
  display: flex;
  flex-direction: column;
  white-space: nowrap;
}

.logo-text .name {
  font-size: 15px;
  font-weight: 700;
  color: var(--gray-900);
}

.logo-text .sub {
  font-size: 11px;
  color: var(--gray-400);
}

.menu-scroll {
  flex: 1;
  padding: 8px 12px;
}

.menu-group {
  margin-bottom: 4px;
}

.group-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--gray-400);
  padding: 14px 10px 6px;
  letter-spacing: 0.05em;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 10px;
  margin: 2px 0;
  border-radius: 8px;
  font-size: 14px;
  color: var(--gray-600);
  cursor: pointer;
  transition: all var(--duration) var(--ease);
  white-space: nowrap;
}

.menu-item:hover {
  background: var(--gray-100);
  color: var(--gray-900);
}

.menu-item.active {
  background: var(--brand-50);
  color: var(--brand-600);
  font-weight: 600;
}

.menu-item.active .el-icon {
  color: var(--brand-600);
}

.aside-footer {
  padding: 12px;
  border-top: 1px solid var(--gray-100);
}

.role-card {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--gray-50);
}

.role-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--gray-400);
}
.role-dot.admin { background: var(--danger); }
.role-dot.doctor { background: var(--success); }
.role-dot.patient { background: var(--info); }

.role-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--gray-700);
}

/* ---------- 顶栏 ---------- */
.header {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--gray-200);
  height: 60px;
  padding: 0 20px;
}

.left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  cursor: pointer;
  font-size: 18px;
  color: var(--gray-500);
  transition: color var(--duration);
}
.collapse-btn:hover {
  color: var(--brand-500);
}

.right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 5px 8px;
  border-radius: 10px;
  transition: background var(--duration);
}
.user-info:hover {
  background: var(--gray-100);
}

.avatar {
  background: linear-gradient(135deg, var(--brand-500), #8b5cf6);
  color: #fff;
  font-weight: 600;
}

.user-info .meta {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}
.user-info .name {
  font-size: 13px;
  font-weight: 600;
  color: var(--gray-800);
}
.user-info .account {
  font-size: 11px;
  color: var(--gray-400);
}

.main {
  padding: 20px;
  overflow-y: auto;
}

/* ---------- 页面切换动效 ---------- */
.page-enter-active,
.page-leave-active {
  transition: opacity 0.18s var(--ease), transform 0.18s var(--ease);
}
.page-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.page-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
