package com.basebackend.security.zerotrust;

import com.basebackend.security.zerotrust.policy.ZeroTrustPolicyEngine;
import com.basebackend.security.zerotrust.device.DeviceFingerprintManager;
import com.basebackend.security.zerotrust.risk.RiskAssessmentEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 零信任健康指示器单元测试
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@DisplayName("ZeroTrustHealthIndicator 单元测试")
class ZeroTrustHealthIndicatorTest {

    private ZeroTrustHealthIndicator healthIndicator;
    private ZeroTrustPolicyEngine policyEngine;

    @BeforeEach
    void setUp() {
        DeviceFingerprintManager deviceManager = new DeviceFingerprintManager();
        RiskAssessmentEngine riskEngine = new RiskAssessmentEngine();
        policyEngine = new ZeroTrustPolicyEngine(deviceManager, riskEngine);
        healthIndicator = new ZeroTrustHealthIndicator(policyEngine);
    }

    @Nested
    @DisplayName("健康状态测试")
    class HealthStatusTests {

        @Test
        @DisplayName("健康检查应返回状态")
        void health_shouldReturnStatus() {
            // When
            Health health = healthIndicator.health();

            // Then
            assertNotNull(health);
            assertNotNull(health.getStatus());
        }

        @Test
        @DisplayName("正常情况下应返回UP状态")
        void health_shouldReturnUpStatus() {
            // When
            Health health = healthIndicator.health();

            // Then
            assertEquals(Status.UP, health.getStatus());
        }

        @Test
        @DisplayName("健康检查应包含组件名称")
        void health_shouldContainComponentName() {
            // When
            Health health = healthIndicator.health();

            // Then
            assertNotNull(health.getDetails());
            assertEquals("ZeroTrust", health.getDetails().get("component"));
        }
    }

    @Nested
    @DisplayName("详情信息测试")
    class DetailsTests {

        @Test
        @DisplayName("健康检查应包含策略引擎信息")
        void health_shouldContainPolicyEngineInfo() {
            // When
            Health health = healthIndicator.health();

            // Then
            assertTrue(health.getDetails().containsKey("policyEngine"));
            Object engineDetails = health.getDetails().get("policyEngine");
            assertNotNull(engineDetails);
            assertTrue(engineDetails instanceof Map);
        }

        @Test
        @DisplayName("策略引擎信息应包含可用状态")
        void policyEngineInfo_shouldContainAvailability() {
            // When
            Health health = healthIndicator.health();

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> engineDetails = (Map<String, Object>) health.getDetails().get("policyEngine");
            assertTrue(engineDetails.containsKey("available"));
            assertEquals(true, engineDetails.get("available"));
        }

        @Test
        @DisplayName("策略引擎信息应包含执行模式")
        void policyEngineInfo_shouldContainEnforceMode() {
            // Given
            policyEngine.setEnforceMode(true);

            // When
            Health health = healthIndicator.health();

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> engineDetails = (Map<String, Object>) health.getDetails().get("policyEngine");
            assertTrue(engineDetails.containsKey("enforceMode"));
            assertEquals(true, engineDetails.get("enforceMode"));
        }

        @Test
        @DisplayName("健康检查应包含内存信息")
        void health_shouldContainMemoryInfo() {
            // When
            Health health = healthIndicator.health();

            // Then
            assertTrue(health.getDetails().containsKey("memory"));
            Object memoryDetails = health.getDetails().get("memory");
            assertNotNull(memoryDetails);
            assertTrue(memoryDetails instanceof Map);
        }

        @Test
        @DisplayName("内存信息应包含使用率")
        void memoryInfo_shouldContainUsedRatio() {
            // When
            Health health = healthIndicator.health();

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> memoryDetails = (Map<String, Object>) health.getDetails().get("memory");
            assertTrue(memoryDetails.containsKey("usedRatio"));
            String usedRatio = (String) memoryDetails.get("usedRatio");
            assertTrue(usedRatio.endsWith("%"));
        }

        @Test
        @DisplayName("健康检查应包含运行时信息")
        void health_shouldContainRuntimeInfo() {
            // When
            Health health = healthIndicator.health();

            // Then
            assertTrue(health.getDetails().containsKey("runtime"));
            Object runtimeDetails = health.getDetails().get("runtime");
            assertNotNull(runtimeDetails);
            assertTrue(runtimeDetails instanceof Map);
        }

        @Test
        @DisplayName("运行时信息应包含启动时间")
        void runtimeInfo_shouldContainStartTime() {
            // When
            Health health = healthIndicator.health();

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> runtimeDetails = (Map<String, Object>) health.getDetails().get("runtime");
            assertTrue(runtimeDetails.containsKey("startTime"));
        }

        @Test
        @DisplayName("运行时信息应包含运行时长")
        void runtimeInfo_shouldContainUptime() {
            // When
            Health health = healthIndicator.health();

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> runtimeDetails = (Map<String, Object>) health.getDetails().get("runtime");
            assertTrue(runtimeDetails.containsKey("uptime"));
        }

        @Test
        @DisplayName("健康检查应包含检查时间")
        void health_shouldContainCheckTime() {
            // When
            Health health = healthIndicator.health();

            // Then
            assertTrue(health.getDetails().containsKey("checkTime"));
        }
    }

    @Nested
    @DisplayName("配置变化测试")
    class ConfigurationChangeTests {

        @Test
        @DisplayName("策略引擎配置变化应反映在健康检查中")
        void configurationChange_shouldBeReflectedInHealth() {
            // Given
            policyEngine.setEnforceMode(false);
            policyEngine.setAuditEnabled(false);

            // When
            Health health = healthIndicator.health();

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> engineDetails = (Map<String, Object>) health.getDetails().get("policyEngine");
            assertEquals(false, engineDetails.get("enforceMode"));
            assertEquals(false, engineDetails.get("auditEnabled"));
        }
    }
}
