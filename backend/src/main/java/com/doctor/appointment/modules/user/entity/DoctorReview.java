package com.doctor.appointment.modules.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 医生执业资格审核记录
 */
@Data
@TableName("doctor_review")
public class DoctorReview {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 doctor_info.id */
    private Long doctorId;

    /** 证书名称，如：医师执业证书 */
    private String certName;

    /** 证书编号 */
    private String certNo;

    /** 材料附件地址（可多个，逗号分隔） */
    private String materialUrl;

    /** 审核状态：0 待审核，1 通过，2 驳回 */
    private Integer status;

    /** 审核人（管理员 userId） */
    private Long auditBy;

    private LocalDateTime auditTime;

    /** 审核意见 */
    private String remark;

    @TableField(exist = false)
    private String doctorName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
