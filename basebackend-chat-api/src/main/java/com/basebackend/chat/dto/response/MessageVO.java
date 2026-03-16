package com.basebackend.chat.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息响应体
 */
@Data
@Builder
public class MessageVO {

    private Long messageId;
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private Integer type;
    private String content;
    private Object extra;
    private MessageVO quoteMessage;
    private Object atUserIds;
    private String clientMsgId;
    private LocalDateTime sendTime;
    private Integer status;
}
