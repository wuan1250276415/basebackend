package com.basebackend.album.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 相册实体
 *
 * @author BearTeam
 */
@Data
@TableName("album_album")
public class Album {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 相册名称 */
    private String name;

    /** 描述 */
    private String description;

    /** 封面照片ID */
    private Long coverPhotoId;

    /** 所属家庭ID（NULL=个人相册） */
    private Long familyId;

    /** 创建者ID */
    private Long ownerId;

    /** 类型: 0=普通 1=时间轴自动 2=智能 */
    private Integer type;

    /** 可见性: 0=私有 1=家庭 2=链接公开 */
    private Integer visibility;

    /** 照片数量 */
    private Integer photoCount;

    /** 排序 */
    private Integer sortOrder;

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
