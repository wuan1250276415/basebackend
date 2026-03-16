package com.basebackend.album.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 照片点赞实体（无逻辑删除，直接物理删除）
 *
 * @author BearTeam
 */
@Data
@TableName("album_like")
public class PhotoLike {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 照片ID */
    private Long photoId;

    /** 用户ID */
    private Long userId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
