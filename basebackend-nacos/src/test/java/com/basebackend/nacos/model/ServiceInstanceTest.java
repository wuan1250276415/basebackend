package com.basebackend.nacos.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceInstanceTest {

    @Test
    void shouldBuildServiceInstance() {
        ServiceInstance instance = ServiceInstance.builder()
            .serviceName("order-service")
            .groupName("DEFAULT_GROUP")
            .clusterName("DEFAULT")
            .ip("127.0.0.1")
            .port(8080)
            .weight(1.0)
            .healthy(true)
            .enabled(true)
            .ephemeral(true)
            .metadata(Map.of("zone", "sh"))
            .instanceId("order-service-127.0.0.1-8080")
            .build();

        assertThat(instance.getServiceName()).isEqualTo("order-service");
        assertThat(instance.getIp()).isEqualTo("127.0.0.1");
        assertThat(instance.getPort()).isEqualTo(8080);
        assertThat(instance.getMetadata()).containsEntry("zone", "sh");
    }
}
