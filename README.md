# 基于 Spring Boot 的私人医生预约服务平台

> 毕业设计项目：前后端分离的私人医生预约服务平台，覆盖用户、医生、预约、患者、处方、药品、排班、系统管理八大模块。
> **质量验证：86 项端到端真实数据库接口测试全部通过**，详见 [docs/测试报告.md](docs/测试报告.md)。

## 技术栈

| 端 | 技术 |
|---|---|
| 后端 | Spring Boot 3.3、JDK 21、MyBatis-Plus 3.5、JWT、Knife4j（接口文档）、Lombok |
| 前端 | Vue 3、Vite、Element Plus、Pinia、Vue Router、Axios、ECharts |
| 数据库 | MySQL 5.7 / 8.0（脚本与驱动均兼容） |

## 目录结构

```
├── backend/                 # 后端 Spring Boot 工程
│   ├── src/main/java/com/doctor/appointment/
│   │   ├── common/          # 统一响应、全局异常
│   │   ├── config/          # MyBatis-Plus、跨域、接口文档、数据初始化
│   │   ├── security/        # JWT、登录拦截器、角色注解、用户上下文
│   │   └── modules/         # 业务模块（user/department/schedule/appointment/
│   │                        #   prescription/medication/stats/system）
│   └── src/main/resources/
│       ├── application.yml  # 数据库等配置
│       └── sql/schema.sql   # 建库建表脚本（含基础数据）
├── frontend/                # 前端 Vue3 工程
│   └── src/
│       ├── api/             # Axios 封装与接口定义
│       ├── layout/          # 主布局（侧边栏按角色过滤）
│       ├── router/          # 路由与登录/角色守卫
│       ├── store/           # Pinia 用户状态
│       └── views/           # 各模块页面
└── docs/                    # 设计文档
```

## 快速启动

### 1. 初始化数据库

```bash
mysql -u root -p < backend/src/main/resources/sql/schema.sql
```

修改 `backend/src/main/resources/application.yml` 中的数据库账号密码。

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

- 接口地址：http://localhost:8080
- 接口文档（Knife4j）：http://localhost:8080/doc.html
- 首次启动自动创建初始账号（初始密码均为 **123456**，登录后请及时修改）：
  - 管理员 `admin`
  - 医生 `zhoujianguo` 周建国（已带内科主治医师档案，资质已通过）
  - 患者 `lihuimin` 李慧敏

> 国内网络拉取 Maven 依赖较慢时，可配置阿里云镜像，见 `docs/阿里云Maven镜像settings.xml`。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:5173 （已配置代理转发到 8080）。

## 八大模块功能对照

| 模块 | 子功能 | 实现入口 |
|---|---|---|
| 用户管理 | 注册登录 / 权限管理 / 信息维护 | `/login`、`/users`、`/profile` |
| 医生管理 | 医生归类 / 执业资格审核 / 医德评价 | `/doctors`、`/reviews`、`/evaluations` |
| 预约管理 | 线上预约 / 线下登记 / 预约派单 / 可视化分析 | `/doctors`、`/appointments`、`/dashboard` |
| 患者管理 | 病情归档 / 医生分配 / 就诊管控 | `/records`、`/appointments` |
| 处方管理 | 处方开立 / 状态管控 / 资料归档 | `/prescriptions` |
| 药品管理 | 药品品类 / 供应商资信 / 出入库 | `/medications`、`/suppliers` |
| 排班管理 | 批量出诊规划 / 冲突检查 / 停诊调班 | `/schedules` |
| 系统管理 | 运行日志 / 账号管控 / 系统分析 | `/logs`、`/users`、`/dashboard` |

## 核心业务闭环

```
患者注册 → 浏览医生/号源 → 线上预约（号源原子扣减）
        → 管理员/医生派单（未指定号源的预约进入待派单池）
        → 医生面诊 → 填写病历（预约自动完结，病历归档）
        → 开具电子处方（含药品明细与金额）
        → 患者缴费（模拟）→ 药房发药（事务内扣库存 + 写出入库流水）
        → 患者评价医生（医德评价，可回复）
```

## 业务闭环验证路径

1. `zhoujianguo` 登录 → 排班管理 → 批量排班（本周，上午+下午）
2. `lihuimin` 登录 → 预约挂号 → 选医生选时段 → 提交
3. `zhoujianguo` 登录 → 预约管理 → 填写病历 → 保存归档
4. `zhoujianguo` → 就诊病历 → 开处方（选药品）→ 开立
5. `lihuimin` → 电子处方 → 缴费
6. `admin` → 电子处方 → 发药（库存自动扣减）→ 药品管理查看流水
7. `lihuimin` → 预约管理 → 评价医生
8. `admin` → 数据看板查看 ECharts 统计图表
