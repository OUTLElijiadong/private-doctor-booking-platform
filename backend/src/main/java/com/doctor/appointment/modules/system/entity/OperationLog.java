package com.doctor.appointment.modules.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志（系统管理-运行日志）
 */
@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    /** 所属模块 */
    private String module;

    /** 操作描述 */
    private String action;

    /** 请求方法，如：POST /api/appointments */
    private String method;

    /** 请求参数摘要 */
    private String params;

    private String ip;

    /** 执行状态：1 成功，0 失败 */
    private Integer status;

    private String errorMsg;

    /** 耗时（毫秒） */
    private Long costMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
