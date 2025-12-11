package com.basebackend.gateway.ratelimit;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayParamFlowItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 限流规则管理器
 * <p>
 * 实现 IP、用户、接口、全局四个维度的限流。
 * 支持规则的增量更新，避免每次修改都重新加载全部规则。
 * </p>
 *
 * <h3>优化特性：</h3>
 * <ul>
 * <li>增量更新：只更新变化的规则</li>
 * <li>规则缓存：本地缓存规则，减少 Sentinel 调用</li>
 * <li>并发安全：使用读写锁保护规则更新</li>
 * <li>批量操作：支持批量添加/删除规则</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "spring.cloud.sentinel.enabled", havingValue = "true", matchIfMissing = false)
public class RateLimitRuleManager {

        /**
         * 规则本地缓存（按资源名索引）
         */
        private final Map<String, GatewayFlowRule> ruleCache = new ConcurrentHashMap<>();

        /**
         * API 定义本地缓存
         */
        private final Map<String, ApiDefinition> apiCache = new ConcurrentHashMap<>();

        /**
         * 读写锁保护规则更新
         */
        private final ReadWriteLock ruleLock = new ReentrantReadWriteLock();

        /**
         * 规则变更标记
         */
        private volatile boolean rulesDirty = false;

        @PostConstruct
        public void initRules() {
                initApiDefinitions();
                initGatewayRules();
        }

        /**
         * 初始化 API 定义
         */
        private void initApiDefinitions() {
                Set<ApiDefinition> definitions = new HashSet<>();

                // 定义登录 API
                Set<ApiPredicateItem> authPredicateItems = new HashSet<>();
                authPredicateItems.add(new ApiPathPredicateItem().setPattern("/admin-api/api/auth/**"));
                authPredicateItems.add(new ApiPathPredicateItem().setPattern("/basebackend-demo-api/api/auth/**"));
                ApiDefinition authApi = new ApiDefinition("auth_api")
                                .setPredicateItems(authPredicateItems);
                definitions.add(authApi);
                apiCache.put("auth_api", authApi);

                // 定义用户 API
                Set<ApiPredicateItem> userPredicateItems = new HashSet<>();
                userPredicateItems.add(new ApiPathPredicateItem().setPattern("/admin-api/api/user/**"));
                ApiDefinition userApi = new ApiDefinition("user_api")
                                .setPredicateItems(userPredicateItems);
                definitions.add(userApi);
                apiCache.put("user_api", userApi);

                // 定义文件 API
                Set<ApiPredicateItem> filePredicateItems = new HashSet<>();
                filePredicateItems.add(new ApiPathPredicateItem().setPattern("/api/files/**"));
                ApiDefinition fileApi = new ApiDefinition("file_api")
                                .setPredicateItems(filePredicateItems);
                definitions.add(fileApi);
                apiCache.put("file_api", fileApi);

                GatewayApiDefinitionManager.loadApiDefinitions(definitions);
                log.info("初始化 API 定义完成, 共{}个", definitions.size());
        }

        /**
         * 初始化网关限流规则
         */
        private void initGatewayRules() {
                ruleLock.writeLock().lock();
                try {
                        Set<GatewayFlowRule> rules = new HashSet<>();

                        // 1. 全局限流：整个网关每秒最多 1000 个请求
                        GatewayFlowRule globalRule = new GatewayFlowRule()
                                        .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                                        .setResource("global")
                                        .setCount(1000)
                                        .setIntervalSec(1);
                        rules.add(globalRule);
                        cacheRule("global", globalRule);

                        // 2. 接口限流：登录接口每秒最多 10 个请求
                        GatewayFlowRule authApiRule = new GatewayFlowRule("auth_api")
                                        .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                                        .setCount(10)
                                        .setIntervalSec(1);
                        rules.add(authApiRule);
                        cacheRule("auth_api", authApiRule);

                        // 3. 接口限流：用户 API 每秒最多 50 个请求
                        GatewayFlowRule userApiRule = new GatewayFlowRule("user_api")
                                        .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                                        .setCount(50)
                                        .setIntervalSec(1);
                        rules.add(userApiRule);
                        cacheRule("user_api", userApiRule);

                        // 4. IP 限流：单个 IP 每秒最多 20 个请求
                        GatewayFlowRule ipLimitRule = new GatewayFlowRule("admin-api")
                                        .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_ROUTE_ID)
                                        .setCount(20)
                                        .setIntervalSec(1)
                                        .setParamItem(new GatewayParamFlowItem()
                                                        .setParseStrategy(
                                                                        SentinelGatewayConstants.PARAM_PARSE_STRATEGY_CLIENT_IP));
                        rules.add(ipLimitRule);
                        cacheRule("admin-api:ip", ipLimitRule);

                        // 5. 用户限流：单个用户每秒最多 30 个请求
                        GatewayFlowRule userLimitRule = new GatewayFlowRule("admin-api")
                                        .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_ROUTE_ID)
                                        .setCount(30)
                                        .setIntervalSec(1)
                                        .setParamItem(new GatewayParamFlowItem()
                                                        .setParseStrategy(
                                                                        SentinelGatewayConstants.PARAM_PARSE_STRATEGY_HEADER)
                                                        .setFieldName("X-User-Id"));
                        rules.add(userLimitRule);
                        cacheRule("admin-api:user", userLimitRule);

                        // 6. 文件上传限流：每秒最多 5 个请求
                        GatewayFlowRule fileUploadRule = new GatewayFlowRule("file_api")
                                        .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                                        .setCount(5)
                                        .setIntervalSec(1);
                        rules.add(fileUploadRule);
                        cacheRule("file_api", fileUploadRule);

                        GatewayRuleManager.loadRules(rules);
                        rulesDirty = false;
                        log.info("初始化限流规则完成, 共{}条", rules.size());
                } finally {
                        ruleLock.writeLock().unlock();
                }
        }

        /**
         * 缓存规则
         */
        private void cacheRule(String key, GatewayFlowRule rule) {
                ruleCache.put(key, rule);
        }

        /**
         * 动态添加限流规则（增量更新）
         *
         * @param rule 限流规则
         */
        public void addRule(GatewayFlowRule rule) {
                ruleLock.writeLock().lock();
                try {
                        String key = generateRuleKey(rule);
                        ruleCache.put(key, rule);
                        flushRulesToSentinel();
                        log.info("添加限流规则: {}", rule);
                } finally {
                        ruleLock.writeLock().unlock();
                }
        }

        /**
         * 批量添加限流规则
         *
         * @param rules 规则集合
         */
        public void addRules(Set<GatewayFlowRule> rules) {
                if (rules == null || rules.isEmpty()) {
                        return;
                }

                ruleLock.writeLock().lock();
                try {
                        for (GatewayFlowRule rule : rules) {
                                String key = generateRuleKey(rule);
                                ruleCache.put(key, rule);
                        }
                        flushRulesToSentinel();
                        log.info("批量添加限流规则: {}条", rules.size());
                } finally {
                        ruleLock.writeLock().unlock();
                }
        }

        /**
         * 动态删除限流规则
         *
         * @param resource 资源名称
         */
        public void removeRule(String resource) {
                ruleLock.writeLock().lock();
                try {
                        // 移除匹配的规则
                        ruleCache.entrySet().removeIf(entry -> entry.getKey().equals(resource)
                                        || entry.getKey().startsWith(resource + ":"));
                        flushRulesToSentinel();
                        log.info("删除限流规则: {}", resource);
                } finally {
                        ruleLock.writeLock().unlock();
                }
        }

        /**
         * 批量删除限流规则
         *
         * @param resources 资源名称集合
         */
        public void removeRules(Set<String> resources) {
                if (resources == null || resources.isEmpty()) {
                        return;
                }

                ruleLock.writeLock().lock();
                try {
                        for (String resource : resources) {
                                ruleCache.entrySet().removeIf(entry -> entry.getKey().equals(resource)
                                                || entry.getKey().startsWith(resource + ":"));
                        }
                        flushRulesToSentinel();
                        log.info("批量删除限流规则: {}条", resources.size());
                } finally {
                        ruleLock.writeLock().unlock();
                }
        }

        /**
         * 更新限流规则
         *
         * @param resource 资源名称
         * @param rule     新规则
         */
        public void updateRule(String resource, GatewayFlowRule rule) {
                ruleLock.writeLock().lock();
                try {
                        String key = generateRuleKey(rule);
                        if (ruleCache.containsKey(key)) {
                                ruleCache.put(key, rule);
                                flushRulesToSentinel();
                                log.info("更新限流规则: {}", rule);
                        } else {
                                log.warn("规则不存在，无法更新: {}", resource);
                        }
                } finally {
                        ruleLock.writeLock().unlock();
                }
        }

        /**
         * 将缓存的规则刷新到 Sentinel
         */
        private void flushRulesToSentinel() {
                Set<GatewayFlowRule> rules = new HashSet<>(ruleCache.values());
                GatewayRuleManager.loadRules(rules);
                rulesDirty = false;
        }

        /**
         * 生成规则缓存 key
         */
        private String generateRuleKey(GatewayFlowRule rule) {
                String resource = rule.getResource();
                GatewayParamFlowItem paramItem = rule.getParamItem();
                if (paramItem != null) {
                        int strategy = paramItem.getParseStrategy();
                        if (strategy == SentinelGatewayConstants.PARAM_PARSE_STRATEGY_CLIENT_IP) {
                                return resource + ":ip";
                        } else if (strategy == SentinelGatewayConstants.PARAM_PARSE_STRATEGY_HEADER) {
                                return resource + ":header:" + paramItem.getFieldName();
                        } else if (strategy == SentinelGatewayConstants.PARAM_PARSE_STRATEGY_URL_PARAM) {
                                return resource + ":param:" + paramItem.getFieldName();
                        }
                }
                return resource;
        }

        /**
         * 获取所有限流规则
         *
         * @return 规则集合
         */
        public Set<GatewayFlowRule> getAllRules() {
                ruleLock.readLock().lock();
                try {
                        return new HashSet<>(ruleCache.values());
                } finally {
                        ruleLock.readLock().unlock();
                }
        }

        /**
         * 获取指定资源的规则
         *
         * @param resource 资源名称
         * @return 规则（如果存在）
         */
        public Optional<GatewayFlowRule> getRule(String resource) {
                ruleLock.readLock().lock();
                try {
                        return Optional.ofNullable(ruleCache.get(resource));
                } finally {
                        ruleLock.readLock().unlock();
                }
        }

        /**
         * 获取规则数量
         *
         * @return 规则数量
         */
        public int getRuleCount() {
                return ruleCache.size();
        }

        /**
         * 获取 API 定义
         *
         * @param apiName API 名称
         * @return API 定义（如果存在）
         */
        public Optional<ApiDefinition> getApiDefinition(String apiName) {
                return Optional.ofNullable(apiCache.get(apiName));
        }

        /**
         * 添加 API 定义
         *
         * @param apiDefinition API 定义
         */
        public void addApiDefinition(ApiDefinition apiDefinition) {
                apiCache.put(apiDefinition.getApiName(), apiDefinition);
                GatewayApiDefinitionManager.loadApiDefinitions(new HashSet<>(apiCache.values()));
                log.info("添加 API 定义: {}", apiDefinition.getApiName());
        }

        /**
         * 删除 API 定义
         *
         * @param apiName API 名称
         */
        public void removeApiDefinition(String apiName) {
                apiCache.remove(apiName);
                GatewayApiDefinitionManager.loadApiDefinitions(new HashSet<>(apiCache.values()));
                log.info("删除 API 定义: {}", apiName);
        }

        /**
         * 清除所有规则
         */
        public void clearAllRules() {
                ruleLock.writeLock().lock();
                try {
                        ruleCache.clear();
                        GatewayRuleManager.loadRules(new HashSet<>());
                        log.warn("已清除所有限流规则");
                } finally {
                        ruleLock.writeLock().unlock();
                }
        }

        /**
         * 重新加载所有规则
         */
        public void reloadRules() {
                ruleLock.writeLock().lock();
                try {
                        ruleCache.clear();
                        apiCache.clear();
                        initApiDefinitions();
                        initGatewayRules();
                        log.info("已重新加载所有限流规则");
                } finally {
                        ruleLock.writeLock().unlock();
                }
        }
}
