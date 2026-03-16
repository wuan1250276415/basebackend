package com.basebackend.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建/打开会话请求
 */
@Data
public class CreateConversationRequest {

    /** 会话类型: 1-私聊 2-群聊 */
    @NotNull(message = "会话类型不能为空")
    private Integer type;

    /** 目标ID: 私聊=对方用户ID 群聊=群ID */
    @NotNull(message = "目标ID不能为空")
    private Long targetId;
}
