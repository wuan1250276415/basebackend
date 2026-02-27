package com.basebackend.chat.service.impl;

import cn.hutool.core.util.StrUtil;
import com.basebackend.chat.dto.ChatMessageSearchDoc;
import com.basebackend.chat.entity.ChatMessage;
import com.basebackend.chat.enums.MessageStatus;
import com.basebackend.chat.service.ChatMessageSearchService;
import com.basebackend.search.client.SearchClient;
import com.basebackend.search.model.IndexDefinition;
import com.basebackend.search.model.SearchResult;
import com.basebackend.search.query.SearchQuery;
import com.basebackend.search.query.SearchQuery.Condition;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 消息搜索服务实现 — 基于 SearchClient 进行 ES 索引和检索
 * <p>
 * 仅当 {@code basebackend.search.enabled=true} 时注册为 Bean。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "basebackend.search", name = "enabled", havingValue = "true")
public class ChatMessageSearchServiceImpl implements ChatMessageSearchService {

    private final SearchClient searchClient;

    /** 索引名称 */
    private static final String INDEX_NAME = "chat_message";

    /** 索引定义 */
    private static final IndexDefinition INDEX_DEFINITION = IndexDefinition.builder(INDEX_NAME)
            .textField("content", "ik_max_word")
            .textField("senderName", "ik_max_word")
            .longField("messageId")
            .longField("tenantId")
            .longField("conversationId")
            .longField("senderId")
            .integerField("type")
            .integerField("status")
            .dateField("sendTime")
            .shards(3)
            .replicas(1)
            .build();

    /**
     * 应用启动时自动创建索引（如果不存在）
     */
    @PostConstruct
    @Override
    public void initIndex() {
        try {
            if (!searchClient.indexExists(INDEX_NAME)) {
                boolean created = searchClient.createIndex(INDEX_DEFINITION);
                log.info("聊天消息搜索索引创建{}: {}", created ? "成功" : "失败", INDEX_NAME);
            } else {
                log.info("聊天消息搜索索引已存在: {}", INDEX_NAME);
            }
        } catch (Exception e) {
            log.warn("聊天消息搜索索引初始化失败，搜索功能可能不可用: {}", e.getMessage());
        }
    }

    @Async
    @Override
    public void indexMessage(ChatMessage message) {
        try {
            ChatMessageSearchDoc doc = toSearchDoc(message);
            searchClient.index(INDEX_NAME, String.valueOf(message.getId()), doc);
        } catch (Exception e) {
            log.warn("消息索引失败, messageId={}: {}", message.getId(), e.getMessage());
        }
    }

    @Override
    public void deleteMessage(Long messageId) {
        try {
            searchClient.delete(INDEX_NAME, String.valueOf(messageId));
        } catch (Exception e) {
            log.warn("消息索引删除失败, messageId={}: {}", messageId, e.getMessage());
        }
    }

    @Override
    public SearchResult<ChatMessageSearchDoc> searchMessages(Long tenantId, Long currentUserId,
                                                              String keyword, Long conversationId,
                                                              LocalDateTime startTime, LocalDateTime endTime,
                                                              int page, int size) {
        var builder = SearchQuery.builder(INDEX_NAME)
                .filter(Condition.term("tenantId", tenantId))
                .filter(Condition.term("status", MessageStatus.SENT.getCode()));

        // 关键词全文检索
        if (StrUtil.isNotBlank(keyword)) {
            builder.must(Condition.match("content", keyword));
            builder.highlight("content");
        }

        // 限定会话范围
        if (conversationId != null) {
            builder.filter(Condition.term("conversationId", conversationId));
        }

        // 时间范围过滤
        if (startTime != null || endTime != null) {
            builder.filter(Condition.range("sendTime", startTime, endTime));
        }

        builder.sortBy("sendTime", SearchQuery.SortOrder.DESC)
               .page(page, Math.min(size, 50));

        return searchClient.search(builder.build(), ChatMessageSearchDoc.class);
    }

    // ======================== 内部工具方法 ========================

    /**
     * 消息实体转搜索文档
     */
    private ChatMessageSearchDoc toSearchDoc(ChatMessage message) {
        ChatMessageSearchDoc doc = new ChatMessageSearchDoc();
        doc.setMessageId(message.getId());
        doc.setTenantId(message.getTenantId());
        doc.setConversationId(message.getConversationId());
        doc.setSenderId(message.getSenderId());
        doc.setSenderName(message.getSenderName());
        doc.setType(message.getType());
        doc.setContent(message.getContent());
        doc.setStatus(message.getStatus());
        doc.setSendTime(message.getSendTime());
        return doc;
    }
}
