package com.basebackend.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发送好友申请请求
 */
@Data
public class FriendRequestDTO {

    @NotNull(message = "目标用户ID不能为空")
    private Long toUserId;

    /** 验证消息 */
    private String message;

    /** 来源: 0-搜索 1-群聊 2-名片 3-扫码 */
    private Integer source;
}
