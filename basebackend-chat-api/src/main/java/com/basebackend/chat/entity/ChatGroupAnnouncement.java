package com.basebackend.chat.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 群公告实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_group_announcement")
public class ChatGroupAnnouncement extends BaseEntity {

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("group_id")
    private Long groupId;

    @TableField("publisher_id")
    private Long publisherId;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    /** 是否置顶: 0-否 1-是 */
    @TableField("is_pinned")
    private Integer isPinned;

    /** 是否需要确认: 0-否 1-是 */
    @TableField("is_confirmed")
    private Integer isConfirmed;

    @TableField("confirm_count")
    private Integer confirmCount;

    @TableField("publish_time")
    private LocalDateTime publishTime;
}
