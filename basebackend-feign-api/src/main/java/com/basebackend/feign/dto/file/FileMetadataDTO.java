package com.basebackend.feign.dto.file;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件元数据 DTO（用于 Feign 调用）
 *
 * @author Claude Code
 * @since 2025-01-07
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "文件元数据信息")
public record FileMetadataDTO(

        @Schema(description = "主键ID")
        Long id,

        @Schema(description = "文件唯一标识")
        String fileId,

        @Schema(description = "文件名")
        String fileName,

        @Schema(description = "原始文件名")
        String originalName,

        @Schema(description = "存储路径")
        String filePath,

        @Schema(description = "文件大小(字节)")
        Long fileSize,

        @Schema(description = "文件MIME类型")
        String contentType,

        @Schema(description = "文件扩展名")
        String fileExtension,

        @Schema(description = "文件MD5值")
        String md5,

        @Schema(description = "文件SHA256值")
        String sha256,

        @Schema(description = "存储类型")
        String storageType,

        @Schema(description = "存储桶名称")
        String bucketName,

        @Schema(description = "所属文件夹ID")
        Long folderId,

        @Schema(description = "文件夹路径")
        String folderPath,

        @Schema(description = "是否为文件夹")
        Boolean isFolder,

        @Schema(description = "所有者ID")
        Long ownerId,

        @Schema(description = "所有者名称")
        String ownerName,

        @Schema(description = "是否公开")
        Boolean isPublic,

        @Schema(description = "是否删除(软删除)")
        Boolean isDeleted,

        @Schema(description = "删除时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        LocalDateTime deletedAt,

        @Schema(description = "删除人ID")
        Long deletedBy,

        @Schema(description = "当前版本号")
        Integer version,

        @Schema(description = "最新版本ID")
        Long latestVersionId,

        @Schema(description = "下载次数")
        Integer downloadCount,

        @Schema(description = "浏览次数")
        Integer viewCount,

        @Schema(description = "缩略图路径")
        String thumbnailPath,

        @Schema(description = "标签(JSON数组)")
        String tags,

        @Schema(description = "文件描述")
        String description,

        @Schema(description = "扩展元数据")
        String metadata,

        @Schema(description = "创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        LocalDateTime createTime,

        @Schema(description = "更新时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        LocalDateTime updateTime

) implements Serializable {
}
