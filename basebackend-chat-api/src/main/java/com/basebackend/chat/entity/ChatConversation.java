package com.basebackend.chat.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 会话实体 — 每个私聊/群聊对应一条会话记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_conversation")
public class ChatConversation extends BaseEntity {

    @TableField("tenant_id")
    private Long tenantId;

    /** 会话类型: 1-私聊 2-群聊 */
    @TableField("type")
    private Integer type;

    /** 目标ID: 私聊=对方用户ID 群聊=群ID */
    @TableField("target_id")
    private Long targetId;

    @TableField("last_message_id")
    private Long lastMessageId;

    @TableField("last_message_time")
    private LocalDateTime lastMessageTime;

    @TableField("last_message_preview")
    private String lastMessagePreview;

    @TableField("last_sender_id")
    private Long lastSenderId;

    @TableField("member_count")
    private Integer memberCount;
}
