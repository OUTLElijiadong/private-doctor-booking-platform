package com.doctor.appointment.modules.department.controller;

import com.doctor.appointment.common.Result;
import com.doctor.appointment.modules.department.entity.Department;
import com.doctor.appointment.modules.department.service.DepartmentService;
import com.doctor.appointment.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 科室管理
 */
@Tag(name = "科室管理")
@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(summary = "启用科室列表（所有角色）")
    @GetMapping("/enabled")
    public Result<List<Department>> enabled() {
        return Result.ok(departmentService.listEnabled());
    }

    @Operation(summary = "全部科室列表（管理员）")
    @RequireRole("ADMIN")
    @GetMapping
    public Result<List<Department>> list() {
        return Result.ok(departmentService.list());
    }

    @Operation(summary = "新增科室")
    @RequireRole("ADMIN")
    @PostMapping
    public Result<Void> create(@RequestBody Department department) {
        departmentService.save(department);
        return Result.ok();
    }

    @Operation(summary = "编辑科室")
    @RequireRole("ADMIN")
    @PutMapping
    public Result<Void> update(@RequestBody Department department) {
        departmentService.updateById(department);
        return Result.ok();
    }

    @Operation(summary = "删除科室")
    @RequireRole("ADMIN")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        long doctors = departmentService.countDoctors(id);
        if (doctors > 0) {
            return Result.error("该科室下仍有 " + doctors + " 位医生，请先为医生调整科室");
        }
        departmentService.removeById(id);
        return Result.ok();
    }
}
