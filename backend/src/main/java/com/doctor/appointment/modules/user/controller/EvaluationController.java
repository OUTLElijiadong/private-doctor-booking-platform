package com.doctor.appointment.modules.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doctor.appointment.common.Result;
import com.doctor.appointment.modules.system.log.LogOperation;
import com.doctor.appointment.modules.user.entity.Evaluation;
import com.doctor.appointment.modules.user.service.EvaluationService;
import com.doctor.appointment.security.RequireRole;
import com.doctor.appointment.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 医德评价
 */
@Tag(name = "医德评价")
@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @Operation(summary = "患者提交评价")
    @LogOperation(module = "医生管理", value = "提交医德评价")
    @RequireRole("PATIENT")
    @PostMapping
    public Result<Void> create(@RequestBody Evaluation evaluation) {
        evaluationService.create(UserContext.getUserId(), evaluation);
        return Result.ok();
    }

    @Operation(summary = "回复评价")
    @RequireRole({"DOCTOR", "ADMIN"})
    @PutMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable Long id, @RequestBody Map<String, String> body) {
        boolean isAdmin = "ADMIN".equals(UserContext.getRole());
        evaluationService.reply(id, body.get("reply"), UserContext.getUserId(), isAdmin);
        return Result.ok();
    }

    @Operation(summary = "评价分页（按角色隔离数据）")
    @GetMapping
    public Result<Page<Evaluation>> page(@RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "10") int pageSize,
                                         @RequestParam(required = false) Long doctorId) {
        return Result.ok(evaluationService.pageEvaluations(pageNum, pageSize, doctorId,
                UserContext.getUserId(), UserContext.getRole()));
    }
}
