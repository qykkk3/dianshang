package com.mall.controller.admin;

import com.mall.common.Result;
import com.mall.dto.AdminLoginDTO;
import com.mall.service.AdminAuthService;
import com.mall.vo.AdminLoginVO;
import com.mall.vo.MenuVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端登录鉴权接口
 */
@Tag(name = "管理端-登录鉴权")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @Operation(summary = "后台登录")
    @PostMapping("/login")
    public Result<AdminLoginVO> login(@Valid @RequestBody AdminLoginDTO dto, HttpServletRequest request) {
        return Result.success(adminAuthService.login(dto, request));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        adminAuthService.logout();
        return Result.success();
    }

    @Operation(summary = "当前登录管理员信息（含权限与菜单）")
    @GetMapping("/me")
    public Result<AdminLoginVO> me() {
        return Result.success(adminAuthService.getCurrentInfo());
    }

    @Operation(summary = "当前登录管理员菜单树")
    @GetMapping("/menus")
    public Result<List<MenuVO>> menus() {
        return Result.success(adminAuthService.listMenus());
    }
}
