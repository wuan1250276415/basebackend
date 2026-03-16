package com.basebackend.nacos.service;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.basebackend.nacos.model.ServiceInstance;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ServiceDiscoveryManagerTest {

    @Test
    void shouldRegisterInstanceSuccessfully() throws Exception {
        NamingService namingService = mock(NamingService.class);
        ServiceDiscoveryManager manager = new ServiceDiscoveryManager(namingService);

        ServiceInstance serviceInstance = ServiceInstance.builder()
            .serviceName("order-service")
            .groupName("DEFAULT_GROUP")
            .clusterName("DEFAULT")
            .ip("127.0.0.1")
            .port(8080)
            .weight(1.0)
            .healthy(true)
            .enabled(true)
            .ephemeral(true)
            .build();

        boolean registered = manager.registerInstance(serviceInstance);

        assertThat(registered).isTrue();
        verify(namingService).registerInstance(eq("order-service"), eq("DEFAULT_GROUP"), any(Instance.class));
    }

    @Test
    void shouldReturnFalseWhenRegisterThrowsException() throws Exception {
        NamingService namingService = mock(NamingService.class);
        doThrow(new RuntimeException("register failed")).when(namingService).registerInstance(eq("order-service"), eq("DEFAULT_GROUP"), any(Instance.class));

        ServiceDiscoveryManager manager = new ServiceDiscoveryManager(namingService);
        ServiceInstance serviceInstance = ServiceInstance.builder()
            .serviceName("order-service")
            .groupName("DEFAULT_GROUP")
            .clusterName("DEFAULT")
            .ip("127.0.0.1")
            .port(8080)
            .weight(1.0)
            .healthy(true)
            .enabled(true)
            .ephemeral(true)
            .build();

        assertThat(manager.registerInstance(serviceInstance)).isFalse();
    }
}
