package com.doctor.appointment.security;

import lombok.Data;

/**
 * 当前登录用户（ThreadLocal 上下文）
 */
@Data
public class LoginUser {

    private Long userId;
    private String username;
    private String realName;
    private String role;
}
