package com.basebackend.gateway.route;

import lombok.Data;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * 路由定义DTO
 * 用于API接口的路由配置传输
 */
@Data
public class RouteDefinitionDTO {

    /**
     * 路由ID
     */
    private String id;

    /**
     * 路由URI
     */
    private String uri;

    /**
     * 路由断言
     */
    private List<PredicateDefinition> predicates = new ArrayList<>();

    /**
     * 路由过滤器
     */
    private List<FilterDefinition> filters = new ArrayList<>();

    /**
     * 路由顺序
     */
    private int order = 0;

    /**
     * 路由元数据
     */
    private String metadata;
}
