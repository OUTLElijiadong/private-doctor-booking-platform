package com.doctor.appointment.modules.schedule.entity;

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
 * 医生出诊排班
 */
@Data
@TableName("schedule")
public class Schedule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 医生 userId */
    private Long doctorId;

    /** 出诊日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate workDate;

    /** 时段，如：上午 08:00-12:00 / 下午 14:00-17:00 / 晚上 18:00-21:00 */
    private String timeSlot;

    /** 号源总数 */
    private Integer maxCount;

    /** 已预约数量 */
    private Integer bookedCount;

    /** 状态：0 停诊，1 正常 */
    private Integer status;

    /** 备注（停诊/调班原因） */
    private String remark;

    @TableField(exist = false)
    private String doctorName;
    @TableField(exist = false)
    private String departmentName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
