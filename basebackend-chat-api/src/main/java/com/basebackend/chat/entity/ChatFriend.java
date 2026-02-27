package com.basebackend.chat.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 好友关系实体 — 单向存储，A加B产生两条记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_friend")
public class ChatFriend extends BaseEntity {

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("user_id")
    private Long userId;

    @TableField("friend_id")
    private Long friendId;

    @TableField("remark")
    private String remark;

    @TableField("group_id")
    private Long groupId;

    /** 添加来源: 0-搜索 1-群聊 2-名片 3-扫码 */
    @TableField("source")
    private Integer source;

    /** 状态: 0-待验证 1-正常 2-已删除 */
    @TableField("status")
    private Integer status;

    @TableField("extra")
    private String extra;
}
