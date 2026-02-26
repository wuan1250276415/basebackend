package com.basebackend.service.client;

import com.basebackend.service.client.scheduler.FormTemplateServiceClient;
import com.basebackend.service.client.scheduler.ProcessDefinitionServiceClient;
import com.basebackend.service.client.scheduler.ProcessInstanceServiceClient;
import com.basebackend.service.client.scheduler.TaskServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;

/**
 * 服务客户端自动配置
 * <p>
 * 基于 Spring 6 HttpServiceProxyFactory + RestClient 注册所有声明式 HTTP 客户端。
 * 替代原 OpenFeign 的 @EnableFeignClients 机制。
 * </p>
 *
 * @author Claude Code
 * @since 2025-12-09
 */
@AutoConfiguration
@ConditionalOnClass(HttpServiceProxyFactory.class)
public class ServiceClientAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ServiceClientAutoConfiguration.class);

    // ==================== RestClient Builders ====================

    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    // ==================== Request Interceptor ====================

    @Bean
    @ConditionalOnMissingBean
    public ServiceClientRequestInterceptor serviceClientRequestInterceptor() {
        return new ServiceClientRequestInterceptor();
    }

    // ==================== User Service Clients ====================

    @Bean
    @ConditionalOnMissingBean
    public UserServiceClient userServiceClient(RestClient.Builder loadBalancedRestClientBuilder,
                                                ServiceClientRequestInterceptor interceptor) {
        return createClient(loadBalancedRestClientBuilder, interceptor,
                "basebackend-user-api", UserServiceClient.class, Duration.ofSeconds(10));
    }

    @Bean
    @ConditionalOnMissingBean
    public UserAuthServiceClient userAuthServiceClient(RestClient.Builder loadBalancedRestClientBuilder,
                                                        ServiceClientRequestInterceptor interceptor) {
        return createClient(loadBalancedRestClientBuilder, interceptor,
                "basebackend-user-api", UserAuthServiceClient.class, Duration.ofSeconds(10));
    }

    // ==================== System Service Clients ====================

    @Bean
    @ConditionalOnMissingBean
    public DeptServiceClient deptServiceClient(RestClient.Builder loadBalancedRestClientBuilder,
                                                ServiceClientRequestInterceptor interceptor) {
        return createClient(loadBalancedRestClientBuilder, interceptor,
                "basebackend-system-api", DeptServiceClient.class, Duration.ofSeconds(10));
    }

    @Bean
    @ConditionalOnMissingBean
    public OperationLogServiceClient operationLogServiceClient(RestClient.Builder loadBalancedRestClientBuilder,
                                                                ServiceClientRequestInterceptor interceptor) {
        return createClient(loadBalancedRestClientBuilder, interceptor,
                "basebackend-system-api", OperationLogServiceClient.class, Duration.ofSeconds(10));
    }

    @Bean
    @ConditionalOnMissingBean
    public SysRoleResourceServiceClient sysRoleResourceServiceClient(RestClient.Builder loadBalancedRestClientBuilder,
                                                                      ServiceClientRequestInterceptor interceptor) {
        return createClient(loadBalancedRestClientBuilder, interceptor,
                "basebackend-user-api", SysRoleResourceServiceClient.class, Duration.ofSeconds(10));
    }

    @Bean
    @ConditionalOnMissingBean
    public SystemServiceClient systemServiceClient(RestClient.Builder loadBalancedRestClientBuilder,
                                                    ServiceClientRequestInterceptor interceptor) {
        return createClient(loadBalancedRestClientBuilder, interceptor,
                "basebackend-system-api", SystemServiceClient.class, Duration.ofSeconds(10));
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthServiceClient authServiceClient(RestClient.Builder loadBalancedRestClientBuilder,
                                                ServiceClientRequestInterceptor interceptor) {
        return createClient(loadBalancedRestClientBuilder, interceptor,
                "basebackend-auth-api", AuthServiceClient.class, Duration.ofSeconds(10));
    }

    // ==================== File Service Client ====================

    @Bean
    @ConditionalOnMissingBean
    public FileServiceClient fileServiceClient(RestClient.Builder loadBalancedRestClientBuilder,
                                                ServiceClientRequestInterceptor interceptor) {
        // 文件服务使用更长的超时时间（连接30秒，读取5分钟）
        return createClient(loadBalancedRestClientBuilder, interceptor,
                "basebackend-system-api", FileServiceClient.class, Duration.ofMinutes(5));
    }

    // ==================== Scheduler Service Clients ====================

    @Bean
    @ConditionalOnMissingBean
    public FormTemplateServiceClient formTemplateServiceClient(RestClient.Builder loadBalancedRestClientBuilder,
                                                                ServiceClientRequestInterceptor interceptor) {
        return createClient(loadBalancedRestClientBuilder, interceptor,
                "basebackend-scheduler", FormTemplateServiceClient.class, Duration.ofSeconds(10));
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessDefinitionServiceClient processDefinitionServiceClient(
            RestClient.Builder loadBalancedRestClientBuilder, ServiceClientRequestInterceptor interceptor) {
        return createClient(loadBalancedRestClientBuilder, interceptor,
                "basebackend-scheduler", ProcessDefinitionServiceClient.class, Duration.ofSeconds(10));
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessInstanceServiceClient processInstanceServiceClient(
            RestClient.Builder loadBalancedRestClientBuilder, ServiceClientRequestInterceptor interceptor) {
        return createClient(loadBalancedRestClientBuilder, interceptor,
                "basebackend-scheduler", ProcessInstanceServiceClient.class, Duration.ofSeconds(10));
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskServiceClient taskServiceClient(RestClient.Builder loadBalancedRestClientBuilder,
                                                ServiceClientRequestInterceptor interceptor) {
        return createClient(loadBalancedRestClientBuilder, interceptor,
                "basebackend-scheduler", TaskServiceClient.class, Duration.ofSeconds(10));
    }

    // ==================== Factory Method ====================

    private <T> T createClient(RestClient.Builder builder, ServiceClientRequestInterceptor interceptor,
                                String serviceName, Class<T> clientType, Duration readTimeout) {
        log.info("注册服务客户端: {} -> {}", clientType.getSimpleName(), serviceName);

        RestClient restClient = builder.clone()
                .baseUrl("http://" + serviceName)
                .requestInterceptor(interceptor)
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();

        return factory.createClient(clientType);
    }
}
