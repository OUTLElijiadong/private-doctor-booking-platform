package com.doctor.appointment.modules.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doctor.appointment.common.Result;
import com.doctor.appointment.modules.system.log.LogOperation;
import com.doctor.appointment.modules.user.entity.DoctorInfo;
import com.doctor.appointment.modules.user.service.DoctorService;
import com.doctor.appointment.security.RequireRole;
import com.doctor.appointment.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 医生信息管理
 */
@Tag(name = "医生管理")
@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @Operation(summary = "医生分页（患者端仅显示资质已通过的医生）")
    @GetMapping
    public Result<Page<DoctorInfo>> page(@RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "10") int pageSize,
                                         @RequestParam(required = false) Long departmentId,
                                         @RequestParam(required = false) String keyword) {
        boolean onlyApproved = !"ADMIN".equals(UserContext.getRole());
        return Result.ok(doctorService.pageDoctors(pageNum, pageSize, departmentId, keyword, onlyApproved));
    }

    @Operation(summary = "医生详情")
    @GetMapping("/{id}")
    public Result<DoctorInfo> detail(@PathVariable Long id) {
        return Result.ok(doctorService.detail(id));
    }

    @Operation(summary = "医生完善/修改执业信息")
    @LogOperation(module = "医生管理", value = "修改执业信息")
    @RequireRole({"DOCTOR", "ADMIN"})
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody DoctorInfo dto) {
        DoctorInfo info = "ADMIN".equals(UserContext.getRole())
                ? (dto.getId() == null ? null : doctorService.getById(dto.getId()))
                : doctorService.getByUserId(UserContext.getUserId());
        if (info == null) {
            return Result.error("医生信息不存在");
        }
        info.setDepartmentId(dto.getDepartmentId());
        info.setTitle(dto.getTitle());
        info.setSpecialty(dto.getSpecialty());
        info.setIntroduction(dto.getIntroduction());
        info.setYears(dto.getYears());
        doctorService.updateById(info);
        return Result.ok();
    }

    @Operation(summary = "当前医生的执业信息")
    @RequireRole("DOCTOR")
    @GetMapping("/mine")
    public Result<DoctorInfo> mine() {
        return Result.ok(doctorService.getByUserId(UserContext.getUserId()));
    }
}
