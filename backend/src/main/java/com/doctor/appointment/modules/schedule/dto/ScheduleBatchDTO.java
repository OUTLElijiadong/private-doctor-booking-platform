package com.doctor.appointment.modules.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 批量排班请求：为医生在日期范围内按多个时段批量生成排班
 */
@Data
public class ScheduleBatchDTO {

    /** 医生 userId（医生本人操作时可不传，取当前登录人） */
    private Long doctorId;

    @NotNull(message = "开始日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /** 时段列表，如：["上午 08:00-12:00", "下午 14:00-17:00"] */
    @NotEmpty(message = "请至少选择一个时段")
    private List<String> timeSlots;

    /** 每时段号源数 */
    @NotNull(message = "号源数不能为空")
    @Min(value = 1, message = "号源数至少为1")
    @Max(value = 200, message = "单时段号源数不能超过200")
    private Integer maxCount;
}
