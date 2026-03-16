package com.basebackend.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 处理好友申请请求
 */
@Data
public class HandleFriendRequestDTO {

    /** accept 或 reject */
    @NotNull(message = "操作类型不能为空")
    private String action;

    /** 好友备注名 */
    private String remark;

    /** 好友分组ID */
    private Long groupId;
}
