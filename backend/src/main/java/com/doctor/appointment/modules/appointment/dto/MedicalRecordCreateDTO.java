package com.doctor.appointment.modules.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 病历填写请求（医生面诊后归档）
 */
@Data
public class MedicalRecordCreateDTO {

    @NotNull(message = "预约ID不能为空")
    private Long appointmentId;

    /** 主诉 */
    @NotBlank(message = "主诉不能为空")
    private String chiefComplaint;

    /** 诊断结果 */
    @NotBlank(message = "诊断结果不能为空")
    private String diagnosis;

    /** 治疗方案 / 医嘱 */
    private String treatment;
}
