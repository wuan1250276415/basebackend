package com.basebackend.logging.audit.model;

import com.basebackend.logging.audit.AuditEventType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 审计日志条目
 *
 * 完整的审计记录实体，包含所有必要的审计字段。
 * 支持数字签名和哈希链完整性校验。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditLogEntry {

    /**
     * 审计日志唯一标识符
     */
    private String id;

    /**
     * 审计事件发生时间戳
     */
    private Instant timestamp;

    /**
     * 操作用户 ID
     */
    private String userId;

    /**
     * 用户会话 ID
     */
    private String sessionId;

    /**
     * 审计事件类型
     */
    private AuditEventType eventType;

    /**
     * 操作资源（URL、文件路径、表名等）
     */
    private String resource;

    /**
     * 操作结果（SUCCESS、FAILURE、PENDING 等）
     */
    private String result;

    /**
     * 客户端 IP 地址
     */
    private String clientIp;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 客户端设备信息
     */
    private String deviceInfo;

    /**
     * 地理位置信息
     */
    private String location;

    /**
     * 关联的业务实体 ID
     */
    private String entityId;

    /**
     * 业务操作类型
     */
    private String operation;

    /**
     * 详细的操作信息（JSON 格式）
     */
    private Map<String, Object> details;

    /**
     * 操作耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误消息
     */
    private String errorMessage;

    /**
     * 请求追踪 ID（Trace ID）
     */
    private String traceId;

    /**
     * 请求跨度 ID（Span ID）
     */
    private String spanId;

    /**
     * 上一个审计日志的哈希值（用于哈希链）
     */
    private String prevHash;

    /**
     * 当前审计日志的哈希值
     */
    private String entryHash;

    /**
     * 数字签名
     */
    private String signature;

    /**
     * 证书 ID
     */
    private String certificateId;

    /**
     * 创建审计日志条目
     */
    public static AuditLogEntry create() {
        return AuditLogEntry.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 序列化为 JSON 字符串
     */
    public String toJson(ObjectMapper mapper) {
        try {
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException("审计日志序列化失败", e);
        }
    }

    /**
     * 从 JSON 字符串反序列化
     */
    public static AuditLogEntry fromJson(ObjectMapper mapper, String json) {
        try {
            return mapper.readValue(json, AuditLogEntry.class);
        } catch (Exception e) {
            throw new RuntimeException("审计日志反序列化失败", e);
        }
    }

    /**
     * 获取摘要信息
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("时间: ").append(timestamp != null ? timestamp.toString() : "N/A");
        sb.append(", 用户: ").append(userId != null ? userId : "N/A");
        sb.append(", 操作: ").append(eventType != null ? eventType.name() : "N/A");
        sb.append(", 资源: ").append(resource != null ? resource : "N/A");
        sb.append(", 结果: ").append(result != null ? result : "N/A");
        if (durationMs != null) {
            sb.append(", 耗时: ").append(durationMs).append("ms");
        }
        return sb.toString();
    }

    /**
     * 检查是否为成功操作
     */
    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(result);
    }

    /**
     * 检查是否为失败操作
     */
    public boolean isFailure() {
        return "FAILURE".equalsIgnoreCase(result);
    }

    /**
     * 检查是否包含敏感信息
     */
    public boolean containsSensitiveInfo() {
        return eventType != null && eventType.requiresProtection();
    }

    /**
     * 获取严重级别数值
     */
    public int getSeverityLevel() {
        return eventType != null ? eventType.getSeverityLevel() : 2;
    }
}
