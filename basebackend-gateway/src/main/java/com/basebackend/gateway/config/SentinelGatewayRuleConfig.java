package com.basebackend.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;

import java.util.HashSet;
import java.util.Set;

/**
 * Sentinel 网关规则配置
 * 配置白名单路径，避免认证接口被限流和权限控制
 */
@Slf4j
@Configuration
public class SentinelGatewayRuleConfig {

    @PostConstruct
    public void initGatewayRules() {
        // 配置请求来源解析器，标记认证请求为可信来源
        GatewayCallbackManager.setRequestOriginParser(this::parseOrigin);

        // 定义认证API组
        Set<ApiDefinition> definitions = new HashSet<>();
        ApiDefinition authApi = new ApiDefinition("auth_api")
                .setPredicateItems(new HashSet<ApiPredicateItem>() {{
                    add(new ApiPathPredicateItem()
                            .setPattern("/admin-api/api/admin/auth/**")
                            .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX));
                }});
        definitions.add(authApi);

        // 定义工作流API组
        ApiDefinition workflowApi = new ApiDefinition("workflow_api")
                .setPredicateItems(new HashSet<ApiPredicateItem>() {{
                    add(new ApiPathPredicateItem()
                            .setPattern("/api/workflow/**")
                            .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX));
                }});
        definitions.add(workflowApi);

        GatewayApiDefinitionManager.loadApiDefinitions(definitions);

        log.info("Sentinel网关规则配置完成，已定义API组: auth_api, workflow_api");
    }

    /**
     * 解析请求来源
     * 对于认证相关路径，返回特殊标识，用于绕过权限控制
     */
    private String parseOrigin(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();

        // 认证相关路径标记为可信来源，绕过Sentinel权限控制
        if (isAuthPath(path)) {
            log.debug("认证路径 {} 标记为可信来源", path);
            return "trusted-auth-request";
        }

        // 其他请求返回默认来源
        return "default";
    }

    /**
     * 判断是否是认证相关路径
     */
    private boolean isAuthPath(String path) {
        return path != null && (
            path.contains("/auth/login") ||
            path.contains("/auth/register") ||
            path.contains("/auth/logout") ||
            path.contains("/auth/refresh") ||
            path.contains("/auth/captcha") ||
            path.contains("/auth/sms-code")
        );
    }
}
