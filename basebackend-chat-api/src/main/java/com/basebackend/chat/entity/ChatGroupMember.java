package com.basebackend.chat.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 群成员实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_group_member")
public class ChatGroupMember extends BaseEntity {

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("group_id")
    private Long groupId;

    @TableField("user_id")
    private Long userId;

    @TableField("nickname")
    private String nickname;

    /** 角色: 0-普通成员 1-管理员 2-群主 */
    @TableField("role")
    private Integer role;

    @TableField("inviter_id")
    private Long inviterId;

    /** 个人禁言: 0-否 1-是 */
    @TableField("is_muted")
    private Integer isMuted;

    @TableField("mute_expire_time")
    private LocalDateTime muteExpireTime;

    @TableField("join_time")
    private LocalDateTime joinTime;

    @TableField("last_active_time")
    private LocalDateTime lastActiveTime;
}
