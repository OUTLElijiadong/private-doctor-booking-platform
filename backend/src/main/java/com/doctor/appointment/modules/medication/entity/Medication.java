package com.doctor.appointment.modules.medication.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 药品信息
 */
@Data
@TableName("medication")
public class Medication {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 药品分类 */
    private Long categoryId;

    /** 药品名称 */
    private String name;

    /** 规格，如：0.25g*24片/盒 */
    private String spec;

    /** 单位：盒 / 瓶 / 支 */
    private String unit;

    /** 生产厂家 */
    private String manufacturer;

    /** 供应商 */
    private Long supplierId;

    /** 单价 */
    private BigDecimal price;

    /** 当前库存 */
    private Integer stock;

    /** 库存预警线 */
    private Integer warningStock;

    /** 状态：0 下架，1 在售 */
    private Integer status;

    /** 药品说明 */
    private String description;

    @TableField(exist = false)
    private String categoryName;
    @TableField(exist = false)
    private String supplierName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
