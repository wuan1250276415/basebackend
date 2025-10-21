package com.basebackend.gateway.ratelimit;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayParamFlowItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;

/**
 * 限流规则管理器
 * 实现IP、用户、接口、全局四个维度的限流
 */
@Slf4j
@Component
public class RateLimitRuleManager {

    @PostConstruct
    public void initRules() {
        initApiDefinitions();
        initGatewayRules();
    }

    /**
     * 初始化API定义
     */
    private void initApiDefinitions() {
        Set<ApiDefinition> definitions = new HashSet<>();

        // 定义登录API
        ApiDefinition authApi = new ApiDefinition("auth_api")
                .setPredicateItems(new HashSet<ApiPredicateItem>() {{
                    add(new ApiPathPredicateItem().setPattern("/admin-api/api/auth/**"));
                    add(new ApiPathPredicateItem().setPattern("/basebackend-demo-api/api/auth/**"));
                }});
        definitions.add(authApi);

        // 定义用户API
        ApiDefinition userApi = new ApiDefinition("user_api")
                .setPredicateItems(new HashSet<ApiPredicateItem>() {{
                    add(new ApiPathPredicateItem().setPattern("/admin-api/api/user/**"));
                }});
        definitions.add(userApi);

        // 定义文件API
        ApiDefinition fileApi = new ApiDefinition("file_api")
                .setPredicateItems(new HashSet<ApiPredicateItem>() {{
                    add(new ApiPathPredicateItem().setPattern("/api/files/**"));
                }});
        definitions.add(fileApi);

        GatewayApiDefinitionManager.loadApiDefinitions(definitions);
        log.info("初始化API定义完成, 共{}个", definitions.size());
    }

    /**
     * 初始化网关限流规则
     */
    private void initGatewayRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();

        // 1. 全局限流：整个网关每秒最多1000个请求
        GatewayFlowRule globalRule = new GatewayFlowRule()
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setResource("global")
                .setCount(1000)
                .setIntervalSec(1);
        rules.add(globalRule);

        // 2. 接口限流：登录接口每秒最多10个请求
        GatewayFlowRule authApiRule = new GatewayFlowRule("auth_api")
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setCount(10)
                .setIntervalSec(1);
        rules.add(authApiRule);

        // 3. 接口限流：用户API每秒最多50个请求
        GatewayFlowRule userApiRule = new GatewayFlowRule("user_api")
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setCount(50)
                .setIntervalSec(1);
        rules.add(userApiRule);

        // 4. IP限流：单个IP每秒最多20个请求
        GatewayFlowRule ipLimitRule = new GatewayFlowRule("admin-api")
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_ROUTE_ID)
                .setCount(20)
                .setIntervalSec(1)
                .setParamItem(new GatewayParamFlowItem()
                        .setParseStrategy(SentinelGatewayConstants.PARAM_PARSE_STRATEGY_CLIENT_IP)
                );
        rules.add(ipLimitRule);

        // 5. 用户限流：单个用户每秒最多30个请求（基于Header中的userId）
        GatewayFlowRule userLimitRule = new GatewayFlowRule("admin-api")
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_ROUTE_ID)
                .setCount(30)
                .setIntervalSec(1)
                .setParamItem(new GatewayParamFlowItem()
                        .setParseStrategy(SentinelGatewayConstants.PARAM_PARSE_STRATEGY_HEADER)
                        .setFieldName("X-User-Id")
                );
        rules.add(userLimitRule);

        // 6. 文件上传限流：每秒最多5个请求
        GatewayFlowRule fileUploadRule = new GatewayFlowRule("file_api")
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setCount(5)
                .setIntervalSec(1);
        rules.add(fileUploadRule);

        GatewayRuleManager.loadRules(rules);
        log.info("初始化限流规则完成, 共{}条", rules.size());
    }

    /**
     * 动态添加限流规则
     */
    public void addRule(GatewayFlowRule rule) {
        Set<GatewayFlowRule> rules = new HashSet<>(GatewayRuleManager.getRules());
        rules.add(rule);
        GatewayRuleManager.loadRules(rules);
        log.info("添加限流规则: {}", rule);
    }

    /**
     * 动态删除限流规则
     */
    public void removeRule(String resource) {
        Set<GatewayFlowRule> rules = new HashSet<>(GatewayRuleManager.getRules());
        rules.removeIf(rule -> rule.getResource().equals(resource));
        GatewayRuleManager.loadRules(rules);
        log.info("删除限流规则: {}", resource);
    }

    /**
     * 获取所有限流规则
     */
    public Set<GatewayFlowRule> getAllRules() {
        return GatewayRuleManager.getRules();
    }
}
