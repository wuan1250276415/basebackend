package com.basebackend.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件元数据实体
 */
@Data
@TableName("file_metadata")
public class FileMetadata {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文件唯一标识
     */
    private String fileId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 存储路径
     */
    private String filePath;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 文件MIME类型
     */
    private String contentType;

    /**
     * 文件扩展名
     */
    private String fileExtension;

    /**
     * 文件MD5值
     */
    private String md5;

    /**
     * 文件SHA256值
     */
    private String sha256;

    /**
     * 存储类型
     */
    private String storageType;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 所属文件夹ID
     */
    private Long folderId;

    /**
     * 文件夹路径
     */
    private String folderPath;

    /**
     * 是否为文件夹
     */
    private Boolean isFolder;

    /**
     * 所有者ID
     */
    private Long ownerId;

    /**
     * 所有者名称
     */
    private String ownerName;

    /**
     * 是否公开
     */
    private Boolean isPublic;

    /**
     * 是否删除(软删除)
     */
    @TableLogic
    private Boolean isDeleted;

    /**
     * 删除时间
     */
    private LocalDateTime deletedAt;

    /**
     * 删除人ID
     */
    private Long deletedBy;

    /**
     * 当前版本号
     */
    private Integer version;

    /**
     * 最新版本ID
     */
    private Long latestVersionId;

    /**
     * 下载次数
     */
    private Integer downloadCount;

    /**
     * 浏览次数
     */
    private Integer viewCount;

    /**
     * 缩略图路径
     */
    private String thumbnailPath;

    /**
     * 标签(JSON数组)
     */
    private String tags;

    /**
     * 文件描述
     */
    private String description;

    /**
     * 扩展元数据
     */
    private String metadata;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
