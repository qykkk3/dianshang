package com.mall.controller.admin;

import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.common.annotation.OperLog;
import com.mall.dto.SysOperLogQueryDTO;
import com.mall.service.SysOperLogService;
import com.mall.vo.SysOperLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端 - 操作日志
 */
@RestController
@RequestMapping("/api/admin/sys/logs")
@RequiredArgsConstructor
@Tag(name = "管理端-操作日志", description = "后台操作日志查询、清理与批量删除")
public class AdminSysLogController {

    private final SysOperLogService sysOperLogService;

    @GetMapping({"", "/"})
    @PreAuthorize("hasAuthority('sys:log:list')")
    @Operation(summary = "操作日志分页列表")
    public Result<PageResult<SysOperLogVO>> page(@Valid SysOperLogQueryDTO query) {
        return Result.success(sysOperLogService.page(query));
    }

    /**
     * 清理历史日志，days 缺省 30，传 0 或负数表示清空全部
     */
    @DeleteMapping({"", "/"})
    @PreAuthorize("hasAuthority('sys:log:delete')")
    @OperLog(module = "系统管理", value = "清空操作日志")
    @Operation(summary = "按天数清理操作日志")
    public Result<Void> clean(@RequestParam(value = "days", required = false, defaultValue = "30") Integer days) {
        sysOperLogService.clean(days);
        return Result.success();
    }

    @DeleteMapping("/batch")
    @PreAuthorize("hasAuthority('sys:log:delete')")
    @OperLog(module = "系统管理", value = "批量删除操作日志")
    @Operation(summary = "批量删除操作日志")
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        sysOperLogService.deleteByIds(ids);
        return Result.success();
    }
}
