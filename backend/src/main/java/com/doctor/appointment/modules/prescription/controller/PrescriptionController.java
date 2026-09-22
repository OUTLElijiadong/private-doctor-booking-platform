package com.doctor.appointment.modules.prescription.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doctor.appointment.common.Result;
import com.doctor.appointment.modules.prescription.dto.PrescriptionCreateDTO;
import com.doctor.appointment.modules.prescription.entity.Prescription;
import com.doctor.appointment.modules.prescription.service.PrescriptionService;
import com.doctor.appointment.modules.system.log.LogOperation;
import com.doctor.appointment.security.RequireRole;
import com.doctor.appointment.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 处方管理：开立、缴费、发药、作废、归档
 */
@Tag(name = "处方管理")
@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @Operation(summary = "医生开具处方")
    @LogOperation(module = "处方管理", value = "开具电子处方")
    @RequireRole("DOCTOR")
    @PostMapping
    public Result<Prescription> create(@Valid @RequestBody PrescriptionCreateDTO dto) {
        return Result.ok(prescriptionService.create(UserContext.getUserId(), dto));
    }

    @Operation(summary = "患者缴费（模拟）")
    @LogOperation(module = "处方管理", value = "处方缴费")
    @RequireRole("PATIENT")
    @PutMapping("/{id}/pay")
    public Result<Void> pay(@PathVariable Long id) {
        prescriptionService.pay(id, UserContext.getUserId());
        return Result.ok();
    }

    @Operation(summary = "发药（扣减库存，管理员/医生）")
    @LogOperation(module = "处方管理", value = "处方发药出库")
    @RequireRole({"ADMIN", "DOCTOR"})
    @PutMapping("/{id}/dispense")
    public Result<Void> dispense(@PathVariable Long id) {
        prescriptionService.dispense(id, UserContext.getUserId());
        return Result.ok();
    }

    @Operation(summary = "作废处方（医生/管理员）")
    @LogOperation(module = "处方管理", value = "作废处方")
    @RequireRole({"DOCTOR", "ADMIN"})
    @PutMapping("/{id}/void")
    public Result<Void> cancel(@PathVariable Long id) {
        prescriptionService.cancel(id, UserContext.getUserId(), UserContext.getRole());
        return Result.ok();
    }

    @Operation(summary = "处方分页（按角色隔离数据）")
    @GetMapping
    public Result<Page<Prescription>> page(@RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "10") int pageSize,
                                           @RequestParam(required = false) Integer status) {
        return Result.ok(prescriptionService.pagePrescriptions(pageNum, pageSize, status,
                UserContext.getRole(), UserContext.getUserId()));
    }

    @Operation(summary = "处方详情（含明细）")
    @GetMapping("/{id}")
    public Result<Prescription> detail(@PathVariable Long id) {
        return Result.ok(prescriptionService.detail(id, UserContext.getRole(), UserContext.getUserId()));
    }
}
