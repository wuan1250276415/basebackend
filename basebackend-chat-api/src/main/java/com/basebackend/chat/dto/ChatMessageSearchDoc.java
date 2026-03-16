package com.basebackend.chat.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息搜索文档 — 映射到 Elasticsearch 索引字段
 */
@Data
public class ChatMessageSearchDoc {

    /** 消息ID */
    private Long messageId;

    /** 租户ID */
    private Long tenantId;

    /** 会话ID */
    private Long conversationId;

    /** 发送者ID */
    private Long senderId;

    /** 发送者昵称 */
    private String senderName;

    /** 消息类型: 1-文本 2-图片 3-文件 等 */
    private Integer type;

    /** 消息正文（全文检索主字段） */
    private String content;

    /** 消息状态: 0-发送中 1-已发送 2-已撤回 */
    private Integer status;

    /** 发送时间 */
    private LocalDateTime sendTime;
}
