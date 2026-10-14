package com.mall.controller.shop;

import com.mall.common.Result;
import com.mall.dto.BuyerLoginDTO;
import com.mall.dto.RegisterDTO;
import com.mall.service.AuthService;
import com.mall.vo.BuyerLoginVO;
import com.mall.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 买家认证接口
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "买家认证", description = "注册、登录、退出登录与当前用户信息")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "买家注册")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return Result.success("注册成功", null);
    }

    @PostMapping("/login")
    @Operation(summary = "买家登录")
    public Result<BuyerLoginVO> login(@Valid @RequestBody BuyerLoginDTO dto) {
        return Result.success("登录成功", authService.login(dto));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public Result<Void> logout() {
        authService.logout();
        return Result.success("已退出登录", null);
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前登录买家信息")
    public Result<UserInfoVO> me() {
        return Result.success(authService.getCurrentUserInfo());
    }
}
