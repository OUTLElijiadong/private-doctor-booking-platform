package com.doctor.appointment.modules.appointment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doctor.appointment.common.Result;
import com.doctor.appointment.modules.appointment.dto.AppointmentAssignDTO;
import com.doctor.appointment.modules.appointment.dto.AppointmentCreateDTO;
import com.doctor.appointment.modules.appointment.entity.Appointment;
import com.doctor.appointment.modules.appointment.service.AppointmentService;
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

import java.util.Map;

/**
 * 预约管理：线上预约、线下登记、派单、取消、完成
 */
@Tag(name = "预约管理")
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @Operation(summary = "线上预约（患者）")
    @LogOperation(module = "预约管理", value = "线上预约")
    @RequireRole("PATIENT")
    @PostMapping("/online")
    public Result<Appointment> createOnline(@Valid @RequestBody AppointmentCreateDTO dto) {
        return Result.ok(appointmentService.create(dto, UserContext.getUserId(), true));
    }

    @Operation(summary = "线下登记预约（管理员/医生）")
    @LogOperation(module = "预约管理", value = "线下登记预约")
    @RequireRole({"ADMIN", "DOCTOR"})
    @PostMapping("/offline")
    public Result<Appointment> createOffline(@Valid @RequestBody AppointmentCreateDTO dto) {
        return Result.ok(appointmentService.create(dto, UserContext.getUserId(), false));
    }

    @Operation(summary = "预约派单（管理员/医生）")
    @LogOperation(module = "预约管理", value = "预约派单")
    @RequireRole({"ADMIN", "DOCTOR"})
    @PostMapping("/assign")
    public Result<Void> assign(@Valid @RequestBody AppointmentAssignDTO dto) {
        appointmentService.assign(dto);
        return Result.ok();
    }

    @Operation(summary = "取消预约")
    @LogOperation(module = "预约管理", value = "取消预约")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        appointmentService.cancel(id, body == null ? null : body.get("reason"),
                UserContext.getUserId(), UserContext.getRole());
        return Result.ok();
    }

    @Operation(summary = "标记就诊完成（医生）")
    @RequireRole("DOCTOR")
    @PutMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id) {
        appointmentService.complete(id, UserContext.getUserId());
        return Result.ok();
    }

    @Operation(summary = "预约分页（按角色隔离数据）")
    @GetMapping
    public Result<Page<Appointment>> page(@RequestParam(defaultValue = "1") int pageNum,
                                          @RequestParam(defaultValue = "10") int pageSize,
                                          @RequestParam(required = false) Integer status,
                                          @RequestParam(required = false) Long departmentId) {
        return Result.ok(appointmentService.pageAppointments(pageNum, pageSize, status,
                UserContext.getRole(), UserContext.getUserId(), departmentId));
    }

    @Operation(summary = "预约详情（按角色隔离数据）")
    @GetMapping("/{id}")
    public Result<Appointment> detail(@PathVariable Long id) {
        return Result.ok(appointmentService.detailFor(id, UserContext.getRole(), UserContext.getUserId()));
    }
}
