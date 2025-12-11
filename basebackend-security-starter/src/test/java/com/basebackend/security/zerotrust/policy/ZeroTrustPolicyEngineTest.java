package com.basebackend.security.zerotrust.policy;

import com.basebackend.security.zerotrust.device.DeviceFingerprintManager;
import com.basebackend.security.zerotrust.risk.RiskAssessmentEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 零信任策略引擎单元测试
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@DisplayName("ZeroTrustPolicyEngine 单元测试")
class ZeroTrustPolicyEngineTest {

    private ZeroTrustPolicyEngine policyEngine;

    @BeforeEach
    void setUp() {
        DeviceFingerprintManager deviceManager = new DeviceFingerprintManager();
        RiskAssessmentEngine riskEngine = new RiskAssessmentEngine();
        policyEngine = new ZeroTrustPolicyEngine(deviceManager, riskEngine);
    }

    @Test
    @DisplayName("默认配置正确初始化")
    void defaultConfigurationInitialized() {
        assertTrue(policyEngine.isEnforceMode());
        assertTrue(policyEngine.isAuditEnabled());
        assertTrue(policyEngine.isPolicyCacheEnabled());
        assertEquals(70, policyEngine.getTrustScoreThreshold());
    }

    @Test
    @DisplayName("设置信任分数阈值生效")
    void setTrustScoreThresholdWorks() {
        policyEngine.setTrustScoreThreshold(80);
        assertEquals(80, policyEngine.getTrustScoreThreshold());
    }

    @Test
    @DisplayName("设置执行模式生效")
    void setEnforceModeWorks() {
        policyEngine.setEnforceMode(false);
        assertFalse(policyEngine.isEnforceMode());
    }

    @Test
    @DisplayName("设置审计模式生效")
    void setAuditEnabledWorks() {
        policyEngine.setAuditEnabled(false);
        assertFalse(policyEngine.isAuditEnabled());
    }

    @Test
    @DisplayName("设置缓存启用生效")
    void setPolicyCacheEnabledWorks() {
        policyEngine.setPolicyCacheEnabled(false);
        assertFalse(policyEngine.isPolicyCacheEnabled());
    }

    @Test
    @DisplayName("评估访问返回决策")
    void evaluateAccessReturnsDecision() {
        ZeroTrustDecision decision = policyEngine.evaluateAccess("user1", "/api/data");

        assertNotNull(decision);
        assertEquals("user1", decision.getUserId());
        assertEquals("/api/data", decision.getResource());
        assertNotNull(decision.getTimestamp());
    }

    @Test
    @DisplayName("多次评估返回有效决策")
    void multipleEvaluationsReturnValidDecisions() {
        ZeroTrustDecision d1 = policyEngine.evaluateAccess("user1", "/api/a");
        ZeroTrustDecision d2 = policyEngine.evaluateAccess("user1", "/api/b");
        ZeroTrustDecision d3 = policyEngine.evaluateAccess("user2", "/api/c");

        assertNotNull(d1);
        assertNotNull(d2);
        assertNotNull(d3);
        assertEquals("user1", d1.getUserId());
        assertEquals("user2", d3.getUserId());
    }
}
