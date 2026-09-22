package com.doctor.appointment.modules.medication.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.doctor.appointment.common.Result;
import com.doctor.appointment.modules.medication.entity.Supplier;
import com.doctor.appointment.modules.medication.mapper.SupplierMapper;
import com.doctor.appointment.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 药品供应商管理（药品资信管理）
 */
@Tag(name = "供应商管理")
@RestController
@RequestMapping("/api/suppliers")
@RequireRole("ADMIN")
@RequiredArgsConstructor
public class SupplierController extends ServiceImpl<SupplierMapper, Supplier> {

    @Operation(summary = "供应商分页")
    @GetMapping
    public Result<Page<Supplier>> page(@RequestParam(defaultValue = "1") int pageNum,
                                       @RequestParam(defaultValue = "10") int pageSize,
                                       @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<Supplier>()
                .like(StringUtils.hasText(keyword), Supplier::getName, keyword)
                .orderByDesc(Supplier::getCreatedAt);
        return Result.ok(page(new Page<>(pageNum, pageSize), wrapper));
    }

    @Operation(summary = "供应商下拉列表")
    @GetMapping("/options")
    public Result<List<Supplier>> options() {
        return Result.ok(list());
    }

    @Operation(summary = "新增供应商")
    @PostMapping
    public Result<Void> create(@RequestBody Supplier supplier) {
        save(supplier);
        return Result.ok();
    }

    @Operation(summary = "编辑供应商")
    @PutMapping
    public Result<Void> update(@RequestBody Supplier supplier) {
        updateById(supplier);
        return Result.ok();
    }

    @Operation(summary = "删除供应商")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        removeById(id);
        return Result.ok();
    }
}
