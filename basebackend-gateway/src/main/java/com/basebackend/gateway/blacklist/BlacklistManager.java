package com.basebackend.gateway.blacklist;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP 黑白名单管理器
 * <p>
 * 支持静态配置（yml）和运行时动态管理（API），提供 IP 和路径两个维度的访问控制。
 *
 * <pre>
 * gateway:
 *   blacklist:
 *     enabled: true
 *     denied-ips:
 *       - 192.168.1.100
 *       - 10.0.0.0/8
 *     allowed-ips: []          # 为空表示不启用白名单模式
 *     denied-paths:
 *       - /admin/debug/**
 * </pre>
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "gateway.blacklist")
public class BlacklistManager {

    /** 是否启用黑白名单 */
    private boolean enabled = false;

    /** 静态黑名单 IP 列表（配置文件） */
    private List<String> deniedIps = new ArrayList<>();

    /** 静态白名单 IP 列表（配置文件，非空时启用白名单模式） */
    private List<String> allowedIps = new ArrayList<>();

    /** 静态黑名单路径列表 */
    private List<String> deniedPaths = new ArrayList<>();

    /**
     * 受信代理 CIDR 列表，仅当来源命中此列表时才信任 X-Forwarded-For / X-Real-IP
     * 默认值仅本地回环，避免外部伪造转发头
     */
    private List<String> trustedProxyCidrs = new ArrayList<>(Arrays.asList(
            "127.0.0.1/32",
            "::1/128"
    ));

    /** 动态黑名单 IP（运行时通过 API 添加） */
    private final Set<String> dynamicDeniedIps = ConcurrentHashMap.newKeySet();

    /** 动态白名单 IP */
    private final Set<String> dynamicAllowedIps = ConcurrentHashMap.newKeySet();

    /** 动态黑名单路径 */
    private final Set<String> dynamicDeniedPaths = ConcurrentHashMap.newKeySet();

    /** IP 封禁原因记录 */
    private final Map<String, String> banReasons = new ConcurrentHashMap<>();

    /**
     * 检查 IP 是否被拒绝
     *
     * @param ip 客户端 IP
     * @return true = 被拒绝
     */
    public boolean isIpDenied(String ip) {
        if (!enabled || ip == null) {
            return false;
        }

        // 白名单模式：只有白名单中的 IP 才允许
        if (!allowedIps.isEmpty() || !dynamicAllowedIps.isEmpty()) {
            return !allowedIps.contains(ip) && !dynamicAllowedIps.contains(ip);
        }

        // 黑名单模式
        return deniedIps.contains(ip) || dynamicDeniedIps.contains(ip);
    }

    /**
     * 检查路径是否被拒绝
     *
     * @param path 请求路径
     * @return true = 被拒绝
     */
    public boolean isPathDenied(String path) {
        if (!enabled || path == null) {
            return false;
        }

        return deniedPaths.stream().anyMatch(p -> matchPath(path, p))
                || dynamicDeniedPaths.stream().anyMatch(p -> matchPath(path, p));
    }

    // --- 动态管理 API ---

    /** 动态封禁 IP */
    public void denyIp(String ip, String reason) {
        dynamicDeniedIps.add(ip);
        banReasons.put(ip, reason);
        log.info("动态封禁 IP: {}, 原因: {}", ip, reason);
    }

    /** 动态解封 IP */
    public void allowIp(String ip) {
        dynamicDeniedIps.remove(ip);
        banReasons.remove(ip);
        log.info("动态解封 IP: {}", ip);
    }

    /** 动态封禁路径 */
    public void denyPath(String path) {
        dynamicDeniedPaths.add(path);
        log.info("动态封禁路径: {}", path);
    }

    /** 动态解封路径 */
    public void allowPath(String path) {
        dynamicDeniedPaths.remove(path);
        log.info("动态解封路径: {}", path);
    }

    /** 获取封禁原因 */
    public String getBanReason(String ip) {
        return banReasons.get(ip);
    }

    /** 获取所有动态封禁 IP */
    public Set<String> getDynamicDeniedIps() {
        return Set.copyOf(dynamicDeniedIps);
    }

    /** 获取所有动态封禁路径 */
    public Set<String> getDynamicDeniedPaths() {
        return Set.copyOf(dynamicDeniedPaths);
    }

    // --- 路径匹配（简易 Ant 风格） ---

    private boolean matchPath(String path, String pattern) {
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.equals(prefix) || path.startsWith(prefix + "/");
        }
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            // 匹配 prefix/ 后面只有一层路径（不含更多 /）
            if (!path.startsWith(prefix + "/")) {
                return false;
            }
            String remaining = path.substring(prefix.length() + 1);
            return !remaining.isEmpty() && !remaining.contains("/");
        }
        return path.equals(pattern);
    }
}
