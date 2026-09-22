-- ============================================================================
-- 私人医生预约服务平台 数据库脚本
-- 兼容 MySQL 5.7 / 8.0（utf8mb4 字符集，InnoDB 引擎）
-- 初始账号（应用首次启动自动创建，初始密码均为 123456，登录后请及时修改）：
--   admin（管理员）/ zhoujianguo（医生 周建国）/ lihuimin（患者 李慧敏）
-- ============================================================================

CREATE DATABASE IF NOT EXISTS private_doctor_appointment
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE private_doctor_appointment;

-- 命令行导入时必须声明 UTF-8，否则中文会按 latin1 写入产生乱码
SET NAMES utf8mb4;

-- ----------------------------------------------------------------------------
-- 1. 系统用户表（患者/医生/管理员三类角色共用）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT      PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    username        VARCHAR(50) NOT NULL COMMENT '登录账号',
    password        VARCHAR(100) NOT NULL COMMENT '密码（BCrypt）',
    real_name       VARCHAR(50) NOT NULL DEFAULT '' COMMENT '真实姓名',
    role            VARCHAR(20) NOT NULL DEFAULT 'PATIENT' COMMENT '角色：ADMIN/DOCTOR/PATIENT',
    gender          VARCHAR(10) DEFAULT NULL COMMENT '性别',
    phone           VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email           VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    avatar          VARCHAR(500) DEFAULT NULL COMMENT '头像地址',
    status          TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：0冻结 1正常',
    last_login_at   DATETIME    DEFAULT NULL COMMENT '最后登录时间',
    created_at      DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='系统用户表';

-- ----------------------------------------------------------------------------
-- 2. 科室表（医生归类）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS department (
    id          BIGINT      PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL COMMENT '科室名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '科室介绍',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序号',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：0停用 1启用',
    created_at  DATETIME    DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='科室表';

-- ----------------------------------------------------------------------------
-- 3. 医生信息表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS doctor_info (
    id             BIGINT       PRIMARY KEY AUTO_INCREMENT,
    user_id        BIGINT       NOT NULL COMMENT '关联 sys_user.id',
    department_id  BIGINT       DEFAULT NULL COMMENT '所属科室',
    title          VARCHAR(50)  DEFAULT NULL COMMENT '职称',
    specialty      VARCHAR(500) DEFAULT NULL COMMENT '擅长领域',
    introduction   TEXT         DEFAULT NULL COMMENT '简介',
    years          INT          DEFAULT NULL COMMENT '从业年限',
    audit_status   TINYINT      NOT NULL DEFAULT 0 COMMENT '资质审核：0未提交 1待审核 2已通过 3已驳回',
    audit_remark   VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
    created_at     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_department (department_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='医生信息表';

-- ----------------------------------------------------------------------------
-- 4. 医生执业资格审核表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS doctor_review (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
    doctor_id    BIGINT       NOT NULL COMMENT '关联 doctor_info.id',
    cert_name    VARCHAR(100) NOT NULL COMMENT '证书名称',
    cert_no      VARCHAR(100) NOT NULL COMMENT '证书编号',
    material_url VARCHAR(1000) DEFAULT NULL COMMENT '材料附件地址',
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0待审核 1通过 2驳回',
    audit_by     BIGINT       DEFAULT NULL COMMENT '审核人',
    audit_time   DATETIME     DEFAULT NULL COMMENT '审核时间',
    remark       VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    KEY idx_doctor (doctor_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='执业资格审核表';

-- ----------------------------------------------------------------------------
-- 5. 排班表（出诊规划/动态管控）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS schedule (
    id           BIGINT      PRIMARY KEY AUTO_INCREMENT,
    doctor_id    BIGINT      NOT NULL COMMENT '医生 userId',
    work_date    DATE        NOT NULL COMMENT '出诊日期',
    time_slot    VARCHAR(50) NOT NULL COMMENT '时段，如：上午 08:00-12:00',
    max_count    INT         NOT NULL DEFAULT 20 COMMENT '号源总数',
    booked_count INT         NOT NULL DEFAULT 0 COMMENT '已预约数',
    status       TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：0停诊 1正常',
    remark       VARCHAR(500) DEFAULT NULL COMMENT '停诊/调班原因',
    created_at   DATETIME    DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_doctor_date_slot (doctor_id, work_date, time_slot),
    KEY idx_work_date (work_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='医生排班表';

-- ----------------------------------------------------------------------------
-- 6. 预约表（线上/线下统一）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS appointment (
    id             BIGINT       PRIMARY KEY AUTO_INCREMENT,
    appointment_no VARCHAR(40)  NOT NULL COMMENT '预约编号',
    patient_id     BIGINT       NOT NULL COMMENT '患者 userId',
    doctor_id      BIGINT       DEFAULT NULL COMMENT '医生 userId（待派单时为空）',
    department_id  BIGINT       DEFAULT NULL COMMENT '科室',
    schedule_id    BIGINT       DEFAULT NULL COMMENT '关联排班',
    appoint_date   DATE         NOT NULL COMMENT '就诊日期',
    time_slot      VARCHAR(50)  DEFAULT NULL COMMENT '就诊时段',
    symptoms       VARCHAR(1000) DEFAULT NULL COMMENT '症状描述',
    source         VARCHAR(20)  NOT NULL DEFAULT 'ONLINE' COMMENT '来源：ONLINE线上/OFFLINE线下',
    status         TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0待派单 1已派单 2已完成 3已取消',
    cancel_reason  VARCHAR(500) DEFAULT NULL COMMENT '取消原因',
    created_at     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_appointment_no (appointment_no),
    KEY idx_patient (patient_id),
    KEY idx_doctor (doctor_id),
    KEY idx_status (status),
    KEY idx_appoint_date (appoint_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='预约表';

-- ----------------------------------------------------------------------------
-- 7. 电子病历表（病情归档）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS medical_record (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    record_no       VARCHAR(40)  NOT NULL COMMENT '病历编号',
    appointment_id  BIGINT       NOT NULL COMMENT '关联预约',
    patient_id      BIGINT       NOT NULL,
    doctor_id       BIGINT       NOT NULL,
    chief_complaint VARCHAR(1000) NOT NULL COMMENT '主诉',
    diagnosis       VARCHAR(1000) NOT NULL COMMENT '诊断结果',
    treatment       VARCHAR(2000) DEFAULT NULL COMMENT '治疗方案/医嘱',
    visit_time      DATETIME     DEFAULT NULL COMMENT '就诊时间',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_record_no (record_no),
    UNIQUE KEY uk_appointment (appointment_id),
    KEY idx_patient (patient_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='电子病历表';

-- ----------------------------------------------------------------------------
-- 8. 处方表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS prescription (
    id              BIGINT        PRIMARY KEY AUTO_INCREMENT,
    prescription_no VARCHAR(40)   NOT NULL COMMENT '处方编号',
    record_id       BIGINT        NOT NULL COMMENT '关联病历',
    patient_id      BIGINT        NOT NULL,
    doctor_id       BIGINT        NOT NULL,
    diagnosis       VARCHAR(1000) DEFAULT NULL COMMENT '临床诊断',
    advice          VARCHAR(1000) DEFAULT NULL COMMENT '医嘱',
    total_amount    DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '总金额',
    status          TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0待缴费 1已缴费 2已发药 3已作废',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_prescription_no (prescription_no),
    UNIQUE KEY uk_record (record_id),
    KEY idx_patient (patient_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='处方表';

-- ----------------------------------------------------------------------------
-- 9. 处方明细表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS prescription_item (
    id              BIGINT        PRIMARY KEY AUTO_INCREMENT,
    prescription_id BIGINT        NOT NULL,
    medication_id   BIGINT        NOT NULL,
    medication_name VARCHAR(100)  NOT NULL COMMENT '药品名称快照',
    dosage          VARCHAR(100)  DEFAULT NULL COMMENT '单次剂量',
    usage_note      VARCHAR(500)  DEFAULT NULL COMMENT '用法用量',
    quantity        INT           NOT NULL COMMENT '数量',
    unit_price      DECIMAL(10, 2) NOT NULL COMMENT '单价快照',
    subtotal        DECIMAL(10, 2) NOT NULL COMMENT '小计',
    KEY idx_prescription (prescription_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='处方明细表';

-- ----------------------------------------------------------------------------
-- 10. 药品分类表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS medication_category (
    id      BIGINT      PRIMARY KEY AUTO_INCREMENT,
    name    VARCHAR(50) NOT NULL COMMENT '分类名称',
    remark  VARCHAR(500) DEFAULT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='药品分类表';

-- ----------------------------------------------------------------------------
-- 11. 药品供应商表（药品资信管理）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS supplier (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL COMMENT '供应商名称',
    contact       VARCHAR(50)  DEFAULT NULL COMMENT '联系人',
    phone         VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
    license_no    VARCHAR(100) DEFAULT NULL COMMENT '药品经营许可证号',
    qualification VARCHAR(500) DEFAULT NULL COMMENT '资质/经营范围说明',
    address       VARCHAR(200) DEFAULT NULL,
    remark        VARCHAR(500) DEFAULT NULL,
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='药品供应商表';

-- ----------------------------------------------------------------------------
-- 12. 药品信息表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS medication (
    id            BIGINT        PRIMARY KEY AUTO_INCREMENT,
    category_id   BIGINT        DEFAULT NULL COMMENT '分类',
    name          VARCHAR(100)  NOT NULL COMMENT '药品名称',
    spec          VARCHAR(100)  DEFAULT NULL COMMENT '规格',
    unit          VARCHAR(20)   DEFAULT NULL COMMENT '单位',
    manufacturer  VARCHAR(100)  DEFAULT NULL COMMENT '生产厂家',
    supplier_id   BIGINT        DEFAULT NULL COMMENT '供应商',
    price         DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '单价',
    stock         INT           NOT NULL DEFAULT 0 COMMENT '库存',
    warning_stock INT           NOT NULL DEFAULT 10 COMMENT '库存预警线',
    status        TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：0下架 1在售',
    description   VARCHAR(1000) DEFAULT NULL COMMENT '药品说明',
    created_at    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_category (category_id),
    KEY idx_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='药品信息表';

-- ----------------------------------------------------------------------------
-- 13. 药品出入库流水表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS medication_stock_log (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
    medication_id BIGINT      NOT NULL,
    type         VARCHAR(10)  NOT NULL COMMENT 'IN入库/OUT出库',
    quantity     INT          NOT NULL COMMENT '变动数量',
    before_stock INT          NOT NULL COMMENT '变动前库存',
    after_stock  INT          NOT NULL COMMENT '变动后库存',
    ref_type     VARCHAR(20)  DEFAULT NULL COMMENT '来源：PURCHASE采购/PRESCRIPTION处方/ADJUST调整',
    ref_id       BIGINT       DEFAULT NULL COMMENT '关联单据ID',
    remark       VARCHAR(500) DEFAULT NULL,
    created_by   BIGINT       DEFAULT NULL COMMENT '操作人',
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    KEY idx_medication (medication_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='药品出入库流水表';

-- ----------------------------------------------------------------------------
-- 14. 医德评价表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS evaluation (
    id             BIGINT      PRIMARY KEY AUTO_INCREMENT,
    appointment_id BIGINT      NOT NULL COMMENT '关联预约（一次就诊一次评价）',
    patient_id     BIGINT      NOT NULL,
    doctor_id      BIGINT      NOT NULL,
    score          TINYINT     NOT NULL COMMENT '评分1-5',
    content        VARCHAR(1000) DEFAULT NULL COMMENT '评价内容',
    reply          VARCHAR(1000) DEFAULT NULL COMMENT '医生/管理员回复',
    created_at     DATETIME    DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_appointment (appointment_id),
    KEY idx_doctor (doctor_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='医德评价表';

-- ----------------------------------------------------------------------------
-- 15. 操作日志表（运行日志）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS operation_log (
    id         BIGINT       PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT       DEFAULT NULL,
    username   VARCHAR(50)  DEFAULT NULL,
    module     VARCHAR(50)  DEFAULT NULL COMMENT '所属模块',
    action     VARCHAR(100) DEFAULT NULL COMMENT '操作描述',
    method     VARCHAR(255) DEFAULT NULL COMMENT '请求方法',
    params     TEXT         DEFAULT NULL COMMENT '参数摘要（已脱敏）',
    ip         VARCHAR(50)  DEFAULT NULL,
    status     TINYINT      DEFAULT 1 COMMENT '1成功 0失败',
    error_msg  VARCHAR(500) DEFAULT NULL,
    cost_ms    BIGINT       DEFAULT NULL COMMENT '耗时毫秒',
    created_at DATETIME     DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user (user_id),
    KEY idx_module (module),
    KEY idx_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='操作日志表';

-- ----------------------------------------------------------------------------
-- 基础数据（账号由应用启动时自动创建，此处仅初始化科室/分类/药品/供应商资料）
-- ----------------------------------------------------------------------------
INSERT INTO department (name, description, sort, status)
SELECT * FROM (
    SELECT '内科'   AS name, '普通内科疾病诊治' AS description, 1 AS sort, 1 AS status
    UNION ALL SELECT '外科', '普通外科疾病诊治', 2, 1
    UNION ALL SELECT '儿科', '儿童疾病诊治', 3, 1
    UNION ALL SELECT '妇科', '妇科疾病诊治', 4, 1
    UNION ALL SELECT '眼科', '眼部疾病诊治', 5, 1
    UNION ALL SELECT '口腔科', '口腔疾病诊治', 6, 1
    UNION ALL SELECT '皮肤科', '皮肤疾病诊治', 7, 1
    UNION ALL SELECT '中医科', '中医调理与诊治', 8, 1
) t
WHERE NOT EXISTS (SELECT 1 FROM department LIMIT 1);

INSERT INTO medication_category (name, remark)
SELECT * FROM (
    SELECT '感冒用药' AS name, '感冒、发热相关药品' AS remark
    UNION ALL SELECT '消炎镇痛', '抗生素与镇痛类药品'
    UNION ALL SELECT '心脑血管', '降压、降脂等药品'
    UNION ALL SELECT '消化系统', '肠胃用药'
    UNION ALL SELECT '外用药品', '外用消毒、药膏等'
    UNION ALL SELECT '维生素类', '维生素与营养补充剂'
) t
WHERE NOT EXISTS (SELECT 1 FROM medication_category LIMIT 1);

INSERT INTO supplier (name, contact, phone, license_no, qualification, address)
SELECT '黑龙江康泰医药有限公司', '王经理', '0451-88888888', '黑药经营许20230001',
       '药品批发（中成药、化学药制剂、抗生素）', '哈尔滨市南岗区学府路100号'
WHERE NOT EXISTS (SELECT 1 FROM supplier LIMIT 1);

INSERT INTO medication (category_id, name, spec, unit, manufacturer, supplier_id, price, stock, warning_stock, status, description)
SELECT * FROM (
    SELECT 1 AS category_id, '感冒灵颗粒' AS name, '10g*9袋/盒' AS spec, '盒' AS unit,
           '华润三九医药' AS manufacturer, 1 AS supplier_id, 15.80 AS price, 200 AS stock, 20 AS warning_stock, 1 AS status,
           '用于感冒引起的头痛、发热、鼻塞、流涕' AS description
    UNION ALL SELECT 1, '布洛芬缓释胶囊', '0.3g*20粒/盒', '盒', '中美天津史克制药', 1, 18.50, 150, 20, 1, '解热镇痛'
    UNION ALL SELECT 2, '阿莫西林胶囊', '0.25g*24粒/盒', '盒', '哈药集团', 1, 12.00, 300, 30, 1, '抗生素，需凭处方使用'
    UNION ALL SELECT 3, '硝苯地平控释片', '30mg*7片/盒', '盒', '拜耳医药', 1, 32.00, 100, 15, 1, '高血压用药'
    UNION ALL SELECT 4, '奥美拉唑肠溶胶囊', '20mg*14粒/盒', '盒', '阿斯利康', 1, 28.00, 120, 15, 1, '胃酸相关疾病'
    UNION ALL SELECT 6, '维生素C片', '0.1g*100片/瓶', '瓶', '东北制药', 1, 8.50, 500, 50, 1, '补充维生素C'
) t
WHERE NOT EXISTS (SELECT 1 FROM medication LIMIT 1);
