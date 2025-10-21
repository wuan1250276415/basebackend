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
     */
    public String addRoute(RouteDefinition routeDefinition) {
        try {
            routeDefinitionWriter.save(Mono.just(routeDefinition)).subscribe();
            notifyRouteChanged();
            log.info("添加路由成功: {}", routeDefinition.getId());
            return "success";
        } catch (Exception e) {
            log.error("添加路由失败", e);
            return "failed: " + e.getMessage();
        }
    }

    /**
     * 更新路由
     */
    public String updateRoute(RouteDefinition routeDefinition) {
        try {
            // 先删除后添加
            routeDefinitionWriter.delete(Mono.just(routeDefinition.getId())).subscribe();
            routeDefinitionWriter.save(Mono.just(routeDefinition)).subscribe();
            notifyRouteChanged();
            log.info("更新路由成功: {}", routeDefinition.getId());
            return "success";
        } catch (Exception e) {
            log.error("更新路由失败", e);
            return "failed: " + e.getMessage();
        }
    }

    /**
     * 删除路由
     */
    public String deleteRoute(String routeId) {
        try {
            routeDefinitionWriter.delete(Mono.just(routeId)).subscribe();
            notifyRouteChanged();
            log.info("删除路由成功: {}", routeId);
            return "success";
        } catch (Exception e) {
            log.error("删除路由失败", e);
            return "failed: " + e.getMessage();
        }
    }

    /**
     * 通知路由变更
     */
    private void notifyRouteChanged() {
        eventPublisher.publishEvent(new RefreshRoutesEvent(this));
    }
}
