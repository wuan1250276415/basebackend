package com.basebackend.websocket.broadcast;

import com.basebackend.websocket.session.SessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;

/**
 * Redis Pub/Sub 广播策略（集群模式）
 * <p>
 * 工作原理：
 * <ol>
 *   <li>发布方调用 {@link #broadcast(String)}，将消息发布到 Redis channel</li>
 *   <li>集群中每个节点订阅同一 Redis channel（通过 {@code RedisMessageListenerContainer} 注册）</li>
 *   <li>各节点收到订阅消息后，由 {@link #onMessage} 触发本地 {@link SessionManager#broadcast}</li>
 * </ol>
 * <p>
 * 配置示例：
 * <pre>
 * basebackend:
 *   websocket:
 *     broadcast:
 *       type: redis
 *       redis-topic: ws:broadcast
 * </pre>
 * <p>
 * 仅在 {@code spring-boot-starter-data-redis} 存在于类路径时生效（通过 {@code @ConditionalOnClass} 保护）。
 */
@Slf4j
public class RedisBroadcastStrategy implements BroadcastStrategy, MessageListener {

    private final SessionManager sessionManager;
    private final StringRedisTemplate redisTemplate;
    private final String topic;

    public RedisBroadcastStrategy(SessionManager sessionManager,
                                   StringRedisTemplate redisTemplate,
                                   String topic) {
        this.sessionManager = sessionManager;
        this.redisTemplate = redisTemplate;
        this.topic = topic;
    }

    /**
     * 将消息发布到 Redis channel；各节点订阅者收到后触发本地广播
     */
    @Override
    public void broadcast(String message) {
        redisTemplate.convertAndSend(topic, message);
        log.debug("Redis 广播已发布: topic={}", topic);
    }

    /**
     * Redis 订阅回调：将收到的消息本地广播到当前节点所有在线会话
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        int sent = sessionManager.broadcast(payload);
        log.debug("Redis 订阅消息本地广播: sent={}", sent);
    }
}
