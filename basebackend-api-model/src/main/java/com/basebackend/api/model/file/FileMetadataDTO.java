package com.basebackend.api.model.file;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件元数据信息 DTO
 *
 * @author Claude Code
 * @since 2025-11-08
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "文件元数据信息")
public record FileMetadataDTO(
        Long id,
        String fileId,
        String fileName,
        String originalName,
        String filePath,
        Long fileSize,
        String contentType,
        String fileExtension,
        String md5,
        String sha256,
        String storageType,
        String bucketName,
        Long folderId,
        String folderPath,
        Boolean isFolder,
        Long ownerId,
        String ownerName,
        Boolean isPublic,
        Boolean isDeleted,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime deletedAt,
        Long deletedBy,
        Integer version,
        Long latestVersionId,
        Integer downloadCount,
        Integer viewCount,
        String thumbnailPath,
        String tags,
        String description,
        String metadata,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createTime,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updateTime
) implements Serializable {
}
