package com.basebackend.chat.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 消息已读状态实体 — 群聊已读回执
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_message_read")
public class ChatMessageRead extends BaseEntity {

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("conversation_id")
    private Long conversationId;

    @TableField("message_id")
    private Long messageId;

    @TableField("user_id")
    private Long userId;

    @TableField("read_time")
    private LocalDateTime readTime;
}
