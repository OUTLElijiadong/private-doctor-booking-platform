package com.doctor.appointment.modules.appointment.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 电子病历（就诊记录，一次就诊一条，永久归档）
 */
@Data
@TableName("medical_record")
public class MedicalRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 病历编号 */
    private String recordNo;

    /** 关联 appointment.id */
    private Long appointmentId;

    private Long patientId;

    private Long doctorId;

    /** 主诉 */
    private String chiefComplaint;

    /** 诊断结果 */
    private String diagnosis;

    /** 治疗方案 / 医嘱 */
    private String treatment;

    /** 就诊时间 */
    private LocalDateTime visitTime;

    @TableField(exist = false)
    private String patientName;
    @TableField(exist = false)
    private String doctorName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
