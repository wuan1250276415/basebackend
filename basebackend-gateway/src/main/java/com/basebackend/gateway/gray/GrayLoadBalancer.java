package com.basebackend.gateway.gray;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 灰度路由过滤器
 * 支持基于Header、用户、IP、权重的灰度路由
 */
@Slf4j
@Component
public class GrayLoadBalancer {

    private final GrayRouteProperties grayRouteProperties;

    /**
     * 用于校验受信代理的 CIDR 列表
     */
    private final List<CidrMatcher> trustedProxyMatchers;

    /**
     * 构造函数，初始化受信代理匹配器
     */
    public GrayLoadBalancer(GrayRouteProperties grayRouteProperties) {
        this.grayRouteProperties = grayRouteProperties;
        this.trustedProxyMatchers = grayRouteProperties.getTrustedProxyCidrs()
                .stream()
                .map(CidrMatcher::new)
                .collect(Collectors.toList());
    }

    /**
     * CIDR匹配器 - 简单的IP范围匹配（支持IPv4和IPv6）
     */
    private static class CidrMatcher {
        private final String cidr;
        private final InetAddress networkAddress;
        private final int prefixLength;
        private final int maxPrefixLength;

        public CidrMatcher(String cidr) {
            this.cidr = cidr;
            String[] parts = cidr.split("/");
            String ip = parts[0];

            try {
                this.networkAddress = InetAddress.getByName(ip);
                byte[] addressBytes = networkAddress.getAddress();
                // IPv4: 4字节, IPv6: 16字节
                this.maxPrefixLength = addressBytes.length * 8;

                // 设置默认前缀长度
                if (parts.length > 1) {
                    this.prefixLength = Integer.parseInt(parts[1]);
                } else {
                    // 无掩码时使用默认：IPv4为32，IPv6为128
                    this.prefixLength = maxPrefixLength;
                }

                // 校验前缀长度合法性
                if (this.prefixLength < 0 || this.prefixLength > this.maxPrefixLength) {
                    throw new IllegalArgumentException(
                            String.format("Invalid prefix length %d for CIDR %s (max: %d)",
                                    this.prefixLength, cidr, this.maxPrefixLength));
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid CIDR: " + cidr, e);
            }
        }

        public boolean matches(String ip) {
            try {
                InetAddress address = InetAddress.getByName(ip);
                byte[] addressBytes = address.getAddress();
                byte[] networkBytes = networkAddress.getAddress();

                // 检查地址族是否一致（IPv4 vs IPv6）
                if (addressBytes.length != networkBytes.length) {
                    log.debug("地址族不匹配: CIDR {} ({}) vs IP {} ({})",
                            cidr, networkBytes.length + "字节", ip, addressBytes.length + "字节");
                    return false;
                }

                int bytesToCompare = prefixLength / 8;
                int bitsToCompare = prefixLength % 8;

                // 完全比较前面的字节
                for (int i = 0; i < bytesToCompare; i++) {
                    if (addressBytes[i] != networkBytes[i]) {
                        return false;
                    }
                }

                // 比较剩余的位
                if (bitsToCompare > 0) {
                    byte mask = (byte) (0xFF << (8 - bitsToCompare));
                    return (addressBytes[bytesToCompare] & mask) == (networkBytes[bytesToCompare] & mask);
                }

                return true;
            } catch (Exception e) {
                log.warn("IP匹配失败: {}", ip, e);
                return false;
            }
        }
    }

    /**
     * 选择服务实例（支持灰度路由和会话黏性）
     */
    public ServiceInstance choose(String serviceId, ServerWebExchange exchange, List<ServiceInstance> instances) {
        if (!grayRouteProperties.getEnabled() || instances == null || instances.isEmpty()) {
            // 未启用灰度或无实例，返回第一个
            return instances != null && !instances.isEmpty() ? instances.get(0) : null;
        }

        // 查找服务的灰度规则
        GrayRouteProperties.GrayRule grayRule = findGrayRule(serviceId);
        if (grayRule == null) {
            // 无灰度规则，使用会话黏性选择实例
            return chooseStickyInstance(instances, exchange);
        }

        // 根据策略选择目标版本
        String targetVersion = determineTargetVersion(grayRule, exchange);
        log.debug("服务: {}, 目标版本: {}", serviceId, targetVersion);

        // 筛选符合版本的实例
        List<ServiceInstance> matchedInstances = instances.stream()
                .filter(instance -> {
                    String version = instance.getMetadata().get("version");
                    return targetVersion.equals(version);
                })
                .collect(Collectors.toList());

        if (matchedInstances.isEmpty()) {
            log.warn("未找到版本{}的实例，使用会话黏性选择默认实例", targetVersion);
            return chooseStickyInstance(instances, exchange);
        }

        // 使用会话黏性选择符合版本的实例
        return chooseStickyInstance(matchedInstances, exchange);
    }

    /**
     * 确定目标版本
     */
    private String determineTargetVersion(GrayRouteProperties.GrayRule rule, ServerWebExchange exchange) {
        String strategy = rule.getStrategy();
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();

        switch (strategy) {
            case "header":
                // 基于Header
                String headerValue = headers.getFirst(rule.getHeaderName());
                if (rule.getHeaderValue().equals(headerValue)) {
                    return rule.getGrayVersion();
                }
                break;

            case "user":
                // 基于用户ID
                String userId = headers.getFirst("X-User-Id");
                if (StringUtils.hasText(userId) && rule.getUserIds() != null && rule.getUserIds().contains(userId)) {
                    return rule.getGrayVersion();
                }
                break;

            case "ip":
                // 基于IP
                String clientIp = getClientIp(request);
                if (rule.getIpList() != null && rule.getIpList().contains(clientIp)) {
                    return rule.getGrayVersion();
                }
                break;

            case "weight":
                // 基于权重，使用稳定哈希驱动，确保同一用户落同一版本
                int bucket = stableBucket(exchange, 100);
                if (bucket < rule.getWeight()) {
                    return rule.getGrayVersion();
                }
                break;

            default:
                log.warn("未知的灰度策略: {}", strategy);
        }

        // 默认返回稳定版本
        return rule.getStableVersion();
    }

    /**
     * 查找服务的灰度规则
     */
    private GrayRouteProperties.GrayRule findGrayRule(String serviceId) {
        return grayRouteProperties.getRules().stream()
                .filter(rule -> rule.getServiceName().equals(serviceId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取客户端IP（安全的IP获取，仅在受信代理时信任转发头）
     */
    private String getClientIp(ServerHttpRequest request) {
        // 仅当请求来自受信代理时才信任 Forwarded / X-Forwarded-For / X-Real-IP
        boolean trusted = isFromTrustedProxy(request);
        HttpHeaders headers = request.getHeaders();

        if (trusted) {
            // 尝试解析 Forwarded 头部
            String forwarded = headers.getFirst("Forwarded");
            if (StringUtils.hasText(forwarded)) {
                // 简单解析 for= 实体，形如: for=1.2.3.4;proto=https
                String forPart = Arrays.stream(forwarded.split(";"))
                        .map(String::trim)
                        .filter(s -> s.startsWith("for="))
                        .map(s -> s.substring(4).replace("\"", ""))
                        .findFirst().orElse(null);
                if (StringUtils.hasText(forPart) && !"unknown".equalsIgnoreCase(forPart)) {
                    return stripPort(forPart);
                }
            }

            // 尝试解析 X-Forwarded-For
            String xff = headers.getFirst("X-Forwarded-For");
            if (StringUtils.hasText(xff) && !"unknown".equalsIgnoreCase(xff)) {
                return stripPort(xff.split(",")[0]);
            }

            // 尝试解析 X-Real-IP
            String xri = headers.getFirst("X-Real-IP");
            if (StringUtils.hasText(xri) && !"unknown".equalsIgnoreCase(xri)) {
                return stripPort(xri);
            }
        }

        // 非受信来源，回退到 RemoteAddress
        return request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    /**
     * 检查请求是否来自受信代理
     */
    private boolean isFromTrustedProxy(ServerHttpRequest request) {
        if (request.getRemoteAddress() == null) {
            return false;
        }
        String sourceIp = request.getRemoteAddress().getAddress().getHostAddress();
        return trustedProxyMatchers.stream().anyMatch(matcher -> matcher.matches(sourceIp));
    }

    /**
     * 剥离IP地址中的端口号
     * 正确处理IPv4、IPv6和带端口的情况
     */
    private String stripPort(String hostPort) {
        // IPv6带端口格式: [2001:db8::1]:8080
        if (hostPort.startsWith("[")) {
            int closeBracketIdx = hostPort.indexOf(']');
            if (closeBracketIdx > 0) {
                if (closeBracketIdx + 1 < hostPort.length() && hostPort.charAt(closeBracketIdx + 1) == ':') {
                    // 带有端口号，返回括号内容
                    return hostPort.substring(0, closeBracketIdx + 1);
                }
                // 无端口号，直接返回
                return hostPort.substring(0, closeBracketIdx + 1);
            }
        }

        // IPv4格式: 192.168.1.1:8080 或 IPv6无括号: 2001:db8::1
        // 通过冒号数量判断：IPv6有多个冒号，IPv4只有一个冒号
        long colonCount = hostPort.chars().filter(c -> c == ':').count();
        if (colonCount == 1) {
            // IPv4:port格式，截取端口
            int idx = hostPort.lastIndexOf(':');
            return hostPort.substring(0, idx);
        }

        // IPv6无端口号或单个冒号但不是端口分隔符
        return hostPort;
    }

    /**
     * 使用会话黏性选择实例（基于用户ID或IP的稳定哈希）
     */
    private ServiceInstance chooseStickyInstance(List<ServiceInstance> instances, ServerWebExchange exchange) {
        String stickyKey = resolveStickyKey(exchange);
        if (!StringUtils.hasText(stickyKey)) {
            // 无黏性键时，使用ThreadLocalRandom随机选择
            return instances.get(ThreadLocalRandom.current().nextInt(instances.size()));
        }
        int idx = Math.floorMod(stableHash(stickyKey), instances.size());
        return instances.get(idx);
    }

    /**
     * 解析会话黏性键（优先使用用户ID，其次使用IP）
     */
    private String resolveStickyKey(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        // 优先使用用户ID
        String userId = headers.getFirst("X-User-Id");
        if (StringUtils.hasText(userId)) {
            return "user:" + userId;
        }
        // 备选使用IP
        String ip = getClientIp(exchange.getRequest());
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return "ip:" + ip;
        }
        return null;
    }

    /**
     * 计算稳定哈希值（基于String.hashCode）
     * 使用floorMod避免Integer.MIN_VALUE的边界情况
     */
    private int stableHash(String value) {
        int h = value.hashCode();
        return h == Integer.MIN_VALUE ? 0 : h;
    }

    /**
     * 计算稳定桶编号（用于权重策略）
     */
    private int stableBucket(ServerWebExchange exchange, int modulo) {
        String key = resolveStickyKey(exchange);
        if (!StringUtils.hasText(key)) {
            return ThreadLocalRandom.current().nextInt(modulo);
        }
        return Math.floorMod(stableHash(key), modulo);
    }
}
