package com.mall.controller.admin;

import com.mall.common.BusinessException;
import com.mall.common.Result;
import com.mall.common.annotation.OperLog;
import com.mall.dto.RolePermissionDTO;
import com.mall.dto.SysRoleSaveDTO;
import com.mall.service.SysRoleService;
import com.mall.vo.SysRoleVO;
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

import java.util.List;
import java.util.Map;

/**
 * 管理端 - 角色与权限
 */
@RestController
@RequestMapping("/api/admin/sys/roles")
@RequiredArgsConstructor
@Tag(name = "管理端-角色权限", description = "角色管理、启用停用与权限分配")
public class AdminSysRoleController {

    private final SysRoleService sysRoleService;

    @GetMapping({"", "/"})
    @PreAuthorize("hasAuthority('sys:role:list')")
    @Operation(summary = "角色列表")
    public Result<List<SysRoleVO>> list() {
        return Result.success(sysRoleService.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:role:list')")
    @Operation(summary = "角色详情")
    public Result<SysRoleVO> getById(@PathVariable Long id) {
        return Result.success(sysRoleService.getById(id));
    }

    @PostMapping({"", "/"})
    @PreAuthorize("hasAuthority('sys:role:add')")
    @OperLog(module = "系统管理", value = "新增角色")
    @Operation(summary = "新增角色")
    public Result<Void> add(@Valid @RequestBody SysRoleSaveDTO dto) {
        dto.setId(null);
        sysRoleService.save(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:role:edit')")
    @OperLog(module = "系统管理", value = "编辑角色")
    @Operation(summary = "编辑角色")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SysRoleSaveDTO dto) {
        dto.setId(id);
        sysRoleService.save(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:role:delete')")
    @OperLog(module = "系统管理", value = "删除角色")
    @Operation(summary = "删除角色")
    public Result<Void> delete(@PathVariable Long id) {
        sysRoleService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('sys:role:assign')")
    @OperLog(module = "系统管理", value = "分配角色权限")
    @Operation(summary = "分配角色权限")
    public Result<Void> assignPermissions(@PathVariable Long id,
                                         @RequestBody(required = false) RolePermissionDTO dto) {
        sysRoleService.assignPermissions(id, dto);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('sys:role:edit')")
    @OperLog(module = "系统管理", value = "启用/禁用角色")
    @Operation(summary = "启用/禁用角色")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @RequestParam(value = "status", required = false) Integer status,
                                     @RequestBody(required = false) Map<String, Object> body) {
        sysRoleService.updateStatus(id, resolveStatus(status, body));
        return Result.success();
    }

    /**
     * 兼容 ?status=1 与请求体 {"status":1} 两种传参方式
     */
    private static Integer resolveStatus(Integer status, Map<String, Object> body) {
        if (status != null) {
            return status;
        }
        Object value = body == null ? null : body.get("status");
        if (value == null) {
            throw new BusinessException("缺少参数 status");
        }
        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("status 参数不合法");
        }
    }
}
