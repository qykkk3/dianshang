package com.mall.controller.shop;

import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.dto.ProductQueryDTO;
import com.mall.security.SecurityUtils;
import com.mall.service.ProductService;
import com.mall.vo.ProductDetailVO;
import com.mall.vo.ProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 买家端商品接口（公开访问）
 */
@Tag(name = "买家端-商品")
@RestController
@RequestMapping("/api/shop/products")
@RequiredArgsConstructor
public class ShopProductController {

    private final ProductService productService;

    @Operation(summary = "商品分页查询（仅上架商品）")
    @GetMapping
    public Result<PageResult<ProductVO>> page(@Valid @ModelAttribute ProductQueryDTO query) {
        return Result.success(productService.pageShopProducts(query));
    }

    @Operation(summary = "商品详情（未登录时 favorited 为 false）")
    @GetMapping("/{id}")
    public Result<ProductDetailVO> detail(@PathVariable Long id) {
        return Result.success(productService.getShopDetail(id, SecurityUtils.getUserIdOrNull()));
    }

    @Operation(summary = "热销商品")
    @GetMapping("/hot")
    public Result<List<ProductVO>> hot(@RequestParam(value = "limit", defaultValue = "8") Integer limit) {
        return Result.success(productService.listHot(limit));
    }

    @Operation(summary = "新品商品")
    @GetMapping("/new")
    public Result<List<ProductVO>> latest(@RequestParam(value = "limit", defaultValue = "8") Integer limit) {
        return Result.success(productService.listNew(limit));
    }

    @Operation(summary = "推荐商品")
    @GetMapping("/recommend")
    public Result<List<ProductVO>> recommend(@RequestParam(value = "limit", defaultValue = "8") Integer limit) {
        return Result.success(productService.listRecommend(limit));
    }
}
