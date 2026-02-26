package com.basebackend.websocket.config;

import com.basebackend.websocket.channel.ChannelManager;
import com.basebackend.websocket.handler.WsMessageHandler;
import com.basebackend.websocket.interceptor.AuthHandshakeInterceptor;
import com.basebackend.websocket.session.SessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 自动配置
 * <p>
 * 需要显式启用：{@code basebackend.websocket.enabled=true}
 */
@Slf4j
@AutoConfiguration
@EnableWebSocket
@EnableConfigurationProperties(WebSocketProperties.class)
@ConditionalOnProperty(prefix = "basebackend.websocket", name = "enabled", havingValue = "true")
@ConditionalOnClass(name = "org.springframework.web.socket.WebSocketHandler")
public class WebSocketAutoConfiguration implements WebSocketConfigurer {

    private final WebSocketProperties properties;
    private final SessionManager sessionManager;
    private final ChannelManager channelManager;
    private final WsMessageHandler messageHandler;

    public WebSocketAutoConfiguration(WebSocketProperties properties,
                                       SessionManager sessionManager,
                                       ChannelManager channelManager,
                                       WsMessageHandler messageHandler) {
        this.properties = properties;
        this.sessionManager = sessionManager;
        this.channelManager = channelManager;
        this.messageHandler = messageHandler;
        log.info("WebSocket 模块已启用, endpoint={}", properties.getEndpoint());
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        var registration = registry.addHandler(messageHandler, properties.getEndpoint())
                .addInterceptors(new AuthHandshakeInterceptor())
                .setAllowedOrigins(properties.getAllowedOrigins().toArray(String[]::new));

        if (properties.isSockjsEnabled()) {
            registration.withSockJS();
            log.info("SockJS 回退已启用");
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public SessionManager sessionManager() {
        log.info("注册 WebSocket SessionManager");
        return new SessionManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public ChannelManager channelManager() {
        log.info("注册 WebSocket ChannelManager");
        return new ChannelManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public WsMessageHandler wsMessageHandler(SessionManager sessionManager,
                                              ChannelManager channelManager,
                                              ObjectMapper objectMapper) {
        log.info("注册 WebSocket MessageHandler");
        return new WsMessageHandler(sessionManager, channelManager, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthHandshakeInterceptor authHandshakeInterceptor() {
        return new AuthHandshakeInterceptor();
    }
}
