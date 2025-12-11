package com.basebackend.featuretoggle.audit;

import com.basebackend.featuretoggle.model.FeatureContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 审计服务单元测试
 */
@DisplayName("FeatureToggleAuditService 单元测试")
class FeatureToggleAuditServiceTest {

    private FeatureToggleAuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new FeatureToggleAuditService();
        auditService.clearRecords();
    }

    @Test
    @DisplayName("记录访问日志")
    void shouldRecordAccess() {
        FeatureContext context = FeatureContext.builder().userId("user-123").build();
        auditService.recordAccess("feature.test", "FEATURE_TOGGLE", true, context);

        List<FeatureToggleAuditService.AuditRecord> records = auditService.getRecentRecords(10);
        assertEquals(1, records.size());
        assertEquals("feature.test", records.get(0).getFeatureName());
        assertEquals("user-123", records.get(0).getUserId());
    }

    @Test
    @DisplayName("记录带变体的访问日志")
    void shouldRecordAccessWithVariant() {
        FeatureContext context = FeatureContext.builder().userId("user-123").build();
        auditService.recordAccessWithVariant("feature.abtest", "AB_TEST", true, context, "variant-A");

        List<FeatureToggleAuditService.AuditRecord> records = auditService.getRecentRecords(10);
        assertEquals(1, records.size());
        assertEquals("variant-A", records.get(0).getVariantName());
    }

    @Test
    @DisplayName("记录配置变更")
    void shouldRecordConfigChange() {
        auditService.recordConfigChange("feature.test", "ENABLED", "false", "true", "admin");

        List<FeatureToggleAuditService.AuditRecord> records = auditService.getRecentRecords(10);
        assertEquals(1, records.size());
        assertEquals("CONFIG_CHANGE", records.get(0).getOperationType());
        assertEquals("admin", records.get(0).getOperator());
    }

    @Test
    @DisplayName("按特性名称查询")
    void shouldQueryByFeatureName() {
        FeatureContext context = FeatureContext.builder().userId("user-1").build();
        auditService.recordAccess("feature.a", "FEATURE_TOGGLE", true, context);
        auditService.recordAccess("feature.b", "FEATURE_TOGGLE", false, context);
        auditService.recordAccess("feature.a", "FEATURE_TOGGLE", true, context);

        List<FeatureToggleAuditService.AuditRecord> records = auditService.getRecordsByFeature("feature.a", 10);
        assertEquals(2, records.size());
    }

    @Test
    @DisplayName("获取统计信息")
    void shouldGetStatistics() {
        FeatureContext context = FeatureContext.builder().userId("user-1").build();
        auditService.recordAccess("feature.a", "FEATURE_TOGGLE", true, context);
        auditService.recordAccess("feature.b", "GRADUAL_ROLLOUT", false, context);
        auditService.recordAccessWithVariant("feature.c", "AB_TEST", true, context, "v1");

        FeatureToggleAuditService.AuditStatistics stats = auditService.getStatistics();
        assertEquals(3, stats.getTotalRecords());
        assertEquals(2, stats.getEnabledCount());
        assertEquals(1, stats.getFeatureToggleCount());
        assertEquals(1, stats.getGradualRolloutCount());
        assertEquals(1, stats.getAbTestCount());
    }

    @Test
    @DisplayName("清空记录")
    void shouldClearRecords() {
        FeatureContext context = FeatureContext.builder().userId("user-1").build();
        auditService.recordAccess("feature.a", "FEATURE_TOGGLE", true, context);
        auditService.clearRecords();

        List<FeatureToggleAuditService.AuditRecord> records = auditService.getRecentRecords(10);
        assertTrue(records.isEmpty());
    }
}
