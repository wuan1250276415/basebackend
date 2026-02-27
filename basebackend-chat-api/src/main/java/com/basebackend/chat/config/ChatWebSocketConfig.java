package com.basebackend.chat.config;

import com.basebackend.chat.mapper.ChatConversationMemberMapper;
import com.basebackend.chat.mapper.ChatMessageMapper;
import com.basebackend.chat.service.ChatConversationService;
import com.basebackend.chat.service.ChatMessageService;
import com.basebackend.chat.service.OnlineStatusService;
import com.basebackend.chat.websocket.ChatAuthHandshakeInterceptor;
import com.basebackend.chat.websocket.ChatWebSocketHandler;
import com.basebackend.jwt.JwtUtil;
import com.basebackend.websocket.session.SessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 聊天 WebSocket 配置
 * <p>
 * 仅在 {@code basebackend.chat.enabled=true} 时激活。
 * 注册 JWT 握手鉴权拦截器、聊天 WebSocket 处理器、心跳超时调度。
 */
@Slf4j
@Configuration
@EnableWebSocket
@EnableScheduling
@EnableConfigurationProperties(ChatProperties.class)
@ConditionalOnProperty(prefix = "basebackend.chat", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class ChatWebSocketConfig implements WebSocketConfigurer {

    private final ChatProperties chatProperties;
    private final ObjectProvider<ChatWebSocketHandler> handlerProvider;
    private final ObjectProvider<ChatAuthHandshakeInterceptor> interceptorProvider;

    @Bean
    @ConditionalOnMissingBean(ChatAuthHandshakeInterceptor.class)
    @ConditionalOnBean(JwtUtil.class)
    public ChatAuthHandshakeInterceptor chatAuthHandshakeInterceptor(JwtUtil jwtUtil) {
        log.info("初始化 ChatAuthHandshakeInterceptor (JWT 握手鉴权)");
        return new ChatAuthHandshakeInterceptor(jwtUtil);
    }

    @Bean
    @ConditionalOnMissingBean(ChatWebSocketHandler.class)
    @ConditionalOnBean(SessionManager.class)
    public ChatWebSocketHandler chatWebSocketHandler(
            SessionManager sessionManager,
            ChatMessageService messageService,
            ChatConversationService conversationService,
            OnlineStatusService onlineStatusService,
            ChatConversationMemberMapper conversationMemberMapper,
            ChatMessageMapper messageMapper,
            ObjectMapper objectMapper) {
        log.info("初始化 ChatWebSocketHandler, endpoint={}", chatProperties.getWsEndpoint());
        return new ChatWebSocketHandler(
                sessionManager, messageService, conversationService,
                onlineStatusService, conversationMemberMapper, messageMapper,
                objectMapper);
    }

    @Bean
    @ConditionalOnBean(ChatWebSocketHandler.class)
    public ChatHeartbeatScheduler chatHeartbeatScheduler(ChatWebSocketHandler handler) {
        log.info("初始化心跳超时检测调度, timeout={}ms", chatProperties.getHeartbeatTimeoutMs());
        return new ChatHeartbeatScheduler(handler, chatProperties.getHeartbeatTimeoutMs());
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        ChatWebSocketHandler handler = handlerProvider.getIfAvailable();
        if (handler == null) {
            log.warn("跳过 WebSocket 端点注册: ChatWebSocketHandler 未就绪（SessionManager 可能不存在）");
            return;
        }

        var registration = registry
                .addHandler(handler, chatProperties.getWsEndpoint())
                .setAllowedOrigins(chatProperties.getAllowedOrigins().toArray(String[]::new));

        ChatAuthHandshakeInterceptor interceptor = interceptorProvider.getIfAvailable();
        if (interceptor != null) {
            registration.addInterceptors(interceptor);
            log.info("聊天 WebSocket 端点已注册 (JWT 鉴权): {}", chatProperties.getWsEndpoint());
        } else {
            log.info("聊天 WebSocket 端点已注册 (无鉴权): {}", chatProperties.getWsEndpoint());
        }
    }
}
