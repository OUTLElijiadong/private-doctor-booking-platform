package com.doctor.appointment.modules.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 医德评价（患者对已完成就诊的医生的评价）
 */
@Data
@TableName("evaluation")
public class Evaluation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 appointment.id（一次就诊只能评价一次） */
    private Long appointmentId;

    private Long patientId;

    private Long doctorId;

    /** 评分 1-5 */
    private Integer score;

    /** 评价内容 */
    private String content;

    /** 医生/管理员回复 */
    private String reply;

    @TableField(exist = false)
    private String patientName;
    @TableField(exist = false)
    private String doctorName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
