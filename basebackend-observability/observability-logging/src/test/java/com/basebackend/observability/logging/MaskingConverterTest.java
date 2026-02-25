package com.basebackend.observability.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.basebackend.observability.logging.config.LoggingProperties;
import com.basebackend.observability.logging.masking.MaskingConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MaskingConverter 脱敏测试")
class MaskingConverterTest {

    @AfterEach
    void tearDown() {
        // 清理自定义规则，避免跨用例污染
        MaskingConverter.setConfiguredRules(Collections.emptyList());
    }

    @Test
    @DisplayName("应脱敏手机号")
    void shouldMaskPhoneNumber() {
        LoggingEvent event = new LoggingEvent();
        event.setLevel(Level.INFO);
        event.setMessage("User phone: 13800138000");

        String masked = new MaskingConverter().convert(event);

        assertThat(masked)
                .contains("138****8000")
                .doesNotContain("13800138000");
    }

    @Test
    @DisplayName("应脱敏身份证")
    void shouldMaskIdCard() {
        LoggingEvent event = new LoggingEvent();
        event.setLevel(Level.INFO);
        event.setMessage("ID card: 110101199001011234");

        String masked = new MaskingConverter().convert(event);

        assertThat(masked)
                .contains("110****99001011234")
                .doesNotContain("110101199001011234");
    }

    @Test
    @DisplayName("应脱敏邮箱地址")
    void shouldMaskEmail() {
        LoggingEvent event = new LoggingEvent();
        event.setLevel(Level.INFO);
        event.setMessage("Email: user@example.com");

        String masked = new MaskingConverter().convert(event);

        assertThat(masked)
                .contains("u***@example.com")
                .doesNotContain("user@example.com");
    }

    @Test
    @DisplayName("应脱敏密码字段")
    void shouldMaskPassword() {
        LoggingEvent event = new LoggingEvent();
        event.setLevel(Level.INFO);
        event.setMessage("Login with password=secret123");

        String masked = new MaskingConverter().convert(event);

        assertThat(masked)
                .contains("password=******")
                .doesNotContain("secret123");
    }

    @Test
    @DisplayName("应脱敏银行卡号")
    void shouldMaskBankCard() {
        LoggingEvent event = new LoggingEvent();
        event.setLevel(Level.INFO);
        event.setMessage("Card: 6222021234567890");

        String masked = new MaskingConverter().convert(event);

        assertThat(masked)
                .contains("622****234567890")
                .doesNotContain("6222021234567890");
    }

    @Test
    @DisplayName("应应用自定义 HIDE 规则")
    void shouldApplyCustomHideRule() {
        LoggingProperties.MaskingRule rule = new LoggingProperties.MaskingRule();
        // 修复：使用更精确的正则表达式来匹配字段和值
        rule.setFieldPattern("apiKey\\s*[:=]\\s*.*");
        rule.setStrategy("HIDE");
        rule.setPartialPattern("0-0");
        MaskingConverter.setConfiguredRules(List.of(rule));

        LoggingEvent event = new LoggingEvent();
        event.setLevel(Level.INFO);
        event.setMessage("apiKey=abc123xyz");

        String masked = new MaskingConverter().convert(event);

        assertThat(masked)
                .doesNotContain("abc123xyz");
    }

    @Test
    @DisplayName("应支持 HASH 脱敏策略")
    void shouldHashWhenHashStrategyConfigured() {
        LoggingProperties.MaskingRule rule = new LoggingProperties.MaskingRule();
        // 修复：使用更精确的正则表达式来匹配字段和值
        rule.setFieldPattern("token\\s*[:=]\\s*.*");
        rule.setStrategy("HASH");
        rule.setPartialPattern("0-0");
        MaskingConverter.setConfiguredRules(List.of(rule));

        LoggingEvent event = new LoggingEvent();
        event.setLevel(Level.INFO);
        event.setMessage("token=secret-token");

        String masked = new MaskingConverter().convert(event);

        assertThat(masked)
                .doesNotContain("secret-token");
    }

    @Test
    @DisplayName("无匹配规则时应保持原样")
    void shouldReturnOriginalWhenNoRuleMatches() {
        LoggingEvent event = new LoggingEvent();
        event.setLevel(Level.INFO);
        event.setMessage("normal message without sensitive data");

        String masked = new MaskingConverter().convert(event);

        assertThat(masked).isEqualTo("normal message without sensitive data");
    }

    @Test
    @DisplayName("应处理空或空字符串消息")
    void shouldHandleNullOrEmptyMessagesGracefully() {
        ILoggingEvent nullMessageEvent = mock(ILoggingEvent.class);
        when(nullMessageEvent.getFormattedMessage()).thenReturn(null);

        LoggingEvent emptyMessageEvent = new LoggingEvent();
        emptyMessageEvent.setLevel(Level.INFO);
        emptyMessageEvent.setMessage("");

        MaskingConverter converter = new MaskingConverter();

        assertThat(converter.convert(nullMessageEvent)).isNull();
        assertThat(converter.convert(emptyMessageEvent)).isEmpty();
    }
}
