package com.doctor.appointment.modules.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 医生信息（与 sys_user 一对一，承载执业相关信息）
 */
@Data
@TableName("doctor_info")
public class DoctorInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 sys_user.id */
    private Long userId;

    /** 所属科室 */
    private Long departmentId;

    /** 职称：主任医师 / 副主任医师 / 主治医师 / 住院医师 */
    private String title;

    /** 擅长领域 */
    private String specialty;

    /** 医生简介 */
    private String introduction;

    /** 从业年限 */
    private Integer years;

    /** 资质审核状态：0 未提交，1 待审核，2 已通过，3 已驳回 */
    private Integer auditStatus;

    /** 审核备注 */
    private String auditRemark;

    /** 表外展示字段（非数据库列） */
    @TableField(exist = false)
    private String realName;
    @TableField(exist = false)
    private String departmentName;
    @TableField(exist = false)
    private String avatar;
    @TableField(exist = false)
    private Double avgScore;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
