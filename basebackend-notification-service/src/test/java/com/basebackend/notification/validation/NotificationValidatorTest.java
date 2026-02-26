package com.basebackend.notification.validation;

import com.basebackend.common.exception.BusinessException;
import com.basebackend.notification.config.NotificationSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * NotificationValidator 单元测试
 */
class NotificationValidatorTest {

    private NotificationValidator validator;

    @BeforeEach
    void setUp() {
        var config = new NotificationSecurityConfig();
        config.setXssFilterEnabled(true);
        config.setEmailContentMaxLength(50000);
        config.setNotificationContentMaxLength(10000);
        validator = new NotificationValidator(config);
    }

    // ========== validateEmail ==========

    @Nested
    @DisplayName("邮箱验证")
    class ValidateEmail {

        @Test
        @DisplayName("合法邮箱通过验证")
        void shouldPassForValidEmail() {
            validator.validateEmail("test@example.com");
            validator.validateEmail("user.name+tag@domain.co");
        }

        @Test
        @DisplayName("null 邮箱抛出异常")
        void shouldThrowForNullEmail() {
            assertThatThrownBy(() -> validator.validateEmail(null))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("空邮箱抛出异常")
        void shouldThrowForBlankEmail() {
            assertThatThrownBy(() -> validator.validateEmail(""))
                    .isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> validator.validateEmail("   "))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("非法格式邮箱抛出异常")
        void shouldThrowForInvalidFormat() {
            assertThatThrownBy(() -> validator.validateEmail("not-an-email"))
                    .isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> validator.validateEmail("@no-user.com"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("超长邮箱抛出异常")
        void shouldThrowForTooLongEmail() {
            String longEmail = "a".repeat(250) + "@b.com";
            assertThatThrownBy(() -> validator.validateEmail(longEmail))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ========== sanitizeEmailContent ==========

    @Nested
    @DisplayName("邮件内容清理")
    class SanitizeEmailContent {

        @Test
        @DisplayName("正常内容原样返回")
        void shouldReturnNormalContent() {
            String content = "这是一封普通邮件内容";
            assertThat(validator.sanitizeEmailContent(content)).isEqualTo(content);
        }

        @Test
        @DisplayName("null 内容返回 null")
        void shouldReturnNullForNull() {
            assertThat(validator.sanitizeEmailContent(null)).isNull();
        }

        @Test
        @DisplayName("XSS 内容被转义")
        void shouldEscapeXssContent() {
            String xss = "<script>alert('xss')</script>";
            String result = validator.sanitizeEmailContent(xss);
            assertThat(result).doesNotContain("<script>");
        }

        @Test
        @DisplayName("超长内容抛出异常")
        void shouldThrowForTooLongContent() {
            String longContent = "a".repeat(50001);
            assertThatThrownBy(() -> validator.sanitizeEmailContent(longContent))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("XSS 过滤关闭时不转义")
        void shouldNotEscapeWhenXssFilterDisabled() {
            var config = new NotificationSecurityConfig();
            config.setXssFilterEnabled(false);
            config.setEmailContentMaxLength(50000);
            var noFilterValidator = new NotificationValidator(config);

            String xss = "<script>alert('xss')</script>";
            assertThat(noFilterValidator.sanitizeEmailContent(xss)).isEqualTo(xss);
        }
    }

    // ========== sanitizeNotificationContent ==========

    @Nested
    @DisplayName("通知内容清理")
    class SanitizeNotificationContent {

        @Test
        @DisplayName("正常内容原样返回")
        void shouldReturnNormalContent() {
            String content = "系统通知：您有新的待办事项";
            assertThat(validator.sanitizeNotificationContent(content)).isEqualTo(content);
        }

        @Test
        @DisplayName("XSS 内容被转义")
        void shouldEscapeXss() {
            String xss = "<iframe src='evil.com'>";
            String result = validator.sanitizeNotificationContent(xss);
            assertThat(result).doesNotContain("<iframe");
        }

        @Test
        @DisplayName("超长内容抛出异常")
        void shouldThrowForTooLongContent() {
            String longContent = "a".repeat(10001);
            assertThatThrownBy(() -> validator.sanitizeNotificationContent(longContent))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ========== sanitizeTitle ==========

    @Nested
    @DisplayName("标题清理")
    class SanitizeTitle {

        @Test
        @DisplayName("正常标题通过并 trim")
        void shouldTrimAndReturn() {
            assertThat(validator.sanitizeTitle("  系统通知  ")).isEqualTo("系统通知");
        }

        @Test
        @DisplayName("null 标题抛出异常")
        void shouldThrowForNull() {
            assertThatThrownBy(() -> validator.sanitizeTitle(null))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("空标题抛出异常")
        void shouldThrowForBlank() {
            assertThatThrownBy(() -> validator.sanitizeTitle("   "))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("超长标题抛出异常")
        void shouldThrowForTooLongTitle() {
            assertThatThrownBy(() -> validator.sanitizeTitle("a".repeat(201)))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("HTML 字符被转义")
        void shouldEscapeHtml() {
            String result = validator.sanitizeTitle("标题<b>加粗</b>");
            assertThat(result).doesNotContain("<b>");
        }
    }

    // ========== validateUrl ==========

    @Nested
    @DisplayName("URL 验证")
    class ValidateUrl {

        @Test
        @DisplayName("合法 URL 通过")
        void shouldPassForValidUrl() {
            validator.validateUrl("https://example.com");
            validator.validateUrl("http://localhost:8080/api");
        }

        @Test
        @DisplayName("null/空 URL 通过（可选字段）")
        void shouldPassForNullOrBlank() {
            validator.validateUrl(null);
            validator.validateUrl("");
            validator.validateUrl("   ");
        }

        @Test
        @DisplayName("javascript: 协议拒绝")
        void shouldRejectJavascriptProtocol() {
            assertThatThrownBy(() -> validator.validateUrl("javascript:alert(1)"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("data: 协议拒绝")
        void shouldRejectDataProtocol() {
            assertThatThrownBy(() -> validator.validateUrl("data:text/html,<h1>hack</h1>"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("超长 URL 拒绝")
        void shouldRejectTooLongUrl() {
            assertThatThrownBy(() -> validator.validateUrl("https://example.com/" + "a".repeat(2048)))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
