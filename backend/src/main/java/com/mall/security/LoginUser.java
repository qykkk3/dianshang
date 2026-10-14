package com.mall.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 登录用户信息（买家 / 管理员共用），缓存于 Redis
 *
 * <p>刻意不实现 Spring Security 的 UserDetails：该类会被序列化进 Redis，
 * 计算型 getter 会干扰 JSON 反序列化，权限集合在过滤器中另行构建。</p>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户类型：买家端 */
    public static final String TYPE_BUYER = "BUYER";
    /** 用户类型：管理端 */
    public static final String TYPE_ADMIN = "ADMIN";

    /** 用户ID（买家为 user.id，管理员为 sys_user.id） */
    private Long userId;

    /** 登录账号 */
    private String username;

    /** 昵称 / 姓名 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 用户类型 BUYER / ADMIN */
    private String userType;

    /** 角色ID（管理端） */
    private Long roleId;

    /** 角色编码（管理端）ADMIN / OPERATOR / WAREHOUSE */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 权限标识集合（管理端） */
    private List<String> permissions = new ArrayList<>();

    /** 当前有效 Token */
    private String token;

    /** 登录时间戳 */
    private Long loginTime;

    /** 过期时间戳 */
    private Long expireTime;

    public boolean isAdmin() {
        return TYPE_ADMIN.equals(this.userType);
    }
}
