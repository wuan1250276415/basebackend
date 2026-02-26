package com.basebackend.ai.conversation;

import com.basebackend.ai.client.AiMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于 Redis 的对话管理器
 * <p>
 * 支持分布式环境下的对话上下文共享，自动 TTL 过期。
 */
@Slf4j
public class RedisConversationManager implements ConversationManager {

    private static final String KEY_PREFIX = "ai:conversation:";
    private static final TypeReference<List<AiMessage>> MESSAGE_LIST_TYPE = new TypeReference<>() {};

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final int maxHistory;
    private final Duration ttl;

    public RedisConversationManager(StringRedisTemplate redisTemplate, int maxHistory, Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.maxHistory = maxHistory;
        this.ttl = ttl;
    }

    @Override
    public void addMessage(String conversationId, AiMessage message) {
        String key = buildKey(conversationId);
        List<AiMessage> messages = getMessages(conversationId);

        List<AiMessage> mutable = new ArrayList<>(messages);
        mutable.add(message);

        // 超出最大历史时裁剪
        while (mutable.size() > maxHistory) {
            if (mutable.size() > 1 && AiMessage.ROLE_SYSTEM.equals(mutable.getFirst().role())) {
                mutable.remove(1);
            } else {
                mutable.removeFirst();
            }
        }

        try {
            String json = objectMapper.writeValueAsString(mutable);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException e) {
            log.error("序列化对话消息失败, conversationId={}", conversationId, e);
        }
    }

    @Override
    public List<AiMessage> getMessages(String conversationId) {
        String key = buildKey(conversationId);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, MESSAGE_LIST_TYPE);
        } catch (JsonProcessingException e) {
            log.error("反序列化对话消息失败, conversationId={}", conversationId, e);
            return List.of();
        }
    }

    @Override
    public List<AiMessage> getRecentMessages(String conversationId, int limit) {
        List<AiMessage> all = getMessages(conversationId);
        if (all.size() <= limit) {
            return all;
        }
        return all.subList(all.size() - limit, all.size());
    }

    @Override
    public void clearConversation(String conversationId) {
        redisTemplate.delete(buildKey(conversationId));
    }

    @Override
    public boolean exists(String conversationId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(conversationId)));
    }

    private String buildKey(String conversationId) {
        return KEY_PREFIX + conversationId;
    }
}
