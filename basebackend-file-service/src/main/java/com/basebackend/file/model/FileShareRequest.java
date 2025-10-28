package com.basebackend.file.model;

import com.basebackend.common.exception.BusinessException;
import lombok.Data;
import org.springframework.util.StringUtils;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 创建分享请求
 */
@Data
public class FileShareRequest {

    @NotBlank(message = "文件ID不能为空")
    private String fileId;

    private String sharePassword;

    /**
     * 过期时间（ISO 8601字符串）
     */
    private String expireTime;

    private Integer downloadLimit;

    private Boolean allowDownload;

    private Boolean allowPreview;

    /**
     * 解析过期时间
     */
    public LocalDateTime resolveExpireTime() {
        if (!StringUtils.hasText(expireTime)) {
            return null;
        }
        try {
            return LocalDateTime.parse(expireTime, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new BusinessException("分享过期时间格式不正确");
        }
    }
}
