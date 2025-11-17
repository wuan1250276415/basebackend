package com.basebackend.common.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.attributes.HttpAttributes.HttpClientAttributes;
import io.opentelemetry.semconv.attributes.HttpAttributes.HttpServerAttributes;
import io.opentelemetry.semconv.db.attributes.DbAttributes;
import io.opentelemetry.semconv.rpc.attributes.RpcAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 链路追踪工具类
 * 提供简化的链路追踪 API，方便在业务代码中快速添加追踪
 *
 * 主要功能:
 * 1. 手动创建 Span
 * 2. 业务操作追踪
 * 3. HTTP 调用追踪
 * 4. 数据库操作追踪
 * 5. Feign 调用追踪
 * 6. 定时任务追踪
 * 7. 批量操作追踪
 * 8. 异常追踪
 *
 * 使用示例:
 * ```java
 * // 基本使用
 * TracingUtil.startSpan("user.operation", span -> {
 *     // 业务逻辑
 *     String result = userService.getUser(id);
 *     span.setAttribute("user.id", id);
 *     return result;
 * });
 *
 * // HTTP 调用追踪
 * TracingUtil.traceHttpCall("GET", "http://api.example.com/users",
 *     () -> restTemplate.getForObject("http://api.example.com/users", String.class));
 *
 * // 数据库操作追踪
 * TracingUtil.traceDatabaseQuery("SELECT * FROM users WHERE id = ?",
 *     () -> userDao.findById(id));
 * ```
 *
 * @author basebackend team
 * @version 1.0
 */
@Component
public class TracingUtil {

    private static final Logger logger = LoggerFactory.getLogger(TracingUtil.class);

    private final Tracer tracer;
    private final OpenTelemetry openTelemetry;

    public TracingUtil(Tracer tracer, OpenTelemetry openTelemetry) {
        this.tracer = tracer;
        this.openTelemetry = openTelemetry;
    }

    // ==================== 基础 Span 操作 ====================

    /**
     * 创建基础 Span
     */
    public Span createSpan(String operationName, SpanKind kind) {
        return tracer.spanBuilder(operationName)
            .setSpanKind(kind)
            .startSpan();
    }

    /**
     * 创建 Span 并执行代码 (自动管理生命周期)
     */
    public <T> T startSpan(String operationName, SpanKind kind, Supplier<T> operation) {
        Span span = createSpan(operationName, kind);
        try (Scope scope = span.makeCurrent()) {
            return operation.get();
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * 创建 Span 并执行代码 (无返回值)
     */
    public void startSpan(String operationName, SpanKind kind, Consumer<Span> operation) {
        Span span = createSpan(operationName, kind);
        try (Scope scope = span.makeCurrent()) {
            operation.accept(span);
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    // ==================== HTTP 追踪 ====================

    /**
     * HTTP 调用追踪
     */
    public <T> T traceHttpCall(String method, String url, Supplier<T> operation) {
        return startSpan("HTTP " + method, SpanKind.CLIENT, span -> {
            span.setAttribute(HttpAttributes.HTTP_METHOD, method);
            span.setAttribute(HttpAttributes.HTTP_URL, url);
            span.setAttribute("http.request.start", System.currentTimeMillis());

            T result = operation.get();

            span.setAttribute("http.request.duration", System.currentTimeMillis() - (Long) span.getAttributes().get("http.request.start"));
            return result;
        });
    }

    /**
     * HTTP GET 调用追踪
     */
    public <T> T traceHttpGet(String url, Supplier<T> operation) {
        return traceHttpCall("GET", url, operation);
    }

    /**
     * HTTP POST 调用追踪
     */
    public <T> T traceHttpPost(String url, Supplier<T> operation) {
        return traceHttpCall("POST", url, operation);
    }

    // ==================== 数据库追踪 ====================

    /**
     * 数据库查询追踪
     */
    public <T> T traceDatabaseQuery(String query, Supplier<T> operation) {
        return startSpan("Database Query", SpanKind.CLIENT, span -> {
            span.setAttribute(DbAttributes.DB_OPERATION, query);
            span.setAttribute(DbAttributes.DB_SYSTEM, "mysql");
            span.setAttribute("db.query.start", System.currentTimeMillis());

            T result = operation.get();

            span.setAttribute("db.query.duration", System.currentTimeMillis() - (Long) span.getAttributes().get("db.query.start"));
            return result;
        });
    }

    /**
     * 数据库写入追踪
     */
    public <T> T traceDatabaseWrite(String operation, Supplier<T> operationImpl) {
        return startSpan("Database " + operation, SpanKind.CLIENT, span -> {
            span.setAttribute(DbAttributes.DB_OPERATION, operation);
            span.setAttribute(DbAttributes.DB_SYSTEM, "mysql");
            return operationImpl.get();
        });
    }

    // ==================== 业务操作追踪 ====================

    /**
     * 业务操作追踪
     */
    public <T> T traceBusinessOperation(String operationName, String entityType, String entityId, Supplier<T> operation) {
        return startSpan("Business." + operationName, SpanKind.INTERNAL, span -> {
            span.setAttribute("business.operation", operationName);
            span.setAttribute("business.entity.type", entityType);
            span.setAttribute("business.entity.id", entityId);

            return operation.get();
        });
    }

    /**
     * 用户操作追踪
     */
    public <T> T traceUserOperation(String operation, String userId, Supplier<T> operationImpl) {
        return startSpan("User." + operation, SpanKind.INTERNAL, span -> {
            span.setAttribute("user.operation", operation);
            span.setAttribute("user.id", userId);
            return operationImpl.get();
        });
    }

    /**
     * 认证操作追踪
     */
    public <T> T traceAuthOperation(String operation, String userId, String status, Supplier<T> operationImpl) {
        return startSpan("Auth." + operation, SpanKind.INTERNAL, span -> {
            span.setAttribute("auth.operation", operation);
            if (userId != null) {
                span.setAttribute("auth.user.id", userId);
            }
            span.setAttribute("auth.status", status);
            span.addEvent("Auth operation started");

            T result = operationImpl.get();

            span.addEvent("Auth operation completed");
            return result;
        });
    }

    // ==================== 定时任务追踪 ====================

    /**
     * 定时任务追踪
     */
    public void traceScheduledTask(String taskName, Runnable task) {
        startSpan("Scheduled." + taskName, SpanKind.INTERNAL, span -> {
            span.setAttribute("scheduled.task", taskName);
            span.setAttribute("scheduled.start.time", Instant.now().toString());

            task.run();

            span.setAttribute("scheduled.end.time", Instant.now().toString());
        });
    }

    /**
     * 定时任务追踪 (有返回值)
     */
    public <T> T traceScheduledTask(String taskName, Supplier<T> task) {
        return startSpan("Scheduled." + taskName, SpanKind.INTERNAL, span -> {
            span.setAttribute("scheduled.task", taskName);
            span.setAttribute("scheduled.start.time", Instant.now().toString());

            T result = task.get();

            span.setAttribute("scheduled.end.time", Instant.now().toString());
            return result;
        });
    }

    // ==================== 批量操作追踪 ====================

    /**
     * 批量操作追踪
     */
    public <T> T traceBatchOperation(String operationName, int batchSize, Supplier<T> operation) {
        return startSpan("Batch." + operationName, SpanKind.INTERNAL, span -> {
            span.setAttribute("batch.operation", operationName);
            span.setAttribute("batch.size", batchSize);
            span.setAttribute("batch.start.time", Instant.now().toString());

            T result = operation.get();

            span.setAttribute("batch.end.time", Instant.now().toString());
            return result;
        });
    }

    // ==================== 异步操作追踪 ====================

    /**
     * 异步操作追踪
     */
    public <T> CompletableFuture<T> traceAsyncOperation(String operationName, Supplier<T> operation) {
        return CompletableFuture.supplyAsync(() -> {
            return startSpan("Async." + operationName, SpanKind.INTERNAL, operation);
        });
    }

    // ==================== 缓存操作追踪 ====================

    /**
     * 缓存操作追踪
     */
    public <T> T traceCacheOperation(String operation, String cacheName, String key, Supplier<T> operationImpl) {
        return startSpan("Cache." + operation, SpanKind.INTERNAL, span -> {
            span.setAttribute("cache.operation", operation);
            span.setAttribute("cache.name", cacheName);
            span.setAttribute("cache.key", key);

            return operationImpl.get();
        });
    }

    // ==================== 消息队列追踪 ====================

    /**
     * 消息发送追踪
     */
    public void traceMessageProduce(String topic, String messageKey, int partition, Consumer<Span> operation) {
        startSpan("Message.Produce", SpanKind.PRODUCER, span -> {
            span.setAttribute("message.topic", topic);
            span.setAttribute("message.key", messageKey);
            span.setAttribute("message.partition", partition);
            operation.accept(span);
        });
    }

    /**
     * 消息消费追踪
     */
    public <T> T traceMessageConsume(String topic, String groupId, Supplier<T> operation) {
        return startSpan("Message.Consume", SpanKind.CONSUMER, span -> {
            span.setAttribute("message.topic", topic);
            span.setAttribute("message.consumer.group", groupId);
            return operation.get();
        });
    }

    // ==================== 工具方法 ====================

    /**
     * 添加自定义属性到当前 Span
     */
    public void addAttribute(String key, String value) {
        Span span = Span.current();
        if (span != null && span.isRecording()) {
            span.setAttribute(key, value);
        }
    }

    /**
     * 添加事件到当前 Span
     */
    public void addEvent(String eventName) {
        Span span = Span.current();
        if (span != null && span.isRecording()) {
            span.addEvent(eventName);
        }
    }

    /**
     * 记录异常到当前 Span
     */
    public void recordException(Throwable throwable) {
        Span span = Span.current();
        if (span != null && span.isRecording()) {
            span.recordException(throwable);
            span.setStatus(StatusCode.ERROR, throwable.getMessage());
        }
    }

    /**
     * 获取当前 TraceID
     */
    public String getCurrentTraceId() {
        Span span = Span.current();
        return span != null ? span.getSpanContext().getTraceId() : null;
    }

    /**
     * 获取当前 SpanID
     */
    public String getCurrentSpanId() {
        Span span = Span.current();
        return span != null ? span.getSpanContext().getSpanId() : null;
    }

    /**
     * 检查链路追踪是否启用
     */
    public boolean isTracingEnabled() {
        return Span.current() != null && Span.current().isRecording();
    }
}
