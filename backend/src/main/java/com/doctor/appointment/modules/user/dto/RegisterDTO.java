package com.doctor.appointment.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求（默认患者账号；选择医生身份注册后需提交执业资质审核，通过后方可出诊）
 */
@Data
public class RegisterDTO {

    @NotBlank(message = "账号不能为空")
    @Size(min = 4, max = 20, message = "账号长度需为4-20位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需为6-20位")
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    private String gender;

    /** 注册身份：PATIENT 患者（默认）/ DOCTOR 医生（注册后需提交执业资质审核） */
    private String role;
}
