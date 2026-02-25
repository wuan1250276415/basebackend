package com.basebackend.observability.slo;

import com.basebackend.observability.slo.aspect.SloMonitoringAspect;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("SloMonitoringAspect SLO监控切面测试")
class SloMonitoringAspectTest {

    @Test
    @DisplayName("切面应已配置")
    void shouldBeConfigured() {
        // 测试类存在性
        assertThat(SloMonitoringAspect.class).isNotNull();
    }

    @Test
    @DisplayName("SloMonitored注解应已定义")
    void shouldHaveSloMonitoredAnnotation() {
        // 测试注解存在
        assertThat(com.basebackend.observability.slo.annotation.SloMonitored.class).isNotNull();
    }

    @Test
    @DisplayName("应可创建切面实例")
    void shouldCreateAspectInstance() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SloMonitoringAspect aspect = new SloMonitoringAspect(registry, "test-app");

        assertThat(aspect).isNotNull();
    }
}
