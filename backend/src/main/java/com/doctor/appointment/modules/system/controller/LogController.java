package com.doctor.appointment.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doctor.appointment.common.Result;
import com.doctor.appointment.modules.system.entity.OperationLog;
import com.doctor.appointment.modules.system.mapper.OperationLogMapper;
import com.doctor.appointment.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运行日志（系统管理）
 */
@Tag(name = "运行日志")
@RestController
@RequestMapping("/api/logs")
@RequireRole("ADMIN")
@RequiredArgsConstructor
public class LogController {

    private final OperationLogMapper operationLogMapper;

    @Operation(summary = "日志分页")
    @GetMapping
    public Result<Page<OperationLog>> page(@RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "10") int pageSize,
                                           @RequestParam(required = false) String username,
                                           @RequestParam(required = false) String module) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .like(StringUtils.hasText(username), OperationLog::getUsername, username)
                .eq(StringUtils.hasText(module), OperationLog::getModule, module)
                .orderByDesc(OperationLog::getCreatedAt);
        return Result.ok(operationLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }
}
