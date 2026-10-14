package com.mall.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具：生成与解析令牌
 */
@Slf4j
@Component
public class JwtUtil {

    private static final String CLAIM_UID = "uid";
    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ROLE = "role";

    @Value("${mall.jwt.secret}")
    private String secret;

    @Value("${mall.jwt.expire:604800}")
    private Long expireSeconds;

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Token
     */
    public String createToken(LoginUser loginUser) {
        long now = System.currentTimeMillis();
        Date expiration = new Date(now + expireSeconds * 1000);
        return Jwts.builder()
                .id(UUID.randomUUID().toString().replace("-", ""))
                .subject(loginUser.getUsername())
                .claim(CLAIM_UID, loginUser.getUserId())
                .claim(CLAIM_TYPE, loginUser.getUserType())
                .claim(CLAIM_ROLE, loginUser.getRoleCode())
                .issuedAt(new Date(now))
                .expiration(expiration)
                .signWith(secretKey())
                .compact();
    }

    /**
     * 解析 Token，失败返回 null
     */
    public Claims parseToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return Jwts.parser()
                    .verifyWith(secretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.debug("Token 解析失败: {}", e.getMessage());
            return null;
        }
    }

    public Long getUserId(Claims claims) {
        Object uid = claims.get(CLAIM_UID);
        return uid == null ? null : Long.valueOf(String.valueOf(uid));
    }

    public String getUserType(Claims claims) {
        Object type = claims.get(CLAIM_TYPE);
        return type == null ? null : String.valueOf(type);
    }

    public String getRoleCode(Claims claims) {
        Object role = claims.get(CLAIM_ROLE);
        return role == null ? null : String.valueOf(role);
    }

    /** Token 有效期（秒） */
    public Long getExpireSeconds() {
        return expireSeconds;
    }
}
