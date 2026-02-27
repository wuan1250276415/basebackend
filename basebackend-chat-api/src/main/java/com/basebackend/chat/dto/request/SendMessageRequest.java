package com.basebackend.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 发送消息请求
 */
@Data
public class SendMessageRequest {

    @NotNull(message = "会话ID不能为空")
    private Long conversationId;

    @NotNull(message = "消息类型不能为空")
    private Integer type;

    private String content;

    /** 客户端消息ID，用于去重 */
    private String clientMsgId;

    /** 引用消息ID */
    private Long quoteMessageId;

    /** @用户ID列表 */
    private List<String> atUserIds;

    /** 扩展信息 JSON */
    private String extra;
}
