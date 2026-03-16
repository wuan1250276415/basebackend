package com.basebackend.album.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 家庭组实体
 *
 * @author BearTeam
 */
@Data
@TableName("album_family_group")
public class FamilyGroup {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 家庭名称 */
    private String name;

    /** 描述 */
    private String description;

    /** 家庭头像URL */
    private String avatar;

    /** 创建者用户ID */
    private Long ownerId;

    /** 邀请码 */
    private String inviteCode;

    /** 最大成员数 */
    private Integer maxMembers;

    /** 最大存储空间(GB) */
    private Integer maxStorageGb;

    /** 已用存储(字节) */
    private Long usedStorageBytes;

    /** 租户ID */
    private Long tenantId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除: 0=未删除 1=已删除 */
    @TableLogic
    private Integer deleted;
}
