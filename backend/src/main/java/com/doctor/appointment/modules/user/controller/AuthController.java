package com.doctor.appointment.modules.user.controller;

import com.doctor.appointment.common.Result;
import com.doctor.appointment.modules.system.log.LogOperation;
import com.doctor.appointment.modules.user.dto.LoginDTO;
import com.doctor.appointment.modules.user.dto.LoginVO;
import com.doctor.appointment.modules.user.dto.PasswordChangeDTO;
import com.doctor.appointment.modules.user.dto.RegisterDTO;
import com.doctor.appointment.modules.user.dto.ResetPasswordDTO;
import com.doctor.appointment.modules.user.entity.SysUser;
import com.doctor.appointment.modules.user.service.UserService;
import com.doctor.appointment.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证与个人中心
 */
@Tag(name = "认证与个人中心")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(summary = "登录")
    @LogOperation(module = "用户管理", value = "登录系统")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(userService.login(dto));
    }

    @Operation(summary = "注册（患者）")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return Result.ok();
    }

    @Operation(summary = "找回密码")
    @PostMapping("/password/reset")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        userService.resetPassword(dto);
        return Result.ok();
    }

    @Operation(summary = "当前登录人信息")
    @GetMapping("/profile")
    public Result<SysUser> profile() {
        return Result.ok(userService.getById(UserContext.getUserId()));
    }

    @Operation(summary = "修改个人资料")
    @LogOperation(module = "用户管理", value = "修改个人资料")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody SysUser user) {
        SysUser db = userService.getById(UserContext.getUserId());
        db.setRealName(user.getRealName());
        db.setGender(user.getGender());
        db.setPhone(user.getPhone());
        db.setEmail(user.getEmail());
        db.setAvatar(user.getAvatar());
        userService.updateById(db);
        return Result.ok();
    }

    @Operation(summary = "修改密码")
    @LogOperation(module = "用户管理", value = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody PasswordChangeDTO dto) {
        userService.changePassword(UserContext.getUserId(), dto);
        return Result.ok();
    }
}
