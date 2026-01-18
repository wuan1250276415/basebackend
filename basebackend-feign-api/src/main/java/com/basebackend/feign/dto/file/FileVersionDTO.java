package com.basebackend.feign.dto.file;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件版本 DTO（用于 Feign 调用）
 *
 * @author Claude Code
 * @since 2025-01-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "文件版本信息")
public class FileVersionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "版本主键ID")
    private Long id;

    @Schema(description = "文件ID")
    private String fileId;

    @Schema(description = "版本号")
    private Integer versionNumber;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "文件大小")
    private Long fileSize;

    @Schema(description = "文件MD5值")
    private String md5;

    @Schema(description = "变更说明")
    private String changeDescription;

    @Schema(description = "创建人ID")
    private Long createdBy;

    @Schema(description = "创建人名称")
    private String createdByName;

    @Schema(description = "是否当前版本")
    private Boolean isCurrent;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
