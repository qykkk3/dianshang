package com.mall.security;

import com.mall.common.BusinessException;
import com.mall.common.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具：获取当前登录用户
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户，未登录返回 null
     */
    public static LoginUser getLoginUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof LoginUser loginUser ? loginUser : null;
    }

    /**
     * 获取当前登录用户，未登录抛 401
     */
    public static LoginUser getLoginUser() {
        LoginUser loginUser = getLoginUserOrNull();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    /**
     * 当前登录买家ID，未登录抛 401
     */
    public static Long getUserId() {
        return getLoginUser().getUserId();
    }

    /**
     * 当前登录买家ID，未登录返回 null（用于前台「可选登录」场景，如是否已收藏）
     */
    public static Long getUserIdOrNull() {
        LoginUser loginUser = getLoginUserOrNull();
        return loginUser == null ? null : loginUser.getUserId();
    }

    /** 当前登录账号 */
    public static String getUsername() {
        LoginUser loginUser = getLoginUserOrNull();
        return loginUser == null ? "匿名" : loginUser.getUsername();
    }

    /** 是否为管理端登录 */
    public static boolean isAdmin() {
        LoginUser loginUser = getLoginUserOrNull();
        return loginUser != null && loginUser.isAdmin();
    }

    /** 当前请求的 Token */
    public static String getToken() {
        LoginUser loginUser = getLoginUserOrNull();
        return loginUser == null ? null : loginUser.getToken();
    }
}
