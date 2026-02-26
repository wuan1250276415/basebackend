package com.basebackend.api.model.file;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件版本信息 DTO
 *
 * @author Claude Code
 * @since 2025-11-08
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "文件版本信息")
public record FileVersionDTO(
        Long id,
        String fileId,
        Integer versionNumber,
        String filePath,
        Long fileSize,
        String md5,
        String changeDescription,
        Long createdBy,
        String createdByName,
        Boolean isCurrent,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createTime,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updateTime
) implements Serializable {
}
