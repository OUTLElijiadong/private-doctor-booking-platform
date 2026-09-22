package com.doctor.appointment.modules.appointment.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约订单
 */
@Data
@TableName("appointment")
public class Appointment {

    /** 状态：0 待派单，1 已派单，2 已完成，3 已取消 */
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_ASSIGNED = 1;
    public static final int STATUS_COMPLETED = 2;
    public static final int STATUS_CANCELLED = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预约编号 */
    private String appointmentNo;

    /** 患者 userId */
    private Long patientId;

    /** 医生 userId（待派单时可为空） */
    private Long doctorId;

    /** 科室 */
    private Long departmentId;

    /** 关联排班（可为空，如线下直接登记） */
    private Long scheduleId;

    /** 就诊日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate appointDate;

    /** 就诊时段 */
    private String timeSlot;

    /** 症状描述 */
    private String symptoms;

    /** 来源：ONLINE 线上 / OFFLINE 线下 */
    private String source;

    /** 状态：0 待派单，1 已派单，2 已完成，3 已取消 */
    private Integer status;

    /** 取消原因 */
    private String cancelReason;

    /** 表外展示字段 */
    @TableField(exist = false)
    private String patientName;
    @TableField(exist = false)
    private String doctorName;
    @TableField(exist = false)
    private String departmentName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
