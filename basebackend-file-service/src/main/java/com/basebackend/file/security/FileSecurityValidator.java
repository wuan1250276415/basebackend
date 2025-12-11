package com.basebackend.file.security;

import com.basebackend.common.exception.BusinessException;
import com.basebackend.file.config.FileSecurityProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 文件安全验证器
 * 
 * 提供以下安全功能：
 * 1. 文件类型验证（扩展名 + MIME类型 + 魔数检测）
 * 2. 路径遍历攻击防护
 * 3. 文件名安全验证
 * 
 * 支持通过 FileSecurityProperties 进行外部化配置
 *
 * @author BaseBackend Security Team
 * @since 2025-12-07
 */
@Slf4j
@Component
public class FileSecurityValidator {

    private final Tika tika;
    private final FileSecurityProperties properties;
    
    /**
     * 文件名验证正则（预编译，避免ReDoS风险）
     * 允许：字母、数字、下划线、中划线、点、中文
     */
    private static final Pattern SAFE_FILENAME_PATTERN = 
        Pattern.compile("^[a-zA-Z0-9_\\-\\.\\u4e00-\\u9fa5]{1,255}$");
    
    /**
     * 默认构造函数（使用默认配置）
     */
    public FileSecurityValidator() {
        this.tika = new Tika();
        this.properties = new FileSecurityProperties();
    }
    
    /**
     * 推荐的构造函数（支持依赖注入）
     * 
     * @param properties 文件安全配置属性
     */
    public FileSecurityValidator(FileSecurityProperties properties) {
        this.tika = new Tika();
        this.properties = properties != null ? properties : new FileSecurityProperties();
    }
    
    /**
     * 完整构造函数（便于测试）
     * 
     * @param tika Tika实例
     * @param properties 文件安全配置属性
     */
    public FileSecurityValidator(Tika tika, FileSecurityProperties properties) {
        this.tika = tika != null ? tika : new Tika();
        this.properties = properties != null ? properties : new FileSecurityProperties();
    }

    /**
     * 完整的文件安全验证
     *
     * @param file 上传的文件
     * @param allowedExtensions 允许的扩展名列表
     * @throws BusinessException 如果验证失败
     */
    public void validateFile(MultipartFile file, String[] allowedExtensions) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.paramError("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw BusinessException.paramError("文件名不能为空");
        }

        // 1. 验证文件名安全性
        validateFileName(originalFilename);

        // 2. 获取并验证扩展名
        String extension = getExtension(originalFilename).toLowerCase();
        validateExtension(extension, allowedExtensions);

        // 3. 验证MIME类型（基于文件内容）
        validateMimeType(file, extension);

        log.debug("文件安全验证通过: filename={}, extension={}", originalFilename, extension);
    }

    /**
     * 验证文件名安全性
     */
    public void validateFileName(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw BusinessException.paramError("文件名不能为空");
        }
        
        // 检查文件名长度（防止过长文件名攻击）
        int maxLength = properties.getMaxFilenameLength();
        if (filename.length() > maxLength) {
            log.warn("文件名过长: length={}", filename.length());
            throw BusinessException.paramError("文件名长度不能超过" + maxLength + "个字符");
        }

        // 检查路径遍历攻击
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            log.warn("检测到路径遍历攻击尝试: filename_hash={}", sanitizeForLog(filename));
            throw BusinessException.paramError("文件名包含非法字符");
        }

        // 检查空字节注入
        if (filename.contains("\0")) {
            log.warn("检测到空字节注入攻击: filename_hash={}", sanitizeForLog(filename));
            throw BusinessException.paramError("文件名包含非法字符");
        }

        // 使用预编译的正则表达式检查（避免ReDoS）
        if (!SAFE_FILENAME_PATTERN.matcher(filename).matches()) {
            log.warn("文件名包含特殊字符: filename_hash={}", sanitizeForLog(filename));
            throw BusinessException.paramError("文件名只能包含字母、数字、下划线、中划线、点和中文");
        }
    }
    
    /**
     * 对日志输出进行安全处理（防止日志注入）
     * 
     * @param input 原始输入
     * @return 安全的日志字符串
     */
    private String sanitizeForLog(String input) {
        if (input == null) {
            return "null";
        }
        // 返回哈希值而非原始内容，防止日志注入
        return "hash_" + Integer.toHexString(input.hashCode());
    }

    /**
     * 验证扩展名
     */
    private void validateExtension(String extension, String[] allowedExtensions) {
        // 检查是否为危险扩展名（使用配置）
        if (properties.isDangerousExtension(extension)) {
            log.warn("检测到危险文件类型: {}", extension);
            throw BusinessException.fileTypeNotSupported("不允许上传此类型的文件: " + extension);
        }

        // 检查是否在允许列表中
        Set<String> allowed = new HashSet<>();
        for (String ext : allowedExtensions) {
            allowed.add(ext.toLowerCase());
        }

        if (!allowed.contains(extension)) {
            throw BusinessException.fileTypeNotSupported("不支持的文件类型: " + extension);
        }
    }

    /**
     * 验证MIME类型（基于文件内容检测）
     */
    private void validateMimeType(MultipartFile file, String extension) {
        // 检查是否启用内容检测
        if (!properties.isContentDetectionEnabled()) {
            log.debug("文件内容检测已禁用，跳过MIME验证");
            return;
        }
        
        try (InputStream is = file.getInputStream()) {
            String detectedMimeType = tika.detect(is, file.getOriginalFilename());

            // 获取该扩展名允许的MIME类型（使用配置）
            Set<String> allowedMimeTypes = properties.getAllowedMimeTypes(extension);
            if (allowedMimeTypes == null) {
                // 如果没有配置，根据严格模式决定行为
                if (properties.isStrictMimeValidation()) {
                    log.warn("扩展名 {} 未配置MIME类型映射，严格模式下拒绝", extension);
                    throw BusinessException.fileTypeNotSupported("不支持的文件类型: " + extension);
                }
                log.debug("扩展名 {} 未配置MIME类型映射，跳过MIME验证", extension);
                return;
            }

            // 验证检测到的MIME类型是否匹配
            if (!allowedMimeTypes.contains(detectedMimeType)) {
                log.warn("MIME类型不匹配: extension={}, declared={}, detected={}",
                    extension, file.getContentType(), detectedMimeType);
                throw BusinessException.fileTypeNotSupported(
                    "文件内容与扩展名不匹配，可能存在安全风险");
            }

        } catch (IOException e) {
            log.error("MIME类型检测失败", e);
            throw BusinessException.fileUploadFailed("文件验证失败: " + e.getMessage());
        }
    }

    /**
     * 验证存储路径安全性（防止路径遍历）
     *
     * @param basePath 基础路径
     * @param userPath 用户提供的路径
     * @return 安全的规范化路径
     */
    public String validateAndNormalizePath(String basePath, String userPath) {
        if (userPath == null || userPath.trim().isEmpty()) {
            throw BusinessException.paramError("路径不能为空");
        }

        // 规范化路径
        Path base = Paths.get(basePath).toAbsolutePath().normalize();
        Path resolved = base.resolve(userPath).normalize();

        // 确保解析后的路径仍在基础路径下
        if (!resolved.startsWith(base)) {
            log.warn("检测到路径遍历攻击: basePath_hash={}, userPath_hash={}",
                sanitizeForLog(basePath), sanitizeForLog(userPath));
            throw BusinessException.forbidden("非法的文件路径");
        }

        return resolved.toString();
    }

    /**
     * 清理文件路径（移除危险字符）
     */
    public String sanitizePath(String path) {
        if (path == null) {
            return null;
        }
        // 移除路径遍历字符
        return path.replace("..", "")
                   .replace("//", "/")
                   .replace("\\\\", "/")
                   .replace("\\", "/")
                   .replaceAll("[<>:\"|?*]", "");
    }

    /**
     * 获取文件扩展名
     */
    private String getExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }

    /**
     * 检查扩展名是否在允许列表中
     */
    public boolean isAllowedExtension(String extension, String[] allowedExtensions) {
        if (extension == null || allowedExtensions == null) {
            return false;
        }
        String ext = extension.toLowerCase();
        for (String allowed : allowedExtensions) {
            if (allowed.toLowerCase().equals(ext)) {
                return true;
            }
        }
        return false;
    }
}
