package com.basebackend.web.version;

import com.basebackend.web.annotation.ApiVersion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

/**
 * API 版本拦截器
 * 自动识别和验证 API 版本
 *
 * @author basebackend
 * @since 2025-11-23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VersionInterceptor implements HandlerInterceptor {

    private final ApiVersionManager versionManager;

    private static final String VERSION_KEY = "api_version";
    private static final String HEADER_NAME = "X-API-Version";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        Object bean = handlerMethod.getBean();

        // 获取方法上的版本注解
        ApiVersion methodVersion = method.getAnnotation(ApiVersion.class);
        if (methodVersion == null) {
            methodVersion = bean.getClass().getAnnotation(ApiVersion.class);
        }

        // 如果没有版本注解，直接通过
        if (methodVersion == null) {
            request.setAttribute(VERSION_KEY, "v1"); // 默认版本
            return true;
        }

        // 获取请求版本
        String requestVersion = extractVersion(request);

        // 验证版本是否支持
        if (!versionManager.isVersionSupported(requestVersion)) {
            log.warn("Unsupported API version: {} for path: {}", requestVersion, request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            response.getWriter().write("{\"code\":406,\"message\":\"Unsupported API version: " + requestVersion + "\"}");
            return false;
        }

        // 记录版本信息
        log.debug("API version detected: {} for path: {}", requestVersion, request.getRequestURI());
        request.setAttribute(VERSION_KEY, requestVersion);
        request.setAttribute(HEADER_NAME, requestVersion);

        return true;
    }

    /**
     * 从请求中提取版本信息
     */
    private String extractVersion(HttpServletRequest request) {
        // 1. 优先从请求头获取
        String version = request.getHeader(HEADER_NAME);
        if (version != null && !version.isEmpty()) {
            return normalizeVersion(version);
        }

        // 2. 从Accept头获取
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("version=")) {
            String[] parts = accept.split(";");
            for (String part : parts) {
                if (part.trim().startsWith("version=")) {
                    version = part.trim().substring("version=".length());
                    return normalizeVersion(version);
                }
            }
        }

        // 3. 从URL路径获取
        version = versionManager.getVersionFromPath(request.getRequestURI());
        return normalizeVersion(version);
    }

    /**
     * 标准化版本号格式
     */
    private String normalizeVersion(String version) {
        if (version == null || version.isEmpty()) {
            return "v1";
        }

        // 如果没有v前缀，自动添加
        if (!version.startsWith("v")) {
            return "v" + version;
        }

        return version;
    }

    /**
     * 获取请求的版本号
     */
    public static String getVersionFromRequest(HttpServletRequest request) {
        Object version = request.getAttribute(VERSION_KEY);
        return version != null ? version.toString() : "v1";
    }
}
