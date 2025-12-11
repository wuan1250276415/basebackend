package com.basebackend.gateway.controller;

import com.basebackend.gateway.route.DynamicRouteService;
import com.basebackend.gateway.route.RouteDefinitionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * 动态路由管理控制器
 * 提供路由的增删改查接口
 */
@Slf4j
@RestController
@RequestMapping("/actuator/gateway/routes")
@RequiredArgsConstructor
@Validated
public class RouteController {

    private final DynamicRouteService dynamicRouteService;
    private final RouteDefinitionLocator routeDefinitionLocator;

    /**
     * 获取所有路由
     */
    @GetMapping
    public Flux<RouteDefinition> getAllRoutes() {
        return routeDefinitionLocator.getRouteDefinitions();
    }

    /**
     * 添加路由
     */
    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> addRoute(@RequestBody RouteDefinitionDTO routeDTO) {
        RouteDefinition routeDefinition = convertToRouteDefinition(routeDTO);
        return dynamicRouteService.addRoute(routeDefinition)
                .map(result -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", "success".equals(result));
                    response.put("message", result);
                    response.put("routeId", routeDTO.getId());
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(e -> {
                    log.error("添加路由失败", e);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                });
    }

    /**
     * 更新路由
     */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> updateRoute(
            @PathVariable String id,
            @RequestBody RouteDefinitionDTO routeDTO) {
        routeDTO.setId(id);
        RouteDefinition routeDefinition = convertToRouteDefinition(routeDTO);
        return dynamicRouteService.updateRoute(routeDefinition)
                .map(result -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", "success".equals(result));
                    response.put("message", result);
                    response.put("routeId", id);
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(e -> {
                    log.error("更新路由失败", e);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                });
    }

    /**
     * 删除路由
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> deleteRoute(@PathVariable String id) {
        return dynamicRouteService.deleteRoute(id)
                .map(result -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", "success".equals(result));
                    response.put("message", result);
                    response.put("routeId", id);
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(e -> {
                    log.error("删除路由失败", e);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                });
    }

    /**
     * 转换DTO为RouteDefinition
     */
    private RouteDefinition convertToRouteDefinition(RouteDefinitionDTO dto) {
        RouteDefinition definition = new RouteDefinition();
        definition.setId(dto.getId());
        definition.setUri(URI.create(dto.getUri()));
        definition.setPredicates(dto.getPredicates());
        definition.setFilters(dto.getFilters());
        definition.setOrder(dto.getOrder());
        return definition;
    }
}
