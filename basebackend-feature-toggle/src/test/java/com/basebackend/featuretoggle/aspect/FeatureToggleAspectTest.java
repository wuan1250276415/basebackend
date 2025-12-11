package com.basebackend.featuretoggle.aspect;

import com.basebackend.featuretoggle.audit.FeatureToggleAuditService;
import com.basebackend.featuretoggle.context.FeatureContextHolder;
import com.basebackend.featuretoggle.metrics.FeatureToggleMetrics;
import com.basebackend.featuretoggle.model.FeatureContext;
import com.basebackend.featuretoggle.model.Variant;
import com.basebackend.featuretoggle.service.FeatureToggleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 特性开关切面单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeatureToggleAspect 单元测试")
class FeatureToggleAspectTest {

    @Mock
    private FeatureToggleService featureToggleService;
    @Mock
    private FeatureToggleMetrics featureToggleMetrics;
    @Mock
    private FeatureToggleAuditService auditService;

    private FeatureToggleAspect aspect;

    @BeforeEach
    void setUp() {
        when(featureToggleService.getProviderName()).thenReturn("MockProvider");
        aspect = new FeatureToggleAspect(featureToggleService, featureToggleMetrics, auditService);
    }

    @AfterEach
    void tearDown() {
        FeatureContextHolder.clear();
    }

    @Test
    @DisplayName("切面初始化成功")
    void shouldInitializeSuccessfully() {
        assertNotNull(aspect);
        verify(featureToggleService).getProviderName();
    }

    @Test
    @DisplayName("上下文持有器正确存储和获取上下文")
    void shouldStoreAndRetrieveContext() {
        FeatureContext context = FeatureContext.builder()
                .userId("user-123").tenantId("tenant-456").build();
        FeatureContextHolder.set(context);
        FeatureContext retrieved = FeatureContextHolder.get();

        assertNotNull(retrieved);
        assertEquals("user-123", retrieved.getUserId());
        assertEquals("tenant-456", retrieved.getTenantId());
    }

    @Test
    @DisplayName("上下文持有器清除后返回空上下文")
    void shouldReturnEmptyContextAfterClear() {
        FeatureContextHolder.set(FeatureContext.builder().userId("user-123").build());
        FeatureContextHolder.clear();
        FeatureContext retrieved = FeatureContextHolder.get();

        assertNotNull(retrieved);
        assertNull(retrieved.getUserId());
    }

    @Test
    @DisplayName("变体持有器正确存储和获取变体")
    void shouldStoreAndRetrieveVariant() {
        Variant variant = Variant.builder().name("variant-A").enabled(true).build();
        FeatureContextHolder.setCurrentVariant(variant);
        Variant retrieved = FeatureContextHolder.getCurrentVariant();

        assertNotNull(retrieved);
        assertEquals("variant-A", retrieved.getName());
        assertTrue(Boolean.TRUE.equals(retrieved.getEnabled()));
    }

    @Test
    @DisplayName("runWithContext 正确执行并恢复上下文")
    void shouldRunWithContextAndRestore() {
        FeatureContext original = FeatureContext.builder().userId("original").build();
        FeatureContext newCtx = FeatureContext.builder().userId("new").build();
        FeatureContextHolder.set(original);

        FeatureContextHolder.runWithContext(newCtx, () -> {
            assertEquals("new", FeatureContextHolder.get().getUserId());
        });

        assertEquals("original", FeatureContextHolder.get().getUserId());
    }
}
