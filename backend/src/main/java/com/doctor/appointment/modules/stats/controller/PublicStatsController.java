package com.doctor.appointment.modules.stats.controller;

import com.doctor.appointment.common.Result;
import com.doctor.appointment.modules.stats.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 公开平台数据（登录页展示，仅返回聚合统计数字）
 */
@Tag(name = "公开平台数据")
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicStatsController {

    private final StatsService statsService;

    @Operation(summary = "平台实时统计（在册医生/开诊科室/累计预约）")
    @GetMapping("/platform-stats")
    public Result<Map<String, Object>> platformStats() {
        return Result.ok(statsService.publicStats());
    }
}
