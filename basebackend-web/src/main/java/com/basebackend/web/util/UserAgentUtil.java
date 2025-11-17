package com.basebackend.web.util;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * User-Agent解析工具类
 */
@Slf4j
public class UserAgentUtil {

    /**
     * 获取浏览器信息
     */
    public static String getBrowser(HttpServletRequest request) {
        if (request == null) {
            return "Unknown";
        }

        String userAgent = request.getHeader("User-Agent");
        return getBrowserFromUserAgent(userAgent);
    }

    /**
     * 从User-Agent字符串中解析浏览器
     */
    public static String getBrowserFromUserAgent(String userAgent) {
        if (StrUtil.isBlank(userAgent)) {
            return "Unknown";
        }

        userAgent = userAgent.toLowerCase();

        if (userAgent.contains("edge") || userAgent.contains("edg")) {
            return "Edge";
        } else if (userAgent.contains("chrome")) {
            return "Chrome";
        } else if (userAgent.contains("firefox")) {
            return "Firefox";
        } else if (userAgent.contains("safari") && !userAgent.contains("chrome")) {
            return "Safari";
        } else if (userAgent.contains("opera") || userAgent.contains("opr")) {
            return "Opera";
        } else if (userAgent.contains("msie") || userAgent.contains("trident")) {
            return "IE";
        } else {
            return "Other";
        }
    }

    /**
     * 获取操作系统信息
     */
    public static String getOperatingSystem(HttpServletRequest request) {
        if (request == null) {
            return "Unknown";
        }

        String userAgent = request.getHeader("User-Agent");
        return getOsFromUserAgent(userAgent);
    }

    /**
     * 从User-Agent字符串中解析操作系统
     */
    public static String getOsFromUserAgent(String userAgent) {
        if (StrUtil.isBlank(userAgent)) {
            return "Unknown";
        }

        userAgent = userAgent.toLowerCase();

        if (userAgent.contains("windows nt 10.0")) {
            return "Windows 10";
        } else if (userAgent.contains("windows nt 6.3")) {
            return "Windows 8.1";
        } else if (userAgent.contains("windows nt 6.2")) {
            return "Windows 8";
        } else if (userAgent.contains("windows nt 6.1")) {
            return "Windows 7";
        } else if (userAgent.contains("windows nt 6.0")) {
            return "Windows Vista";
        } else if (userAgent.contains("windows nt 5.1")) {
            return "Windows XP";
        } else if (userAgent.contains("windows")) {
            return "Windows";
        } else if (userAgent.contains("mac os x")) {
            return "Mac OS X";
        } else if (userAgent.contains("mac")) {
            return "Mac";
        } else if (userAgent.contains("linux")) {
            return "Linux";
        } else if (userAgent.contains("android")) {
            return "Android";
        } else if (userAgent.contains("iphone") || userAgent.contains("ipad")) {
            return "iOS";
        } else {
            return "Other";
        }
    }
}
