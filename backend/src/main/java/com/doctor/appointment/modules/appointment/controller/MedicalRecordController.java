package com.doctor.appointment.modules.appointment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doctor.appointment.common.Result;
import com.doctor.appointment.modules.appointment.dto.MedicalRecordCreateDTO;
import com.doctor.appointment.modules.appointment.entity.MedicalRecord;
import com.doctor.appointment.modules.appointment.service.MedicalRecordService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 患者管理：病历归档与查询
 */
@Tag(name = "患者病历")
@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService recordService;

    @Operation(summary = "医生填写病历（归档并完结预约）")
    @LogOperation(module = "患者管理", value = "填写病历归档")
    @RequireRole("DOCTOR")
    @PostMapping
    public Result<MedicalRecord> create(@Valid @RequestBody MedicalRecordCreateDTO dto) {
        return Result.ok(recordService.create(UserContext.getUserId(), dto));
    }

    @Operation(summary = "病历分页（患者查本人/医生查接诊/管理员查全部）")
    @GetMapping
    public Result<Page<MedicalRecord>> page(@RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize,
                                            @RequestParam(required = false) Long patientId) {
        return Result.ok(recordService.pageRecords(pageNum, pageSize,
                UserContext.getRole(), UserContext.getUserId(), patientId));
    }

    @Operation(summary = "病历详情")
    @GetMapping("/{id}")
    public Result<MedicalRecord> detail(@PathVariable Long id) {
        return Result.ok(recordService.detailFor(id, UserContext.getRole(), UserContext.getUserId()));
    }
}
