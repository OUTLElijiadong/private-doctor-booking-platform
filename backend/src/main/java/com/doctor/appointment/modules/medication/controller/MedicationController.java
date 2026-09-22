package com.doctor.appointment.modules.medication.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doctor.appointment.common.Result;
import com.doctor.appointment.modules.medication.entity.Medication;
import com.doctor.appointment.modules.medication.entity.MedicationCategory;
import com.doctor.appointment.modules.medication.entity.MedicationStockLog;
import com.doctor.appointment.modules.medication.mapper.MedicationCategoryMapper;
import com.doctor.appointment.modules.medication.service.MedicationService;
import com.doctor.appointment.modules.system.log.LogOperation;
import com.doctor.appointment.security.RequireRole;
import com.doctor.appointment.security.UserContext;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 药品管理：药品、分类、出入库
 */
@Tag(name = "药品管理")
@RestController
@RequestMapping("/api/medications")
@RequiredArgsConstructor
public class MedicationController {

    private final MedicationService medicationService;
    private final MedicationCategoryMapper categoryMapper;

    @Operation(summary = "药品分页")
    @RequireRole("ADMIN")
    @GetMapping
    public Result<Page<Medication>> page(@RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "10") int pageSize,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Long categoryId) {
        return Result.ok(medicationService.pageMedications(pageNum, pageSize, keyword, categoryId));
    }

    @Operation(summary = "在售药品列表（开处方用）")
    @RequireRole({"DOCTOR", "ADMIN"})
    @GetMapping("/on-sale")
    public Result<List<Medication>> onSale() {
        return Result.ok(medicationService.listOnSale());
    }

    @Operation(summary = "新增药品")
    @LogOperation(module = "药品管理", value = "新增药品")
    @RequireRole("ADMIN")
    @PostMapping
    public Result<Void> create(@RequestBody Medication medication) {
        medication.setId(null);
        if (medication.getStock() == null) {
            medication.setStock(0);
        }
        medicationService.save(medication);
        return Result.ok();
    }

    @Operation(summary = "编辑药品")
    @LogOperation(module = "药品管理", value = "编辑药品")
    @RequireRole("ADMIN")
    @PutMapping
    public Result<Void> update(@RequestBody Medication medication) {
        // 库存只能通过入库/调整接口变动（会写出入库流水），编辑接口屏蔽库存字段
        medication.setStock(null);
        medicationService.updateById(medication);
        return Result.ok();
    }

    @Operation(summary = "删除药品")
    @LogOperation(module = "药品管理", value = "删除药品")
    @RequireRole("ADMIN")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        medicationService.removeById(id);
        return Result.ok();
    }

    @Operation(summary = "药品入库/库存调整（quantity 正数为入库，负数为调整出库）")
    @LogOperation(module = "药品管理", value = "药品出入库登记")
    @RequireRole("ADMIN")
    @PostMapping("/{id}/stock")
    public Result<Void> changeStock(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer quantity = (Integer) body.get("quantity");
        if (quantity == null || quantity == 0) {
            return Result.error("变动数量不能为0");
        }
        String remark = (String) body.get("remark");
        String refType = quantity > 0 ? "PURCHASE" : "ADJUST";
        medicationService.changeStock(id, quantity, refType, remark, UserContext.getUserId());
        return Result.ok();
    }

    @Operation(summary = "出入库流水分页")
    @RequireRole("ADMIN")
    @GetMapping("/stock-logs")
    public Result<Page<MedicationStockLog>> stockLogs(@RequestParam(defaultValue = "1") int pageNum,
                                                      @RequestParam(defaultValue = "10") int pageSize,
                                                      @RequestParam(required = false) Long medicationId,
                                                      @RequestParam(required = false) String type) {
        return Result.ok(medicationService.pageStockLogs(pageNum, pageSize, medicationId, type));
    }

    // ---------- 药品分类 ----------

    @Operation(summary = "分类列表")
    @GetMapping("/categories")
    public Result<List<MedicationCategory>> categories() {
        return Result.ok(categoryMapper.selectList(new LambdaQueryWrapper<>()));
    }

    @Operation(summary = "新增分类")
    @RequireRole("ADMIN")
    @PostMapping("/categories")
    public Result<Void> createCategory(@RequestBody MedicationCategory category) {
        categoryMapper.insert(category);
        return Result.ok();
    }

    @Operation(summary = "编辑分类")
    @RequireRole("ADMIN")
    @PutMapping("/categories")
    public Result<Void> updateCategory(@RequestBody MedicationCategory category) {
        categoryMapper.updateById(category);
        return Result.ok();
    }

    @Operation(summary = "删除分类")
    @RequireRole("ADMIN")
    @DeleteMapping("/categories/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        long inUse = medicationService.count(new LambdaQueryWrapper<Medication>()
                .eq(Medication::getCategoryId, id));
        if (inUse > 0) {
            return Result.error("该分类下仍有 " + inUse + " 个药品，请先调整药品分类");
        }
        categoryMapper.deleteById(id);
        return Result.ok();
    }
}
