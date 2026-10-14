package com.mall.controller.admin;

import com.mall.common.BusinessException;
import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.common.annotation.OperLog;
import com.mall.dto.NoticeSaveDTO;
import com.mall.service.NoticeService;
import com.mall.vo.NoticeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端公告接口
 */
@Tag(name = "管理端-公告管理")
@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "公告列表（含草稿）")
    @PreAuthorize("hasAuthority('notice:list')")
    @GetMapping
    public Result<PageResult<NoticeVO>> list(@RequestParam(required = false, defaultValue = "1") Integer page,
                                             @RequestParam(required = false, defaultValue = "10") Integer size,
                                             @RequestParam(required = false) Integer type,
                                             @RequestParam(required = false) Integer status) {
        return Result.success(noticeService.pageAdmin(page, size, type, status));
    }

    @Operation(summary = "新增公告")
    @PreAuthorize("hasAuthority('notice:add')")
    @OperLog(module = "营销管理", value = "新增公告")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody NoticeSaveDTO dto) {
        dto.setId(null);
        noticeService.save(dto);
        return Result.success();
    }

    @Operation(summary = "编辑公告")
    @PreAuthorize("hasAuthority('notice:edit')")
    @OperLog(module = "营销管理", value = "编辑公告")
    @PutMapping("/{id}")
    public Result<Void> edit(@PathVariable Long id, @Valid @RequestBody NoticeSaveDTO dto) {
        dto.setId(id);
        noticeService.save(dto);
        return Result.success();
    }

    @Operation(summary = "删除公告")
    @PreAuthorize("hasAuthority('notice:delete')")
    @OperLog(module = "营销管理", value = "删除公告")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return Result.success();
    }

    @Operation(summary = "发布 / 撤回公告")
    @PreAuthorize("hasAuthority('notice:publish')")
    @OperLog(module = "营销管理", value = "发布公告")
    @PutMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id,
                                @RequestParam(value = "status", required = false) Integer status,
                                @RequestBody(required = false) Map<String, Object> body) {
        noticeService.publish(id, resolveStatus(body, status));
        return Result.success();
    }

    /**
     * 兼容两种传参：?status=1 或 JSON 请求体 {"status":1}；未传时默认按发布处理
     */
    private Integer resolveStatus(Map<String, Object> body, Integer status) {
        if (status != null) {
            return status;
        }
        Object value = body == null ? null : body.get("status");
        if (value == null) {
            return 1;
        }
        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("状态参数不合法");
        }
    }
}
