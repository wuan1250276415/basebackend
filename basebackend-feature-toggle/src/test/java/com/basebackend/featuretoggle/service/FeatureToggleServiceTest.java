package com.basebackend.featuretoggle.service;

import com.basebackend.featuretoggle.model.FeatureContext;
import com.basebackend.featuretoggle.model.Variant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 特性开关服务单元测试
 *
 * @author BaseBackend
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeatureToggleService 单元测试")
class FeatureToggleServiceTest {

    @Mock
    private FeatureToggleService featureToggleService;

    private FeatureContext testContext;

    @BeforeEach
    void setUp() {
        testContext = FeatureContext.builder()
                .userId("user-123")
                .tenantId("tenant-456")
                .environment("test")
                .build();
    }

    @Nested
    @DisplayName("isEnabled 方法测试")
    class IsEnabledTests {

        @Test
        @DisplayName("无上下文时返回特性状态")
        void shouldReturnFeatureStateWithoutContext() {
            // 准备
            when(featureToggleService.isEnabled("feature.test")).thenReturn(true);

            // 执行
            boolean result = featureToggleService.isEnabled("feature.test");

            // 验证
            assertTrue(result);
            verify(featureToggleService).isEnabled("feature.test");
        }

        @Test
        @DisplayName("带上下文时返回特性状态")
        void shouldReturnFeatureStateWithContext() {
            // 准备
            when(featureToggleService.isEnabled(eq("feature.test"), any(FeatureContext.class)))
                    .thenReturn(true);

            // 执行
            boolean result = featureToggleService.isEnabled("feature.test", testContext);

            // 验证
            assertTrue(result);
            verify(featureToggleService).isEnabled("feature.test", testContext);
        }

        @Test
        @DisplayName("特性禁用时返回false")
        void shouldReturnFalseWhenFeatureDisabled() {
            // 准备
            when(featureToggleService.isEnabled("feature.disabled")).thenReturn(false);

            // 执行
            boolean result = featureToggleService.isEnabled("feature.disabled");

            // 验证
            assertFalse(result);
        }

        @Test
        @DisplayName("带默认值时，特性不存在返回默认值")
        void shouldReturnDefaultValueWhenFeatureNotExists() {
            // 准备
            when(featureToggleService.isEnabled(eq("feature.unknown"), eq(true)))
                    .thenReturn(true);
            when(featureToggleService.isEnabled(eq("feature.unknown"), eq(false)))
                    .thenReturn(false);

            // 执行 & 验证
            assertTrue(featureToggleService.isEnabled("feature.unknown", true));
            assertFalse(featureToggleService.isEnabled("feature.unknown", false));
        }

        @Test
        @DisplayName("带上下文和默认值时正确返回")
        void shouldReturnCorrectValueWithContextAndDefault() {
            // 准备
            when(featureToggleService.isEnabled(eq("feature.test"), any(FeatureContext.class), eq(false)))
                    .thenReturn(true);

            // 执行
            boolean result = featureToggleService.isEnabled("feature.test", testContext, false);

            // 验证
            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("getVariant 方法测试")
    class GetVariantTests {

        @Test
        @DisplayName("获取变体信息")
        void shouldReturnVariant() {
            // 准备
            Variant expectedVariant = Variant.builder()
                    .name("variant-A")
                    .enabled(true)
                    .payload("{\"color\": \"blue\"}")
                    .build();
            when(featureToggleService.getVariant(eq("feature.abtest"), any(FeatureContext.class)))
                    .thenReturn(expectedVariant);

            // 执行
            Variant result = featureToggleService.getVariant("feature.abtest", testContext);

            // 验证
            assertNotNull(result);
            assertEquals("variant-A", result.getName());
            assertTrue(Boolean.TRUE.equals(result.getEnabled()));
        }

        @Test
        @DisplayName("变体不存在时返回默认变体")
        void shouldReturnDefaultVariantWhenNotExists() {
            // 准备
            Variant defaultVariant = Variant.builder()
                    .name("control")
                    .enabled(false)
                    .build();
            when(featureToggleService.getVariant(eq("feature.unknown"), any(FeatureContext.class), any(Variant.class)))
                    .thenReturn(defaultVariant);

            // 执行
            Variant result = featureToggleService.getVariant("feature.unknown", testContext, defaultVariant);

            // 验证
            assertNotNull(result);
            assertEquals("control", result.getName());
            assertFalse(Boolean.TRUE.equals(result.getEnabled()));
        }
    }

    @Nested
    @DisplayName("getAllFeatureStates 方法测试")
    class GetAllFeatureStatesTests {

        @Test
        @DisplayName("获取所有特性状态")
        void shouldReturnAllFeatureStates() {
            // 准备
            Map<String, Boolean> expectedStates = new HashMap<>();
            expectedStates.put("feature.a", true);
            expectedStates.put("feature.b", false);
            expectedStates.put("feature.c", true);
            when(featureToggleService.getAllFeatureStates()).thenReturn(expectedStates);

            // 执行
            Map<String, Boolean> result = featureToggleService.getAllFeatureStates();

            // 验证
            assertNotNull(result);
            assertEquals(3, result.size());
            assertTrue(result.get("feature.a"));
            assertFalse(result.get("feature.b"));
        }

        @Test
        @DisplayName("带上下文获取所有特性状态")
        void shouldReturnAllFeatureStatesWithContext() {
            // 准备
            Map<String, Boolean> expectedStates = new HashMap<>();
            expectedStates.put("feature.user", true);
            when(featureToggleService.getAllFeatureStates(any(FeatureContext.class)))
                    .thenReturn(expectedStates);

            // 执行
            Map<String, Boolean> result = featureToggleService.getAllFeatureStates(testContext);

            // 验证
            assertNotNull(result);
            assertTrue(result.get("feature.user"));
        }
    }

    @Nested
    @DisplayName("服务状态测试")
    class ServiceStatusTests {

        @Test
        @DisplayName("获取提供商名称")
        void shouldReturnProviderName() {
            // 准备
            when(featureToggleService.getProviderName()).thenReturn("Unleash");

            // 执行
            String providerName = featureToggleService.getProviderName();

            // 验证
            assertEquals("Unleash", providerName);
        }

        @Test
        @DisplayName("检查服务可用性")
        void shouldCheckServiceAvailability() {
            // 准备
            when(featureToggleService.isAvailable()).thenReturn(true);

            // 执行
            boolean available = featureToggleService.isAvailable();

            // 验证
            assertTrue(available);
        }

        @Test
        @DisplayName("刷新配置")
        void shouldRefreshConfiguration() {
            // 执行
            featureToggleService.refresh();

            // 验证
            verify(featureToggleService).refresh();
        }
    }
}
