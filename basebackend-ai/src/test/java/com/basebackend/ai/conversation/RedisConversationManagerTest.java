package com.basebackend.ai.conversation;

import com.basebackend.ai.client.AiMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("RedisConversationManager 测试")
class RedisConversationManagerTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisConversationManager manager;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        manager = new RedisConversationManager(redisTemplate, 5, Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("addMessage 使用 Lua 脚本原子写入")
    void addMessageUsesLuaScript() {
        when(redisTemplate.execute(any(), anyList(), any(), any(), any())).thenReturn("ok");

        manager.addMessage("conv1", AiMessage.user("hello"));

        verify(redisTemplate).execute(any(), eq(List.of("ai:conversation:conv1")), any(), eq("5"), any());
    }

    @Test
    @DisplayName("getMessages 正常反序列化")
    void getMessages() {
        when(valueOperations.get("ai:conversation:conv1"))
                .thenReturn("[{\"role\":\"user\",\"content\":\"hello\"}]");

        List<AiMessage> messages = manager.getMessages("conv1");

        assertThat(messages).containsExactly(AiMessage.user("hello"));
    }

    @Test
    @DisplayName("getMessages 遇到脏数据时抛异常而不是静默吞掉")
    void getMessagesCorruptedPayload() {
        when(valueOperations.get("ai:conversation:conv1")).thenReturn("{bad json}");

        assertThatThrownBy(() -> manager.getMessages("conv1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("conversationId=conv1");
    }

    @Test
    @DisplayName("addMessage 脚本执行失败时抛异常")
    void addMessageScriptFailed() {
        when(redisTemplate.execute(any(), anyList(), any(), any(), any()))
                .thenThrow(new RuntimeException("redis error"));

        assertThatThrownBy(() -> manager.addMessage("conv1", AiMessage.user("hello")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("conversationId=conv1");
    }
}
