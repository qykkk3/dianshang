package com.mall.controller.shop;

import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.dto.PasswordUpdateDTO;
import com.mall.dto.ProfileUpdateDTO;
import com.mall.service.UserService;
import com.mall.vo.FavoriteVO;
import com.mall.vo.HistoryVO;
import com.mall.vo.UserCouponVO;
import com.mall.vo.UserProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

/**
 * 买家个人中心接口（资料 / 收藏 / 足迹 / 我的优惠券）
 */
@Tag(name = "买家-个人中心")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserCenterController {

    private final UserService userService;

    @Operation(summary = "获取个人资料")
    @GetMapping("/profile")
    public Result<UserProfileVO> profile() {
        return Result.success(userService.getProfile());
    }

    @Operation(summary = "修改个人资料")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody ProfileUpdateDTO dto) {
        userService.updateProfile(dto);
        return Result.success();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordUpdateDTO dto) {
        userService.updatePassword(dto);
        return Result.success();
    }

    @Operation(summary = "我的收藏列表")
    @GetMapping("/favorites")
    public Result<PageResult<FavoriteVO>> favorites(@RequestParam(required = false, defaultValue = "1") Integer page,
                                                    @RequestParam(required = false, defaultValue = "10") Integer size) {
        return Result.success(userService.pageFavorites(page, size));
    }

    @Operation(summary = "收藏 / 取消收藏商品")
    @PostMapping("/favorites/{productId}")
    public Result<Boolean> toggleFavorite(@PathVariable Long productId) {
        return Result.success(userService.toggleFavorite(productId));
    }

    @Operation(summary = "取消收藏商品")
    @DeleteMapping("/favorites/{productId}")
    public Result<Void> deleteFavorite(@PathVariable Long productId) {
        userService.deleteFavorite(productId);
        return Result.success();
    }

    @Operation(summary = "我的浏览记录")
    @GetMapping("/history")
    public Result<PageResult<HistoryVO>> history(@RequestParam(required = false, defaultValue = "1") Integer page,
                                                 @RequestParam(required = false, defaultValue = "10") Integer size) {
        return Result.success(userService.pageHistory(page, size));
    }

    @Operation(summary = "记录商品浏览足迹")
    @PostMapping("/history/{productId}")
    public Result<Void> recordHistory(@PathVariable Long productId) {
        userService.recordHistory(productId);
        return Result.success();
    }

    @Operation(summary = "清空浏览记录")
    @DeleteMapping("/history")
    public Result<Void> clearHistory() {
        userService.clearHistory();
        return Result.success();
    }

    @Operation(summary = "我的优惠券")
    @GetMapping("/coupons")
    public Result<PageResult<UserCouponVO>> coupons(@RequestParam(required = false) Integer status,
                                                    @RequestParam(required = false, defaultValue = "1") Integer page,
                                                    @RequestParam(required = false, defaultValue = "10") Integer size) {
        return Result.success(userService.pageMyCoupons(status, page, size));
    }

    @Operation(summary = "领取优惠券")
    @PostMapping("/coupons/{couponId}/receive")
    public Result<Void> receiveCoupon(@PathVariable Long couponId) {
        userService.receiveCoupon(couponId);
        return Result.success();
    }
}
