package com.basebackend.album.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 照片/视频实体
 *
 * @author BearTeam
 */
@Data
@TableName("album_photo")
public class Photo {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属相册ID */
    private Long albumId;

    /** 上传者ID */
    private Long ownerId;

    /** 原始文件名 */
    private String fileName;

    /** 存储路径 */
    private String filePath;

    /** 缩略图路径 */
    private String thumbnailPath;

    /** 文件大小(字节) */
    private Long fileSize;

    /** MIME类型 */
    private String mimeType;

    /** 宽度(px) */
    private Integer width;

    /** 高度(px) */
    private Integer height;

    /** 媒体类型: 0=照片 1=视频 */
    private Integer mediaType;

    /** 视频时长(秒) */
    private Integer duration;

    /** 拍摄时间(EXIF) */
    private LocalDateTime takenAt;

    /** 拍摄地点 */
    private String location;

    /** 纬度 */
    private BigDecimal latitude;

    /** 经度 */
    private BigDecimal longitude;

    /** 描述 */
    private String description;

    /** 标签(逗号分隔) */
    private String tags;

    /** EXIF信息(JSON) */
    private String exifData;

    /** 点赞数 */
    private Integer likeCount;

    /** 评论数 */
    private Integer commentCount;

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
