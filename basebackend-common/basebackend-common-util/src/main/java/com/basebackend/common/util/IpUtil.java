package com.basebackend.common.util;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;

/**
 * IP地址工具类
 * <p>
 * 提供从HTTP请求中获取客户端真实IP地址的功能，
 * 支持代理服务器、负载均衡等场景下的IP获取。
 * </p>
 *
 * @author basebackend
 * @since 1.0.0
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IpUtil {

    private static final String UNKNOWN = "unknown";
    private static final String LOCALHOST_IP = "127.0.0.1";
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";
    private static final String LOCALHOST_IPV6_SHORT = "::1";
    private static final String[] FORWARDED_IP_HEADERS = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA"
    };

    /**
     * 获取客户端IP地址
     * <p>
     * 按优先级检查多个代理相关的HTTP头，获取客户端真实IP地址。
     * </p>
     *
     * @param request HTTP请求对象
     * @return 客户端IP地址
     */
    public static String getIpAddress(HttpServletRequest request) {
        if (request == null) {
            return LOCALHOST_IP;
        }

        String remoteIp = normalizeIp(request.getRemoteAddr());
        if (isInvalidIp(remoteIp)) {
            remoteIp = normalizeIp(request.getHeader("REMOTE_ADDR"));
        }

        if (isTrustedProxy(remoteIp)) {
            String forwardedIp = extractForwardedClientIp(request);
            if (!isInvalidIp(forwardedIp)) {
                return forwardedIp;
            }
        }

        return StrUtil.isBlank(remoteIp) ? LOCALHOST_IP : remoteIp;
    }

    /**
     * 判断IP是否无效
     *
     * @param ip IP地址字符串
     * @return true表示无效
     */
    private static boolean isInvalidIp(String ip) {
        return StrUtil.isBlank(ip) || UNKNOWN.equalsIgnoreCase(ip.trim());
    }

    /**
     * 提取转发链中的客户端IP
     *
     * @param request HTTP请求
     * @return 客户端IP（无可用值返回 null）
     */
    private static String extractForwardedClientIp(HttpServletRequest request) {
        for (String header : FORWARDED_IP_HEADERS) {
            String ip = extractFirstValidIp(request.getHeader(header));
            if (!isInvalidIp(ip)) {
                return ip;
            }
        }
        return null;
    }

    /**
     * 从可能包含多个 IP 的字符串中提取第一个可用 IP
     *
     * @param ipChain IP链（逗号分隔）
     * @return 第一个可用IP
     */
    private static String extractFirstValidIp(String ipChain) {
        if (isInvalidIp(ipChain)) {
            return null;
        }

        String[] parts = ipChain.split(",");
        for (String part : parts) {
            String normalized = normalizeIp(part);
            if (!isInvalidIp(normalized)) {
                return normalized;
            }
        }
        return null;
    }

    /**
     * 判断远端地址是否为可信代理
     *
     * @param remoteIp 远端地址
     * @return true 表示可信代理
     */
    private static boolean isTrustedProxy(String remoteIp) {
        if (isInvalidIp(remoteIp)) {
            return false;
        }
        String normalized = normalizeIp(remoteIp);
        if (LOCALHOST_IP.equals(normalized) || isInternalIp(normalized)) {
            return true;
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        return lower.startsWith("fe80:") || lower.startsWith("fc") || lower.startsWith("fd");
    }

    /**
     * 规范化 IP 值
     *
     * @param ip 原始IP
     * @return 规范化后的IP
     */
    private static String normalizeIp(String ip) {
        if (StrUtil.isBlank(ip)) {
            return ip;
        }
        String normalized = ip.trim();
        if (LOCALHOST_IPV6.equals(normalized) || LOCALHOST_IPV6_SHORT.equals(normalized)) {
            return LOCALHOST_IP;
        }
        return normalized;
    }

    /**
     * 根据IP地址获取地理位置（简化版本）
     * <p>
     * 实际生产环境建议使用 ip2region 或调用第三方IP定位API
     * </p>
     *
     * @param ip IP地址
     * @return 地理位置描述
     */
    public static String getLocationByIp(String ip) {
        if (StrUtil.isBlank(ip)) {
            return "未知";
        }

        // 本地IP
        if (LOCALHOST_IP.equals(ip) || "localhost".equals(ip)) {
            return "本地";
        }

        // 内网IP
        if (isInternalIp(ip)) {
            return "内网IP";
        }

        // 这里简化处理，实际应该使用IP地址库或API进行查询
        return "未知地址";
    }

    /**
     * 判断是否为内网IP
     *
     * @param ip IP地址
     * @return true表示是内网IP
     */
    public static boolean isInternalIp(String ip) {
        if (StrUtil.isBlank(ip)) {
            return false;
        }

        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }

        try {
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);

            // 10.0.0.0 - 10.255.255.255
            if (first == 10) {
                return true;
            }

            // 172.16.0.0 - 172.31.255.255
            if (first == 172 && second >= 16 && second <= 31) {
                return true;
            }

            // 192.168.0.0 - 192.168.255.255
            if (first == 192 && second == 168) {
                return true;
            }

            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
