package com.basebackend.logging.cost;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.spi.FilterReply;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AdaptiveSamplerFilter 自适应采样过滤器测试")
class AdaptiveSamplerFilterTest {

    private LogVolumeTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new LogVolumeTracker(60);
    }

    @Test
    @DisplayName("未超阈值时应返回 NEUTRAL")
    void decide_neutral_whenBelowThreshold() {
        AdaptiveSamplerFilter filter = new AdaptiveSamplerFilter(
                tracker, 1000, 10485760L, 0.1, true);
        filter.start();

        Logger logger = (Logger) LoggerFactory.getLogger("com.example.test");
        FilterReply reply = filter.decide(null, logger, Level.INFO, "hello world", null, null);
        assertThat(reply).isEqualTo(FilterReply.NEUTRAL);
        filter.stop();
    }

    @Test
    @DisplayName("WARN 级别应豁免采样（exemptHighSeverity=true）")
    void decide_neutral_whenWarnAndExempt() {
        // 阈值设为 1，确保超阈值
        AdaptiveSamplerFilter filter = new AdaptiveSamplerFilter(
                tracker, 1, 1, 0.0, true);
        filter.start();

        Logger logger = (Logger) LoggerFactory.getLogger("com.example.test.warn");
        // 先记录一条触发超阈值
        filter.decide(null, logger, Level.WARN, "first", null, null);
        // WARN 级别应不受采样影响
        FilterReply reply = filter.decide(null, logger, Level.WARN, "second warn", null, null);
        assertThat(reply).isEqualTo(FilterReply.NEUTRAL);
        filter.stop();
    }

    @Test
    @DisplayName("ERROR 级别应豁免采样")
    void decide_neutral_whenErrorAndExempt() {
        AdaptiveSamplerFilter filter = new AdaptiveSamplerFilter(
                tracker, 1, 1, 0.0, true);
        filter.start();

        Logger logger = (Logger) LoggerFactory.getLogger("com.example.test.error");
        filter.decide(null, logger, Level.ERROR, "first", null, null);
        FilterReply reply = filter.decide(null, logger, Level.ERROR, "error msg", null, null);
        assertThat(reply).isEqualTo(FilterReply.NEUTRAL);
        filter.stop();
    }

    @Test
    @DisplayName("超阈值且采样率为 0 时应全部 DENY（DEBUG 级别）")
    void decide_deny_whenOverThresholdAndZeroSamplingRate() {
        AdaptiveSamplerFilter filter = new AdaptiveSamplerFilter(
                tracker, 1, 1, 0.0, true);
        filter.start();

        Logger logger = (Logger) LoggerFactory.getLogger("com.example.test.deny");
        // 先记录超过阈值
        filter.decide(null, logger, Level.DEBUG, "warmup", null, null);
        filter.decide(null, logger, Level.DEBUG, "warmup2", null, null);

        // 后续 DEBUG 级别应被 DENY
        FilterReply reply = filter.decide(null, logger, Level.DEBUG, "should be denied", null, null);
        assertThat(reply).isEqualTo(FilterReply.DENY);
        filter.stop();
    }

    @Test
    @DisplayName("format 为 null 时应返回 NEUTRAL")
    void decide_neutral_whenFormatIsNull() {
        AdaptiveSamplerFilter filter = new AdaptiveSamplerFilter(
                tracker, 1000, 10485760L, 0.1, true);
        filter.start();

        Logger logger = (Logger) LoggerFactory.getLogger("com.example.test.null");
        FilterReply reply = filter.decide(null, logger, Level.INFO, null, null, null);
        assertThat(reply).isEqualTo(FilterReply.NEUTRAL);
        filter.stop();
    }

    @Test
    @DisplayName("extractServiceKey 应提取前 3 段包名")
    void extractServiceKey_shouldExtractThreeSegments() {
        AdaptiveSamplerFilter filter = new AdaptiveSamplerFilter(
                tracker, 1000, 10485760L, 0.1, true);
        Logger logger = (Logger) LoggerFactory.getLogger("com.basebackend.logging.cost.Test");
        assertThat(filter.extractServiceKey(logger)).isEqualTo("com.basebackend.logging");
    }

    @Test
    @DisplayName("extractServiceKey 对短名称应返回全名")
    void extractServiceKey_shouldReturnFullName_whenShort() {
        AdaptiveSamplerFilter filter = new AdaptiveSamplerFilter(
                tracker, 1000, 10485760L, 0.1, true);
        Logger logger = (Logger) LoggerFactory.getLogger("com.test");
        assertThat(filter.extractServiceKey(logger)).isEqualTo("com.test");
    }
}
