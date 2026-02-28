package com.basebackend.album.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 照片评论实体
 *
 * @author BearTeam
 */
@Data
@TableName("album_comment")
public class PhotoComment {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 照片ID */
    private Long photoId;

    /** 评论者ID */
    private Long userId;

    /** 评论内容 */
    private String content;

    /** 父评论ID(回复) */
    private Long parentId;

    /** 租户ID */
    private Long tenantId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除: 0=未删除 1=已删除 */
    @TableLogic
    private Integer deleted;
}
