package com.basebackend.nacos.service;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.basebackend.nacos.isolation.ConfigIsolationContext;
import com.basebackend.nacos.isolation.ConfigIsolationManager;
import com.basebackend.nacos.model.ConfigInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Nacos配置服务测试
 * 测试配置的增删改查操作
 *
 * @author BaseBackend
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("NacosConfigService Nacos配置服务测试")
class NacosConfigServiceTest {

    @Mock
    private ConfigService nacosConfigService;

    @Mock
    private ConfigIsolationManager isolationManager;

    @InjectMocks
    private NacosConfigService configService;

    @Test
    @DisplayName("获取配置成功")
    void shouldGetConfigSuccessfully() throws NacosException {
        // Given
        ConfigInfo configInfo = createTestConfigInfo();
        ConfigIsolationContext mockContext = mock(ConfigIsolationContext.class);
        when(isolationManager.createContext("dev", "public", 123L)).thenReturn(mockContext);
        when(mockContext.buildDataId("test-config.yml")).thenReturn("test-config.yml");
        when(mockContext.buildGroup()).thenReturn("DEFAULT_GROUP");
        when(mockContext.buildNamespace()).thenReturn("dev-public-123");
        when(nacosConfigService.getConfig(anyString(), anyString(), eq(5000))).thenReturn("test-content");

        // When
        String result = configService.getConfig(configInfo);

        // Then
        assertThat(result).isEqualTo("test-content");
        verify(nacosConfigService, times(1)).getConfig("test-config.yml", "DEFAULT_GROUP", 5000);
    }

    @Test
    @DisplayName("发布配置成功")
    void shouldPublishConfigSuccessfully() throws NacosException {
        // Given
        ConfigInfo configInfo = createTestConfigInfo();
        ConfigIsolationContext mockContext = mock(ConfigIsolationContext.class);
        when(isolationManager.createContext("dev", "public", 123L)).thenReturn(mockContext);
        when(mockContext.buildDataId("test-config.yml")).thenReturn("test-config.yml");
        when(mockContext.buildGroup()).thenReturn("DEFAULT_GROUP");
        when(nacosConfigService.publishConfig(anyString(), anyString(), anyString(), anyString())).thenReturn(true);

        // When
        boolean result = configService.publishConfig(configInfo);

        // Then
        assertThat(result).isTrue();
        verify(nacosConfigService, times(1)).publishConfig("test-config.yml", "DEFAULT_GROUP", "test-content", "yaml");
    }

    @Test
    @DisplayName("发布配置使用默认类型")
    void shouldPublishConfigWithDefaultType() throws NacosException {
        // Given
        ConfigInfo configInfo = createTestConfigInfo();
        configInfo.setType(null); // 设置为null

        ConfigIsolationContext mockContext = mock(ConfigIsolationContext.class);
        when(isolationManager.createContext("dev", "public", 123L)).thenReturn(mockContext);
        when(mockContext.buildDataId("test-config.yml")).thenReturn("test-config.yml");
        when(mockContext.buildGroup()).thenReturn("DEFAULT_GROUP");
        when(nacosConfigService.publishConfig(anyString(), anyString(), anyString(), anyString())).thenReturn(true);

        // When
        boolean result = configService.publishConfig(configInfo);

        // Then
        assertThat(result).isTrue();
        verify(nacosConfigService, times(1)).publishConfig("test-config.yml", "DEFAULT_GROUP", "test-content", "yaml");
    }

    @Test
    @DisplayName("删除配置成功")
    void shouldRemoveConfigSuccessfully() throws NacosException {
        // Given
        ConfigInfo configInfo = createTestConfigInfo();
        ConfigIsolationContext mockContext = mock(ConfigIsolationContext.class);
        when(isolationManager.createContext("dev", "public", 123L)).thenReturn(mockContext);
        when(mockContext.buildDataId("test-config.yml")).thenReturn("test-config.yml");
        when(mockContext.buildGroup()).thenReturn("DEFAULT_GROUP");
        when(nacosConfigService.removeConfig(anyString(), anyString())).thenReturn(true);

        // When
        boolean result = configService.removeConfig(configInfo);

        // Then
        assertThat(result).isTrue();
        verify(nacosConfigService, times(1)).removeConfig("test-config.yml", "DEFAULT_GROUP");
    }

    @Test
    @DisplayName("获取配置抛出异常")
    void shouldThrowExceptionWhenGetConfigFails() throws NacosException {
        // Given
        ConfigInfo configInfo = createTestConfigInfo();
        ConfigIsolationContext mockContext = mock(ConfigIsolationContext.class);
        when(isolationManager.createContext("dev", "public", 123L)).thenReturn(mockContext);
        when(mockContext.buildDataId("test-config.yml")).thenReturn("test-config.yml");
        when(mockContext.buildGroup()).thenReturn("DEFAULT_GROUP");
        when(nacosConfigService.getConfig(anyString(), anyString(), eq(5000))).thenThrow(new NacosException(500, "Network error"));

        // When & Then
        assertThatThrownBy(() -> configService.getConfig(configInfo))
            .isInstanceOf(NacosException.class)
            .hasMessage("Network error");
    }

    @Test
    @DisplayName("发布配置抛出异常")
    void shouldThrowExceptionWhenPublishConfigFails() throws NacosException {
        // Given
        ConfigInfo configInfo = createTestConfigInfo();
        ConfigIsolationContext mockContext = mock(ConfigIsolationContext.class);
        when(isolationManager.createContext("dev", "public", 123L)).thenReturn(mockContext);
        when(mockContext.buildDataId("test-config.yml")).thenReturn("test-config.yml");
        when(mockContext.buildGroup()).thenReturn("DEFAULT_GROUP");
        when(nacosConfigService.publishConfig(anyString(), anyString(), anyString(), anyString()))
            .thenThrow(new NacosException(500, "Publish failed"));

        // When & Then
        assertThatThrownBy(() -> configService.publishConfig(configInfo))
            .isInstanceOf(NacosException.class)
            .hasMessage("Publish failed");
    }

    @Test
    @DisplayName("删除配置抛出异常")
    void shouldThrowExceptionWhenRemoveConfigFails() throws NacosException {
        // Given
        ConfigInfo configInfo = createTestConfigInfo();
        ConfigIsolationContext mockContext = mock(ConfigIsolationContext.class);
        when(isolationManager.createContext("dev", "public", 123L)).thenReturn(mockContext);
        when(mockContext.buildDataId("test-config.yml")).thenReturn("test-config.yml");
        when(mockContext.buildGroup()).thenReturn("DEFAULT_GROUP");
        when(nacosConfigService.removeConfig(anyString(), anyString())).thenThrow(new NacosException(500, "Remove failed"));

        // When & Then
        assertThatThrownBy(() -> configService.removeConfig(configInfo))
            .isInstanceOf(NacosException.class)
            .hasMessage("Remove failed");
    }

    @Test
    @DisplayName("添加配置监听器")
    void shouldAddConfigListener() throws NacosException {
        // Given
        ConfigInfo configInfo = createTestConfigInfo();
        ConfigIsolationContext mockContext = mock(ConfigIsolationContext.class);
        com.alibaba.nacos.api.config.listener.Listener listener = mock(com.alibaba.nacos.api.config.listener.Listener.class);

        when(isolationManager.createContext("dev", "public", 123L)).thenReturn(mockContext);
        when(mockContext.buildDataId("test-config.yml")).thenReturn("test-config.yml");
        when(mockContext.buildGroup()).thenReturn("DEFAULT_GROUP");

        // When
        configService.addListener(configInfo, listener);

        // Then
        verify(nacosConfigService, times(1)).addListener("test-config.yml", "DEFAULT_GROUP", listener);
    }

    @Test
    @DisplayName("移除配置监听器")
    void shouldRemoveConfigListener() {
        // Given
        ConfigInfo configInfo = createTestConfigInfo();
        ConfigIsolationContext mockContext = mock(ConfigIsolationContext.class);
        com.alibaba.nacos.api.config.listener.Listener listener = mock(com.alibaba.nacos.api.config.listener.Listener.class);

        when(isolationManager.createContext("dev", "public", 123L)).thenReturn(mockContext);
        when(mockContext.buildDataId("test-config.yml")).thenReturn("test-config.yml");
        when(mockContext.buildGroup()).thenReturn("DEFAULT_GROUP");

        // When
        configService.removeListener(configInfo, listener);

        // Then
        verify(nacosConfigService, times(1)).removeListener("test-config.yml", "DEFAULT_GROUP", listener);
    }

    @Test
    @DisplayName("计算MD5成功")
    void shouldCalculateMd5Successfully() {
        // Given
        String content = "test-content";

        // When
        String md5 = configService.calculateMd5(content);

        // Then
        assertThat(md5).isNotNull();
        assertThat(md5).isNotEmpty();
        assertThat(md5).hasSize(32); // MD5 hash length
    }

    @Test
    @DisplayName("空内容计算MD5返回null")
    void shouldReturnNullForNullContent() {
        // When
        String md5 = configService.calculateMd5(null);

        // Then
        assertThat(md5).isNull();
    }

    @Test
    @DisplayName("空字符串计算MD5成功")
    void shouldCalculateMd5ForEmptyString() {
        // When
        String md5 = configService.calculateMd5("");

        // Then
        assertThat(md5).isNotNull();
        assertThat(md5).isNotEmpty();
    }

    @Test
    @DisplayName("相同内容计算MD5应该一致")
    void shouldCalculateConsistentMd5ForSameContent() {
        // Given
        String content = "test-content";

        // When
        String md51 = configService.calculateMd5(content);
        String md52 = configService.calculateMd5(content);

        // Then
        assertThat(md51).isEqualTo(md52);
    }

    @Test
    @DisplayName("不同内容计算MD5应该不同")
    void shouldCalculateDifferentMd5ForDifferentContent() {
        // When
        String md51 = configService.calculateMd5("content-1");
        String md52 = configService.calculateMd5("content-2");

        // Then
        assertThat(md51).isNotEqualTo(md52);
    }

    @Test
    @DisplayName("使用自定义类型发布配置")
    void shouldPublishConfigWithCustomType() throws NacosException {
        // Given
        ConfigInfo configInfo = createTestConfigInfo();
        configInfo.setType("json");

        ConfigIsolationContext mockContext = mock(ConfigIsolationContext.class);
        when(isolationManager.createContext("dev", "public", 123L)).thenReturn(mockContext);
        when(mockContext.buildDataId("test-config.yml")).thenReturn("test-config.yml");
        when(mockContext.buildGroup()).thenReturn("DEFAULT_GROUP");
        when(nacosConfigService.publishConfig(anyString(), anyString(), anyString(), anyString())).thenReturn(true);

        // When
        boolean result = configService.publishConfig(configInfo);

        // Then
        assertThat(result).isTrue();
        verify(nacosConfigService, times(1)).publishConfig("test-config.yml", "DEFAULT_GROUP", "test-content", "json");
    }

    private ConfigInfo createTestConfigInfo() {
        ConfigInfo configInfo = new ConfigInfo();
        configInfo.setDataId("test-config.yml");
        configInfo.setContent("test-content");
        configInfo.setEnvironment("dev");
        configInfo.setTenantId("public");
        configInfo.setAppId(123L);
        configInfo.setType("yaml");
        return configInfo;
    }
}
