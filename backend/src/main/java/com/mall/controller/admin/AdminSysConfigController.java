package com.mall.controller.admin;

import com.mall.common.Result;
import com.mall.common.annotation.OperLog;
import com.mall.dto.SysConfigSaveDTO;
import com.mall.service.SysConfigService;
import com.mall.vo.SysConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端 - 系统配置
 */
@RestController
@RequestMapping("/api/admin/sys/configs")
@RequiredArgsConstructor
@Tag(name = "管理端-系统配置", description = "商城参数配置的查询与批量修改")
public class AdminSysConfigController {

    private final SysConfigService sysConfigService;

    @GetMapping({"", "/"})
    @PreAuthorize("hasAuthority('sys:config:list')")
    @Operation(summary = "系统配置列表")
    public Result<List<SysConfigVO>> list() {
        return Result.success(sysConfigService.list());
    }

    /**
     * 批量修改配置：即使只修改一项也请使用数组提交
     */
    @PutMapping({"", "/"})
    @PreAuthorize("hasAuthority('sys:config:edit')")
    @OperLog(module = "系统管理", value = "修改系统配置")
    @Operation(summary = "批量修改系统配置")
    public Result<Void> batchUpdate(@RequestBody List<SysConfigSaveDTO> configs) {
        sysConfigService.batchUpdate(configs);
        return Result.success();
    }
}
