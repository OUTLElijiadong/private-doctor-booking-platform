package com.doctor.appointment.modules.medication.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 药品供应商（含资质信息）
 */
@Data
@TableName("supplier")
public class Supplier {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 供应商名称 */
    private String name;

    /** 联系人 */
    private String contact;

    private String phone;

    /** 药品经营许可证号 */
    private String licenseNo;

    /** 经营范围 / 资质说明 */
    private String qualification;

    private String address;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
