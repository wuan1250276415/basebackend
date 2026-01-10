package com.basebackend.feign.dto.file;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件元数据 DTO（用于 Feign 调用）
 *
 * @author Claude Code
 * @since 2025-01-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "文件元数据信息")
public class FileMetadataDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "文件唯一标识")
    private String fileId;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "原始文件名")
    private String originalName;

    @Schema(description = "存储路径")
    private String filePath;

    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    @Schema(description = "文件MIME类型")
    private String contentType;

    @Schema(description = "文件扩展名")
    private String fileExtension;

    @Schema(description = "文件MD5值")
    private String md5;

    @Schema(description = "文件SHA256值")
    private String sha256;

    @Schema(description = "存储类型")
    private String storageType;

    @Schema(description = "存储桶名称")
    private String bucketName;

    @Schema(description = "所属文件夹ID")
    private Long folderId;

    @Schema(description = "文件夹路径")
    private String folderPath;

    @Schema(description = "是否为文件夹")
    private Boolean isFolder;

    @Schema(description = "所有者ID")
    private Long ownerId;

    @Schema(description = "所有者名称")
    private String ownerName;

    @Schema(description = "是否公开")
    private Boolean isPublic;

    @Schema(description = "是否删除(软删除)")
    private Boolean isDeleted;

    @Schema(description = "删除时间")
    private LocalDateTime deletedAt;

    @Schema(description = "删除人ID")
    private Long deletedBy;

    @Schema(description = "当前版本号")
    private Integer version;

    @Schema(description = "最新版本ID")
    private Long latestVersionId;

    @Schema(description = "下载次数")
    private Integer downloadCount;

    @Schema(description = "浏览次数")
    private Integer viewCount;

    @Schema(description = "缩略图路径")
    private String thumbnailPath;

    @Schema(description = "标签(JSON数组)")
    private String tags;

    @Schema(description = "文件描述")
    private String description;

    @Schema(description = "扩展元数据")
    private String metadata;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
