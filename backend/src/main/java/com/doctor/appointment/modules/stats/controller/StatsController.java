package com.doctor.appointment.modules.stats.controller;

import com.doctor.appointment.common.Result;
import com.doctor.appointment.modules.stats.service.StatsService;
import com.doctor.appointment.security.RequireRole;
import com.doctor.appointment.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 统计分析（预约可视化 + 系统综合分析）
 */
@Tag(name = "统计分析")
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @RequireRole("ADMIN")
    @Operation(summary = "总览卡片")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(statsService.overview());
    }

    @RequireRole("ADMIN")
    @Operation(summary = "近7天预约趋势")
    @GetMapping("/appointment-trend")
    public Result<List<Map<String, Object>>> appointmentTrend() {
        return Result.ok(statsService.appointmentTrend());
    }

    @RequireRole("ADMIN")
    @Operation(summary = "科室预约分布")
    @GetMapping("/department-distribution")
    public Result<List<Map<String, Object>>> departmentDistribution() {
        return Result.ok(statsService.departmentDistribution());
    }

    @RequireRole("ADMIN")
    @Operation(summary = "医生接诊量TOP10")
    @GetMapping("/doctor-workload")
    public Result<List<Map<String, Object>>> doctorWorkload() {
        return Result.ok(statsService.doctorWorkload());
    }

    @RequireRole("ADMIN")
    @Operation(summary = "药品消耗TOP10")
    @GetMapping("/medication-consumption")
    public Result<List<Map<String, Object>>> medicationConsumption() {
        return Result.ok(statsService.medicationConsumption());
    }

    @RequireRole("DOCTOR")
    @Operation(summary = "医生工作台数据")
    @GetMapping("/doctor-overview")
    public Result<Map<String, Object>> doctorOverview() {
        return Result.ok(statsService.doctorOverview(UserContext.getUserId()));
    }

    @RequireRole("DOCTOR")
    @Operation(summary = "医生近7天接诊趋势")
    @GetMapping("/doctor-trend")
    public Result<List<Map<String, Object>>> doctorTrend() {
        return Result.ok(statsService.doctorTrend(UserContext.getUserId()));
    }

    @RequireRole("PATIENT")
    @Operation(summary = "患者首页数据")
    @GetMapping("/patient-overview")
    public Result<Map<String, Object>> patientOverview() {
        return Result.ok(statsService.patientOverview(UserContext.getUserId()));
    }
}
