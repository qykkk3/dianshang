package com.mall.controller;

import com.mall.common.Result;
import com.mall.dto.CartAddDTO;
import com.mall.dto.CartBatchDTO;
import com.mall.dto.CartCheckedDTO;
import com.mall.dto.CartUpdateDTO;
import com.mall.service.CartService;
import com.mall.vo.CartVO;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 购物车接口（需买家登录）
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "购物车", description = "购物车列表、加购、改数量、勾选与删除")
public class CartController {

    private final CartService cartService;

    @GetMapping("/list")
    @Operation(summary = "购物车列表")
    public Result<List<CartVO>> list() {
        return Result.success(cartService.list());
    }

    @GetMapping("/count")
    @Operation(summary = "购物车条目数（角标）")
    public Result<Integer> count() {
        return Result.success(cartService.count());
    }

    @PostMapping
    @Operation(summary = "加入购物车")
    public Result<Void> add(@Valid @RequestBody CartAddDTO dto) {
        cartService.add(dto);
        return Result.success("已加入购物车", null);
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改购物车条目（数量/勾选）")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CartUpdateDTO dto) {
        cartService.update(id, dto);
        return Result.success();
    }

    @PutMapping("/checked")
    @Operation(summary = "批量勾选/取消勾选，ids 为空时对全部条目生效")
    public Result<Void> updateChecked(@RequestBody CartCheckedDTO dto) {
        cartService.updateChecked(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除购物车条目")
    public Result<Void> delete(@PathVariable Long id) {
        cartService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除购物车条目")
    public Result<Void> deleteBatch(@Valid @RequestBody CartBatchDTO dto) {
        cartService.deleteBatch(dto);
        return Result.success();
    }
}
