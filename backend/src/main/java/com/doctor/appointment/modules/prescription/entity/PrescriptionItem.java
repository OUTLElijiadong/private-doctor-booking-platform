package com.doctor.appointment.modules.prescription.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 处方明细（一处方多药品）
 */
@Data
@TableName("prescription_item")
public class PrescriptionItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long prescriptionId;

    private Long medicationId;

    /** 药品名称快照 */
    private String medicationName;

    /** 单次剂量，如：1片 */
    private String dosage;

    /** 用法用量说明，如：每日3次，饭后服用，连用7天 */
    private String usageNote;

    /** 开药数量 */
    private Integer quantity;

    /** 开药时单价快照 */
    private BigDecimal unitPrice;

    /** 小计金额 */
    private BigDecimal subtotal;
}
