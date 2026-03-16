package com.basebackend.album.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 家庭成员实体
 *
 * @author BearTeam
 */
@Data
@TableName("album_family_member")
public class FamilyMember {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 家庭ID */
    private Long familyId;

    /** 用户ID */
    private Long userId;

    /** 家庭内昵称 */
    private String nickname;

    /** 角色: 0=成员 1=管理员 2=创建者 */
    private Integer role;

    /** 加入时间 */
    private LocalDateTime joinTime;

    /** 租户ID */
    private Long tenantId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除: 0=未删除 1=已删除 */
    @TableLogic
    private Integer deleted;
}
