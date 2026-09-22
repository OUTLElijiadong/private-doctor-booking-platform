package com.doctor.appointment.modules.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doctor.appointment.common.Result;
import com.doctor.appointment.modules.system.log.LogOperation;
import com.doctor.appointment.modules.user.dto.AuditDTO;
import com.doctor.appointment.modules.user.dto.ReviewSubmitDTO;
import com.doctor.appointment.modules.user.entity.DoctorReview;
import com.doctor.appointment.modules.user.service.DoctorReviewService;
import com.doctor.appointment.security.RequireRole;
import com.doctor.appointment.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 医生执业资格审核
 */
@Tag(name = "执业资格审核")
@RestController
@RequestMapping("/api/doctor-reviews")
@RequiredArgsConstructor
public class DoctorReviewController {

    private final DoctorReviewService reviewService;

    @Operation(summary = "医生提交资质材料")
    @LogOperation(module = "医生管理", value = "提交执业资质")
    @RequireRole("DOCTOR")
    @PostMapping
    public Result<Void> submit(@Valid @RequestBody ReviewSubmitDTO dto) {
        reviewService.submit(UserContext.getUserId(), dto);
        return Result.ok();
    }

    @Operation(summary = "管理员审核资质")
    @LogOperation(module = "医生管理", value = "执业资格审核")
    @RequireRole("ADMIN")
    @PostMapping("/audit")
    public Result<Void> audit(@Valid @RequestBody AuditDTO dto) {
        reviewService.audit(UserContext.getUserId(), dto);
        return Result.ok();
    }

    @Operation(summary = "审核记录分页")
    @RequireRole({"ADMIN", "DOCTOR"})
    @GetMapping
    public Result<Page<DoctorReview>> page(@RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "10") int pageSize,
                                           @RequestParam(required = false) Integer status) {
        boolean isAdmin = "ADMIN".equals(UserContext.getRole());
        return Result.ok(reviewService.pageReviews(pageNum, pageSize, status, UserContext.getUserId(), isAdmin));
    }
}
