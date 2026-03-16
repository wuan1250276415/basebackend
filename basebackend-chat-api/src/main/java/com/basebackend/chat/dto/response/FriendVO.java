package com.basebackend.chat.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 好友信息响应体
 */
@Data
@Builder
public class FriendVO {

    private Long userId;
    private String nickname;
    private String remark;
    private String avatar;
    private String status;
    private Long groupId;
    private String groupName;
}
