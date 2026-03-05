package com.basebackend.ai.conversation;

import com.basebackend.ai.client.AiMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
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
    private static final String APPEND_AND_TRIM_SCRIPT = """
            local key = KEYS[1]
            local messageJson = ARGV[1]
            local maxHistory = tonumber(ARGV[2])
            local ttlMillis = tonumber(ARGV[3])

            local messages = {}
            local existing = redis.call('GET', key)
            if existing and existing ~= '' then
                local ok, decoded = pcall(cjson.decode, existing)
                if (not ok) or type(decoded) ~= 'table' then
                    return redis.error_reply('conversation payload corrupted')
                end
                messages = decoded
            end

            local messageOk, message = pcall(cjson.decode, messageJson)
            if (not messageOk) or type(message) ~= 'table' then
                return redis.error_reply('message payload invalid')
            end

            table.insert(messages, message)

            if maxHistory and maxHistory > 0 then
                while #messages > maxHistory do
                    if #messages > 1 and messages[1] and messages[1]['role'] == 'system' then
                        table.remove(messages, 2)
                    else
                        table.remove(messages, 1)
                    end
                end
            end

            local encoded = cjson.encode(messages)
            if ttlMillis and ttlMillis > 0 then
                redis.call('PSETEX', key, ttlMillis, encoded)
            else
                redis.call('SET', key, encoded)
            end
            return encoded
            """;
    private static final DefaultRedisScript<String> APPEND_AND_TRIM_REDIS_SCRIPT = buildAppendAndTrimScript();

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

        try {
            String messageJson = objectMapper.writeValueAsString(message);
            redisTemplate.execute(
                    APPEND_AND_TRIM_REDIS_SCRIPT,
                    List.of(key),
                    messageJson,
                    String.valueOf(maxHistory),
                    String.valueOf(ttl.toMillis())
            );
        } catch (JsonProcessingException e) {
            log.error("序列化消息失败, conversationId={}", conversationId, e);
            throw new IllegalStateException("序列化对话消息失败, conversationId=" + conversationId, e);
        } catch (Exception e) {
            log.error("Redis 原子写入对话失败, conversationId={}", conversationId, e);
            throw new IllegalStateException("Redis 写入对话消息失败, conversationId=" + conversationId, e);
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
            throw new IllegalStateException("反序列化对话消息失败, conversationId=" + conversationId, e);
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

    private static DefaultRedisScript<String> buildAppendAndTrimScript() {
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setScriptText(APPEND_AND_TRIM_SCRIPT);
        script.setResultType(String.class);
        return script;
    }
}
