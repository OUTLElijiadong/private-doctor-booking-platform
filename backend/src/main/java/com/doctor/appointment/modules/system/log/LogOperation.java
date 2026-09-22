package com.doctor.appointment.modules.system.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解：标注在 Controller 方法上，自动记录运行日志
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogOperation {

    /** 所属模块，如：预约管理 */
    String module();

    /** 操作描述，如：创建预约 */
    String value();
}
