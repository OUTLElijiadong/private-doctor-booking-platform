package com.doctor.appointment.modules.user.dto;

import com.doctor.appointment.modules.user.entity.SysUser;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应
 */
@Data
@AllArgsConstructor
public class LoginVO {

    private String token;

    private SysUser userInfo;
}
