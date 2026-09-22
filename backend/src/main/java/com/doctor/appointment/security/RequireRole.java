package com.doctor.appointment.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色访问控制注解，标注在 Controller 类或方法上
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /** 允许的角色：ADMIN 管理员 / DOCTOR 医生 / PATIENT 患者 */
    String[] value();
}
