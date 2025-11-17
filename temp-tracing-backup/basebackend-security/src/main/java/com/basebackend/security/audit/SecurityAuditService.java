package com.basebackend.security.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 安全审计服务
 * 负责记录所有安全相关的事件，包括登录、权限、数据访问等
 */
@Slf4j
@Service
public class SecurityAuditService {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private static final String AUDIT_TOPIC = "security-audit";
    private static final String CRITICAL_AUDIT_TOPIC = "security-audit-critical";

    /**
     * 记录登录事件
     *
     * @param username 用户名
     * @param success 是否成功
     * @param ipAddress IP地址
     * @param userAgent 用户代理
     * @param reason 原因
     */
    @Async
    public void logLogin(String username, boolean success, String ipAddress, String userAgent, String reason) {
        Map<String, Object> event = new HashMap<>();
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("eventType", "LOGIN");
        event.put("username", username);
        event.put("success", success);
        event.put("ipAddress", ipAddress);
        event.put("userAgent", userAgent);
        event.put("reason", reason);
        event.put("severity", success ? "INFO" : "WARNING");

        // 发送到Kafka主题
        kafkaTemplate.send(AUDIT_TOPIC, event);

        log.info("登录事件: 用户={}, 成功={}, IP={}, 原因={}", username, success, ipAddress, reason);
    }

    /**
     * 记录登出事件
     *
     * @param username 用户名
     * @param ipAddress IP地址
     */
    @Async
    public void logLogout(String username, String ipAddress) {
        Map<String, Object> event = new HashMap<>();
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("eventType", "LOGOUT");
        event.put("username", username);
        event.put("ipAddress", ipAddress);
        event.put("severity", "INFO");

        kafkaTemplate.send(AUDIT_TOPIC, event);

        log.info("登出事件: 用户={}, IP={}", username, ipAddress);
    }

    /**
     * 记录权限变更事件
     *
     * @param adminUsername 管理员用户名
     * @param targetUsername 目标用户名
     * @param action 操作类型 (GRANT, REVOKE, MODIFY)
     * @param permission 权限
     * @param resource 资源
     */
    @Async
    public void logPermissionChange(String adminUsername, String targetUsername,
                                   String action, String permission, String resource) {
        Map<String, Object> event = new HashMap<>();
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("eventType", "PERMISSION_CHANGE");
        event.put("adminUsername", adminUsername);
        event.put("targetUsername", targetUsername);
        event.put("action", action);
        event.put("permission", permission);
        event.put("resource", resource);
        event.put("severity", "WARNING");

        kafkaTemplate.send(CRITICAL_AUDIT_TOPIC, event);

        log.warn("权限变更事件: 管理员={}, 目标={}, 操作={}, 权限={}",
                adminUsername, targetUsername, action, permission);
    }

    /**
     * 记录数据访问事件
     *
     * @param username 用户名
     * @param resource 资源
     * @param operation 操作 (READ, WRITE, DELETE, EXPORT)
     * @param ipAddress IP地址
     * @param success 是否成功
     */
    @Async
    public void logDataAccess(String username, String resource, String operation,
                             String ipAddress, boolean success) {
        Map<String, Object> event = new HashMap<>();
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("eventType", "DATA_ACCESS");
        event.put("username", username);
        event.put("resource", resource);
        event.put("operation", operation);
        event.put("ipAddress", ipAddress);
        event.put("success", success);
        event.put("severity", success ? "INFO" : "WARNING");

        kafkaTemplate.send(AUDIT_TOPIC, event);

        log.info("数据访问事件: 用户={}, 资源={}, 操作={}, IP={}, 成功={}",
                username, resource, operation, ipAddress, success);
    }

    /**
     * 记录敏感数据访问事件
     *
     * @param username 用户名
     * @param dataType 数据类型
     * @param recordId 记录ID
     * @param ipAddress IP地址
     */
    @Async
    public void logSensitiveDataAccess(String username, String dataType, String recordId, String ipAddress) {
        Map<String, Object> event = new HashMap<>();
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("eventType", "SENSITIVE_DATA_ACCESS");
        event.put("username", username);
        event.put("dataType", dataType);
        event.put("recordId", recordId);
        event.put("ipAddress", ipAddress);
        event.put("severity", "WARNING");

        kafkaTemplate.send(CRITICAL_AUDIT_TOPIC, event);

        log.warn("敏感数据访问事件: 用户={}, 数据类型={}, 记录ID={}, IP={}",
                username, dataType, recordId, ipAddress);
    }

    /**
     * 记录密码修改事件
     *
     * @param username 用户名
     * @param success 是否成功
     * @param ipAddress IP地址
     * @param reason 原因
     */
    @Async
    public void logPasswordChange(String username, boolean success, String ipAddress, String reason) {
        Map<String, Object> event = new HashMap<>();
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("eventType", "PASSWORD_CHANGE");
        event.put("username", username);
        event.put("success", success);
        event.put("ipAddress", ipAddress);
        event.put("reason", reason);
        event.put("severity", success ? "INFO" : "WARNING");

        kafkaTemplate.send(CRITICAL_AUDIT_TOPIC, event);

        log.warn("密码修改事件: 用户={}, 成功={}, IP={}, 原因={}", username, success, ipAddress, reason);
    }

    /**
     * 记录系统配置变更事件
     *
     * @param username 用户名
     * @param configKey 配置键
     * @param oldValue 旧值
     * @param newValue 新值
     * @param ipAddress IP地址
     */
    @Async
    public void logConfigChange(String username, String configKey, String oldValue, String newValue, String ipAddress) {
        Map<String, Object> event = new HashMap<>();
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("eventType", "CONFIG_CHANGE");
        event.put("username", username);
        event.put("configKey", configKey);
        event.put("oldValue", oldValue);
        event.put("newValue", newValue);
        event.put("ipAddress", ipAddress);
        event.put("severity", "CRITICAL");

        kafkaTemplate.send(CRITICAL_AUDIT_TOPIC, event);

        log.error("系统配置变更事件: 用户={}, 配置项={}, 旧值={}, 新值={}, IP={}",
                username, configKey, oldValue, newValue, ipAddress);
    }

    /**
     * 记录API调用事件
     *
     * @param username 用户名
     * @param endpoint 接口地址
     * @param method HTTP方法
     * @param ipAddress IP地址
     * @param userAgent 用户代理
     * @param statusCode 状态码
     * @param responseTime 响应时间(毫秒)
     */
    @Async
    public void logApiCall(String username, String endpoint, String method, String ipAddress,
                          String userAgent, int statusCode, long responseTime) {
        Map<String, Object> event = new HashMap<>();
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("eventType", "API_CALL");
        event.put("username", username);
        event.put("endpoint", endpoint);
        event.put("method", method);
        event.put("ipAddress", ipAddress);
        event.put("userAgent", userAgent);
        event.put("statusCode", statusCode);
        event.put("responseTime", responseTime);
        event.put("severity", statusCode >= 400 ? "WARNING" : "INFO");

        kafkaTemplate.send(AUDIT_TOPIC, event);

        if (statusCode >= 400) {
            log.warn("API调用异常: 用户={}, 接口={}, 方法={}, 状态码={}, IP={}",
                    username, endpoint, method, statusCode, ipAddress);
        }
    }

    /**
     * 记录异常安全事件
     *
     * @param eventType 事件类型
     * @param description 描述
     * @param severity 严重性
     * @param details 详细信息
     */
    @Async
    public void logSecurityException(String eventType, String description, String severity, Map<String, Object> details) {
        Map<String, Object> event = new HashMap<>();
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("eventType", eventType);
        event.put("description", description);
        event.put("severity", severity);
        event.put("details", details);

        String topic = severity.equals("CRITICAL") ? CRITICAL_AUDIT_TOPIC : AUDIT_TOPIC;
        kafkaTemplate.send(topic, event);

        log.error("安全异常事件: 类型={}, 描述={}, 严重性={}", eventType, description, severity);
    }

    /**
     * 记录文件操作事件
     *
     * @param username 用户名
     * @param filePath 文件路径
     * @param operation 操作类型 (UPLOAD, DOWNLOAD, DELETE)
     * @param ipAddress IP地址
     * @param success 是否成功
     */
    @Async
    public void logFileOperation(String username, String filePath, String operation, String ipAddress, boolean success) {
        Map<String, Object> event = new HashMap<>();
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("eventType", "FILE_OPERATION");
        event.put("username", username);
        event.put("filePath", filePath);
        event.put("operation", operation);
        event.put("ipAddress", ipAddress);
        event.put("success", success);
        event.put("severity", success ? "INFO" : "WARNING");

        kafkaTemplate.send(AUDIT_TOPIC, event);

        log.info("文件操作事件: 用户={}, 文件={}, 操作={}, 成功={}, IP={}",
                username, filePath, operation, success, ipAddress);
    }

    /**
     * 记录数据库访问事件
     *
     * @param username 用户名
     * @param tableName 表名
     * @param operation 操作类型 (SELECT, INSERT, UPDATE, DELETE)
     * @param recordCount 记录数量
     * @param ipAddress IP地址
     */
    @Async
    public void logDatabaseAccess(String username, String tableName, String operation, int recordCount, String ipAddress) {
        Map<String, Object> event = new HashMap<>();
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("eventType", "DATABASE_ACCESS");
        event.put("username", username);
        event.put("tableName", tableName);
        event.put("operation", operation);
        event.put("recordCount", recordCount);
        event.put("ipAddress", ipAddress);
        event.put("severity", "INFO");

        kafkaTemplate.send(AUDIT_TOPIC, event);

        log.debug("数据库访问事件: 用户={}, 表={}, 操作={}, 记录数={}, IP={}",
                username, tableName, operation, recordCount, ipAddress);
    }

    /**
     * 记录用户注册事件
     *
     * @param username 用户名
     * @param email 邮箱
     * @param ipAddress IP地址
     * @param userAgent 用户代理
     * @param success 是否成功
     */
    @Async
    public void logUserRegistration(String username, String email, String ipAddress, String userAgent, boolean success) {
        Map<String, Object> event = new HashMap<>();
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("eventType", "USER_REGISTRATION");
        event.put("username", username);
        event.put("email", email);
        event.put("ipAddress", ipAddress);
        event.put("userAgent", userAgent);
        event.put("success", success);
        event.put("severity", success ? "INFO" : "WARNING");

        kafkaTemplate.send(AUDIT_TOPIC, event);

        log.info("用户注册事件: 用户={}, 邮箱={}, 成功={}, IP={}",
                username, email, success, ipAddress);
    }

    /**
     * 记录会话事件
     *
     * @param username 用户名
     * @param sessionId 会话ID
     * @param action 操作 (CREATE, DESTROY, TIMEOUT)
     * @param ipAddress IP地址
     */
    @Async
    public void logSessionEvent(String username, String sessionId, String action, String ipAddress) {
        Map<String, Object> event = new HashMap<>();
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("eventType", "SESSION_EVENT");
        event.put("username", username);
        event.put("sessionId", sessionId);
        event.put("action", action);
        event.put("ipAddress", ipAddress);
        event.put("severity", "INFO");

        kafkaTemplate.send(AUDIT_TOPIC, event);

        log.info("会话事件: 用户={}, 会话ID={}, 操作={}, IP={}",
                username, sessionId, action, ipAddress);
    }
}
