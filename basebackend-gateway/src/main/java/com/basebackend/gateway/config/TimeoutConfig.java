package com.basebackend.gateway.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Gateway 超时与连接池配置
 *
 * 通过统一的 HttpClient Bean 管理连接池和各类超时，避免在其他位置重复配置。
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "gateway.timeout")
@Data
public class TimeoutConfig {

    /**
     * 默认超时时间
     */
    private TimeoutSettings defaults = new TimeoutSettings(3000, 5000, 30000, 30000);

    /**
     * 针对不同服务的定制超时
     */
    private Map<String, TimeoutSettings> services = new HashMap<>();

    /**
     * 初始化内置服务的默认值，避免忘记在配置文件中声明
     */
    @PostConstruct
    public void initDefaultServiceTimeouts() {
        if (!services.isEmpty()) {
            return;
        }
        services.put("basebackend-admin-api", new TimeoutSettings(3000, 5000, 30000, 30000));
        services.put("basebackend-demo-api", new TimeoutSettings(2000, 3000, 10000, 10000));
        services.put("file-service", new TimeoutSettings(5000, 30000, 60000, 60000));
        services.put("static-service", new TimeoutSettings(1000, 1000, 5000, 5000));
        log.info("已按默认值初始化常用服务的超时配置");
    }

    /**
     * 自定义 HttpClient Bean，Spring Cloud Gateway 会自动注入该实例
     */
    @Bean
    public HttpClient httpClient() {
        TimeoutSettings defaultTimeout = defaults;

        ConnectionProvider connectionProvider = ConnectionProvider.builder("gateway-connection-pool")
                .maxConnections(500)
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofSeconds(60))
                .pendingAcquireTimeout(Duration.ofSeconds(45))
                .evictInBackground(Duration.ofSeconds(120))
                .build();

        return HttpClient.create(connectionProvider)
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, defaultTimeout.getConnectTimeout())
                .responseTimeout(Duration.ofMillis(defaultTimeout.getResponseTimeout()))
                .keepAlive(true)
                .option(io.netty.channel.ChannelOption.TCP_NODELAY, true)
                .option(io.netty.channel.ChannelOption.SO_KEEPALIVE, true);
    }

    /**
     * 对外暴露读取到的服务级超时，方便其他组件引用
     */
    public TimeoutSettings getServiceTimeout(String serviceId) {
        return services.getOrDefault(serviceId, defaults);
    }

    /**
     * 超时配置载体
     */
    @Data
    public static class TimeoutSettings {
        private int connectTimeout;
        private int responseTimeout;
        private int readTimeout;
        private int writeTimeout;

        public TimeoutSettings() {
        }

        public TimeoutSettings(int connectTimeout, int responseTimeout, int readTimeout, int writeTimeout) {
            this.connectTimeout = connectTimeout;
            this.responseTimeout = responseTimeout;
            this.readTimeout = readTimeout;
            this.writeTimeout = writeTimeout;
        }
    }
}
