package com.basebackend.gateway.route;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 动态路由服务
 * 支持运行时动态添加、更新、删除路由
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicRouteService {

    private final RouteDefinitionWriter routeDefinitionWriter;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 添加路由
     *
     * @param routeDefinition 路由定义
     * @return 操作结果的Mono，成功返回"success"，失败返回错误信息
     */
    public Mono<String> addRoute(RouteDefinition routeDefinition) {
        return routeDefinitionWriter.save(Mono.just(routeDefinition))
                .then(Mono.fromRunnable(this::notifyRouteChanged))
                .thenReturn("success")
                .doOnSuccess(result -> log.info("添加路由成功: {}", routeDefinition.getId()))
                .onErrorResume(e -> {
                    log.error("添加路由失败: {}", routeDefinition.getId(), e);
                    return Mono.just("failed: " + e.getMessage());
                });
    }

    /**
     * 更新路由
     * 先删除旧路由，再保存新路由，最后通知路由变更
     *
     * @param routeDefinition 路由定义
     * @return 操作结果的Mono，成功返回"success"，失败返回错误信息
     */
    public Mono<String> updateRoute(RouteDefinition routeDefinition) {
        return routeDefinitionWriter.delete(Mono.just(routeDefinition.getId()))
                .onErrorResume(e -> {
                    // 仅忽略"路由不存在"的错误，其他错误（如网络/权限）应该中断
                    if (e.getMessage() != null && e.getMessage().contains("not found")) {
                        log.debug("删除旧路由时未找到: {}", routeDefinition.getId());
                        return Mono.empty();
                    }
                    // 其他错误向上抛出
                    return Mono.error(e);
                })
                .then(routeDefinitionWriter.save(Mono.just(routeDefinition)))
                .then(Mono.fromRunnable(this::notifyRouteChanged))
                .thenReturn("success")
                .doOnSuccess(result -> log.info("更新路由成功: {}", routeDefinition.getId()))
                .onErrorResume(e -> {
                    log.error("更新路由失败: {}", routeDefinition.getId(), e);
                    return Mono.just("failed: " + e.getMessage());
                });
    }

    /**
     * 删除路由
     *
     * @param routeId 路由ID
     * @return 操作结果的Mono，成功返回"success"，失败返回错误信息
     */
    public Mono<String> deleteRoute(String routeId) {
        return routeDefinitionWriter.delete(Mono.just(routeId))
                .then(Mono.fromRunnable(this::notifyRouteChanged))
                .thenReturn("success")
                .doOnSuccess(result -> log.info("删除路由成功: {}", routeId))
                .onErrorResume(e -> {
                    log.error("删除路由失败: {}", routeId, e);
                    return Mono.just("failed: " + e.getMessage());
                });
    }

    /**
     * 通知路由变更
     */
    private void notifyRouteChanged() {
        eventPublisher.publishEvent(new RefreshRoutesEvent(this));
    }
}
