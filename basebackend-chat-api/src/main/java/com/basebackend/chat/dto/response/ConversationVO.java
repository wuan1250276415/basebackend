package com.basebackend.chat.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表响应项
 */
@Data
@Builder
public class ConversationVO {

    private Long conversationId;
    private Integer type;
    private Long targetId;
    private String targetName;
    private String targetAvatar;
    private LastMessageVO lastMessage;
    private Integer unreadCount;
    private Boolean isPinned;
    private Boolean isMuted;
    private Integer memberCount;
    private String draft;
    private LocalDateTime updateTime;

    @Data
    @Builder
    public static class LastMessageVO {
        private Long messageId;
        private Integer type;
        private String content;
        private String senderName;
        private LocalDateTime sendTime;
    }
}
