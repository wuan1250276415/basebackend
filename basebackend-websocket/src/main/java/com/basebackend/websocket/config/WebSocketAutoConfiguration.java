package com.basebackend.websocket.config;

import com.basebackend.websocket.broadcast.BroadcastStrategy;
import com.basebackend.websocket.broadcast.MemoryBroadcastStrategy;
import com.basebackend.websocket.broadcast.RedisBroadcastStrategy;
import com.basebackend.websocket.channel.ChannelManager;
import com.basebackend.websocket.handler.WsMessageHandler;
import com.basebackend.websocket.interceptor.AuthHandshakeInterceptor;
import com.basebackend.websocket.message.WsMessage;
import com.basebackend.websocket.session.SessionManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 自动配置
 * <p>
 * 需要显式启用：{@code basebackend.websocket.enabled=true}
 *
 * <p>Bean 注册顺序（通过 @Bean 方法参数注入，非构造器注入，避免循环依赖）：
 * <ol>
 *   <li>{@link SessionManager}</li>
 *   <li>{@link ChannelManager}</li>
 *   <li>{@link BroadcastStrategy}（memory / redis，按配置选择）</li>
 *   <li>{@link WsMessageHandler}（依赖上述三者）</li>
 *   <li>WebSocket handler 注册 + 心跳调度器</li>
 * </ol>
 */
@Slf4j
@AutoConfiguration
@EnableWebSocket
@EnableConfigurationProperties(WebSocketProperties.class)
@ConditionalOnProperty(prefix = "basebackend.websocket", name = "enabled", havingValue = "true")
@ConditionalOnClass(name = "org.springframework.web.socket.WebSocketHandler")
public class WebSocketAutoConfiguration implements WebSocketConfigurer {

    /**
     * 字段注入（非构造器），确保 @Bean 方法执行后 handler 已就绪，
     * registerWebSocketHandlers 调用时直接使用。
     */
    private WsMessageHandler messageHandler;
    private WebSocketProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public SessionManager sessionManager(WebSocketProperties wsProperties) {
        log.info("注册 WebSocket SessionManager: maxConnections={}, maxUserConnections={}",
                wsProperties.getMaxConnections(), wsProperties.getMaxUserConnections());
        return new SessionManager(
                wsProperties.getMaxConnections(),
                wsProperties.getMaxUserConnections(),
                wsProperties.getSendTimeLimitMs(),
                wsProperties.getSendBufferSizeKb()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public ChannelManager channelManager() {
        log.info("注册 WebSocket ChannelManager");
        return new ChannelManager();
    }

    // --- 广播策略 ---

    /**
     * 内存广播（默认，单机模式）
     * 当 {@code broadcast.type} 未配置或为 {@code memory} 时生效
     */
    @Bean
    @ConditionalOnMissingBean(BroadcastStrategy.class)
    @ConditionalOnProperty(
            prefix = "basebackend.websocket.broadcast", name = "type",
            havingValue = "memory", matchIfMissing = true)
    public BroadcastStrategy memoryBroadcastStrategy(SessionManager sessionManager) {
        log.info("WebSocket 广播模式: memory（单机）");
        return new MemoryBroadcastStrategy(sessionManager);
    }

    /**
     * Redis 广播（集群模式）
     * 通过嵌套 {@link RedisConfig} 配置，仅在 Redis 类路径存在时加载。
     */
    @Configuration
    @ConditionalOnClass(name = {
            "org.springframework.data.redis.core.StringRedisTemplate",
            "org.springframework.data.redis.listener.RedisMessageListenerContainer"
    })
    @ConditionalOnProperty(
            prefix = "basebackend.websocket.broadcast", name = "type", havingValue = "redis")
    static class RedisConfig {

        @Bean
        @ConditionalOnMissingBean(BroadcastStrategy.class)
        public BroadcastStrategy redisBroadcastStrategy(SessionManager sessionManager,
                                                         StringRedisTemplate redisTemplate,
                                                         WebSocketProperties wsProperties) {
            String topic = wsProperties.getBroadcast().getRedisTopic();
            log.info("WebSocket 广播模式: redis（集群）, topic={}", topic);
            return new RedisBroadcastStrategy(sessionManager, redisTemplate, topic);
        }

        /**
         * Redis 消息监听容器：订阅广播 topic，收到消息后触发各节点本地广播
         */
        @Bean
        @ConditionalOnMissingBean(name = "websocketRedisListenerContainer")
        public RedisMessageListenerContainer websocketRedisListenerContainer(
                RedisConnectionFactory connectionFactory,
                BroadcastStrategy broadcastStrategy,
                WebSocketProperties wsProperties) {

            if (!(broadcastStrategy instanceof RedisBroadcastStrategy redisBroadcastStrategy)) {
                throw new IllegalStateException("Redis 模式下 BroadcastStrategy 必须是 RedisBroadcastStrategy");
            }
            RedisMessageListenerContainer container = new RedisMessageListenerContainer();
            container.setConnectionFactory(connectionFactory);
            container.addMessageListener(redisBroadcastStrategy,
                    new PatternTopic(wsProperties.getBroadcast().getRedisTopic()));
            log.info("Redis WebSocket 监听容器已注册: topic={}",
                    wsProperties.getBroadcast().getRedisTopic());
            return container;
        }
    }

    // --- Handler ---

    @Bean
    @ConditionalOnMissingBean
    public WsMessageHandler wsMessageHandler(SessionManager sessionManager,
                                              ChannelManager channelManager,
                                              ObjectMapper objectMapper,
                                              WebSocketProperties wsProperties,
                                              BroadcastStrategy broadcastStrategy) {
        log.info("注册 WebSocket MessageHandler: maxMessageSizeKb={}", wsProperties.getMaxMessageSizeKb());
        WsMessageHandler handler = new WsMessageHandler(
                sessionManager, channelManager, objectMapper,
                wsProperties.getMaxMessageSizeKb() * 1024,
                broadcastStrategy
        );
        this.messageHandler = handler;
        this.properties = wsProperties;
        return handler;
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthHandshakeInterceptor authHandshakeInterceptor() {
        return new AuthHandshakeInterceptor();
    }

    // --- 心跳调度器 ---

    /**
     * 心跳调度器：定期向所有在线会话发送 server ping，并清理超时会话。
     * <p>
     * 使用 daemon 线程，JVM 退出时自动终止（{@code destroyMethod="shutdown"}）。
     */
    @Bean(name = "websocketHeartbeatScheduler", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "websocketHeartbeatScheduler")
    public ScheduledExecutorService websocketHeartbeatScheduler(
            WebSocketProperties wsProperties,
            SessionManager sessionManager,
            ObjectMapper objectMapper,
            BroadcastStrategy broadcastStrategy) {

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ws-heartbeat");
            t.setDaemon(true);
            return t;
        });

        long intervalMs = wsProperties.getHeartbeat().getInterval().toMillis();
        Duration timeout = wsProperties.getHeartbeat().getTimeout();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                // 1. 广播 server ping 到所有节点
                String ping = objectMapper.writeValueAsString(WsMessage.heartbeat());
                broadcastStrategy.broadcast(ping);

                // 2. 关闭超时未活跃的会话（仅清理本节点）
                List<String> stale = sessionManager.getStaleSessionIds(timeout);
                if (!stale.isEmpty()) {
                    log.info("清理超时会话: count={}", stale.size());
                    stale.forEach(sessionManager::disconnectSession);
                }
            } catch (JsonProcessingException e) {
                log.warn("心跳消息序列化失败", e);
            } catch (Exception e) {
                log.warn("心跳调度异常", e);
            }
        }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);

        log.info("WebSocket 心跳调度器已启动: interval={}ms, timeout={}", intervalMs, timeout);
        return scheduler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        if (messageHandler == null || properties == null) {
            log.error("WebSocket Handler 未初始化，请检查 Bean 注册顺序");
            return;
        }
        var registration = registry.addHandler(messageHandler, properties.getEndpoint())
                .addInterceptors(authHandshakeInterceptor())
                .setAllowedOrigins(properties.getAllowedOrigins().toArray(String[]::new));

        if (properties.isSockjsEnabled()) {
            registration.withSockJS();
            log.info("SockJS 回退已启用");
        }
        log.info("WebSocket 模块已启用: endpoint={}", properties.getEndpoint());
    }
}
