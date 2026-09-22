package com.doctor.appointment.modules.medication.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 药品出入库记录
 */
@Data
@TableName("medication_stock_log")
public class MedicationStockLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long medicationId;

    /** 类型：IN 入库 / OUT 出库 */
    private String type;

    /** 变动数量（正数） */
    private Integer quantity;

    /** 变动前库存 */
    private Integer beforeStock;

    /** 变动后库存 */
    private Integer afterStock;

    /** 业务来源：PURCHASE 采购入库 / PRESCRIPTION 处方发药 / ADJUST 人工调整 */
    private String refType;

    /** 关联单据ID（如处方ID） */
    private Long refId;

    private String remark;

    /** 操作人 userId */
    private Long createdBy;

    @TableField(exist = false)
    private String medicationName;
    @TableField(exist = false)
    private String operatorName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
