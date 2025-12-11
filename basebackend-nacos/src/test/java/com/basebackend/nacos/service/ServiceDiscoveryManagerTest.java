package com.basebackend.nacos.service;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.basebackend.nacos.model.ServiceInstance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 服务发现管理器测试
 * 测试服务注册、心跳、实例管理等功能
 *
 * @author BaseBackend
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceDiscoveryManager 服务发现管理器测试")
class ServiceDiscoveryManagerTest {

    @Mock
    private NamingService namingService;

    @InjectMocks
    private ServiceDiscoveryManager serviceDiscoveryManager;

    @Test
    @DisplayName("获取所有服务列表成功")
    void shouldGetAllServicesSuccessfully() throws Exception {
        // Given
        List<String> serviceList = Arrays.asList("service1", "service2", "service3");
        when(namingService.getServicesOfServer(1, 10)).thenReturn((ListView<String>) serviceList);

        // When
        List<String> services = serviceDiscoveryManager.getAllServices(1, 10);

        // Then
        assertThat(services).hasSize(3);
        assertThat(services).containsExactly("service1", "service2", "service3");
        verify(namingService, times(1)).getServicesOfServer(1, 10);
    }

    @Test
    @DisplayName("获取服务列表异常时返回空列表")
    void shouldReturnEmptyListWhenGetAllServicesFails() throws Exception {
        // Given
        when(namingService.getServicesOfServer(anyInt(), anyInt())).thenThrow(new RuntimeException("Network error"));

        // When
        List<String> services = serviceDiscoveryManager.getAllServices(1, 10);

        // Then
        assertThat(services).isEmpty();
        verify(namingService, times(1)).getServicesOfServer(1, 10);
    }

    @Test
    @DisplayName("获取服务实例成功")
    void shouldGetServiceInstancesSuccessfully() throws Exception {
        // Given
        Instance instance1 = createTestInstance("192.168.1.1", 8080, true, true);
        Instance instance2 = createTestInstance("192.168.1.2", 8080, false, true);
        when(namingService.getAllInstances("test-service", "DEFAULT_GROUP"))
            .thenReturn(Arrays.asList(instance1, instance2));

        // When
        List<ServiceInstance> instances = serviceDiscoveryManager.getServiceInstances("test-service", "DEFAULT_GROUP");

        // Then
        assertThat(instances).hasSize(2);
        assertThat(instances.get(0).getIp()).isEqualTo("192.168.1.1");
        assertThat(instances.get(0).getPort()).isEqualTo(8080);
        assertThat(instances.get(0).getHealthy()).isTrue();
        assertThat(instances.get(0).getEnabled()).isTrue();
        assertThat(instances.get(1).getIp()).isEqualTo("192.168.1.2");
        assertThat(instances.get(1).getHealthy()).isFalse();
        assertThat(instances.get(1).getEnabled()).isTrue();
    }

    @Test
    @DisplayName("获取服务实例异常时返回空列表")
    void shouldReturnEmptyListWhenGetServiceInstancesFails() throws Exception {
        // Given
        when(namingService.getAllInstances(anyString(), anyString())).thenThrow(new RuntimeException("Network error"));

        // When
        List<ServiceInstance> instances = serviceDiscoveryManager.getServiceInstances("test-service", "DEFAULT_GROUP");

        // Then
        assertThat(instances).isEmpty();
        verify(namingService, times(1)).getAllInstances("test-service", "DEFAULT_GROUP");
    }

    @Test
    @DisplayName("获取健康实例成功")
    void shouldGetHealthyInstancesSuccessfully() throws Exception {
        // Given
        Instance healthyInstance = createTestInstance("192.168.1.1", 8080, true, true);
        Instance unhealthyInstance = createTestInstance("192.168.1.2", 8080, false, true);
        Instance disabledInstance = createTestInstance("192.168.1.3", 8080, true, false);

        when(namingService.selectInstances("test-service", "DEFAULT_GROUP", true))
            .thenReturn(Arrays.asList(healthyInstance));

        // When
        List<ServiceInstance> instances = serviceDiscoveryManager.getHealthyInstances("test-service", "DEFAULT_GROUP");

        // Then
        assertThat(instances).hasSize(1);
        assertThat(instances.get(0).getHealthy()).isTrue();
        assertThat(instances.get(0).getEnabled()).isTrue();
    }

    @Test
    @DisplayName("注册服务实例成功")
    void shouldRegisterInstanceSuccessfully() throws Exception {
        // Given
        ServiceInstance serviceInstance = createTestServiceInstance("test-service", "DEFAULT_GROUP");

        // When
        boolean result = serviceDiscoveryManager.registerInstance(serviceInstance);

        // Then
        assertThat(result).isTrue();
        verify(namingService, times(1))
            .registerInstance(eq("test-service"), eq("DEFAULT_GROUP"), any(Instance.class));
    }

    @Test
    @DisplayName("注册服务实例失败")
    void shouldFailToRegisterInstance() throws Exception {
        // Given
        ServiceInstance serviceInstance = createTestServiceInstance("test-service", "DEFAULT_GROUP");
        doThrow(new RuntimeException("Registration failed")).when(namingService)
            .registerInstance(anyString(), anyString(), any(Instance.class));

        // When
        boolean result = serviceDiscoveryManager.registerInstance(serviceInstance);

        // Then
        assertThat(result).isFalse();
        verify(namingService, times(1))
            .registerInstance(eq("test-service"), eq("DEFAULT_GROUP"), any(Instance.class));
    }

    @Test
    @DisplayName("注销服务实例成功")
    void shouldDeregisterInstanceSuccessfully() throws Exception {
        // Given
        ServiceInstance serviceInstance = createTestServiceInstance("test-service", "DEFAULT_GROUP");

        // When
        boolean result = serviceDiscoveryManager.deregisterInstance(serviceInstance);

        // Then
        assertThat(result).isTrue();
        verify(namingService, times(1))
            .deregisterInstance("test-service", "DEFAULT_GROUP", "192.168.1.1", 8080);
    }

    @Test
    @DisplayName("注销服务实例失败")
    void shouldFailToDeregisterInstance() throws Exception {
        // Given
        ServiceInstance serviceInstance = createTestServiceInstance("test-service", "DEFAULT_GROUP");
        doThrow(new RuntimeException("Deregistration failed")).when(namingService)
            .deregisterInstance(anyString(), anyString(), anyString(), anyInt());

        // When
        boolean result = serviceDiscoveryManager.deregisterInstance(serviceInstance);

        // Then
        assertThat(result).isFalse();
        verify(namingService, times(1))
            .deregisterInstance("test-service", "DEFAULT_GROUP", "192.168.1.1", 8080);
    }

    @Test
    @DisplayName("实例上线成功")
    void shouldEnableInstanceSuccessfully() throws Exception {
        // Given
        Instance targetInstance = createTestInstance("192.168.1.1", 8080, true, false); // initially disabled
        when(namingService.getAllInstances("test-service", "DEFAULT_GROUP"))
            .thenReturn(Arrays.asList(targetInstance));

        // When
        boolean result = serviceDiscoveryManager.enableInstance("test-service", "DEFAULT_GROUP", "192.168.1.1", 8080);

        // Then
        assertThat(result).isTrue();
        assertThat(targetInstance.isEnabled()).isTrue();
        verify(namingService, times(1)).getAllInstances("test-service", "DEFAULT_GROUP");
        verify(namingService, times(1)).registerInstance("test-service", "DEFAULT_GROUP", targetInstance);
    }

    @Test
    @DisplayName("实例上线失败 - 实例不存在")
    void shouldFailToEnableNonExistentInstance() throws Exception {
        // Given
        when(namingService.getAllInstances("test-service", "DEFAULT_GROUP"))
            .thenReturn(Arrays.asList());

        // When
        boolean result = serviceDiscoveryManager.enableInstance("test-service", "DEFAULT_GROUP", "192.168.1.1", 8080);

        // Then
        assertThat(result).isFalse();
        verify(namingService, times(1)).getAllInstances("test-service", "DEFAULT_GROUP");
        verify(namingService, never()).registerInstance(anyString(), anyString(), any(Instance.class));
    }

    @Test
    @DisplayName("实例下线成功")
    void shouldDisableInstanceSuccessfully() throws Exception {
        // Given
        Instance targetInstance = createTestInstance("192.168.1.1", 8080, true, true); // initially enabled
        when(namingService.getAllInstances("test-service", "DEFAULT_GROUP"))
            .thenReturn(Arrays.asList(targetInstance));

        // When
        boolean result = serviceDiscoveryManager.disableInstance("test-service", "DEFAULT_GROUP", "192.168.1.1", 8080);

        // Then
        assertThat(result).isTrue();
        assertThat(targetInstance.isEnabled()).isFalse();
        verify(namingService, times(1)).getAllInstances("test-service", "DEFAULT_GROUP");
        verify(namingService, times(1)).registerInstance("test-service", "DEFAULT_GROUP", targetInstance);
    }

    @Test
    @DisplayName("实例下线失败 - 实例不存在")
    void shouldFailToDisableNonExistentInstance() throws Exception {
        // Given
        when(namingService.getAllInstances("test-service", "DEFAULT_GROUP"))
            .thenReturn(Arrays.asList());

        // When
        boolean result = serviceDiscoveryManager.disableInstance("test-service", "DEFAULT_GROUP", "192.168.1.1", 8080);

        // Then
        assertThat(result).isFalse();
        verify(namingService, times(1)).getAllInstances("test-service", "DEFAULT_GROUP");
        verify(namingService, never()).registerInstance(anyString(), anyString(), any(Instance.class));
    }

    @Test
    @DisplayName("更新实例权重成功")
    void shouldUpdateInstanceWeightSuccessfully() throws Exception {
        // Given
        Instance targetInstance = createTestInstance("192.168.1.1", 8080, true, true);
        targetInstance.setWeight(1.0);
        when(namingService.getAllInstances("test-service", "DEFAULT_GROUP"))
            .thenReturn(Arrays.asList(targetInstance));

        // When
        boolean result = serviceDiscoveryManager.updateInstanceWeight(
            "test-service", "DEFAULT_GROUP", "192.168.1.1", 8080, 2.5);

        // Then
        assertThat(result).isTrue();
        assertThat(targetInstance.getWeight()).isEqualTo(2.5);
        verify(namingService, times(1)).getAllInstances("test-service", "DEFAULT_GROUP");
        verify(namingService, times(1)).registerInstance("test-service", "DEFAULT_GROUP", targetInstance);
    }

    @Test
    @DisplayName("更新实例权重失败 - 实例不存在")
    void shouldFailToUpdateWeightOfNonExistentInstance() throws Exception {
        // Given
        when(namingService.getAllInstances("test-service", "DEFAULT_GROUP"))
            .thenReturn(Arrays.asList());

        // When
        boolean result = serviceDiscoveryManager.updateInstanceWeight(
            "test-service", "DEFAULT_GROUP", "192.168.1.1", 8080, 2.5);

        // Then
        assertThat(result).isFalse();
        verify(namingService, times(1)).getAllInstances("test-service", "DEFAULT_GROUP");
        verify(namingService, never()).registerInstance(anyString(), anyString(), any(Instance.class));
    }

    @Test
    @DisplayName("订阅服务成功")
    void shouldSubscribeServiceSuccessfully() throws Exception {
        // Given
        com.alibaba.nacos.api.naming.listener.EventListener listener = mock(com.alibaba.nacos.api.naming.listener.EventListener.class);

        // When
        serviceDiscoveryManager.subscribe("test-service", "DEFAULT_GROUP", listener);

        // Then
        verify(namingService, times(1)).subscribe("test-service", "DEFAULT_GROUP", listener);
    }

    @Test
    @DisplayName("订阅服务失败")
    void shouldHandleSubscriptionFailure() throws Exception {
        // Given
        com.alibaba.nacos.api.naming.listener.EventListener listener = mock(com.alibaba.nacos.api.naming.listener.EventListener.class);
        doThrow(new RuntimeException("Subscription failed")).when(namingService)
            .subscribe(anyString(), anyString(), any(com.alibaba.nacos.api.naming.listener.EventListener.class));

        // When - should not throw exception
        serviceDiscoveryManager.subscribe("test-service", "DEFAULT_GROUP", listener);

        // Then - should catch exception and log error
        verify(namingService, times(1)).subscribe("test-service", "DEFAULT_GROUP", listener);
    }

    @Test
    @DisplayName("取消订阅服务成功")
    void shouldUnsubscribeServiceSuccessfully() throws Exception {
        // Given
        com.alibaba.nacos.api.naming.listener.EventListener listener = mock(com.alibaba.nacos.api.naming.listener.EventListener.class);

        // When
        serviceDiscoveryManager.unsubscribe("test-service", "DEFAULT_GROUP", listener);

        // Then
        verify(namingService, times(1)).unsubscribe("test-service", "DEFAULT_GROUP", listener);
    }

    @Test
    @DisplayName("取消订阅服务失败")
    void shouldHandleUnsubscriptionFailure() throws Exception {
        // Given
        com.alibaba.nacos.api.naming.listener.EventListener listener = mock(com.alibaba.nacos.api.naming.listener.EventListener.class);
        doThrow(new RuntimeException("Unsubscription failed")).when(namingService)
            .unsubscribe(anyString(), anyString(), any(com.alibaba.nacos.api.naming.listener.EventListener.class));

        // When - should not throw exception
        serviceDiscoveryManager.unsubscribe("test-service", "DEFAULT_GROUP", listener);

        // Then - should catch exception and log error
        verify(namingService, times(1)).unsubscribe("test-service", "DEFAULT_GROUP", listener);
    }

    @Test
    @DisplayName("实例转换正确")
    void shouldConvertInstanceCorrectly() throws Exception {
        // Given
        Instance nacosInstance = createTestInstance("192.168.1.1", 8080, true, true);
        nacosInstance.setInstanceId("instance-001");
        nacosInstance.setMetadata(Map.of("version", "1.0.0", "region", "us-east-1"));

        when(namingService.getAllInstances("test-service", "DEFAULT_GROUP"))
            .thenReturn(Arrays.asList(nacosInstance));

        // When
        List<ServiceInstance> instances = serviceDiscoveryManager.getServiceInstances("test-service", "DEFAULT_GROUP");

        // Then
        assertThat(instances).hasSize(1);
        ServiceInstance serviceInstance = instances.get(0);
        assertThat(serviceInstance.getInstanceId()).isEqualTo("instance-001");
        assertThat(serviceInstance.getMetadata().get("version")).isEqualTo("1.0.0");
        assertThat(serviceInstance.getMetadata().get("region")).isEqualTo("us-east-1");
    }

    private Instance createTestInstance(String ip, int port, boolean healthy, boolean enabled) {
        Instance instance = new Instance();
        instance.setIp(ip);
        instance.setPort(port);
        instance.setWeight(1.0);
        instance.setHealthy(healthy);
        instance.setEnabled(enabled);
        instance.setEphemeral(true);
        instance.setClusterName("DEFAULT");
        instance.setServiceName("test-service");
        instance.setMetadata(new HashMap<>());
        return instance;
    }

    private ServiceInstance createTestServiceInstance(String serviceName, String groupName) {
        return ServiceInstance.builder()
            .serviceName(serviceName)
            .groupName(groupName)
            .clusterName("DEFAULT")
            .ip("192.168.1.1")
            .port(8080)
            .weight(1.0)
            .healthy(true)
            .enabled(true)
            .ephemeral(true)
            .metadata(new HashMap<>())
            .instanceId("instance-001")
            .build();
    }
}
