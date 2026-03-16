package com.basebackend.chat.service;

import com.basebackend.chat.dto.ChatMessageSearchDoc;
import com.basebackend.chat.entity.ChatMessage;
import com.basebackend.search.model.SearchResult;

import java.time.LocalDateTime;

/**
 * 消息全文搜索服务
 * <p>
 * 基于 basebackend-search 模块的 SearchClient，对聊天消息进行全文索引与检索。
 * 仅当 {@code basebackend.search.enabled=true} 时激活。
 */
public interface ChatMessageSearchService {

    /**
     * 异步索引单条消息
     *
     * @param message 消息实体
     */
    void indexMessage(ChatMessage message);

    /**
     * 删除消息索引
     *
     * @param messageId 消息ID
     */
    void deleteMessage(Long messageId);

    /**
     * 搜索消息
     *
     * @param tenantId       租户ID
     * @param currentUserId  当前用户ID（预留权限校验）
     * @param keyword        搜索关键词
     * @param conversationId 会话ID（可选，限定搜索范围）
     * @param startTime      起始时间（可选）
     * @param endTime        结束时间（可选）
     * @param page           页码（从1开始）
     * @param size           每页条数
     * @return 搜索结果
     */
    SearchResult<ChatMessageSearchDoc> searchMessages(Long tenantId, Long currentUserId,
                                                       String keyword, Long conversationId,
                                                       LocalDateTime startTime, LocalDateTime endTime,
                                                       int page, int size);

    /**
     * 初始化搜索索引（创建 mapping）
     */
    void initIndex();
}
