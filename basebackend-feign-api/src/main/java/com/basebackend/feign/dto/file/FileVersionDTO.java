package com.basebackend.feign.dto.file;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件版本 DTO（用于 Feign 调用）
 *
 * @author Claude Code
 * @since 2025-01-07
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "文件版本信息")
public record FileVersionDTO(

        @Schema(description = "版本主键ID")
        Long id,

        @Schema(description = "文件ID")
        String fileId,

        @Schema(description = "版本号")
        Integer versionNumber,

        @Schema(description = "文件路径")
        String filePath,

        @Schema(description = "文件大小")
        Long fileSize,

        @Schema(description = "文件MD5值")
        String md5,

        @Schema(description = "变更说明")
        String changeDescription,

        @Schema(description = "创建人ID")
        Long createdBy,

        @Schema(description = "创建人名称")
        String createdByName,

        @Schema(description = "是否当前版本")
        Boolean isCurrent,

        @Schema(description = "创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        LocalDateTime createTime,

        @Schema(description = "更新时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        LocalDateTime updateTime

) implements Serializable {
}
