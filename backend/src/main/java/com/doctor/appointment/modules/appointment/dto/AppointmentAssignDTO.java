package com.doctor.appointment.modules.appointment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 预约派单请求（管理员/医生将待派单订单分配给指定医生与时段）
 */
@Data
public class AppointmentAssignDTO {

    @NotNull(message = "预约ID不能为空")
    private Long appointmentId;

    @NotNull(message = "请选择接诊医生")
    private Long doctorId;

    @NotNull(message = "请选择就诊日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate appointDate;

    @NotNull(message = "请填写就诊时段")
    private String timeSlot;
}
