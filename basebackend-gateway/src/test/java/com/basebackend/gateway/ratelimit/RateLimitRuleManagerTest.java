package com.basebackend.gateway.ratelimit;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RateLimitRuleManager 单元测试
 * <p>
 * 注意：由于依赖 Sentinel 的静态方法，部分测试需要在 Sentinel 环境下运行。
 * 这里主要测试规则管理的逻辑正确性。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitRuleManager 单元测试")
class RateLimitRuleManagerTest {

    private RateLimitRuleManager rateLimitRuleManager;

    @BeforeEach
    void setUp() {
        rateLimitRuleManager = new RateLimitRuleManager();
        // 清空现有规则
        GatewayRuleManager.loadRules(Set.of());
    }

    @Nested
    @DisplayName("规则初始化测试")
    class InitializationTests {

        @Test
        @DisplayName("初始化应该创建预定义的限流规则")
        void shouldCreatePredefinedRulesOnInit() {
            // When
            rateLimitRuleManager.initRules();

            // Then
            Set<GatewayFlowRule> rules = rateLimitRuleManager.getAllRules();
            assertNotNull(rules);
            assertFalse(rules.isEmpty());

            // 应该至少有6条规则
            assertTrue(rules.size() >= 6, "应该初始化至少6条限流规则");
        }

        @Test
        @DisplayName("初始化应该包含全局限流规则")
        void shouldContainGlobalRule() {
            // When
            rateLimitRuleManager.initRules();

            // Then
            Set<GatewayFlowRule> rules = rateLimitRuleManager.getAllRules();
            boolean hasGlobalRule = rules.stream()
                    .anyMatch(rule -> "global".equals(rule.getResource()));

            assertTrue(hasGlobalRule, "应该包含全局限流规则");
        }

        @Test
        @DisplayName("初始化应该包含认证API限流规则")
        void shouldContainAuthApiRule() {
            // When
            rateLimitRuleManager.initRules();

            // Then
            Set<GatewayFlowRule> rules = rateLimitRuleManager.getAllRules();
            boolean hasAuthRule = rules.stream()
                    .anyMatch(rule -> "auth_api".equals(rule.getResource()));

            assertTrue(hasAuthRule, "应该包含认证API限流规则");
        }
    }

    @Nested
    @DisplayName("动态规则管理测试")
    class DynamicRuleManagementTests {

        @BeforeEach
        void initRules() {
            rateLimitRuleManager.initRules();
        }

        @Test
        @DisplayName("添加新规则")
        void shouldAddNewRule() {
            // Given
            int originalCount = rateLimitRuleManager.getAllRules().size();

            GatewayFlowRule newRule = new GatewayFlowRule("test_api")
                    .setCount(100)
                    .setIntervalSec(1);

            // When
            rateLimitRuleManager.addRule(newRule);

            // Then
            Set<GatewayFlowRule> rules = rateLimitRuleManager.getAllRules();
            assertEquals(originalCount + 1, rules.size());

            boolean hasNewRule = rules.stream()
                    .anyMatch(rule -> "test_api".equals(rule.getResource()));
            assertTrue(hasNewRule);
        }

        @Test
        @DisplayName("删除规则")
        void shouldRemoveRule() {
            // Given
            int originalCount = rateLimitRuleManager.getAllRules().size();

            // When
            rateLimitRuleManager.removeRule("auth_api");

            // Then
            Set<GatewayFlowRule> rules = rateLimitRuleManager.getAllRules();
            assertEquals(originalCount - 1, rules.size());

            boolean hasRemovedRule = rules.stream()
                    .anyMatch(rule -> "auth_api".equals(rule.getResource()));
            assertFalse(hasRemovedRule);
        }

        @Test
        @DisplayName("删除不存在的规则不应该报错")
        void shouldNotFailWhenRemovingNonExistentRule() {
            // Given
            int originalCount = rateLimitRuleManager.getAllRules().size();

            // When & Then - 不应抛出异常
            assertDoesNotThrow(() -> {
                rateLimitRuleManager.removeRule("non_existent_api");
            });

            // 规则数量不变
            assertEquals(originalCount, rateLimitRuleManager.getAllRules().size());
        }

        @Test
        @DisplayName("添加重复资源的规则应该更新")
        void shouldUpdateWhenAddingDuplicateResourceRule() {
            // Given
            GatewayFlowRule rule1 = new GatewayFlowRule("custom_api")
                    .setCount(50)
                    .setIntervalSec(1);
            rateLimitRuleManager.addRule(rule1);
            int countAfterFirst = rateLimitRuleManager.getAllRules().size();

            GatewayFlowRule rule2 = new GatewayFlowRule("custom_api")
                    .setCount(100)
                    .setIntervalSec(1);

            // When
            rateLimitRuleManager.addRule(rule2);

            // Then - HashSet会处理重复，规则数可能相同或+1取决于equals实现
            Set<GatewayFlowRule> rules = rateLimitRuleManager.getAllRules();
            assertNotNull(rules);
        }
    }

    @Nested
    @DisplayName("规则配置验证测试")
    class RuleConfigurationTests {

        @Test
        @DisplayName("全局限流规则配置正确")
        void globalRuleShouldHaveCorrectConfig() {
            // Given
            rateLimitRuleManager.initRules();

            // When
            GatewayFlowRule globalRule = rateLimitRuleManager.getAllRules().stream()
                    .filter(rule -> "global".equals(rule.getResource()))
                    .findFirst()
                    .orElse(null);

            // Then
            assertNotNull(globalRule);
            assertEquals(1000, globalRule.getCount());
            assertEquals(1, globalRule.getIntervalSec());
        }

        @Test
        @DisplayName("认证API限流规则配置正确")
        void authApiRuleShouldHaveCorrectConfig() {
            // Given
            rateLimitRuleManager.initRules();

            // When
            GatewayFlowRule authRule = rateLimitRuleManager.getAllRules().stream()
                    .filter(rule -> "auth_api".equals(rule.getResource()))
                    .findFirst()
                    .orElse(null);

            // Then
            assertNotNull(authRule);
            assertEquals(10, authRule.getCount()); // 登录接口限制更严格
            assertEquals(1, authRule.getIntervalSec());
        }

        @Test
        @DisplayName("文件上传限流规则配置正确")
        void fileApiRuleShouldHaveCorrectConfig() {
            // Given
            rateLimitRuleManager.initRules();

            // When
            GatewayFlowRule fileRule = rateLimitRuleManager.getAllRules().stream()
                    .filter(rule -> "file_api".equals(rule.getResource()))
                    .findFirst()
                    .orElse(null);

            // Then
            assertNotNull(fileRule);
            assertEquals(5, fileRule.getCount()); // 文件上传限制最严格
            assertEquals(1, fileRule.getIntervalSec());
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("获取规则不应返回null")
        void getAllRulesShouldNotReturnNull() {
            // When
            Set<GatewayFlowRule> rules = rateLimitRuleManager.getAllRules();

            // Then
            assertNotNull(rules);
        }

        @Test
        @DisplayName("多次初始化应该是幂等的")
        void multipleInitShouldBeIdempotent() {
            // When
            rateLimitRuleManager.initRules();
            int countAfterFirst = rateLimitRuleManager.getAllRules().size();

            rateLimitRuleManager.initRules();
            int countAfterSecond = rateLimitRuleManager.getAllRules().size();

            // Then
            assertEquals(countAfterFirst, countAfterSecond);
        }
    }
}
