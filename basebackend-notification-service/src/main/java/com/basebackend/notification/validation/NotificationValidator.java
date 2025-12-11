package com.basebackend.notification.validation;

import com.basebackend.common.enums.CommonErrorCode;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.notification.config.NotificationSecurityConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.regex.Pattern;

/**
 * 通知输入验证器
 * P0: 输入验证和XSS防护
 *
 * @author BaseBackend Team
 * @since 2025-12-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationValidator {

    private final NotificationSecurityConfig securityConfig;

    // 邮箱格式正则表达式
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    // 危险HTML标签正则（用于XSS检测）
    private static final Pattern XSS_PATTERN = Pattern.compile(
            "<script[^>]*>|</script>|javascript:|on\\w+\\s*=|<iframe|<object|<embed",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 验证邮箱格式
     */
    public void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(CommonErrorCode.PARAM_NOT_NULL, "邮箱地址不能为空");
        }
        if (email.length() > 255) {
            throw new BusinessException(CommonErrorCode.PARAM_FORMAT_ERROR, "邮箱地址长度不能超过255个字符");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException(CommonErrorCode.PARAM_FORMAT_ERROR, "邮箱格式无效");
        }
    }

    /**
     * 验证并清理邮件内容（XSS防护）
     */
    public String sanitizeEmailContent(String content) {
        if (content == null) {
            return null;
        }
        if (content.length() > securityConfig.getEmailContentMaxLength()) {
            throw new BusinessException(CommonErrorCode.PARAM_OUT_OF_RANGE, 
                    "邮件内容长度不能超过" + securityConfig.getEmailContentMaxLength() + "个字符");
        }
        if (!securityConfig.isXssFilterEnabled()) {
            return content;
        }
        if (XSS_PATTERN.matcher(content).find()) {
            log.warn("[安全] 检测到潜在XSS攻击内容，已进行转义处理");
            return HtmlUtils.htmlEscape(content);
        }
        return content;
    }

    /**
     * 验证并清理通知内容
     */
    public String sanitizeNotificationContent(String content) {
        if (content == null) {
            return null;
        }
        if (content.length() > securityConfig.getNotificationContentMaxLength()) {
            throw new BusinessException(CommonErrorCode.PARAM_OUT_OF_RANGE,
                    "通知内容长度不能超过" + securityConfig.getNotificationContentMaxLength() + "个字符");
        }
        if (!securityConfig.isXssFilterEnabled()) {
            return content;
        }
        if (XSS_PATTERN.matcher(content).find()) {
            log.warn("[安全] 检测到潜在XSS攻击内容，已进行转义处理");
            return HtmlUtils.htmlEscape(content);
        }
        return content;
    }

    /**
     * 验证通知标题
     */
    public String sanitizeTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new BusinessException(CommonErrorCode.PARAM_NOT_NULL, "通知标题不能为空");
        }
        if (title.length() > 200) {
            throw new BusinessException(CommonErrorCode.PARAM_OUT_OF_RANGE, "通知标题长度不能超过200个字符");
        }
        return HtmlUtils.htmlEscape(title.trim());
    }

    /**
     * 验证URL格式
     */
    public void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        if (url.length() > 2048) {
            throw new BusinessException(CommonErrorCode.PARAM_OUT_OF_RANGE, "URL长度不能超过2048个字符");
        }
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.startsWith("javascript:") || lowerUrl.startsWith("data:")) {
            throw new BusinessException(CommonErrorCode.PARAM_FORMAT_ERROR, "不允许的URL协议");
        }
    }
}
