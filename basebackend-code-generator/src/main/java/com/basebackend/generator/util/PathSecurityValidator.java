package com.basebackend.generator.util;

import lombok.extern.slf4j.Slf4j;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * 路径安全验证器
 * 防止目录遍历攻击和非法路径注入
 */
@Slf4j
public class PathSecurityValidator {

    /**
     * 禁止的路径模式
     * 包含目录遍历攻击常见模式
     */
    private static final Pattern FORBIDDEN_PATTERNS = Pattern.compile(
            "(?i)(" +
                    "\\.\\." + // 父目录遍历
                    "|~" + // 用户目录
                    "|\\$\\{" + // 变量注入（未替换的模板变量）
                    "|\\x00" + // 空字符注入
                    "|%00" + // URL编码空字符
                    "|/etc/" + // Linux系统目录
                    "|/var/" + // Linux系统目录
                    "|/tmp/" + // 临时目录
                    "|\\\\windows\\\\" + // Windows系统目录
                    "|\\\\system32\\\\" + // Windows系统目录
                    "|\\\\Users\\\\[^\\\\]+\\\\.+" + // Windows用户敏感目录
                    ")");

    /**
     * 允许的文件扩展名白名单
     */
    private static final Pattern ALLOWED_EXTENSIONS = Pattern.compile(
            "(?i)\\.(java|xml|yml|yaml|properties|html|css|js|ts|tsx|jsx|vue|json|md|sql|ftl|vm)$");

    /**
     * 允许的路径字符
     */
    private static final Pattern VALID_PATH_CHARS = Pattern.compile(
            "^[a-zA-Z0-9_\\-./\\\\]+$");

    /**
     * 验证生成的文件路径是否安全
     *
     * @param path 待验证的路径
     * @return 验证结果
     * @throws IllegalArgumentException 如果路径不安全
     */
    public static PathValidationResult validatePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return PathValidationResult.invalid("路径不能为空");
        }

        String normalizedPath = path.trim();

        // 1. 检查禁止的模式
        if (FORBIDDEN_PATTERNS.matcher(normalizedPath).find()) {
            log.warn("检测到潜在的路径遍历攻击: {}", normalizedPath);
            return PathValidationResult.invalid("路径包含禁止的模式");
        }

        // 2. 检查路径字符合法性
        if (!VALID_PATH_CHARS.matcher(normalizedPath).matches()) {
            log.warn("路径包含非法字符: {}", normalizedPath);
            return PathValidationResult.invalid("路径包含非法字符");
        }

        // 3. 检查未替换的模板变量
        if (normalizedPath.contains("${")) {
            log.warn("路径包含未替换的模板变量: {}", normalizedPath);
            return PathValidationResult.invalid("路径包含未替换的模板变量");
        }

        // 4. 检查文件扩展名（如果有）
        if (normalizedPath.contains(".")) {
            if (!ALLOWED_EXTENSIONS.matcher(normalizedPath).find()) {
                log.warn("文件扩展名不在白名单中: {}", normalizedPath);
                return PathValidationResult.invalid("文件扩展名不被允许");
            }
        }

        // 5. 尝试解析为有效路径
        try {
            Path parsedPath = Paths.get(normalizedPath);
            // 规范化路径并检查是否仍然有效
            String normalized = parsedPath.normalize().toString();

            // 确保规范化后的路径没有逃逸到上级目录
            if (normalized.startsWith("..") || normalized.contains("..")) {
                log.warn("路径规范化后包含父目录引用: {} -> {}", path, normalized);
                return PathValidationResult.invalid("路径尝试访问父目录");
            }

            return PathValidationResult.valid(normalized);
        } catch (InvalidPathException e) {
            log.warn("无效的路径格式: {}", normalizedPath, e);
            return PathValidationResult.invalid("无效的路径格式: " + e.getMessage());
        }
    }

    /**
     * 快速验证路径是否安全
     *
     * @param path 待验证的路径
     * @return true表示安全，false表示不安全
     */
    public static boolean isSecurePath(String path) {
        return validatePath(path).isValid();
    }

    /**
     * 验证并返回安全的路径，如果不安全则抛出异常
     *
     * @param path 待验证的路径
     * @return 规范化后的安全路径
     * @throws SecurityException 如果路径不安全
     */
    public static String requireSecurePath(String path) {
        PathValidationResult result = validatePath(path);
        if (!result.isValid()) {
            throw new SecurityException("不安全的文件路径: " + result.getErrorMessage());
        }
        return result.getNormalizedPath();
    }

    /**
     * 路径验证结果
     */
    public static class PathValidationResult {
        private final boolean valid;
        private final String normalizedPath;
        private final String errorMessage;

        private PathValidationResult(boolean valid, String normalizedPath, String errorMessage) {
            this.valid = valid;
            this.normalizedPath = normalizedPath;
            this.errorMessage = errorMessage;
        }

        public static PathValidationResult valid(String normalizedPath) {
            return new PathValidationResult(true, normalizedPath, null);
        }

        public static PathValidationResult invalid(String errorMessage) {
            return new PathValidationResult(false, null, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getNormalizedPath() {
            return normalizedPath;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
