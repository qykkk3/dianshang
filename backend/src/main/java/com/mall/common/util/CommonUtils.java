package com.mall.common.util;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 常用工具方法
 */
public final class CommonUtils {

    private CommonUtils() {
    }

    private static final String UNKNOWN = "unknown";

    /**
     * 获取客户端真实 IP
     */
    public static String getIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (isInvalid(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (isInvalid(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (isInvalid(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (isInvalid(ip)) {
            ip = request.getRemoteAddr();
            if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
                try {
                    ip = InetAddress.getLocalHost().getHostAddress();
                } catch (UnknownHostException ignored) {
                    ip = "127.0.0.1";
                }
            }
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private static boolean isInvalid(String ip) {
        return ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip);
    }
}
