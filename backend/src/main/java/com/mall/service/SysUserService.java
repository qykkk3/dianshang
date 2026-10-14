package com.mall.service;

import com.mall.common.PageResult;
import com.mall.dto.PasswordResetDTO;
import com.mall.dto.SysUserQueryDTO;
import com.mall.dto.SysUserSaveDTO;
import com.mall.vo.SysRoleVO;
import com.mall.vo.SysUserVO;

import java.util.List;

/**
 * 后台管理员服务
 */
public interface SysUserService {

    /**
     * 管理员分页查询（keyword 匹配账号/姓名/手机号，可按状态、角色过滤）
     */
    PageResult<SysUserVO> page(SysUserQueryDTO query);

    /**
     * 管理员详情
     */
    SysUserVO getById(Long id);

    /**
     * 新增 / 编辑管理员（id 为空表示新增）
     */
    void save(SysUserSaveDTO dto);

    /**
     * 删除管理员（逻辑删除）
     */
    void delete(Long id);

    /**
     * 启用 / 禁用管理员
     */
    void updateStatus(Long id, Integer status);

    /**
     * 重置管理员密码
     */
    void resetPassword(Long id, PasswordResetDTO dto);

    /**
     * 全部角色列表，供管理员表单的角色下拉选择使用
     */
    List<SysRoleVO> listAllRoles();
}
