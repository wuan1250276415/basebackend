package com.basebackend.chat.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 消息实体 — 按 conversation_id 写入, ID 即为消息序号
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_message")
public class ChatMessage extends BaseEntity {

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("conversation_id")
    private Long conversationId;

    @TableField("sender_id")
    private Long senderId;

    @TableField("sender_name")
    private String senderName;

    @TableField("sender_avatar")
    private String senderAvatar;

    /** 消息类型: 1-文本 2-图片 3-文件 4-语音 5-视频 6-位置 7-名片 8-表情 9-系统通知 10-撤回 11-合并转发 */
    @TableField("type")
    private Integer type;

    @TableField("content")
    private String content;

    @TableField("reply_to_msg_id")
    private Long replyToMsgId;

    @TableField("forward_from_msg_id")
    private Long forwardFromMsgId;

    @TableField("forward_from_conversation_id")
    private Long forwardFromConversationId;

    @TableField("extra")
    private String extra;

    @TableField("quote_message_id")
    private Long quoteMessageId;

    @TableField("at_user_ids")
    private String atUserIds;

    @TableField("client_msg_id")
    private String clientMsgId;

    @TableField("send_time")
    private LocalDateTime sendTime;

    /** 状态: 0-发送中 1-已发送 2-已撤回 3-审核中 4-已屏蔽 */
    @TableField("status")
    private Integer status;

    @TableField("revoke_time")
    private LocalDateTime revokeTime;
}
