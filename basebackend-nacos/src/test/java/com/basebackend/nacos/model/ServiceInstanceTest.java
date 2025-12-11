package com.basebackend.nacos.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 服务实例模型测试
 * 测试实例属性的设置和获取
 *
 * @author BaseBackend
 */
@DisplayName("ServiceInstance 服务实例模型测试")
class ServiceInstanceTest {

    @Test
    @DisplayName("使用Builder创建实例")
    void shouldCreateInstanceWithBuilder() {
        // Given
        Map<String, String> metadata = new HashMap<>();
        metadata.put("version", "1.0.0");
        metadata.put("region", "us-east-1");

        // When
        ServiceInstance instance = ServiceInstance.builder()
            .serviceName("test-service")
            .groupName("DEFAULT_GROUP")
            .clusterName("DEFAULT")
            .ip("192.168.1.1")
            .port(8080)
            .weight(1.5)
            .healthy(true)
            .enabled(true)
            .ephemeral(false)
            .metadata(metadata)
            .instanceId("instance-001")
            .build();

        // Then
        assertThat(instance.getServiceName()).isEqualTo("test-service");
        assertThat(instance.getGroupName()).isEqualTo("DEFAULT_GROUP");
        assertThat(instance.getClusterName()).isEqualTo("DEFAULT");
        assertThat(instance.getIp()).isEqualTo("192.168.1.1");
        assertThat(instance.getPort()).isEqualTo(8080);
        assertThat(instance.getWeight()).isEqualTo(1.5);
        assertThat(instance.getHealthy()).isTrue();
        assertThat(instance.getEnabled()).isTrue();
        assertThat(instance.getEphemeral()).isFalse();
        assertThat(instance.getMetadata().get("version")).isEqualTo("1.0.0");
        assertThat(instance.getMetadata().get("region")).isEqualTo("us-east-1");
        assertThat(instance.getInstanceId()).isEqualTo("instance-001");
    }

    @Test
    @DisplayName("使用无参构造函数创建实例")
    void shouldCreateInstanceWithNoArgsConstructor() {
        // When
        ServiceInstance instance = new ServiceInstance();

        // Then
        assertThat(instance.getServiceName()).isNull();
        assertThat(instance.getGroupName()).isNull();
        assertThat(instance.getClusterName()).isNull();
        assertThat(instance.getIp()).isNull();
        assertThat(instance.getPort()).isNull();
        assertThat(instance.getWeight()).isNull();
        assertThat(instance.getHealthy()).isNull();
        assertThat(instance.getEnabled()).isNull();
        assertThat(instance.getEphemeral()).isNull();
        assertThat(instance.getMetadata()).isNull();
        assertThat(instance.getInstanceId()).isNull();
    }

    @Test
    @DisplayName("使用全参构造函数创建实例")
    void shouldCreateInstanceWithAllArgsConstructor() {
        // Given
        Map<String, String> metadata = new HashMap<>();
        metadata.put("key", "value");

        // When
        ServiceInstance instance = new ServiceInstance(
            "test-service",
            "DEFAULT_GROUP",
            "DEFAULT",
            "192.168.1.1",
            8080,
            2.0,
            true,
            true,
            false,
            metadata,
            "instance-001"
        );

        // Then
        assertThat(instance.getServiceName()).isEqualTo("test-service");
        assertThat(instance.getGroupName()).isEqualTo("DEFAULT_GROUP");
        assertThat(instance.getClusterName()).isEqualTo("DEFAULT");
        assertThat(instance.getIp()).isEqualTo("192.168.1.1");
        assertThat(instance.getPort()).isEqualTo(8080);
        assertThat(instance.getWeight()).isEqualTo(2.0);
        assertThat(instance.getHealthy()).isTrue();
        assertThat(instance.getEnabled()).isTrue();
        assertThat(instance.getEphemeral()).isFalse();
        assertThat(instance.getMetadata().get("key")).isEqualTo("value");
        assertThat(instance.getInstanceId()).isEqualTo("instance-001");
    }

    @Test
    @DisplayName("设置和获取服务名")
    void shouldSetAndGetServiceName() {
        // Given
        ServiceInstance instance = new ServiceInstance();

        // When
        instance.setServiceName("my-service");

        // Then
        assertThat(instance.getServiceName()).isEqualTo("my-service");
    }

    @Test
    @DisplayName("设置和获取分组名")
    void shouldSetAndGetGroupName() {
        // Given
        ServiceInstance instance = new ServiceInstance();

        // When
        instance.setGroupName("my-group");

        // Then
        assertThat(instance.getGroupName()).isEqualTo("my-group");
    }

    @Test
    @DisplayName("设置和获取集群名")
    void shouldSetAndGetClusterName() {
        // Given
        ServiceInstance instance = new ServiceInstance();

        // When
        instance.setClusterName("my-cluster");

        // Then
        assertThat(instance.getClusterName()).isEqualTo("my-cluster");
    }

    @Test
    @DisplayName("设置和获取IP地址")
    void shouldSetAndGetIp() {
        // Given
        ServiceInstance instance = new ServiceInstance();

        // When
        instance.setIp("10.0.0.1");

        // Then
        assertThat(instance.getIp()).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("设置和获取端口")
    void shouldSetAndGetPort() {
        // Given
        ServiceInstance instance = new ServiceInstance();

        // When
        instance.setPort(9090);

        // Then
        assertThat(instance.getPort()).isEqualTo(9090);
    }

    @Test
    @DisplayName("设置和获取权重")
    void shouldSetAndGetWeight() {
        // Given
        ServiceInstance instance = new ServiceInstance();

        // When
        instance.setWeight(3.5);

        // Then
        assertThat(instance.getWeight()).isEqualTo(3.5);
    }

    @Test
    @DisplayName("设置和获取健康状态")
    void shouldSetAndGetHealthy() {
        // Given
        ServiceInstance instance = new ServiceInstance();

        // When
        instance.setHealthy(false);

        // Then
        assertThat(instance.getHealthy()).isFalse();
    }

    @Test
    @DisplayName("设置和获取启用状态")
    void shouldSetAndGetEnabled() {
        // Given
        ServiceInstance instance = new ServiceInstance();

        // When
        instance.setEnabled(false);

        // Then
        assertThat(instance.getEnabled()).isFalse();
    }

    @Test
    @DisplayName("设置和获取临时实例标志")
    void shouldSetAndGetEphemeral() {
        // Given
        ServiceInstance instance = new ServiceInstance();

        // When
        instance.setEphemeral(true);

        // Then
        assertThat(instance.getEphemeral()).isTrue();
    }

    @Test
    @DisplayName("设置和获取元数据")
    void shouldSetAndGetMetadata() {
        // Given
        ServiceInstance instance = new ServiceInstance();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("key1", "value1");
        metadata.put("key2", "value2");

        // When
        instance.setMetadata(metadata);

        // Then
        assertThat(instance.getMetadata()).hasSize(2);
        assertThat(instance.getMetadata().get("key1")).isEqualTo("value1");
        assertThat(instance.getMetadata().get("key2")).isEqualTo("value2");
    }

    @Test
    @DisplayName("设置和获取实例ID")
    void shouldSetAndGetInstanceId() {
        // Given
        ServiceInstance instance = new ServiceInstance();

        // When
        instance.setInstanceId("my-instance-id");

        // Then
        assertThat(instance.getInstanceId()).isEqualTo("my-instance-id");
    }

    @Test
    @DisplayName("修改元数据应该影响原始映射")
    void shouldReflectMetadataChanges() {
        // Given
        Map<String, String> metadata = new HashMap<>();
        ServiceInstance instance = ServiceInstance.builder()
            .metadata(metadata)
            .build();

        // When
        metadata.put("new-key", "new-value");

        // Then
        assertThat(instance.getMetadata().get("new-key")).isEqualTo("new-value");
    }

    @Test
    @DisplayName("相等实例应该相等")
    void shouldBeEqualForSameValues() {
        // Given
        ServiceInstance instance1 = ServiceInstance.builder()
            .serviceName("test")
            .ip("192.168.1.1")
            .port(8080)
            .build();

        ServiceInstance instance2 = ServiceInstance.builder()
            .serviceName("test")
            .ip("192.168.1.1")
            .port(8080)
            .build();

        // Then
        assertThat(instance1).isEqualTo(instance2);
    }

    @Test
    @DisplayName("不同实例应该不相等")
    void shouldNotBeEqualForDifferentValues() {
        // Given
        ServiceInstance instance1 = ServiceInstance.builder()
            .serviceName("test")
            .ip("192.168.1.1")
            .port(8080)
            .build();

        ServiceInstance instance2 = ServiceInstance.builder()
            .serviceName("test")
            .ip("192.168.1.2")
            .port(8080)
            .build();

        // Then
        assertThat(instance1).isNotEqualTo(instance2);
    }

    @Test
    @DisplayName("相同实例的hashCode应该相等")
    void shouldHaveSameHashCodeForEqualInstances() {
        // Given
        ServiceInstance instance1 = ServiceInstance.builder()
            .serviceName("test")
            .ip("192.168.1.1")
            .port(8080)
            .build();

        ServiceInstance instance2 = ServiceInstance.builder()
            .serviceName("test")
            .ip("192.168.1.1")
            .port(8080)
            .build();

        // Then
        assertThat(instance1.hashCode()).isEqualTo(instance2.hashCode());
    }

    @Test
    @DisplayName("空字符串和null应该被正确处理")
    void shouldHandleEmptyAndNullValues() {
        // When
        ServiceInstance instance = ServiceInstance.builder()
            .serviceName("")
            .ip(null)
            .build();

        // Then
        assertThat(instance.getServiceName()).isEmpty();
        assertThat(instance.getIp()).isNull();
    }

    @Test
    @DisplayName("默认实例应该有默认值")
    void shouldHaveDefaultValues() {
        // When
        ServiceInstance instance = ServiceInstance.builder().build();

        // Then
        assertThat(instance.getWeight()).isNull();
        assertThat(instance.getHealthy()).isNull();
        assertThat(instance.getEnabled()).isNull();
        assertThat(instance.getEphemeral()).isNull();
    }

    @Test
    @DisplayName("支持中文元数据")
    void shouldSupportChineseMetadata() {
        // Given
        Map<String, String> metadata = new HashMap<>();
        metadata.put("名称", "测试服务");
        metadata.put("描述", "这是一个测试服务");

        // When
        ServiceInstance instance = ServiceInstance.builder()
            .metadata(metadata)
            .build();

        // Then
        assertThat(instance.getMetadata().get("名称")).isEqualTo("测试服务");
        assertThat(instance.getMetadata().get("描述")).isEqualTo("这是一个测试服务");
    }

    @Test
    @DisplayName("元数据应该支持特殊字符")
    void shouldSupportSpecialCharactersInMetadata() {
        // Given
        Map<String, String> metadata = new HashMap<>();
        metadata.put("url", "https://example.com/path?param=value&other=test");
        metadata.put("path", "/usr/local/bin");

        // When
        ServiceInstance instance = ServiceInstance.builder()
            .metadata(metadata)
            .build();

        // Then
        assertThat(instance.getMetadata().get("url")).isEqualTo("https://example.com/path?param=value&other=test");
        assertThat(instance.getMetadata().get("path")).isEqualTo("/usr/local/bin");
    }
}
