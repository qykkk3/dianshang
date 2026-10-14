package com.mall.controller.shop;

import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.dto.CommentAddDTO;
import com.mall.service.CommentService;
import com.mall.vo.CommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 买家评价接口（需登录）
 */
@Tag(name = "买家-商品评价")
@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "发表评价")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody CommentAddDTO dto) {
        commentService.addComment(dto);
        return Result.success();
    }

    @Operation(summary = "我的评价")
    @GetMapping("/my")
    public Result<PageResult<CommentVO>> my(@RequestParam(required = false, defaultValue = "1") Integer page,
                                            @RequestParam(required = false, defaultValue = "10") Integer size) {
        return Result.success(commentService.pageMyComments(page, size));
    }

    @Operation(summary = "待评价的订单商品")
    @GetMapping("/pending")
    public Result<PageResult<Map<String, Object>>> pending(@RequestParam(required = false, defaultValue = "1") Integer page,
                                                           @RequestParam(required = false, defaultValue = "10") Integer size) {
        return Result.success(commentService.pagePendingItems(page, size));
    }
}
