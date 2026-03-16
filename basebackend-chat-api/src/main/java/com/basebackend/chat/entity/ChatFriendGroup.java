package com.basebackend.chat.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 好友分组实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_friend_group")
public class ChatFriendGroup extends BaseEntity {

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("user_id")
    private Long userId;

    @TableField("name")
    private String name;

    @TableField("sort_order")
    private Integer sortOrder;

    /** 是否默认分组: 0-否 1-是 */
    @TableField("is_default")
    private Integer isDefault;
}
