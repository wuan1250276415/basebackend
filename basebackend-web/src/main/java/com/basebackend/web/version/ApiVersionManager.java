package com.basebackend.web.version;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API 版本管理器
 * 管理不同版本的API接口
 *
 * @author basebackend
 * @since 2025-11-23
 */
@Slf4j
public class ApiVersionManager {

    private final Map<String, Map<String, Method>> versionedMethods = new ConcurrentHashMap<>();
    private final Map<String, String> versionDescriptions = new ConcurrentHashMap<>();

    /**
     * 注册版本化方法
     */
    public void registerVersionedMethod(String version, String path, Method method, String description) {
        versionedMethods.computeIfAbsent(version, k -> new ConcurrentHashMap<>());
        versionedMethods.get(version).put(path, method);

        if (description != null) {
            versionDescriptions.put(version, description);
        }

        log.info("Registered API version {} - {} : {}", version, path, description);
    }

    /**
     * 获取指定版本的API方法
     */
    public Method getMethod(String version, String path) {
        Map<String, Method> methods = versionedMethods.get(version);
        if (methods == null) {
            return null;
        }
        return methods.get(path);
    }

    /**
     * 获取所有支持的版本
     */
    public List<String> getSupportedVersions() {
        return new ArrayList<>(versionedMethods.keySet());
    }

    /**
     * 从请求头获取版本信息
     */
    public String getVersionFromHeader(HttpServletRequest request) {
        // 优先级：X-API-Version > Accept > 其他
        String version = request.getHeader("X-API-Version");
        if (version != null && !version.isEmpty()) {
            return version;
        }

        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("version=")) {
            return parseVersionFromAcceptHeader(accept);
        }

        return "v1"; // 默认版本
    }

    /**
     * 从URL路径获取版本信息
     */
    public String getVersionFromPath(String path) {
        if (path.startsWith("/v")) {
            int end = path.indexOf('/', 1);
            return end > 1 ? path.substring(1, end) : path.substring(1);
        }
        return "v1";
    }

    private String parseVersionFromAcceptHeader(String accept) {
        // 解析 Accept: application/json;version=v2
        String[] parts = accept.split(";");
        for (String part : parts) {
            if (part.trim().startsWith("version=")) {
                return part.trim().substring("version=".length());
            }
        }
        return "v1";
    }

    /**
     * 检查版本是否支持
     */
    public boolean isVersionSupported(String version) {
        return versionedMethods.containsKey(version);
    }

    /**
     * 获取版本描述
     */
    public String getVersionDescription(String version) {
        return versionDescriptions.getOrDefault(version, "Unknown version");
    }

    /**
     * 注册默认版本
     */
    public void registerDefaultVersion() {
        // 默认支持 v1
        if (!versionedMethods.containsKey("v1")) {
            versionedMethods.put("v1", new ConcurrentHashMap<>());
            versionDescriptions.put("v1", "Initial API version");
            log.info("Registered default version: v1");
        }
    }
}
