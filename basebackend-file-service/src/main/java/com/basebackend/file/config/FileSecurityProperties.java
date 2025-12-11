package com.basebackend.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 文件安全配置属性
 * 
 * 支持通过 application.yml 配置文件类型白名单和黑名单
 * 
 * 配置示例:
 * <pre>
 * file:
 *   security:
 *     max-filename-length: 255
 *     dangerous-extensions:
 *       - exe
 *       - bat
 *       - cmd
 *     allowed-types:
 *       jpg:
 *         - image/jpeg
 *       png:
 *         - image/png
 * </pre>
 *
 * @author BaseBackend Security Team
 * @since 2025-12-07
 */
@Data
@Component
@ConfigurationProperties(prefix = "file.security")
public class FileSecurityProperties {

    /**
     * 文件名最大长度，默认255
     */
    private int maxFilenameLength = 255;

    /**
     * 危险的文件扩展名列表（绝对禁止上传）
     */
    private Set<String> dangerousExtensions = new HashSet<>(Arrays.asList(
        "exe", "bat", "cmd", "sh", "ps1", "vbs", "js", "jar", "war",
        "php", "asp", "aspx", "jsp", "cgi", "pl", "py", "rb",
        "dll", "so", "dylib", "bin", "com", "msi", "scr"
    ));

    /**
     * 允许的文件类型映射（扩展名 -> MIME类型列表）
     */
    private Map<String, Set<String>> allowedTypes = new HashMap<>();

    /**
     * 是否启用严格MIME类型验证，默认true
     */
    private boolean strictMimeValidation = true;

    /**
     * 是否启用文件内容检测，默认true
     */
    private boolean contentDetectionEnabled = true;

    /**
     * 初始化默认允许的文件类型
     */
    public FileSecurityProperties() {
        initDefaultAllowedTypes();
    }

    /**
     * 初始化默认的文件类型映射
     */
    private void initDefaultAllowedTypes() {
        // 图片类型
        allowedTypes.put("jpg", Set.of("image/jpeg"));
        allowedTypes.put("jpeg", Set.of("image/jpeg"));
        allowedTypes.put("png", Set.of("image/png"));
        allowedTypes.put("gif", Set.of("image/gif"));
        allowedTypes.put("bmp", Set.of("image/bmp", "image/x-ms-bmp"));
        allowedTypes.put("webp", Set.of("image/webp"));
        allowedTypes.put("svg", Set.of("image/svg+xml"));

        // 文档类型
        allowedTypes.put("pdf", Set.of("application/pdf"));
        allowedTypes.put("doc", Set.of("application/msword"));
        allowedTypes.put("docx", Set.of(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        allowedTypes.put("xls", Set.of("application/vnd.ms-excel"));
        allowedTypes.put("xlsx", Set.of(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        allowedTypes.put("ppt", Set.of("application/vnd.ms-powerpoint"));
        allowedTypes.put("pptx", Set.of(
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"));
        allowedTypes.put("txt", Set.of("text/plain"));
        allowedTypes.put("csv", Set.of("text/csv", "text/plain", "application/csv"));

        // 压缩文件
        allowedTypes.put("zip", Set.of("application/zip", "application/x-zip-compressed"));
        allowedTypes.put("rar", Set.of("application/x-rar-compressed", "application/vnd.rar"));
        allowedTypes.put("7z", Set.of("application/x-7z-compressed"));
        allowedTypes.put("tar", Set.of("application/x-tar"));
        allowedTypes.put("gz", Set.of("application/gzip", "application/x-gzip"));
    }

    /**
     * 检查扩展名是否为危险类型
     * 
     * @param extension 文件扩展名
     * @return 是否危险
     */
    public boolean isDangerousExtension(String extension) {
        if (extension == null) {
            return false;
        }
        return dangerousExtensions.contains(extension.toLowerCase());
    }

    /**
     * 获取指定扩展名允许的MIME类型
     * 
     * @param extension 文件扩展名
     * @return MIME类型集合，如果未配置返回null
     */
    public Set<String> getAllowedMimeTypes(String extension) {
        if (extension == null) {
            return null;
        }
        return allowedTypes.get(extension.toLowerCase());
    }

    /**
     * 添加自定义文件类型
     * 
     * @param extension 扩展名
     * @param mimeTypes MIME类型列表
     */
    public void addAllowedType(String extension, Set<String> mimeTypes) {
        if (extension != null && mimeTypes != null && !mimeTypes.isEmpty()) {
            allowedTypes.put(extension.toLowerCase(), mimeTypes);
        }
    }

    /**
     * 添加危险扩展名
     * 
     * @param extension 扩展名
     */
    public void addDangerousExtension(String extension) {
        if (extension != null && !extension.trim().isEmpty()) {
            dangerousExtensions.add(extension.toLowerCase());
        }
    }
}
