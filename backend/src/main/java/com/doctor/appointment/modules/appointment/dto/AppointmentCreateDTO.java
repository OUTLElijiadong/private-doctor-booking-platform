package com.doctor.appointment.modules.appointment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建预约请求（线上预约 / 线下登记通用）
 */
@Data
public class AppointmentCreateDTO {

    @NotNull(message = "请选择科室")
    private Long departmentId;

    /** 指定医生（可不选，由平台派单） */
    private Long doctorId;

    /** 选择的排班号源（线上预约必传） */
    private Long scheduleId;

    /** 患者ID：线下登记时由工作人员指定；线上预约自动取当前登录人 */
    private Long patientId;

    @NotNull(message = "请选择就诊日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate appointDate;

    private String timeSlot;

    private String symptoms;

    /** 来源：ONLINE / OFFLINE，默认 ONLINE */
    private String source;
}
