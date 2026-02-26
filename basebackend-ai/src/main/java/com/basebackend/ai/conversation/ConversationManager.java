package com.basebackend.ai.conversation;

import com.basebackend.ai.client.AiMessage;

import java.util.List;

/**
 * 对话上下文管理接口
 * <p>
 * 管理多轮对话的消息历史，支持不同的存储后端（内存 / Redis）。
 */
public interface ConversationManager {

    /**
     * 添加消息到对话
     *
     * @param conversationId 对话 ID
     * @param message        消息
     */
    void addMessage(String conversationId, AiMessage message);

    /**
     * 获取对话历史
     *
     * @param conversationId 对话 ID
     * @return 消息列表
     */
    List<AiMessage> getMessages(String conversationId);

    /**
     * 获取最近 N 条消息
     *
     * @param conversationId 对话 ID
     * @param limit          最大条数
     * @return 消息列表
     */
    List<AiMessage> getRecentMessages(String conversationId, int limit);

    /**
     * 清除对话历史
     *
     * @param conversationId 对话 ID
     */
    void clearConversation(String conversationId);

    /**
     * 对话是否存在
     *
     * @param conversationId 对话 ID
     * @return 是否存在
     */
    boolean exists(String conversationId);
}
