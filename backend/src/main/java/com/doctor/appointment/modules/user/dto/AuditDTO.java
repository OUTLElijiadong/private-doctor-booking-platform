package com.doctor.appointment.modules.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 资质审核请求
 */
@Data
public class AuditDTO {

    @NotNull(message = "审核记录ID不能为空")
    private Long id;

    /** 1 通过，2 驳回 */
    @NotNull(message = "审核结果不能为空")
    private Integer status;

    private String remark;
}
