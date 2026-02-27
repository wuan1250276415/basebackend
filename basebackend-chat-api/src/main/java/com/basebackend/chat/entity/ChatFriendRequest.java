package com.basebackend.chat.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 好友申请实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_friend_request")
public class ChatFriendRequest extends BaseEntity {

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("from_user_id")
    private Long fromUserId;

    @TableField("to_user_id")
    private Long toUserId;

    @TableField("message")
    private String message;

    /** 来源: 0-搜索 1-群聊 2-名片 3-扫码 */
    @TableField("source")
    private Integer source;

    /** 状态: 0-待处理 1-已同意 2-已拒绝 3-已过期 */
    @TableField("status")
    private Integer status;

    @TableField("handle_time")
    private LocalDateTime handleTime;

    @TableField("expire_time")
    private LocalDateTime expireTime;
}
