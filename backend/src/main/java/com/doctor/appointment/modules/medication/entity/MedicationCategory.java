package com.doctor.appointment.modules.medication.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 药品分类
 */
@Data
@TableName("medication_category")
public class MedicationCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类名称，如：感冒用药 / 消炎镇痛 / 心脑血管 */
    private String name;

    private String remark;
}
