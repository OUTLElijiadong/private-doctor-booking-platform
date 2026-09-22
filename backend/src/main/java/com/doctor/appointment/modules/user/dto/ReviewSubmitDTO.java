package com.doctor.appointment.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 医生资质材料提交请求
 */
@Data
public class ReviewSubmitDTO {

    @NotBlank(message = "证书名称不能为空")
    private String certName;

    @NotBlank(message = "证书编号不能为空")
    private String certNo;

    /** 材料附件地址，可多个逗号分隔 */
    private String materialUrl;
}
