package com.mall.controller.shop;

import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.service.CommentService;
import com.mall.vo.CommentStatsVO;
import com.mall.vo.CommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商城商品评价接口（公开，无需登录）
 */
@Tag(name = "商城-商品评价")
@RestController
@RequestMapping("/api/shop/products")
@RequiredArgsConstructor
public class ShopCommentController {

    private final CommentService commentService;

    @Operation(summary = "商品评价列表")
    @GetMapping("/{productId}/comments")
    public Result<PageResult<CommentVO>> comments(@PathVariable Long productId,
                                                  @RequestParam(required = false) Integer star,
                                                  @RequestParam(required = false, defaultValue = "1") Integer page,
                                                  @RequestParam(required = false, defaultValue = "10") Integer size) {
        return Result.success(commentService.pageProductComments(productId, star, page, size));
    }

    @Operation(summary = "商品评价统计")
    @GetMapping("/{productId}/comment-stats")
    public Result<CommentStatsVO> commentStats(@PathVariable Long productId) {
        return Result.success(commentService.getProductCommentStats(productId));
    }
}
