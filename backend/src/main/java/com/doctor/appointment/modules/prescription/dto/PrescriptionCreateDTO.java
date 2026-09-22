package com.doctor.appointment.modules.prescription.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 开具处方请求（含药品明细）
 */
@Data
public class PrescriptionCreateDTO {

    /** 关联病历ID */
    @NotNull(message = "病历ID不能为空")
    private Long recordId;

    /** 临床诊断 */
    private String diagnosis;

    /** 医嘱 */
    private String advice;

    @NotEmpty(message = "处方药品不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull(message = "药品不能为空")
        private Long medicationId;
        /** 单次剂量，如：1片 */
        private String dosage;
        /** 用法用量，如：每日3次，饭后服用 */
        private String usageNote;
        @NotNull(message = "数量不能为空")
        private Integer quantity;
    }
}
