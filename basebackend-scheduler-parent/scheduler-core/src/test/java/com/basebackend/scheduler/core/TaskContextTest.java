//package com.basebackend.scheduler.core;
//
//import org.junit.jupiter.api.Test;
//
//import java.time.Duration;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * TaskContext单元测试
// */
//class TaskContextTest {
//
//    @Test
//    void testBuilder_Basic() {
//        // When
//        TaskContext context = TaskContext.builder("task-001").build();
//
//        // Then
//        assertEquals("task-001", context.getTaskId());
//        assertNull(context.getIdempotentKey());
//        assertEquals(0, context.getRetryCount());
//        assertEquals(Duration.ofSeconds(30), context.getTimeout());
//        assertNotNull(context.getLabels());
//        assertTrue(context.getLabels().isEmpty());
//        assertNotNull(context.getParameters());
//        assertTrue(context.getParameters().isEmpty());
//    }
//
//    @Test
//    void testBuilder_WithAllFields() {
//        // Given
//        Map<String, String> labels = Map.of("env", "test", "version", "1.0");
//        Map<String, Object> params = Map.of("count", 10, "flag", true);
//        Map<String, String> trace = Map.of("traceId", "12345", "spanId", "67890");
//
//        // When
//        TaskContext context = TaskContext.builder("task-002")
//                .idempotentKey("idempotent-123")
//                .labels(labels)
//                .parameters(params)
//                .retryCount(2)
//                .timeout(Duration.ofMinutes(5))
//                .traceContext(trace)
//                .build();
//
//        // Then
//        assertEquals("task-002", context.getTaskId());
//        assertEquals("idempotent-123", context.getIdempotentKey());
//        assertEquals(2, context.getRetryCount());
//        assertEquals(Duration.ofMinutes(5), context.getTimeout());
//        assertEquals(labels, context.getLabels());
//        assertEquals(params, context.getParameters());
//        assertEquals(trace, context.getTraceContext());
//    }
//
//    @Test
//    void testBuilder_WithNullTaskId_ThrowsException() {
//        // When & Then
//        assertThrows(NullPointerException.class, () -> TaskContext.builder(null).build());
//    }
//
//    @Test
//    void testBuilder_WithEmptyTaskId_ThrowsException() {
//        // When & Then
//        assertThrows(NullPointerException.class, () -> TaskContext.builder("").build());
//    }
//
//    @Test
//    void testWithRetryCount() {
//        // Given
//        TaskContext context = TaskContext.builder("task-003").retryCount(1).build();
//
//        // When
//        TaskContext newContext = context.withRetryCount(5);
//
//        // Then
//        assertEquals(1, context.getRetryCount());
//        assertEquals(5, newContext.getRetryCount());
//        assertNotSame(context, newContext);
//    }
//
//    @Test
//    void testIncrementRetryCount() {
//        // Given
//        TaskContext context = TaskContext.builder("task-004").retryCount(2).build();
//
//        // When
//        TaskContext newContext = context.incrementRetryCount();
//
//        // Then
//        assertEquals(2, context.getRetryCount());
//        assertEquals(3, newContext.getRetryCount());
//        assertNotSame(context, newContext);
//    }
//
//    @Test
//    void testWithTimeout() {
//        // Given
//        TaskContext context = TaskContext.builder("task-005")
//                .timeout(Duration.ofSeconds(30))
//                .build();
//
//        // When
//        TaskContext newContext = context.withTimeout(Duration.ofMinutes(10));
//
//        // Then
//        assertEquals(Duration.ofSeconds(30), context.getTimeout());
//        assertEquals(Duration.ofMinutes(10), newContext.getTimeout());
//        assertNotSame(context, newContext);
//    }
//
//    @Test
//    void testToBuilder() {
//        // Given
//        TaskContext original = TaskContext.builder("task-006")
//                .idempotentKey("key-123")
//                .retryCount(3)
//                .build();
//
//        // When
//        TaskContext modified = original.toBuilder()
//                .retryCount(5)
//                .idempotentKey("key-456")
//                .build();
//
//        // Then
//        assertEquals("task-006", modified.getTaskId());
//        assertEquals("key-456", modified.getIdempotentKey());
//        assertEquals(5, modified.getRetryCount());
//
//        // Original should be unchanged
//        assertEquals(3, original.getRetryCount());
//        assertEquals("key-123", original.getIdempotentKey());
//    }
//
//    @Test
//    void testLabels_Immutable() {
//        // Given
//        TaskContext context = TaskContext.builder("task-007")
//                .labels(Map.of("key", "value"))
//                .build();
//
//        // When & Then
//        assertThrows(UnsupportedOperationException.class, () -> {
//            context.getLabels().put("newKey", "newValue");
//        });
//    }
//
//    @Test
//    void testParameters_Immutable() {
//        // Given
//        TaskContext context = TaskContext.builder("task-008")
//                .parameters(Map.of("count", 1))
//                .build();
//
//        // When & Then
//        assertThrows(UnsupportedOperationException.class, () -> {
//            context.getParameters().put("newParam", "value");
//        });
//    }
//
//    @Test
//    void testTraceContext_Immutable() {
//        // Given
//        TaskContext context = TaskContext.builder("task-009")
//                .traceContext(Map.of("trace", "value"))
//                .build();
//
//        // When & Then
//        assertThrows(UnsupportedOperationException.class, () -> {
//            context.getTraceContext().put("newTrace", "value");
//        });
//    }
//
//    @Test
//    void testBuilder_WithNullLabels() {
//        // When
//        TaskContext context = TaskContext.builder("task-010")
//                .labels(null)
//                .build();
//
//        // Then
//        assertNotNull(context.getLabels());
//        assertTrue(context.getLabels().isEmpty());
//    }
//
//    @Test
//    void testBuilder_WithEmptyLabels() {
//        // When
//        TaskContext context = TaskContext.builder("task-011")
//                .labels(Collections.emptyMap())
//                .build();
//
//        // Then
//        assertNotNull(context.getLabels());
//        assertTrue(context.getLabels().isEmpty());
//    }
//
//    @Test
//    void testBuilder_WithNullParameters() {
//        // When
//        TaskContext context = TaskContext.builder("task-012")
//                .parameters(null)
//                .build();
//
//        // Then
//        assertNotNull(context.getParameters());
//        assertTrue(context.getParameters().isEmpty());
//    }
//
//    @Test
//    void testBuilder_WithNullTraceContext() {
//        // When
//        TaskContext context = TaskContext.builder("task-013")
//                .traceContext(null)
//                .build();
//
//        // Then
//        assertNotNull(context.getTraceContext());
//        assertTrue(context.getTraceContext().isEmpty());
//    }
//
//    @Test
//    void testBuilder_NegativeRetryCount() {
//        // When
//        TaskContext context = TaskContext.builder("task-014")
//                .retryCount(-5)
//                .build();
//
//        // Then
//        assertEquals(0, context.getRetryCount());
//    }
//
//    @Test
//    void testBuilder_Labels_ModifyOriginalMap_DoesNotAffectContext() {
//        // Given
//        Map<String, String> originalLabels = new HashMap<>();
//        originalLabels.put("key1", "value1");
//
//        // When
//        TaskContext context = TaskContext.builder("task-015")
//                .labels(originalLabels)
//                .build();
//
//        // Modify original map
//        originalLabels.put("key2", "value2");
//
//        // Then - context should not be affected
//        assertEquals(1, context.getLabels().size());
//        assertFalse(context.getLabels().containsKey("key2"));
//    }
//
//    @Test
//    void testBuilder_Parameters_ModifyOriginalMap_DoesNotAffectContext() {
//        // Given
//        Map<String, Object> originalParams = new HashMap<>();
//        originalParams.put("param1", "value1");
//
//        // When
//        TaskContext context = TaskContext.builder("task-016")
//                .parameters(originalParams)
//                .build();
//
//        // Modify original map
//        originalParams.put("param2", "value2");
//
//        // Then - context should not be affected
//        assertEquals(1, context.getParameters().size());
//        assertFalse(context.getParameters().containsKey("param2"));
//    }
//
//    @Test
//    void testBuilder_TraceContext_ModifyOriginalMap_DoesNotAffectContext() {
//        // Given
//        Map<String, String> originalTrace = new HashMap<>();
//        originalTrace.put("trace1", "value1");
//
//        // When
//        TaskContext context = TaskContext.builder("task-017")
//                .traceContext(originalTrace)
//                .build();
//
//        // Modify original map
//        originalTrace.put("trace2", "value2");
//
//        // Then - context should not be affected
//        assertEquals(1, context.getTraceContext().size());
//        assertFalse(context.getTraceContext().containsKey("trace2"));
//    }
//
//    @Test
//    void testDefaultTimeout() {
//        // When
//        TaskContext context = TaskContext.builder("task-018").build();
//
//        // Then
//        assertEquals(Duration.ofSeconds(30), context.getTimeout());
//    }
//
//    @Test
//    void testNullTimeout() {
//        // When
//        TaskContext context = TaskContext.builder("task-019")
//                .timeout(null)
//                .build();
//
//        // Then
//        assertNull(context.getTimeout());
//    }
//
//    @Test
//    void testZeroTimeout() {
//        // When
//        TaskContext context = TaskContext.builder("task-020")
//                .timeout(Duration.ZERO)
//                .build();
//
//        // Then
//        assertEquals(Duration.ZERO, context.getTimeout());
//    }
//
//    @Test
//    void testMultipleWithRetryCount_Calls() {
//        // Given
//        TaskContext context = TaskContext.builder("task-021").retryCount(0).build();
//
//        // When
//        TaskContext context1 = context.withRetryCount(1);
//        TaskContext context2 = context1.withRetryCount(2);
//        TaskContext context3 = context2.withRetryCount(3);
//
//        // Then
//        assertEquals(0, context.getRetryCount());
//        assertEquals(1, context1.getRetryCount());
//        assertEquals(2, context2.getRetryCount());
//        assertEquals(3, context3.getRetryCount());
//    }
//
//    @Test
//    void testMultipleIncrementRetryCount_Calls() {
//        // Given
//        TaskContext context = TaskContext.builder("task-022").retryCount(0).build();
//
//        // When
//        TaskContext context1 = context.incrementRetryCount();
//        TaskContext context2 = context1.incrementRetryCount();
//        TaskContext context3 = context2.incrementRetryCount();
//
//        // Then
//        assertEquals(0, context.getRetryCount());
//        assertEquals(1, context1.getRetryCount());
//        assertEquals(2, context2.getRetryCount());
//        assertEquals(3, context3.getRetryCount());
//    }
//}
