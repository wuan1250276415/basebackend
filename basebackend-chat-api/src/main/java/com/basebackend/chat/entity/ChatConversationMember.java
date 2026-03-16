package com.basebackend.chat.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 会话成员实体 — 每个用户在每个会话中的独立设置
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_conversation_member")
public class ChatConversationMember extends BaseEntity {

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("conversation_id")
    private Long conversationId;

    @TableField("user_id")
    private Long userId;

    @TableField("unread_count")
    private Integer unreadCount;

    @TableField("last_read_message_id")
    private Long lastReadMessageId;

    @TableField("last_read_time")
    private LocalDateTime lastReadTime;

    /** 是否置顶: 0-否 1-是 */
    @TableField("is_pinned")
    private Integer isPinned;

    /** 是否免打扰: 0-否 1-是 */
    @TableField("is_muted")
    private Integer isMuted;

    /** 是否隐藏: 0-否 1-是 */
    @TableField("is_hidden")
    private Integer isHidden;

    @TableField("draft")
    private String draft;

    @TableField("join_time")
    private LocalDateTime joinTime;
}
