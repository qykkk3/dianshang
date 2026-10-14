package com.mall.controller.admin;

import com.mall.common.BusinessException;
import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.common.annotation.OperLog;
import com.mall.dto.BannerSaveDTO;
import com.mall.service.BannerService;
import com.mall.vo.BannerVO;
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
 * 管理端轮播图接口
 */
@Tag(name = "管理端-轮播图管理")
@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
public class AdminBannerController {

    private final BannerService bannerService;

    @Operation(summary = "轮播图列表")
    @PreAuthorize("hasAuthority('banner:list')")
    @GetMapping
    public Result<PageResult<BannerVO>> list(@RequestParam(required = false, defaultValue = "1") Integer page,
                                             @RequestParam(required = false, defaultValue = "10") Integer size,
                                             @RequestParam(required = false) Integer position,
                                             @RequestParam(required = false) Integer status) {
        return Result.success(bannerService.pageAdmin(page, size, position, status));
    }

    @Operation(summary = "新增轮播图")
    @PreAuthorize("hasAuthority('banner:add')")
    @OperLog(module = "营销管理", value = "新增轮播图")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody BannerSaveDTO dto) {
        dto.setId(null);
        bannerService.save(dto);
        return Result.success();
    }

    @Operation(summary = "编辑轮播图")
    @PreAuthorize("hasAuthority('banner:edit')")
    @OperLog(module = "营销管理", value = "编辑轮播图")
    @PutMapping("/{id}")
    public Result<Void> edit(@PathVariable Long id, @Valid @RequestBody BannerSaveDTO dto) {
        dto.setId(id);
        bannerService.save(dto);
        return Result.success();
    }

    @Operation(summary = "删除轮播图")
    @PreAuthorize("hasAuthority('banner:delete')")
    @OperLog(module = "营销管理", value = "删除轮播图")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bannerService.delete(id);
        return Result.success();
    }

    @Operation(summary = "启用 / 停用轮播图")
    @PreAuthorize("hasAuthority('banner:edit')")
    @OperLog(module = "营销管理", value = "修改轮播图状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @RequestParam(value = "status", required = false) Integer status,
                                     @RequestBody(required = false) Map<String, Object> body) {
        bannerService.updateStatus(id, resolveStatus(body, status));
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
