package com.doctor.appointment.modules.prescription.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 电子处方
 */
@Data
@TableName("prescription")
public class Prescription {

    /** 状态：0 待缴费，1 已缴费，2 已发药，3 已作废 */
    public static final int STATUS_UNPAID = 0;
    public static final int STATUS_PAID = 1;
    public static final int STATUS_DISPENSED = 2;
    public static final int STATUS_VOID = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 处方编号 */
    private String prescriptionNo;

    /** 关联病历 */
    private Long recordId;

    private Long patientId;

    private Long doctorId;

    /** 临床诊断 */
    private String diagnosis;

    /** 医嘱 */
    private String advice;

    /** 处方总金额 */
    private BigDecimal totalAmount;

    /** 状态：0 待缴费，1 已缴费，2 已发药，3 已作废 */
    private Integer status;

    @TableField(exist = false)
    private String patientName;
    @TableField(exist = false)
    private String doctorName;
    /** 处方明细（非数据库列） */
    @TableField(exist = false)
    private List<PrescriptionItem> items;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
