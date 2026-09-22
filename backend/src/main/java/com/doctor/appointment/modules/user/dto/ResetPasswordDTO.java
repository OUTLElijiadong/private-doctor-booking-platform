package com.doctor.appointment.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 找回密码请求（通过账号 + 预留手机号验证身份）
 */
@Data
public class ResetPasswordDTO {

    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "新密码长度需为6-20位")
    private String newPassword;
}
