package com.doctor.appointment.modules.schedule.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doctor.appointment.common.Result;
import com.doctor.appointment.modules.schedule.dto.ScheduleBatchDTO;
import com.doctor.appointment.modules.schedule.entity.Schedule;
import com.doctor.appointment.modules.schedule.service.ScheduleService;
import com.doctor.appointment.modules.system.log.LogOperation;
import com.doctor.appointment.security.RequireRole;
import com.doctor.appointment.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 排班管理：出诊规划、定期检查、动态管控
 */
@Tag(name = "排班管理")
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @Operation(summary = "批量排班（医生本人或管理员）")
    @LogOperation(module = "排班管理", value = "批量出诊规划")
    @RequireRole({"DOCTOR", "ADMIN"})
    @PostMapping("/batch")
    public Result<String> batchCreate(@Valid @RequestBody ScheduleBatchDTO dto) {
        // 医生角色强制为本人，忽略请求体中的 doctorId，防止替他人排班
        boolean isAdmin = "ADMIN".equals(UserContext.getRole());
        Long doctorId = isAdmin ? dto.getDoctorId() : UserContext.getUserId();
        int created = scheduleService.batchCreate(dto, doctorId, isAdmin);
        return Result.ok("成功生成 " + created + " 条排班（重复排班已自动跳过）");
    }

    @Operation(summary = "排班分页查询")
    @GetMapping
    public Result<Page<Schedule>> page(@RequestParam(defaultValue = "1") int pageNum,
                                       @RequestParam(defaultValue = "10") int pageSize,
                                       @RequestParam(required = false) Long doctorId,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                       @RequestParam(required = false) Integer status) {
        // 医生默认只查自己的排班
        if ("DOCTOR".equals(UserContext.getRole())) {
            doctorId = UserContext.getUserId();
        }
        return Result.ok(scheduleService.pageSchedules(pageNum, pageSize, doctorId, startDate, endDate, status));
    }

    @Operation(summary = "可预约号源（患者端）")
    @GetMapping("/available")
    public Result<List<Schedule>> available(@RequestParam Long doctorId,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(scheduleService.listAvailable(doctorId, date));
    }

    @Operation(summary = "停诊/调班/恢复（停诊自动取消关联预约，返回取消笔数）")
    @LogOperation(module = "排班管理", value = "排班动态管控")
    @RequireRole({"DOCTOR", "ADMIN"})
    @PutMapping("/{id}/status")
    public Result<Integer> changeStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object statusObj = body.get("status");
        Integer status = statusObj instanceof Number number ? number.intValue() : null;
        int cancelled = scheduleService.changeStatus(id, status, (String) body.get("remark"),
                UserContext.getUserId(), "ADMIN".equals(UserContext.getRole()));
        return Result.ok(cancelled);
    }

    @Operation(summary = "删除排班（仅限无人预约的排班）")
    @LogOperation(module = "排班管理", value = "删除排班")
    @RequireRole({"DOCTOR", "ADMIN"})
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        scheduleService.deleteSchedule(id, UserContext.getUserId(), "ADMIN".equals(UserContext.getRole()));
        return Result.ok();
    }
}
