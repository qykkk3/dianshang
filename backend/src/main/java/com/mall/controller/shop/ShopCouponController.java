package com.mall.controller.shop;

import com.mall.common.Result;
import com.mall.service.CouponService;
import com.mall.vo.CouponVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商城优惠券接口（公开）
 */
@Tag(name = "商城-优惠券")
@RestController
@RequestMapping("/api/shop/coupons")
@RequiredArgsConstructor
public class ShopCouponController {

    private final CouponService couponService;

    @Operation(summary = "可领取的优惠券列表")
    @GetMapping
    public Result<List<CouponVO>> list() {
        return Result.success(couponService.pageShopCoupons());
    }
}
