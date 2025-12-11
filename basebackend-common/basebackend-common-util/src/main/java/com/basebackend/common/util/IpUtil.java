package com.basebackend.common.util;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

        String ip = request.getHeader("X-Forwarded-For");
        if (isInvalidIp(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (isInvalidIp(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (isInvalidIp(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (isInvalidIp(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED");
        }
        if (isInvalidIp(ip)) {
            ip = request.getHeader("HTTP_X_CLUSTER_CLIENT_IP");
        }
        if (isInvalidIp(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (isInvalidIp(ip)) {
            ip = request.getHeader("HTTP_FORWARDED_FOR");
        }
        if (isInvalidIp(ip)) {
            ip = request.getHeader("HTTP_FORWARDED");
        }
        if (isInvalidIp(ip)) {
            ip = request.getHeader("HTTP_VIA");
        }
        if (isInvalidIp(ip)) {
            ip = request.getHeader("REMOTE_ADDR");
        }
        if (isInvalidIp(ip)) {
            ip = request.getRemoteAddr();
        }

        // 对于通过多个代理的情况，第一个IP为客户端真实IP，多个IP按照','分割
        if (StrUtil.isNotBlank(ip) && ip.indexOf(",") > 0) {
            ip = ip.substring(0, ip.indexOf(","));
        }

        // IPv6本地地址转换为IPv4
        if (LOCALHOST_IPV6.equals(ip)) {
            ip = LOCALHOST_IP;
        }

        return StrUtil.isBlank(ip) ? LOCALHOST_IP : ip;
    }

    /**
     * 判断IP是否无效
     *
     * @param ip IP地址字符串
     * @return true表示无效
     */
    private static boolean isInvalidIp(String ip) {
        return StrUtil.isBlank(ip) || UNKNOWN.equalsIgnoreCase(ip);
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
