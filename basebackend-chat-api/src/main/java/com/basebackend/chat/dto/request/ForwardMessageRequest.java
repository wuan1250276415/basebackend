package com.basebackend.chat.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 转发消息请求
 */
@Data
public class ForwardMessageRequest {

    @NotEmpty(message = "消息ID列表不能为空")
    private List<Long> messageIds;

    @NotEmpty(message = "目标会话列表不能为空")
    private List<Long> targetConversationIds;

    /** 转发类型: single-逐条转发 merge-合并转发 */
    @NotNull(message = "转发类型不能为空")
    private String forwardType;

    /** 合并转发标题 */
    private String title;
}
