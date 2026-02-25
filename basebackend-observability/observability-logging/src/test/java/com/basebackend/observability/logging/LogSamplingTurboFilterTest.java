package com.basebackend.observability.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.spi.FilterReply;
import com.basebackend.observability.logging.config.LoggingProperties;
import com.basebackend.observability.logging.sampling.LogSamplingTurboFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("LogSamplingTurboFilter 采样测试")
class LogSamplingTurboFilterTest {

    @Test
    @DisplayName("100% 采样应放行")
    void shouldPassWhenRateIsOne() {
        LogSamplingTurboFilter filter = new LogSamplingTurboFilter();
        filter.setEnabled(true);
        filter.setRules(List.of(rule("INFO", 1.0, null)));

        FilterReply reply = filter.decide(null, logger("test"), Level.INFO, "msg", null, null);
        assertThat(reply).isEqualTo(FilterReply.NEUTRAL);
    }

    @Test
    @DisplayName("0% 采样应拒绝")
    void shouldDenyWhenRateIsZero() {
        LogSamplingTurboFilter filter = new LogSamplingTurboFilter();
        filter.setEnabled(true);
        filter.setRules(List.of(rule("INFO", 0.0, null)));

        FilterReply reply = filter.decide(null, logger("test"), Level.INFO, "msg", null, null);
        assertThat(reply).isEqualTo(FilterReply.DENY);
    }

    @Test
    @DisplayName("50% 采样应出现放行与拒绝（多次采样）")
    void shouldSometimesPassWhenRateIsHalf() {
        LogSamplingTurboFilter filter = new LogSamplingTurboFilter();
        filter.setEnabled(true);
        filter.setRules(List.of(rule("INFO", 0.5, null)));

        boolean hasPass = false;
        boolean hasDeny = false;
        for (int i = 0; i < 200; i++) {
            FilterReply reply = filter.decide(null, logger("test"), Level.INFO, "msg", null, null);
            hasPass |= reply == FilterReply.NEUTRAL;
            hasDeny |= reply == FilterReply.DENY;
            if (hasPass && hasDeny) {
                break;
            }
        }

        assertThat(hasPass).isTrue();
        assertThat(hasDeny).isTrue();
    }

    @Test
    @DisplayName("包名匹配时应按包规则采样")
    void shouldApplyPackageRuleWhenLoggerMatches() {
        LogSamplingTurboFilter filter = new LogSamplingTurboFilter();
        filter.setEnabled(true);
        filter.setRules(List.of(rule("INFO", 1.0, "com.basebackend.user")));

        FilterReply reply = filter.decide(null, logger("com.basebackend.user.UserService"), Level.INFO, "msg", null, null);
        assertThat(reply).isEqualTo(FilterReply.NEUTRAL);
    }

    @Test
    @DisplayName("包名不匹配应回退到级别规则")
    void shouldFallbackToLevelRuleWhenPackageNotMatch() {
        LogSamplingTurboFilter filter = new LogSamplingTurboFilter();
        filter.setEnabled(true);
        List<LoggingProperties.SamplingRule> rules = new ArrayList<>();
        rules.add(rule("INFO", 0.0, "com.basebackend.other")); // 不匹配的包
        rules.add(rule("INFO", 1.0, null)); // 级别规则
        filter.setRules(rules);

        FilterReply reply = filter.decide(null, logger("com.basebackend.user.UserService"), Level.INFO, "msg", null, null);
        assertThat(reply).isEqualTo(FilterReply.NEUTRAL);
    }

    @Test
    @DisplayName("禁用时应全部放行")
    void shouldPassAllWhenDisabled() {
        LogSamplingTurboFilter filter = new LogSamplingTurboFilter();
        filter.setEnabled(false);

        FilterReply reply = filter.decide(null, logger("test"), Level.INFO, "msg", null, null);
        assertThat(reply).isEqualTo(FilterReply.NEUTRAL);
    }

    @Test
    @DisplayName("无规则时默认放行")
    void shouldPassWhenNoRulesConfigured() {
        LogSamplingTurboFilter filter = new LogSamplingTurboFilter();
        filter.setEnabled(true);
        filter.setRules(List.of()); // 空规则

        FilterReply reply = filter.decide(null, logger("test"), Level.INFO, "msg", null, null);
        assertThat(reply).isEqualTo(FilterReply.NEUTRAL);
    }

    @Test
    @DisplayName("无效级别规则应被忽略并回退默认放行")
    void shouldIgnoreInvalidLevelRule() {
        LogSamplingTurboFilter filter = new LogSamplingTurboFilter();
        filter.setEnabled(true);
        LoggingProperties.SamplingRule invalid = rule("INVALID_LEVEL", 0.0, null);
        filter.setRules(List.of(invalid));

        FilterReply reply = filter.decide(null, logger("test"), Level.INFO, "msg", null, null);
        assertThat(reply).isEqualTo(FilterReply.NEUTRAL);
    }

    @Test
    @DisplayName("更新规则后应生效新规则")
    void shouldApplyUpdatedRules() {
        LogSamplingTurboFilter filter = new LogSamplingTurboFilter();
        filter.setEnabled(true);
        filter.setRules(List.of(rule("INFO", 1.0, null)));

        // 初始为放行
        FilterReply initial = filter.decide(null, logger("test"), Level.INFO, "msg", null, null);
        assertThat(initial).isEqualTo(FilterReply.NEUTRAL);

        // 更新为拒绝
        filter.setRules(List.of(rule("INFO", 0.0, null)));
        FilterReply afterUpdate = filter.decide(null, logger("test"), Level.INFO, "msg", null, null);
        assertThat(afterUpdate).isEqualTo(FilterReply.DENY);
    }

    private LoggingProperties.SamplingRule rule(String level, double rate, String packageName) {
        LoggingProperties.SamplingRule rule = new LoggingProperties.SamplingRule();
        rule.setLevel(level);
        rule.setRate(rate);
        rule.setPackageName(packageName);
        return rule;
    }

    private Logger logger(String name) {
        return (Logger) LoggerFactory.getLogger(name);
    }
}
