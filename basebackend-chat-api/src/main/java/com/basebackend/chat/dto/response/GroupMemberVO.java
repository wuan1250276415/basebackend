package com.basebackend.chat.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群成员响应体
 */
@Data
@Builder
public class GroupMemberVO {

    private Long userId;
    private String nickname;
    private String groupNickname;
    private String avatar;
    private Integer role;
    private String status;
    private Boolean isMuted;
    private LocalDateTime joinTime;
}
