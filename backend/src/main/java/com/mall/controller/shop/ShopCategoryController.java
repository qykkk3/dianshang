package com.mall.controller.shop;

import com.mall.common.Result;
import com.mall.service.CategoryService;
import com.mall.vo.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 买家端商品分类接口（公开访问）
 */
@Tag(name = "买家端-商品分类")
@RestController
@RequestMapping("/api/shop/categories")
@RequiredArgsConstructor
public class ShopCategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "分类树（仅启用分类，两级结构）")
    @GetMapping
    public Result<List<CategoryVO>> listTree() {
        return Result.success(categoryService.listTree());
    }

    @Operation(summary = "分类详情（含子分类）")
    @GetMapping("/{id}")
    public Result<CategoryVO> detail(@PathVariable Long id) {
        return Result.success(categoryService.getById(id));
    }
}
