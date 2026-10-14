package com.mall.service;

import com.mall.dto.AdminLoginDTO;
import com.mall.vo.AdminLoginVO;
import com.mall.vo.MenuVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 管理端登录鉴权服务
 */
public interface AdminAuthService {

    /**
     * 管理端登录
     *
     * @param dto     登录参数
     * @param request 当前请求（用于取登录 IP，可为 null）
     * @return 令牌、用户信息、权限标识与菜单树
     */
    AdminLoginVO login(AdminLoginDTO dto, HttpServletRequest request);

    /**
     * 管理端登录（不记录 IP）
     */
    default AdminLoginVO login(AdminLoginDTO dto) {
        return login(dto, null);
    }

    /**
     * 退出登录
     */
    void logout();

    /**
     * 获取当前登录管理员信息（重新查库，角色/权限变更即时生效）
     */
    AdminLoginVO getCurrentInfo();

    /**
     * 当前登录管理员的菜单树
     */
    List<MenuVO> listMenus();
}
