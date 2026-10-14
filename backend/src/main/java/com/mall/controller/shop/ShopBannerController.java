package com.mall.controller.shop;

import com.mall.common.Result;
import com.mall.service.BannerService;
import com.mall.vo.BannerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商城轮播图接口（公开）
 */
@Tag(name = "商城-轮播图")
@RestController
@RequestMapping("/api/shop/banners")
@RequiredArgsConstructor
public class ShopBannerController {

    private final BannerService bannerService;

    @Operation(summary = "轮播图列表（position 为空返回全部位置）")
    @GetMapping
    public Result<List<BannerVO>> list(@RequestParam(required = false) Integer position) {
        return Result.success(bannerService.listShop(position));
    }
}
