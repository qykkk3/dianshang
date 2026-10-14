package com.mall.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Token 服务：JWT 无状态签名 + Redis 存储实现「可主动失效」的登录态
 *
 * <p>Redis 结构：</p>
 * <ul>
 *   <li>mall:login:{type}:{userId} -> LoginUser（含当前有效 token，TTL = jwt 有效期）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    /** 登录用户缓存前缀 */
    public static final String LOGIN_KEY_PREFIX = "mall:login:";

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    private String buildKey(String userType, Long userId) {
        return LOGIN_KEY_PREFIX + userType + ":" + userId;
    }

    /**
     * 创建 Token 并写入 Redis
     */
    public String createToken(LoginUser loginUser) {
        String token = jwtUtil.createToken(loginUser);
        loginUser.setToken(token);
        long now = System.currentTimeMillis();
        loginUser.setLoginTime(now);
        loginUser.setExpireTime(now + jwtUtil.getExpireSeconds() * 1000);
        refreshToken(loginUser);
        return token;
    }

    /**
     * 刷新登录态缓存
     */
    public void refreshToken(LoginUser loginUser) {
        redisTemplate.opsForValue().set(buildKey(loginUser.getUserType(), loginUser.getUserId()),
                loginUser, jwtUtil.getExpireSeconds(), TimeUnit.SECONDS);
    }

    /**
     * 依据 Token 获取登录用户；Token 非法 / 已过期 / 已登出 时返回 null
     */
    public LoginUser getLoginUser(String token) {
        Claims claims = jwtUtil.parseToken(token);
        if (claims == null) {
            return null;
        }
        Long userId = jwtUtil.getUserId(claims);
        String userType = jwtUtil.getUserType(claims);
        if (userId == null || userType == null) {
            return null;
        }
        Object cached = redisTemplate.opsForValue().get(buildKey(userType, userId));
        if (!(cached instanceof LoginUser loginUser)) {
            return null;
        }
        // 同一账号在新设备登录后，旧 Token 立即失效
        if (!token.equals(loginUser.getToken())) {
            return null;
        }
        return loginUser;
    }

    /**
     * 退出登录：删除登录态
     */
    public void deleteToken(String token) {
        LoginUser loginUser = getLoginUser(token);
        if (loginUser != null) {
            redisTemplate.delete(buildKey(loginUser.getUserType(), loginUser.getUserId()));
        }
    }

    /**
     * 管理员被禁用 / 角色变更时强制下线
     */
    public void kickOut(String userType, Long userId) {
        redisTemplate.delete(buildKey(userType, userId));
    }

    /**
     * 统计当前在线用户数（Redis 中的登录态数量）
     */
    public Long countOnline() {
        Set<String> keys = redisTemplate.keys(LOGIN_KEY_PREFIX + "*");
        return keys == null ? 0L : (long) keys.size();
    }
}
