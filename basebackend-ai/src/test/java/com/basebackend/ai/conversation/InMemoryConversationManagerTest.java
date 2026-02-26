package com.basebackend.ai.conversation;

import com.basebackend.ai.client.AiMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("InMemoryConversationManager 测试")
class InMemoryConversationManagerTest {

    private InMemoryConversationManager manager;

    @BeforeEach
    void setUp() {
        // maxHistory=5, ttl=1小时
        manager = new InMemoryConversationManager(5, 3600_000);
    }

    @Test
    @DisplayName("添加和获取消息")
    void addAndGetMessages() {
        manager.addMessage("conv1", AiMessage.user("你好"));
        manager.addMessage("conv1", AiMessage.assistant("你好！"));

        List<AiMessage> messages = manager.getMessages("conv1");
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).content()).isEqualTo("你好");
        assertThat(messages.get(1).content()).isEqualTo("你好！");
    }

    @Test
    @DisplayName("获取不存在的对话返回空列表")
    void getMessagesNonexistent() {
        assertThat(manager.getMessages("unknown")).isEmpty();
    }

    @Test
    @DisplayName("getRecentMessages 限制条数")
    void getRecentMessages() {
        manager.addMessage("conv1", AiMessage.user("msg1"));
        manager.addMessage("conv1", AiMessage.assistant("msg2"));
        manager.addMessage("conv1", AiMessage.user("msg3"));
        manager.addMessage("conv1", AiMessage.assistant("msg4"));

        List<AiMessage> recent = manager.getRecentMessages("conv1", 2);
        assertThat(recent).hasSize(2);
        assertThat(recent.get(0).content()).isEqualTo("msg3");
        assertThat(recent.get(1).content()).isEqualTo("msg4");
    }

    @Test
    @DisplayName("getRecentMessages 不足 limit 时全部返回")
    void getRecentMessagesLessThanLimit() {
        manager.addMessage("conv1", AiMessage.user("msg1"));
        List<AiMessage> recent = manager.getRecentMessages("conv1", 10);
        assertThat(recent).hasSize(1);
    }

    @Test
    @DisplayName("超出 maxHistory 自动裁剪")
    void maxHistoryTrimming() {
        for (int i = 1; i <= 8; i++) {
            manager.addMessage("conv1", AiMessage.user("msg" + i));
        }

        List<AiMessage> messages = manager.getMessages("conv1");
        assertThat(messages).hasSize(5);
        // 最旧的消息被移除，保留最新的5条
        assertThat(messages.getFirst().content()).isEqualTo("msg4");
        assertThat(messages.getLast().content()).isEqualTo("msg8");
    }

    @Test
    @DisplayName("maxHistory 裁剪时保留 system 消息")
    void maxHistoryPreservesSystemMessage() {
        manager.addMessage("conv1", AiMessage.system("你是助手"));
        for (int i = 1; i <= 6; i++) {
            manager.addMessage("conv1", AiMessage.user("msg" + i));
        }

        List<AiMessage> messages = manager.getMessages("conv1");
        assertThat(messages).hasSize(5);
        // system 消息被保留在首位
        assertThat(messages.getFirst().role()).isEqualTo("system");
        assertThat(messages.getFirst().content()).isEqualTo("你是助手");
    }

    @Test
    @DisplayName("clearConversation 清除对话")
    void clearConversation() {
        manager.addMessage("conv1", AiMessage.user("hello"));
        assertThat(manager.exists("conv1")).isTrue();

        manager.clearConversation("conv1");
        assertThat(manager.exists("conv1")).isFalse();
        assertThat(manager.getMessages("conv1")).isEmpty();
    }

    @Test
    @DisplayName("exists 判断")
    void exists() {
        assertThat(manager.exists("conv1")).isFalse();
        manager.addMessage("conv1", AiMessage.user("hello"));
        assertThat(manager.exists("conv1")).isTrue();
    }

    @Test
    @DisplayName("多个对话互不影响")
    void multipleConversations() {
        manager.addMessage("conv1", AiMessage.user("A"));
        manager.addMessage("conv2", AiMessage.user("B"));

        assertThat(manager.getMessages("conv1")).hasSize(1);
        assertThat(manager.getMessages("conv1").getFirst().content()).isEqualTo("A");
        assertThat(manager.getMessages("conv2")).hasSize(1);
        assertThat(manager.getMessages("conv2").getFirst().content()).isEqualTo("B");
    }

    @Test
    @DisplayName("TTL 过期后对话消失")
    void ttlExpiration() {
        // ttl=1ms 的管理器
        InMemoryConversationManager shortTtl = new InMemoryConversationManager(10, 1);
        shortTtl.addMessage("conv1", AiMessage.user("hello"));

        // 等待过期
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        assertThat(shortTtl.exists("conv1")).isFalse();
        assertThat(shortTtl.getMessages("conv1")).isEmpty();
    }

    @Test
    @DisplayName("cleanup 清理过期对话")
    void cleanup() {
        InMemoryConversationManager shortTtl = new InMemoryConversationManager(10, 1);
        shortTtl.addMessage("conv1", AiMessage.user("hello"));
        shortTtl.addMessage("conv2", AiMessage.user("world"));

        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        shortTtl.cleanup();
        assertThat(shortTtl.getActiveCount()).isZero();
    }

    @Test
    @DisplayName("getActiveCount 统计活跃数")
    void getActiveCount() {
        manager.addMessage("conv1", AiMessage.user("a"));
        manager.addMessage("conv2", AiMessage.user("b"));
        manager.addMessage("conv3", AiMessage.user("c"));

        assertThat(manager.getActiveCount()).isEqualTo(3);

        manager.clearConversation("conv2");
        assertThat(manager.getActiveCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("返回的消息列表是不可变副本")
    void returnedListIsImmutableCopy() {
        manager.addMessage("conv1", AiMessage.user("hello"));
        List<AiMessage> messages = manager.getMessages("conv1");

        assertThatThrownBy(() -> messages.add(AiMessage.user("hack")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("线程安全 - 并发添加消息")
    void threadSafety() throws InterruptedException {
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            int idx = i;
            threads[i] = Thread.ofVirtual().start(() -> {
                manager.addMessage("conv1", AiMessage.user("msg-" + idx));
            });
        }
        for (Thread t : threads) {
            t.join();
        }

        List<AiMessage> messages = manager.getMessages("conv1");
        // maxHistory=5，所以最终只有5条
        assertThat(messages).hasSizeLessThanOrEqualTo(5);
    }
}
