package com.mall.service;

import com.mall.dto.RolePermissionDTO;
import com.mall.dto.SysRoleSaveDTO;
import com.mall.vo.SysPermissionVO;
import com.mall.vo.SysRoleVO;

import java.util.List;

/**
 * 角色与权限服务
 */
public interface SysRoleService {

    /**
     * 角色列表（含 permissionIds 与关联管理员数量）
     */
    List<SysRoleVO> list();

    /**
     * 角色详情（含 permissionIds 与关联管理员数量）
     */
    SysRoleVO getById(Long id);

    /**
     * 新增 / 编辑角色（id 为空表示新增）
     */
    void save(SysRoleSaveDTO dto);

    /**
     * 删除角色（物理删除角色与角色权限关联）
     */
    void delete(Long id);

    /**
     * 启用 / 停用角色
     */
    void updateStatus(Long id, Integer status);

    /**
     * 分配角色权限（全量覆盖）
     */
    void assignPermissions(Long id, RolePermissionDTO dto);

    /**
     * 完整权限树，供角色分配权限使用
     */
    List<SysPermissionVO> listAllPermissions();
}
