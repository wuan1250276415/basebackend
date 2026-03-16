package com.basebackend.notification.validation;

import com.basebackend.common.enums.CommonErrorCode;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.notification.config.NotificationSecurityConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.regex.Pattern;

/**
 * 通知输入验证器
 * <p>
 * XSS 防护策略：
 * <ul>
 *   <li>邮件内容（HTML 格式）：使用 jsoup {@link Safelist#relaxed()} 白名单清理，
 *       保留安全的排版标签，剔除脚本/内联事件/危险属性</li>
 *   <li>通知内容（纯文本）：使用 {@link Safelist#none()} 剥离全部 HTML 标签</li>
 *   <li>标题：转义所有 HTML 特殊字符</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @since 2025-12-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationValidator {

    private final NotificationSecurityConfig securityConfig;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"
    );

    /**
     * jsoup 白名单：允许常见的排版标签和属性，禁止脚本/事件处理器/危险协议。
     * relaxed() 基础上追加安全的样式属性。
     */
    private static final Safelist EMAIL_SAFELIST = Safelist.relaxed()
            .addAttributes(":all", "style")
            .addProtocols("a", "href", "http", "https", "mailto");

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
     * 清理邮件 HTML 内容（白名单过滤）。
     * <p>
     * 使用 jsoup 白名单而非黑名单，可正确处理 CSS 注入、属性注入等绕过场景。
     *
     * @param content 原始 HTML 内容
     * @return 经白名单过滤后的安全 HTML
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
        String cleaned = Jsoup.clean(content, EMAIL_SAFELIST);
        if (!cleaned.equals(content)) {
            log.warn("[安全] 邮件内容经白名单过滤，已移除潜在危险标签或属性");
        }
        return cleaned;
    }

    /**
     * 清理通知内容（剥离全部 HTML 标签，保留纯文本）。
     *
     * @param content 原始内容
     * @return 纯文本内容
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
        String cleaned = Jsoup.clean(content, Safelist.none());
        if (!cleaned.equals(content)) {
            log.warn("[安全] 通知内容包含HTML标签，已全部剥离");
        }
        return cleaned;
    }

    /**
     * 验证并转义通知标题（不允许任何 HTML）
     *
     * @param title 原始标题
     * @return 转义后的安全标题
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
     * 验证 URL 格式（禁止 javascript: 和 data: 协议）
     *
     * @param url 待验证的 URL，允许为空
     */
    public void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        if (url.length() > 2048) {
            throw new BusinessException(CommonErrorCode.PARAM_OUT_OF_RANGE, "URL长度不能超过2048个字符");
        }
        String lowerUrl = url.toLowerCase().stripLeading();
        if (lowerUrl.startsWith("javascript:") || lowerUrl.startsWith("data:")) {
            throw new BusinessException(CommonErrorCode.PARAM_FORMAT_ERROR, "不允许的URL协议");
        }
    }
}
