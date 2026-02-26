package com.basebackend.logging.loglevel;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.spi.FilterReply;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConditionalLogLevelFilter 条件日志级别过滤器测试")
class ConditionalLogLevelFilterTest {

    private ConditionalLogLevelFilter filter;

    @BeforeEach
    void setUp() {
        filter = new ConditionalLogLevelFilter();
        filter.start();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        filter.stop();
    }

    @Test
    @DisplayName("无条件注册时应返回 NEUTRAL")
    void decide_neutral_whenNoConditions() {
        MDC.put("traceId", "abc123");
        FilterReply reply = filter.decide(null, null, Level.DEBUG, "msg", null, null);
        assertThat(reply).isEqualTo(FilterReply.NEUTRAL);
    }

    @Test
    @DisplayName("traceId 匹配且级别满足时应返回 ACCEPT")
    void decide_accept_whenTraceIdMatches() {
        filter.addTraceId("trace-001", Level.DEBUG, 60);
        MDC.put("traceId", "trace-001");

        FilterReply reply = filter.decide(null, null, Level.DEBUG, "msg", null, null);
        assertThat(reply).isEqualTo(FilterReply.ACCEPT);
    }

    @Test
    @DisplayName("userId 匹配且级别满足时应返回 ACCEPT")
    void decide_accept_whenUserIdMatches() {
        filter.addUserId("user-42", Level.DEBUG, 60);
        MDC.put("userId", "user-42");

        FilterReply reply = filter.decide(null, null, Level.INFO, "msg", null, null);
        assertThat(reply).isEqualTo(FilterReply.ACCEPT);
    }

    @Test
    @DisplayName("traceId 不匹配时应返回 NEUTRAL")
    void decide_neutral_whenTraceIdNotMatched() {
        filter.addTraceId("trace-001", Level.DEBUG, 60);
        MDC.put("traceId", "trace-999");

        FilterReply reply = filter.decide(null, null, Level.DEBUG, "msg", null, null);
        assertThat(reply).isEqualTo(FilterReply.NEUTRAL);
    }

    @Test
    @DisplayName("条目过期后应返回 NEUTRAL 并清除")
    void decide_neutral_whenExpired() {
        filter.addTraceId("trace-expired", Level.DEBUG, 0);
        MDC.put("traceId", "trace-expired");

        // TTL=0 means immediate expiry
        FilterReply reply = filter.decide(null, null, Level.DEBUG, "msg", null, null);
        assertThat(reply).isEqualTo(FilterReply.NEUTRAL);
        assertThat(filter.getActiveTraceIds()).doesNotContainKey("trace-expired");
    }

    @Test
    @DisplayName("removeTraceId 应移除指定条目")
    void removeTraceId_removesEntry() {
        filter.addTraceId("trace-rm", Level.DEBUG, 300);
        assertThat(filter.getActiveTraceIds()).containsKey("trace-rm");

        filter.removeTraceId("trace-rm");
        assertThat(filter.getActiveTraceIds()).doesNotContainKey("trace-rm");
    }

    @Test
    @DisplayName("removeUserId 应移除指定条目")
    void removeUserId_removesEntry() {
        filter.addUserId("user-rm", Level.DEBUG, 300);
        assertThat(filter.getActiveUserIds()).containsKey("user-rm");

        filter.removeUserId("user-rm");
        assertThat(filter.getActiveUserIds()).doesNotContainKey("user-rm");
    }

    @Test
    @DisplayName("getActiveTraceIds 返回的 Map 应为不可变副本")
    void getActiveTraceIds_returnsUnmodifiableCopy() {
        filter.addTraceId("t1", Level.DEBUG, 300);
        Map<String, ConditionalLogLevelFilter.DebugEntry> active = filter.getActiveTraceIds();
        assertThat(active).hasSize(1);

        // original map modification should not affect the returned copy
        filter.removeTraceId("t1");
        assertThat(active).hasSize(1);
    }

    @Test
    @DisplayName("MDC 无 traceId/userId 时应返回 NEUTRAL")
    void decide_neutral_whenMdcEmpty() {
        filter.addTraceId("trace-001", Level.DEBUG, 60);
        // MDC is empty — no traceId or userId set

        FilterReply reply = filter.decide(null, null, Level.DEBUG, "msg", null, null);
        assertThat(reply).isEqualTo(FilterReply.NEUTRAL);
    }

    @Test
    @DisplayName("DebugEntry 应正确报告过期状态")
    void debugEntry_expiresCorrectly() {
        ConditionalLogLevelFilter.DebugEntry entry = new ConditionalLogLevelFilter.DebugEntry(Level.DEBUG, 300);
        assertThat(entry.isExpired()).isFalse();
        assertThat(entry.getTargetLevel()).isEqualTo(Level.DEBUG);
        assertThat(entry.getExpiresAt()).isNotNull();
    }
}
