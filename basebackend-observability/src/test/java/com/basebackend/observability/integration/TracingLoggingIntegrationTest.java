//package com.basebackend.observability.integration;
//
//import com.basebackend.observability.logging.format.LogAttributeEnricher;
//import io.opentelemetry.api.trace.Span;
//import io.opentelemetry.api.trace.Tracer;
//import io.opentelemetry.context.Scope;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.slf4j.MDC;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
///**
// * 追踪和日志集成测试
// * <p>
// * 验证 OpenTelemetry 追踪上下文能正确填充到 SLF4J MDC。
// * </p>
// */
//@SpringBootTest
//@ActiveProfiles("test")
//@DisplayName("追踪和日志集成测试")
//class TracingLoggingIntegrationTest {
//
//    @Autowired(required = false)
//    private Tracer tracer;
//
//    @Autowired(required = false)
//    private LogAttributeEnricher enricher;
//
//    @BeforeEach
//    void setUp() {
//        MDC.clear();
//    }
//
//    @AfterEach
//    void tearDown() {
//        MDC.clear();
//    }
//
//    @Test
//    @DisplayName("追踪上下文应自动填充到 MDC")
//    void shouldEnrichMdcWithTraceContext() {
//        // 假设 tracer 和 enricher 都可用
//        if (tracer == null || enricher == null) {
//            return;
//        }
//
//        // 创建一个 Span
//        Span span = tracer.spanBuilder("test-operation").startSpan();
//
//        try (Scope scope = span.makeCurrent()) {
//            // 填充 MDC
//            enricher.enrichFromCurrentSpan();
//
//            // 验证 MDC 包含 traceId 和 spanId
//            String traceId = MDC.get("traceId");
//            String spanId = MDC.get("spanId");
//
//            assertThat(traceId)
//                    .as("MDC should contain traceId")
//                    .isNotNull()
//                    .isNotEmpty();
//
//            assertThat(spanId)
//                    .as("MDC should contain spanId")
//                    .isNotNull()
//                    .isNotEmpty();
//
//            // 验证 traceId 格式（32字符十六进制）
//            assertThat(traceId)
//                    .matches("[a-f0-9]{32}");
//
//            // 验证 spanId 格式（16字符十六进制）
//            assertThat(spanId)
//                    .matches("[a-f0-9]{16}");
//
//        } finally {
//            span.end();
//            enricher.clearAll();
//        }
//    }
//
//    @Test
//    @DisplayName("业务上下文应填充到 MDC")
//    void shouldEnrichMdcWithBusinessContext() {
//        if (enricher == null) {
//            return;
//        }
//
//        // 准备业务上下文
//        Map<String, String> context = new HashMap<>();
//        context.put("X-Tenant-Id", "tenant-123");
//        context.put("X-User-Id", "user-456");
//        context.put("X-Request-Id", "req-789");
//        context.put("X-Channel-Id", "channel-web");
//
//        // 填充 MDC
//        enricher.enrichBusinessContext(context);
//
//        // 验证 MDC 包含业务上下文
//        assertThat(MDC.get("tenantId")).isEqualTo("tenant-123");
//        assertThat(MDC.get("userId")).isEqualTo("user-456");
//        assertThat(MDC.get("requestId")).isEqualTo("req-789");
//        assertThat(MDC.get("channelId")).isEqualTo("channel-web");
//
//        enricher.clearAll();
//    }
//
//    @Test
//    @DisplayName("clearAll 应仅清理托管的 MDC 键")
//    void shouldOnlyClearManagedKeys() {
//        if (enricher == null) {
//            return;
//        }
//
//        // 设置托管和非托管 MDC 键
//        MDC.put("traceId", "trace-123");
//        MDC.put("spanId", "span-456");
//        MDC.put("custom-key", "should-not-be-cleared");
//
//        // 清理
//        enricher.clearAll();
//
//        // 验证托管键被清理
//        assertThat(MDC.get("traceId")).isNull();
//        assertThat(MDC.get("spanId")).isNull();
//
//        // 验证非托管键保留
//        assertThat(MDC.get("custom-key")).isEqualTo("should-not-be-cleared");
//
//        MDC.clear();
//    }
//
//    @Test
//    @DisplayName("无有效 Span 时应清理残留的追踪 ID")
//    void shouldClearStaleTraceIdsWhenNoValidSpan() {
//        if (enricher == null) {
//            return;
//        }
//
//        // 模拟残留的追踪 ID（来自线程池复用）
//        MDC.put("traceId", "stale-trace-id");
//        MDC.put("spanId", "stale-span-id");
//
//        // 在没有活跃 Span 的情况下调用 enrichFromCurrentSpan
//        enricher.enrichFromCurrentSpan();
//
//        // 验证残留的追踪 ID 被清理
//        assertThat(MDC.get("traceId"))
//                .as("Stale traceId should be cleared when no valid span")
//                .isNull();
//        assertThat(MDC.get("spanId"))
//                .as("Stale spanId should be cleared when no valid span")
//                .isNull();
//    }
//}
