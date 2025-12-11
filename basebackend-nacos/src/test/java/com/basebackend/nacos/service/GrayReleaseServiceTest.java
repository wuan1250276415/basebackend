package com.basebackend.nacos.service;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.basebackend.nacos.enums.GrayStrategyType;
import com.basebackend.nacos.event.GrayReleaseHistoryEvent;
import com.basebackend.nacos.model.ConfigInfo;
import com.basebackend.nacos.model.GrayReleaseConfig;
import com.basebackend.nacos.model.GrayReleaseHistory;
import com.basebackend.nacos.repository.GrayReleaseHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 灰度发布服务测试
 * 测试灰度策略、实例选择、配置发布等功能
 *
 * @author BaseBackend
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GrayReleaseService 灰度发布服务测试")
class GrayReleaseServiceTest {

    @Mock
    private NacosConfigService nacosConfigService;

    @Mock
    private NamingService namingService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private GrayReleaseHistoryRepository historyRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private GrayReleaseService grayReleaseService;

    @Test
    @DisplayName("IP灰度策略 - 成功启动灰度发布")
    void shouldStartGrayReleaseWithIpStrategy() throws Exception {
        // Given
        ConfigInfo configInfo = createTestConfigInfo();
        GrayReleaseConfig grayConfig = createIpGrayConfig();

        List<Instance> instances = Arrays.asList(
            createTestInstance("192.168.1.1", 8080),
            createTestInstance("192.168.1.2", 8080),
            createTestInstance("192.168.1.3", 8080)
        );

        when(namingService.getAllInstances("test-service", "DEFAULT_GROUP")).thenReturn(instances);
        when(nacosConfigService.publishConfig(any(ConfigInfo.class))).thenReturn(true);

        // When
        GrayReleaseService.GrayReleaseResult result = grayReleaseService.startGrayRelease(configInfo, grayConfig);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("灰度发布启动成功");
        assertThat(result.getTargetInstances()).hasSize(1);
        verify(nacosConfigService, times(1)).publishConfig(any(ConfigInfo.class));
    }

    @Test
    @DisplayName("百分比灰度策略 - 成功启动灰度发布")
    void shouldStartGrayReleaseWithPercentageStrategy() throws Exception {
        // Given
        ConfigInfo configInfo = createTestConfigInfo();
        GrayReleaseConfig grayConfig = createPercentageGrayConfig(50); // 50%

        List<Instance> instances = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            instances.add(createTestInstance("192.168.1." + (i + 1), 8080));
        }

        when(namingService.getAllInstances("test-service", "DEFAULT_GROUP")).thenReturn(instances);
        when(nacosConfigService.publishConfig(any(ConfigInfo.class))).thenReturn(true);

        // When
        GrayReleaseService.GrayReleaseResult result = grayReleaseService.startGrayRelease(configInfo, grayConfig);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTargetInstances()).hasSizeGreaterThanOrEqualTo(4); // At least 4 instances (50% of 10)
    }

    @Test
    @DisplayName("标签灰度策略 - 成功启动灰度发布")
    void shouldStartGrayReleaseWithLabelStrategy() throws Exception {
        // Given
        ConfigInfo configInfo = createTestConfigInfo();
        GrayReleaseConfig grayConfig = createLabelGrayConfig("env=gray");

        List<Instance> instances = Arrays.asList(
            createTestInstanceWithLabel("192.168.1.1", 8080, "env=gray"),
            createTestInstanceWithLabel("192.168.1.2", 8080, "env=prod"),
            createTestInstanceWithLabel("192.168.1.3", 8080, "env=gray")
        );

        when(namingService.getAllInstances("test-service", "DEFAULT_GROUP")).thenReturn(instances);
        when(nacosConfigService.publishConfig(any(ConfigInfo.class))).thenReturn(true);

        // When
        GrayReleaseService.GrayReleaseResult result = grayReleaseService.startGrayRelease(configInfo, grayConfig);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTargetInstances()).hasSize(2); // 2 instances with env=gray
    }

    @Test
    @DisplayName("灰度发布失败 - 无可用实例")
    void shouldFailWhenNoInstancesAvailable() throws Exception {
        // Given
        ConfigInfo configInfo = createTestConfigInfo();
        GrayReleaseConfig grayConfig = createIpGrayConfig();

        when(namingService.getAllInstances("test-service", "DEFAULT_GROUP")).thenReturn(Collections.emptyList());

        // When
        GrayReleaseService.GrayReleaseResult result = grayReleaseService.startGrayRelease(configInfo, grayConfig);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("未找到可用的服务实例");
    }

    @Test
    @DisplayName("灰度发布失败 - 无匹配的目标实例")
    void shouldFailWhenNoMatchingInstances() throws Exception {
        // Given
        ConfigInfo configInfo = createTestConfigInfo();
        GrayReleaseConfig grayConfig = createIpGrayConfig();

        List<Instance> instances = Arrays.asList(
            createTestInstance("192.168.2.1", 8080),
            createTestInstance("192.168.2.2", 8080)
        );

        when(namingService.getAllInstances("test-service", "DEFAULT_GROUP")).thenReturn(instances);

        // When
        GrayReleaseService.GrayReleaseResult result = grayReleaseService.startGrayRelease(configInfo, grayConfig);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("未找到符合灰度策略的实例");
    }

    @Test
    @DisplayName("灰度发布失败 - 配置发布失败")
    void shouldFailWhenConfigPublishFails() throws Exception {
        // Given
        ConfigInfo configInfo = createTestConfigInfo();
        GrayReleaseConfig grayConfig = createIpGrayConfig();

        List<Instance> instances = Arrays.asList(createTestInstance("192.168.1.1", 8080));
        when(namingService.getAllInstances("test-service", "DEFAULT_GROUP")).thenReturn(instances);
        when(nacosConfigService.publishConfig(any(ConfigInfo.class))).thenReturn(false);

        // When
        GrayReleaseService.GrayReleaseResult result = grayReleaseService.startGrayRelease(configInfo, grayConfig);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("灰度配置发布失败");
    }

    @Test
    @DisplayName("灰度全量发布成功")
    void shouldPromoteToFullSuccessfully() throws Exception {
        // Given
        ConfigInfo configInfo = createTestConfigInfo();
        GrayReleaseConfig grayConfig = createIpGrayConfig();
        grayConfig.setEffectiveInstances(Arrays.asList("192.168.1.1:8080"));

        when(nacosConfigService.publishConfig(configInfo)).thenReturn(true);

        // When
        GrayReleaseService.GrayReleaseResult result = grayReleaseService.promoteToFull(configInfo, grayConfig);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("灰度全量发布成功");
        assertThat(grayConfig.getStatus()).isEqualTo("PROMOTED");
        verify(nacosConfigService, times(1)).publishConfig(configInfo);
    }

    @Test
    @DisplayName("灰度全量发布失败")
    void shouldFailPromoteToFull() throws Exception {
        // Given
        ConfigInfo configInfo = createTestConfigInfo();
        GrayReleaseConfig grayConfig = createIpGrayConfig();

        when(nacosConfigService.publishConfig(configInfo)).thenReturn(false);

        // When
        GrayReleaseService.GrayReleaseResult result = grayReleaseService.promoteToFull(configInfo, grayConfig);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("灰度全量发布失败");
        verify(nacosConfigService, times(1)).publishConfig(configInfo);
    }

    @Test
    @DisplayName("灰度回滚成功")
    void shouldRollbackSuccessfully() throws Exception {
        // Given
        ConfigInfo originalConfig = createTestConfigInfo();
        ConfigInfo configInfo = createTestConfigInfo();
        configInfo.setContent("new-content");
        GrayReleaseConfig grayConfig = createIpGrayConfig();
        grayConfig.setEffectiveInstances(Arrays.asList("192.168.1.1:8080"));

        when(nacosConfigService.publishConfig(originalConfig)).thenReturn(true);

        // When
        GrayReleaseService.GrayReleaseResult result = grayReleaseService.rollbackGrayRelease(originalConfig, grayConfig);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("灰度回滚成功");
        assertThat(grayConfig.getStatus()).isEqualTo("ROLLED_BACK");
        verify(nacosConfigService, times(1)).publishConfig(originalConfig);
    }

    @Test
    @DisplayName("灰度回滚失败")
    void shouldFailRollback() throws Exception {
        // Given
        ConfigInfo originalConfig = createTestConfigInfo();
        GrayReleaseConfig grayConfig = createIpGrayConfig();

        when(nacosConfigService.publishConfig(originalConfig)).thenReturn(false);

        // When
        GrayReleaseService.GrayReleaseResult result = grayReleaseService.rollbackGrayRelease(originalConfig, grayConfig);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("灰度回滚失败");
        verify(nacosConfigService, times(1)).publishConfig(originalConfig);
    }

    @Test
    @DisplayName("灰度配置验证 - 成功")
    void shouldValidateGrayConfigSuccessfully() throws Exception {
        // Given
        GrayReleaseConfig grayConfig = createIpGrayConfig();

        // When & Then - should not throw exception
        try {
            // Use reflection to call private method
            var method = GrayReleaseService.class.getDeclaredMethod("validateGrayConfig", GrayReleaseConfig.class);
            method.setAccessible(true);
            method.invoke(grayReleaseService, grayConfig);
        } catch (Exception e) {
            throw new RuntimeException("Should not throw exception", e);
        }
    }

    @Test
    @DisplayName("灰度配置验证 - 空配置")
    void shouldRejectNullGrayConfig() {
        // Given
        GrayReleaseConfig grayConfig = null;

        // When & Then
        assertThatThrownBy(() -> {
            var method = GrayReleaseService.class.getDeclaredMethod("validateGrayConfig", GrayReleaseConfig.class);
            method.setAccessible(true);
            method.invoke(grayReleaseService, grayConfig);
        }).hasCauseInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("灰度配置不能为空");
    }

    @Test
    @DisplayName("灰度配置验证 - 空的DataId")
    void shouldRejectEmptyDataId() {
        // Given
        GrayReleaseConfig grayConfig = createIpGrayConfig();
        grayConfig.setDataId("");

        // When & Then
        assertThatThrownBy(() -> {
            var method = GrayReleaseService.class.getDeclaredMethod("validateGrayConfig", GrayReleaseConfig.class);
            method.setAccessible(true);
            method.invoke(grayReleaseService, grayConfig);
        }).hasCauseInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("配置Data ID不能为空");
    }

    @Test
    @DisplayName("灰度配置验证 - 空的策略类型")
    void shouldRejectEmptyStrategyType() {
        // Given
        GrayReleaseConfig grayConfig = createIpGrayConfig();
        grayConfig.setStrategyType("");

        // When & Then
        assertThatThrownBy(() -> {
            var method = GrayReleaseService.class.getDeclaredMethod("validateGrayConfig", GrayReleaseConfig.class);
            method.setAccessible(true);
            method.invoke(grayReleaseService, grayConfig);
        }).hasCauseInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("灰度策略类型不能为空");
    }

    @Test
    @DisplayName("灰度配置验证 - 不支持的策略类型")
    void shouldRejectUnsupportedStrategyType() {
        // Given
        GrayReleaseConfig grayConfig = createIpGrayConfig();
        grayConfig.setStrategyType("UNSUPPORTED");

        // When & Then
        assertThatThrownBy(() -> {
            var method = GrayReleaseService.class.getDeclaredMethod("validateGrayConfig", GrayReleaseConfig.class);
            method.setAccessible(true);
            method.invoke(grayReleaseService, grayConfig);
        }).hasCauseInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("不支持的灰度策略");
    }

    @Test
    @DisplayName("灰度配置验证 - IP策略需要目标实例")
    void shouldRequireTargetInstancesForIpStrategy() {
        // Given
        GrayReleaseConfig grayConfig = createIpGrayConfig();
        grayConfig.setTargetInstances("");

        // When & Then
        assertThatThrownBy(() -> {
            var method = GrayReleaseService.class.getDeclaredMethod("validateGrayConfig", GrayReleaseConfig.class);
            method.setAccessible(true);
            method.invoke(grayReleaseService, grayConfig);
        }).hasCauseInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("IP灰度策略必须指定目标实例");
    }

    @Test
    @DisplayName("灰度配置验证 - 百分比策略需要有效百分比")
    void shouldRequireValidPercentageForPercentageStrategy() {
        // Given
        GrayReleaseConfig grayConfig = createPercentageGrayConfig(0); // Invalid percentage

        // When & Then
        assertThatThrownBy(() -> {
            var method = GrayReleaseService.class.getDeclaredMethod("validateGrayConfig", GrayReleaseConfig.class);
            method.setAccessible(true);
            method.invoke(grayReleaseService, grayConfig);
        }).hasCauseInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("百分比灰度策略的百分比必须在1-100之间");
    }

    @Test
    @DisplayName("灰度配置验证 - 标签策略需要标签")
    void shouldRequireLabelsForLabelStrategy() {
        // Given
        GrayReleaseConfig grayConfig = createLabelGrayConfig("");
        grayConfig.setStrategyType("LABEL");

        // When & Then
        assertThatThrownBy(() -> {
            var method = GrayReleaseService.class.getDeclaredMethod("validateGrayConfig", GrayReleaseConfig.class);
            method.setAccessible(true);
            method.invoke(grayReleaseService, grayConfig);
        }).hasCauseInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("标签灰度策略必须指定标签");
    }

    @Test
    @DisplayName("灰度发布启动异常处理")
    void shouldHandleExceptionsDuringGrayRelease() throws Exception {
        // Given
        ConfigInfo configInfo = createTestConfigInfo();
        GrayReleaseConfig grayConfig = createIpGrayConfig();

        when(namingService.getAllInstances("test-service", "DEFAULT_GROUP"))
            .thenThrow(new RuntimeException("Service unavailable"));

        // When
        GrayReleaseService.GrayReleaseResult result = grayReleaseService.startGrayRelease(configInfo, grayConfig);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("灰度发布启动失败");
    }

    @Test
    @DisplayName("全量发布异常处理")
    void shouldHandleExceptionsDuringPromote() throws Exception {
        // Given
        ConfigInfo configInfo = createTestConfigInfo();
        GrayReleaseConfig grayConfig = createIpGrayConfig();

        when(nacosConfigService.publishConfig(configInfo))
            .thenThrow(new RuntimeException("Network error"));

        // When
        GrayReleaseService.GrayReleaseResult result = grayReleaseService.promoteToFull(configInfo, grayConfig);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("灰度全量发布失败");
    }

    @Test
    @DisplayName("回滚异常处理")
    void shouldHandleExceptionsDuringRollback() throws Exception {
        // Given
        ConfigInfo originalConfig = createTestConfigInfo();
        GrayReleaseConfig grayConfig = createIpGrayConfig();

        when(nacosConfigService.publishConfig(originalConfig))
            .thenThrow(new RuntimeException("Network error"));

        // When
        GrayReleaseService.GrayReleaseResult result = grayReleaseService.rollbackGrayRelease(originalConfig, grayConfig);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("灰度回滚失败");
    }

    private ConfigInfo createTestConfigInfo() {
        return ConfigInfo.builder()
            .dataId("test-config.yml")
            .content("test-content")
            .group("DEFAULT_GROUP")
            .namespace("public")
            .environment("dev")
            .tenantId("public")
            .appId(123L)
            .build();
    }

    private GrayReleaseConfig createIpGrayConfig() {
        return GrayReleaseConfig.builder()
            .dataId("test-config.yml")
            .strategyType("IP")
            .targetInstances("192.168.1.1:8080")
            .build();
    }

    private GrayReleaseConfig createPercentageGrayConfig(int percentage) {
        return GrayReleaseConfig.builder()
            .dataId("test-config.yml")
            .strategyType("PERCENTAGE")
            .percentage(percentage)
            .build();
    }

    private GrayReleaseConfig createLabelGrayConfig(String labels) {
        return GrayReleaseConfig.builder()
            .dataId("test-config.yml")
            .strategyType("LABEL")
            .labels(labels)
            .build();
    }

    private Instance createTestInstance(String ip, int port) {
        Instance instance = new Instance();
        instance.setIp(ip);
        instance.setPort(port);
        instance.setWeight(1.0);
        instance.setHealthy(true);
        instance.setEnabled(true);
        instance.setEphemeral(true);
        instance.setClusterName("DEFAULT");
        instance.setServiceName("test-service");
        instance.setMetadata(new HashMap<>());
        return instance;
    }

    private Instance createTestInstanceWithLabel(String ip, int port, String label) {
        Instance instance = createTestInstance(ip, port);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("label", label);
        instance.setMetadata(metadata);
        return instance;
    }
}
