package com.basebackend.common.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.spring.webmvc.v6_0.SpringWebMvcTelemetry;
import io.opentelemetry.instrumentation.spring.webmvc.v6_0.HttpTracingFilter;
import io.opentelemetry.instrumentation.spring.webmvc.v6_0.SpringWebMvcTelemetryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import java.util.EnumSet;

/**
 * 链路追踪自动配置类
 * 自动配置链路追踪相关的 Bean
 *
 * 配置内容:
 * 1. OpenTelemetry Bean
 * 2. Tracer Bean
 * 3. TracingInterceptor
 * 4. TracingUtil
 * 5. HTTP 过滤器
 * 6. Web MVC 配置
 *
 * @author basebackend team
 * @version 1.0
 */
@Configuration
@ConditionalOnClass({OpenTelemetry.class, Tracer.class})
public class TracingAutoConfiguration {

    @Value("${otel.enabled:true}")
    private boolean otelEnabled;

    @Value("${otel.exporter.jaeger.endpoint:http://jaeger-collector.observability.svc.cluster.local:14250}")
    private String jaegerEndpoint;

    @Value("${otel.service.name:basebackend-service}")
    private String serviceName;

    @Value("${otel.exporter.jaeger.timeout:10}")
    private long timeout;

    /**
     * 配置 TracingInterceptor Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public TracingInterceptor tracingInterceptor(Tracer tracer) {
        return new TracingInterceptor(tracer);
    }

    /**
     * 配置 TracingUtil Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public TracingUtil tracingUtil(Tracer tracer, OpenTelemetry openTelemetry) {
        return new TracingUtil(tracer, openTelemetry);
    }

    /**
     * 配置 HTTP 追踪过滤器
     */
    @Bean
    @ConditionalOnProperty(name = "otel.enabled", havingValue = "true")
    public FilterRegistrationBean<Filter> httpTracingFilter(OpenTelemetry openTelemetry) {
        SpringWebMvcTelemetry telemetry = SpringWebMvcTelemetry.builder(openTelemetry)
            .build();

        HttpTracingFilter filter = new HttpTracingFilter(telemetry);

        FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));
        registrationBean.setOrder(FilterRegistrationBean.HIGHEST_PRECEDENCE);

        return registrationBean;
    }

    /**
     * 配置 Web MVC 拦截器
     */
    @Bean
    @ConditionalOnClass({DispatcherServlet.class})
    public WebMvcConfigurer tracingWebMvcConfigurer(TracingInterceptor tracingInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(tracingInterceptor)
                    .addPathPatterns("/**");
            }
        };
    }

    /**
     * 配置链路追踪属性
     */
    @Bean
    @ConditionalOnMissingBean
    public TracingProperties tracingProperties() {
        TracingProperties properties = new TracingProperties();
        properties.setEnabled(otelEnabled);
        properties.setServiceName(serviceName);
        properties.setJaegerEndpoint(jaegerEndpoint);
        properties.setTimeout(timeout);
        return properties;
    }

    /**
     * 链路追踪属性配置类
     */
    public static class TracingProperties {

        private boolean enabled = true;
        private String serviceName;
        private String jaegerEndpoint;
        private long timeout = 10;
        private int samplingRate = 10;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getJaegerEndpoint() {
            return jaegerEndpoint;
        }

        public void setJaegerEndpoint(String jaegerEndpoint) {
            this.jaegerEndpoint = jaegerEndpoint;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }

        public int getSamplingRate() {
            return samplingRate;
        }

        public void setSamplingRate(int samplingRate) {
            this.samplingRate = samplingRate;
        }
    }
}
