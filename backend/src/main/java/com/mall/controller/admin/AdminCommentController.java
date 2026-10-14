package com.mall.controller.admin;

import com.mall.common.BusinessException;
import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.common.annotation.OperLog;
import com.mall.dto.CommentQueryDTO;
import com.mall.dto.CommentReplyDTO;
import com.mall.service.CommentService;
import com.mall.vo.CommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端商品评价接口
 */
@Tag(name = "管理端-评价管理")
@RestController
@RequestMapping("/api/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {

    private final CommentService commentService;

    @Operation(summary = "评价列表")
    @PreAuthorize("hasAuthority('comment:list')")
    @GetMapping
    public Result<PageResult<CommentVO>> list(CommentQueryDTO query) {
        return Result.success(commentService.pageAdminComments(query));
    }

    @Operation(summary = "回复评价")
    @PreAuthorize("hasAuthority('comment:reply')")
    @OperLog(module = "评价管理", value = "回复评价")
    @PutMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable Long id, @Valid @RequestBody CommentReplyDTO dto) {
        commentService.reply(id, dto.getReplyContent());
        return Result.success();
    }

    @Operation(summary = "修改评价显示状态 1显示 0隐藏")
    @PreAuthorize("hasAuthority('comment:status')")
    @OperLog(module = "评价管理", value = "修改评价显示状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @RequestParam(value = "status", required = false) Integer status,
                                     @RequestBody(required = false) Map<String, Object> body) {
        commentService.updateStatus(id, resolveStatus(body, status));
        return Result.success();
    }

    @Operation(summary = "删除评价")
    @PreAuthorize("hasAuthority('comment:delete')")
    @OperLog(module = "评价管理", value = "删除评价")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return Result.success();
    }

    /**
     * 兼容两种传参：?status=1 或 JSON 请求体 {"status":1}
     */
    private Integer resolveStatus(Map<String, Object> body, Integer status) {
        if (status != null) {
            return status;
        }
        Object value = body == null ? null : body.get("status");
        if (value == null) {
            throw new BusinessException("缺少状态参数");
        }
        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("状态参数不合法");
        }
    }
}
