package com.basebackend.chat.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 群组信息实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_group")
public class ChatGroup extends BaseEntity {

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("name")
    private String name;

    @TableField("avatar")
    private String avatar;

    @TableField("description")
    private String description;

    @TableField("owner_id")
    private Long ownerId;

    @TableField("conversation_id")
    private Long conversationId;

    @TableField("max_members")
    private Integer maxMembers;

    @TableField("member_count")
    private Integer memberCount;

    /** 全体禁言: 0-否 1-是 */
    @TableField("is_muted")
    private Integer isMuted;

    /** 入群方式: 0-自由加入 1-需审批 2-仅邀请 */
    @TableField("join_mode")
    private Integer joinMode;

    /** 邀请需确认: 0-直接入群 1-被邀请人确认 */
    @TableField("invite_confirm")
    private Integer inviteConfirm;

    /** 群状态: 0-已解散 1-正常 2-封禁 */
    @TableField("status")
    private Integer status;
}
