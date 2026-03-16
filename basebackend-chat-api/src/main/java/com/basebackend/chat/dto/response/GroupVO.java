package com.basebackend.chat.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群信息响应体
 */
@Data
@Builder
public class GroupVO {

    private Long groupId;
    private String name;
    private String avatar;
    private String description;
    private Long ownerId;
    private String ownerName;
    private Long conversationId;
    private Integer maxMembers;
    private Integer memberCount;
    private Boolean isMuted;
    private Integer joinMode;
    private Boolean inviteConfirm;
    private Integer myRole;
    private LocalDateTime createTime;
}
