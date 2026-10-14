package com.mall.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT 认证过滤器：解析请求头中的 Token，构建 Spring Security 上下文
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** 用户类型权限前缀：TYPE_ADMIN / TYPE_BUYER */
    public static final String TYPE_AUTHORITY_PREFIX = "TYPE_";
    /** 角色权限前缀：ROLE_ADMIN ... */
    public static final String ROLE_AUTHORITY_PREFIX = "ROLE_";

    private final TokenService tokenService;

    @Value("${mall.jwt.header:Authorization}")
    private String header;

    @Value("${mall.jwt.prefix:Bearer }")
    private String prefix;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            LoginUser loginUser = tokenService.getLoginUser(token);
            if (loginUser != null) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(loginUser, null, buildAuthorities(loginUser));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * 从请求头解析 Token
     */
    private String resolveToken(HttpServletRequest request) {
        String value = request.getHeader(header);
        if (value == null || value.isBlank()) {
            return null;
        }
        if (prefix != null && !prefix.isBlank() && value.startsWith(prefix.trim())) {
            value = value.substring(prefix.trim().length());
        }
        value = value.trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * 构建权限集合：用户类型 + 角色 + 具体权限标识
     */
    public static List<GrantedAuthority> buildAuthorities(LoginUser loginUser) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (loginUser.getUserType() != null) {
            authorities.add(new SimpleGrantedAuthority(TYPE_AUTHORITY_PREFIX + loginUser.getUserType()));
        }
        if (loginUser.getRoleCode() != null && !loginUser.getRoleCode().isBlank()) {
            authorities.add(new SimpleGrantedAuthority(ROLE_AUTHORITY_PREFIX + loginUser.getRoleCode()));
        }
        if (loginUser.getPermissions() != null) {
            loginUser.getPermissions().stream()
                    .filter(p -> p != null && !p.isBlank())
                    .distinct()
                    .forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        }
        return authorities;
    }
}
