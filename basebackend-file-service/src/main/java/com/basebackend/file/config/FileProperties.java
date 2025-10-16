package com.basebackend.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "file")
public class FileProperties {

    /**
     * 文件上传路径
     */
    private String uploadPath = "./uploads";

    /**
     * 文件访问前缀
     */
    private String accessPrefix = "/files";

    /**
     * 允许的文件类型
     */
    private String[] allowedTypes = {
            "jpg", "jpeg", "png", "gif", "bmp",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "zip", "rar"
    };

    /**
     * 最大文件大小（字节）默认10MB
     */
    private long maxSize = 10 * 1024 * 1024;
}
