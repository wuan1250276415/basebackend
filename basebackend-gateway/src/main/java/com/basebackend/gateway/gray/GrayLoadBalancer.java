package com.basebackend.gateway.gray;

import lombok.RequiredArgsConstructor;
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

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 灰度路由过滤器
 * 支持基于Header、用户、IP、权重的灰度路由
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GrayLoadBalancer {

    private final GrayRouteProperties grayRouteProperties;
    private final Random random = new Random();

    /**
     * 选择服务实例（支持灰度路由）
     */
    public ServiceInstance choose(String serviceId, ServerWebExchange exchange, List<ServiceInstance> instances) {
        if (!grayRouteProperties.getEnabled() || instances == null || instances.isEmpty()) {
            // 未启用灰度或无实例，返回第一个
            return instances != null && !instances.isEmpty() ? instances.get(0) : null;
        }

        // 查找服务的灰度规则
        GrayRouteProperties.GrayRule grayRule = findGrayRule(serviceId);
        if (grayRule == null) {
            // 无灰度规则，随机返回
            return instances.get(random.nextInt(instances.size()));
        }

        // 根据策略选择实例
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
            log.warn("未找到版本{}的实例，使用默认实例", targetVersion);
            return instances.get(random.nextInt(instances.size()));
        }

        // 随机选择一个匹配的实例
        return matchedInstances.get(random.nextInt(matchedInstances.size()));
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
                // 基于权重
                int randomWeight = random.nextInt(100);
                if (randomWeight < rule.getWeight()) {
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
     * 获取客户端IP
     */
    private String getClientIp(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();

        String ip = headers.getFirst("X-Forwarded-For");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0];
        }

        ip = headers.getFirst("X-Real-IP");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        return request.getRemoteAddress() != null ?
                request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }
}
