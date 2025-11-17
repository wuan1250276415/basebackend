package com.basebackend.common.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 结构化日志工具类
 * 统一日志格式，便于日志聚合和分析
 *
 * 日志格式:
 * {
 *   "timestamp": "2024-01-01T12:00:00.000Z",
 *   "level": "INFO",
 *   "logger": "com.basebackend.service.UserService",
 *   "message": "User login successful",
 *   "trace_id": "abc123",
 *   "span_id": "def456",
 *   "user_id": "12345",
 *   "service": "basebackend-admin-api",
 *   "environment": "production",
 *   "host": "server-1",
 *   "thread": "http-nio-8080-exec-1",
 *   "custom_fields": {...}
 * }
 *
 * @author basebackend team
 * @version 1.0
 */
public class StructuredLogging {

    private static final Logger logger = LoggerFactory.getLogger(StructuredLogging.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 记录 INFO 级别日志
     */
    public static void info(String message) {
        info(message, null, null);
    }

    /**
     * 记录 INFO 级别日志 (带自定义字段)
     */
    public static void info(String message, String traceId, Map<String, Object> customFields) {
        log("INFO", message, traceId, null, customFields);
    }

    /**
     * 记录 INFO 级别日志 (带异常)
     */
    public static void infoWithException(String message, String traceId, Exception exception) {
        log("INFO", message, traceId, exception, null);
    }

    /**
     * 记录 WARN 级别日志
     */
    public static void warn(String message) {
        warn(message, null, null);
    }

    /**
     * 记录 WARN 级别日志 (带自定义字段)
     */
    public static void warn(String message, String traceId, Map<String, Object> customFields) {
        log("WARN", message, traceId, null, customFields);
    }

    /**
     * 记录 WARN 级别日志 (带异常)
     */
    public static void warnWithException(String message, String traceId, Exception exception) {
        log("WARN", message, traceId, exception, null);
    }

    /**
     * 记录 ERROR 级别日志
     */
    public static void error(String message) {
        error(message, null, null);
    }

    /**
     * 记录 ERROR 级别日志 (带自定义字段)
     */
    public static void error(String message, String traceId, Map<String, Object> customFields) {
        log("ERROR", message, traceId, null, customFields);
    }

    /**
     * 记录 ERROR 级别日志 (带异常)
     */
    public static void errorWithException(String message, String traceId, Exception exception) {
        log("ERROR", message, traceId, exception, null);
    }

    /**
     * 记录 DEBUG 级别日志
     */
    public static void debug(String message) {
        debug(message, null, null);
    }

    /**
     * 记录 DEBUG 级别日志 (带自定义字段)
     */
    public static void debug(String message, String traceId, Map<String, Object> customFields) {
        log("DEBUG", message, traceId, null, customFields);
    }

    /**
     * 记录业务操作日志
     */
    public static void businessOperation(String operation, String entity, String entityId,
                                       String status, String traceId) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("operation", operation);
        fields.put("entity", entity);
        fields.put("entity_id", entityId);
        fields.put("status", status);

        info("Business operation: " + operation, traceId, fields);
    }

    /**
     * 记录用户操作日志
     */
    public static void userAction(String userId, String action, String resource,
                                 String status, String traceId) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("user_id", userId);
        fields.put("action", action);
        fields.put("resource", resource);
        fields.put("status", status);

        info("User action: " + action, traceId, fields);
    }

    /**
     * 记录认证日志
     */
    public static void authentication(String userId, String action, String status,
                                    String traceId, String details) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("user_id", userId);
        fields.put("action", action);
        fields.put("status", status);
        if (details != null) {
            fields.put("details", details);
        }

        info("Authentication: " + action, traceId, fields);
    }

    /**
     * 记录 API 调用日志
     */
    public static void apiCall(String method, String path, int statusCode,
                             long responseTime, String traceId) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("http_method", method);
        fields.put("http_path", path);
        fields.put("status_code", statusCode);
        fields.put("response_time_ms", responseTime);

        String logLevel = statusCode >= 500 ? "ERROR" :
                         statusCode >= 400 ? "WARN" : "INFO";

        log(logLevel, "API call: " + method + " " + path, traceId, null, fields);
    }

    /**
     * 记录数据库操作日志
     */
    public static void databaseOperation(String operation, String table, String query,
                                       long duration, String traceId) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("db_operation", operation);
        fields.put("db_table", table);
        fields.put("db_duration_ms", duration);

        String logLevel = duration > 1000 ? "WARN" : "INFO";

        info("Database " + operation + " on " + table, traceId, fields);
    }

    /**
     * 记录缓存操作日志
     */
    public static void cacheOperation(String operation, String cacheName, String key,
                                    String status, String traceId) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("cache_operation", operation);
        fields.put("cache_name", cacheName);
        fields.put("cache_key", key);
        fields.put("status", status);

        info("Cache " + operation + " on " + cacheName, traceId, fields);
    }

    /**
     * 记录定时任务日志
     */
    public static void scheduledTask(String taskName, String status, long duration,
                                   String traceId, Map<String, Object> details) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("task_name", taskName);
        fields.put("task_status", status);
        fields.put("task_duration_ms", duration);
        if (details != null) {
            fields.put("task_details", details);
        }

        String logLevel = "COMPLETED".equals(status) ? "INFO" : "WARN";

        info("Scheduled task: " + taskName, traceId, fields);
    }

    /**
     * 记录性能指标日志
     */
    public static void performanceMetric(String metricName, double value, String unit,
                                       String traceId, Map<String, Object> tags) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("metric_name", metricName);
        fields.put("metric_value", value);
        fields.put("metric_unit", unit);
        if (tags != null) {
            fields.put("metric_tags", tags);
        }

        debug("Performance metric: " + metricName, traceId, fields);
    }

    /**
     * 记录安全事件日志
     */
    public static void securityEvent(String event, String severity, String source,
                                   String details, String traceId) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("security_event", event);
        fields.put("security_severity", severity);
        fields.put("security_source", source);
        fields.put("details", details);

        String logLevel = "HIGH".equals(severity) ? "WARN" : "INFO";

        info("Security event: " + event, traceId, fields);
    }

    /**
     * 核心日志记录方法
     */
    private static void log(String level, String message, String traceId,
                          Exception exception, Map<String, Object> customFields) {
        try {
            // 1. 构建基础日志对象
            ObjectNode logObject = objectMapper.createObjectNode();

            // 2. 添加时间戳
            logObject.put("timestamp", Instant.now().toString());

            // 3. 添加日志级别
            logObject.put("level", level);

            // 4. 添加记录器名称 (通过堆栈获取)
            String loggerName = getLoggerName();
            logObject.put("logger", loggerName);

            // 5. 添加消息
            logObject.put("message", message);

            // 6. 添加 TraceID
            String finalTraceId = traceId != null ? traceId : getTraceIdFromMdc();
            if (finalTraceId != null) {
                logObject.put("trace_id", finalTraceId);
            }

            // 7. 添加 SpanID (如果存在)
            String spanId = getSpanIdFromMdc();
            if (spanId != null) {
                logObject.put("span_id", spanId);
            }

            // 8. 添加服务信息
            logObject.put("service", getServiceName());
            logObject.put("environment", getEnvironment());
            logObject.put("host", getHostName());

            // 9. 添加线程信息
            logObject.put("thread", Thread.currentThread().getName());
            logObject.put("thread_id", Thread.currentThread().getId());

            // 10. 添加异常信息 (如果存在)
            if (exception != null) {
                ObjectNode exceptionNode = objectMapper.createObjectNode();
                exceptionNode.put("type", exception.getClass().getName());
                exceptionNode.put("message", exception.getMessage());
                logObject.set("exception", exceptionNode);
            }

            // 11. 添加自定义字段
            if (customFields != null && !customFields.isEmpty()) {
                ObjectNode customNode = objectMapper.createObjectNode();
                customFields.forEach((key, value) -> {
                    if (value != null) {
                        customNode.put(key, value.toString());
                    }
                });
                logObject.set("custom_fields", customNode);
            }

            // 12. 转换为 JSON 字符串
            String logMessage = objectMapper.writeValueAsString(logObject);

            // 13. 输出日志
            switch (level) {
                case "ERROR":
                    if (exception != null) {
                        logger.error(logMessage, exception);
                    } else {
                        logger.error(logMessage);
                    }
                    break;
                case "WARN":
                    logger.warn(logMessage);
                    break;
                case "INFO":
                    logger.info(logMessage);
                    break;
                case "DEBUG":
                    logger.debug(logMessage);
                    break;
            }

        } catch (Exception e) {
            // 如果日志记录失败，使用默认格式
            logger.error("Failed to write structured log: " + e.getMessage(), e);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 从 MDC 中获取 TraceID
     */
    private static String getTraceIdFromMdc() {
        return MDC.get("trace_id");
    }

    /**
     * 从 MDC 中获取 SpanID
     */
    private static String getSpanIdFromMdc() {
        return MDC.get("span_id");
    }

    /**
     * 获取记录器名称
     */
    private static String getLoggerName() {
        // 从堆栈中获取调用者的类名
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 0; i < stackTrace.length; i++) {
            String className = stackTrace[i].getClassName();
            if (!className.equals(StructuredLogging.class.getName())) {
                return className;
            }
        }
        return "Unknown";
    }

    /**
     * 获取服务名称
     */
    private static String getServiceName() {
        String serviceName = System.getenv("SERVICE_NAME");
        return serviceName != null ? serviceName : "basebackend-service";
    }

    /**
     * 获取环境
     */
    private static String getEnvironment() {
        String environment = System.getenv("ENVIRONMENT");
        return environment != null ? environment : "production";
    }

    /**
     * 获取主机名
     */
    private static String getHostName() {
        return System.getenv("HOSTNAME");
    }

    /**
     * 生成新的 TraceID
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 设置 MDC 上下文
     */
    public static void setMdcContext(String traceId, String spanId) {
        MDC.put("trace_id", traceId);
        if (spanId != null) {
            MDC.put("span_id", spanId);
        }
    }

    /**
     * 清除 MDC 上下文
     */
    public static void clearMdcContext() {
        MDC.clear();
    }
}
