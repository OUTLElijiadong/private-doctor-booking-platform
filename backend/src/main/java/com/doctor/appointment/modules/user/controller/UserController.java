package com.doctor.appointment.modules.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doctor.appointment.common.Result;
import com.doctor.appointment.modules.system.log.LogOperation;
import com.doctor.appointment.modules.user.entity.SysUser;
import com.doctor.appointment.modules.user.service.UserService;
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
 * 用户管理（管理员）
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/users")
@RequireRole("ADMIN")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户分页")
    @GetMapping
    public Result<Page<SysUser>> page(@RequestParam(defaultValue = "1") int pageNum,
                                      @RequestParam(defaultValue = "10") int pageSize,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String role) {
        return Result.ok(userService.pageUsers(pageNum, pageSize, keyword, role));
    }

    @Operation(summary = "新增用户")
    @LogOperation(module = "用户管理", value = "新增用户")
    @PostMapping
    public Result<Void> create(@RequestBody SysUser user) {
        userService.createUser(user);
        return Result.ok();
    }

    @Operation(summary = "编辑用户")
    @LogOperation(module = "用户管理", value = "编辑用户")
    @PutMapping
    public Result<Void> update(@RequestBody SysUser user) {
        user.setPassword(null); // 密码不在此接口修改
        userService.updateById(user);
        return Result.ok();
    }

    @Operation(summary = "冻结/解冻账号")
    @LogOperation(module = "系统管理", value = "账号状态管控")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(body.get("status"));
        userService.updateById(user);
        return Result.ok();
    }

    @Operation(summary = "重置密码为123456")
    @LogOperation(module = "系统管理", value = "重置用户密码")
    @PutMapping("/{id}/password/reset")
    public Result<Void> resetPassword(@PathVariable Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setPassword(userService.encode("123456"));
        userService.updateById(user);
        return Result.ok();
    }

    @Operation(summary = "患者下拉列表（线下登记用，医生/管理员均可）")
    @RequireRole({"ADMIN", "DOCTOR"})
    @GetMapping("/patients")
    public Result<List<SysUser>> patients() {
        return Result.ok(userService.list(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "PATIENT")
                .eq(SysUser::getStatus, 1)
                .orderByDesc(SysUser::getCreatedAt)));
    }

    @Operation(summary = "删除用户（存在业务数据时拒绝）")
    @LogOperation(module = "用户管理", value = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteWithCheck(id, UserContext.getUserId());
        return Result.ok();
    }
}
