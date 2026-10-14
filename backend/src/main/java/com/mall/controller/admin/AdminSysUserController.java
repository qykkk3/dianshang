package com.mall.controller.admin;

import com.mall.common.BusinessException;
import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.common.annotation.OperLog;
import com.mall.dto.PasswordResetDTO;
import com.mall.dto.SysUserQueryDTO;
import com.mall.dto.SysUserSaveDTO;
import com.mall.service.SysUserService;
import com.mall.vo.SysUserVO;
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
 * 管理端 - 管理员管理
 */
@RestController
@RequestMapping("/api/admin/sys/users")
@RequiredArgsConstructor
@Tag(name = "管理端-管理员管理", description = "后台管理员的增删改查、启用禁用与密码重置")
public class AdminSysUserController {

    private final SysUserService sysUserService;

    @GetMapping({"", "/"})
    @PreAuthorize("hasAuthority('sys:user:list')")
    @Operation(summary = "管理员分页列表")
    public Result<PageResult<SysUserVO>> page(@Valid SysUserQueryDTO query) {
        return Result.success(sysUserService.page(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:user:list')")
    @Operation(summary = "管理员详情")
    public Result<SysUserVO> getById(@PathVariable Long id) {
        return Result.success(sysUserService.getById(id));
    }

    @PostMapping({"", "/"})
    @PreAuthorize("hasAuthority('sys:user:add')")
    @OperLog(module = "系统管理", value = "新增管理员")
    @Operation(summary = "新增管理员")
    public Result<Void> add(@Valid @RequestBody SysUserSaveDTO dto) {
        // 新增接口强制走新增分支，避免误传 id 变成修改
        dto.setId(null);
        sysUserService.save(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:user:edit')")
    @OperLog(module = "系统管理", value = "编辑管理员")
    @Operation(summary = "编辑管理员")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SysUserSaveDTO dto) {
        dto.setId(id);
        sysUserService.save(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:user:delete')")
    @OperLog(module = "系统管理", value = "删除管理员")
    @Operation(summary = "删除管理员")
    public Result<Void> delete(@PathVariable Long id) {
        sysUserService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('sys:user:edit')")
    @OperLog(module = "系统管理", value = "启用/禁用管理员")
    @Operation(summary = "启用/禁用管理员")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @RequestParam(value = "status", required = false) Integer status,
                                     @RequestBody(required = false) Map<String, Object> body) {
        sysUserService.updateStatus(id, resolveStatus(status, body));
        return Result.success();
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('sys:user:reset')")
    @OperLog(module = "系统管理", value = "重置管理员密码", saveParams = false)
    @Operation(summary = "重置管理员密码")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordResetDTO dto) {
        sysUserService.resetPassword(id, dto);
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
