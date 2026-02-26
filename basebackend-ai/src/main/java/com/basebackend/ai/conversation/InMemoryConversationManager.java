package com.basebackend.ai.conversation;

import com.basebackend.ai.client.AiMessage;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存的对话管理器
 * <p>
 * 线程安全，支持 TTL 过期和最大历史条数限制。
 */
public class InMemoryConversationManager implements ConversationManager {

    private final Map<String, ConversationEntry> conversations = new ConcurrentHashMap<>();
    private final int maxHistory;
    private final long ttlMillis;

    public InMemoryConversationManager(int maxHistory, long ttlMillis) {
        this.maxHistory = maxHistory;
        this.ttlMillis = ttlMillis;
    }

    @Override
    public void addMessage(String conversationId, AiMessage message) {
        conversations.compute(conversationId, (id, entry) -> {
            if (entry == null || entry.isExpired(ttlMillis)) {
                entry = new ConversationEntry();
            }
            entry.addMessage(message, maxHistory);
            return entry;
        });
    }

    @Override
    public List<AiMessage> getMessages(String conversationId) {
        ConversationEntry entry = conversations.get(conversationId);
        if (entry == null || entry.isExpired(ttlMillis)) {
            return List.of();
        }
        entry.touch();
        return entry.getMessages();
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
        conversations.remove(conversationId);
    }

    @Override
    public boolean exists(String conversationId) {
        ConversationEntry entry = conversations.get(conversationId);
        return entry != null && !entry.isExpired(ttlMillis);
    }

    /** 清理过期对话（可由调度器定期调用） */
    public void cleanup() {
        conversations.entrySet().removeIf(e -> e.getValue().isExpired(ttlMillis));
    }

    /** 当前活跃对话数 */
    public int getActiveCount() {
        return (int) conversations.values().stream()
                .filter(e -> !e.isExpired(ttlMillis))
                .count();
    }

    // --- 内部数据结构 ---

    private static class ConversationEntry {
        private final List<AiMessage> messages = Collections.synchronizedList(new ArrayList<>());
        private volatile long lastAccessTime = System.currentTimeMillis();

        void addMessage(AiMessage message, int maxHistory) {
            messages.add(message);
            // 超出最大历史时，移除最旧的消息（保留 system 消息）
            while (messages.size() > maxHistory) {
                // 跳过第一条 system 消息
                if (messages.size() > 1 && AiMessage.ROLE_SYSTEM.equals(messages.getFirst().role())) {
                    messages.remove(1);
                } else {
                    messages.removeFirst();
                }
            }
            lastAccessTime = System.currentTimeMillis();
        }

        List<AiMessage> getMessages() {
            return List.copyOf(messages);
        }

        void touch() {
            lastAccessTime = System.currentTimeMillis();
        }

        boolean isExpired(long ttlMillis) {
            if (ttlMillis <= 0) return false;
            return System.currentTimeMillis() - lastAccessTime > ttlMillis;
        }
    }
}
