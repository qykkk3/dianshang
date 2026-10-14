package com.mall.controller.admin;

import com.mall.common.Result;
import com.mall.service.SysRoleService;
import com.mall.vo.SysPermissionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端 - 权限菜单
 */
@RestController
@RequestMapping("/api/admin/sys/permissions")
@RequiredArgsConstructor
@Tag(name = "管理端-权限菜单", description = "权限树查询，供角色分配权限使用")
public class AdminSysPermissionController {

    private final SysRoleService sysRoleService;

    @GetMapping({"", "/"})
    @PreAuthorize("hasAuthority('sys:role:list')")
    @Operation(summary = "完整权限树")
    public Result<List<SysPermissionVO>> tree() {
        return Result.success(sysRoleService.listAllPermissions());
    }
}
