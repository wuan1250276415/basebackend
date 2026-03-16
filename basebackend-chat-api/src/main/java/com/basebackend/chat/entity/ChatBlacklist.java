package com.basebackend.chat.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 黑名单实体 — 单向拉黑
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_blacklist")
public class ChatBlacklist extends BaseEntity {

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("user_id")
    private Long userId;

    @TableField("blocked_id")
    private Long blockedId;

    @TableField("reason")
    private String reason;
}
