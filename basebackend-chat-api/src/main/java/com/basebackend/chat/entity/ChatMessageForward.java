package com.basebackend.chat.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 合并转发消息实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_message_forward")
public class ChatMessageForward extends BaseEntity {

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("forward_id")
    private String forwardId;

    @TableField("original_msg_id")
    private Long originalMsgId;

    @TableField("original_conversation_id")
    private Long originalConversationId;

    @TableField("original_sender_id")
    private Long originalSenderId;

    @TableField("original_sender_name")
    private String originalSenderName;

    @TableField("original_content")
    private String originalContent;

    @TableField("original_content_type")
    private Integer originalContentType;

    @TableField("original_send_time")
    private LocalDateTime originalSendTime;

    @TableField("seq_no")
    private Integer seqNo;
}
