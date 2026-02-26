package com.basebackend.logging.audit.export;

import com.basebackend.logging.audit.AuditEventType;
import com.basebackend.logging.audit.AuditSeverity;
import com.basebackend.logging.audit.model.AuditLogEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * OCSF (Open Cybersecurity Schema Framework) 1.1 导出器
 *
 * 将审计日志转换为 OCSF JSON 格式，用于安全分析平台集成。
 *
 * @author basebackend team
 * @since 2025-12-10
 */
@Slf4j
public class OcsfExporter {

    private final String schemaVersion;
    private final String productName;
    private final String vendor;
    private final ObjectMapper objectMapper;

    public OcsfExporter(String schemaVersion, String productName, String vendor) {
        this.schemaVersion = schemaVersion;
        this.productName = productName;
        this.vendor = vendor;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * 将审计日志条目列表导出为 OCSF JSON 数组字符串
     */
    public String export(List<AuditLogEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return "[]";
        }
        List<Map<String, Object>> ocsfEvents = entries.stream()
                .map(this::convertToOcsf)
                .collect(Collectors.toList());
        try {
            return objectMapper.writeValueAsString(ocsfEvents);
        } catch (JsonProcessingException e) {
            log.error("OCSF 导出序列化失败", e);
            return "[]";
        }
    }

    /**
     * 将单条审计日志转换为 OCSF JSON 对象字符串
     */
    public String convertToJson(AuditLogEntry entry) {
        Map<String, Object> ocsf = convertToOcsf(entry);
        try {
            return objectMapper.writeValueAsString(ocsf);
        } catch (JsonProcessingException e) {
            log.error("OCSF 单条导出序列化失败", e);
            return "{}";
        }
    }

    /**
     * 将单条审计日志转换为 OCSF Map 结构
     */
    public Map<String, Object> convertToOcsf(AuditLogEntry entry) {
        Map<String, Object> event = new LinkedHashMap<>();

        // OCSF 基本字段
        event.put("class_uid", mapClassUid(entry.getEventType()));
        event.put("category_uid", mapCategoryUid(entry.getEventType()));
        event.put("severity_id", mapSeverityId(entry));
        event.put("activity_id", mapActivityId(entry));
        event.put("type_uid", computeTypeUid(entry));
        event.put("status_id", "SUCCESS".equalsIgnoreCase(entry.getResult()) ? 1 : 2);
        event.put("status", entry.getResult());

        // 时间
        if (entry.getTimestamp() != null) {
            event.put("time", entry.getTimestamp().toEpochMilli());
        }

        // 消息
        event.put("message", buildMessage(entry));

        // metadata 块
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("version", schemaVersion);
        metadata.put("uid", entry.getId());
        Map<String, Object> metaProduct = new LinkedHashMap<>();
        metaProduct.put("name", productName);
        metaProduct.put("vendor_name", vendor);
        metadata.put("product", metaProduct);
        if (entry.getTraceId() != null) {
            metadata.put("correlation_uid", entry.getTraceId());
        }
        event.put("metadata", metadata);

        // actor 块
        Map<String, Object> actor = new LinkedHashMap<>();
        if (entry.getUserId() != null) {
            Map<String, Object> user = new LinkedHashMap<>();
            user.put("uid", entry.getUserId());
            actor.put("user", user);
        }
        if (entry.getSessionId() != null) {
            Map<String, Object> session = new LinkedHashMap<>();
            session.put("uid", entry.getSessionId());
            actor.put("session", session);
        }
        if (!actor.isEmpty()) {
            event.put("actor", actor);
        }

        // src_endpoint 块
        Map<String, Object> srcEndpoint = new LinkedHashMap<>();
        if (entry.getClientIp() != null) {
            srcEndpoint.put("ip", entry.getClientIp());
        }
        if (entry.getUserAgent() != null) {
            srcEndpoint.put("agent_list", Collections.singletonList(
                    Collections.singletonMap("name", entry.getUserAgent())));
        }
        if (entry.getDeviceInfo() != null) {
            srcEndpoint.put("device", Collections.singletonMap("name", entry.getDeviceInfo()));
        }
        if (entry.getLocation() != null) {
            srcEndpoint.put("location",
                    Collections.singletonMap("desc", entry.getLocation()));
        }
        if (!srcEndpoint.isEmpty()) {
            event.put("src_endpoint", srcEndpoint);
        }

        // 资源
        if (entry.getResource() != null) {
            event.put("resource", Collections.singletonMap("name", entry.getResource()));
        }

        // 详情
        if (entry.getDetails() != null && !entry.getDetails().isEmpty()) {
            event.put("unmapped", entry.getDetails());
        }

        // 耗时
        if (entry.getDurationMs() != null) {
            event.put("duration", entry.getDurationMs());
        }

        // 错误信息
        if (entry.getErrorCode() != null || entry.getErrorMessage() != null) {
            Map<String, Object> errorInfo = new LinkedHashMap<>();
            if (entry.getErrorCode() != null) {
                errorInfo.put("code", entry.getErrorCode());
            }
            if (entry.getErrorMessage() != null) {
                errorInfo.put("message", entry.getErrorMessage());
            }
            event.put("error", errorInfo);
        }

        return event;
    }

    /**
     * OCSF class_uid 映射:
     * - Authentication -> 3002
     * - Authorization -> 3003
     * - 其他 -> 6003 (API Activity)
     */
    int mapClassUid(AuditEventType eventType) {
        if (eventType == null) {
            return 6003;
        }
        String category = eventType.getCategory();
        return switch (category) {
            case "auth" -> 3002;
            case "security" -> 3003;
            default -> 6003;
        };
    }

    /**
     * OCSF category_uid 映射:
     * - auth/security -> 3 (Identity & Access Management)
     * - 其他 -> 6 (Application Activity)
     */
    int mapCategoryUid(AuditEventType eventType) {
        if (eventType == null) {
            return 6;
        }
        String category = eventType.getCategory();
        if ("auth".equals(category) || "security".equals(category)) {
            return 3;
        }
        return 6;
    }

    int mapSeverityId(AuditLogEntry entry) {
        if (entry.getEventType() == null) {
            return 1;
        }
        AuditSeverity severity = entry.getEventType().getSeverity();
        return switch (severity) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
            case CRITICAL -> 4;
            default -> 1;
        };
    }

    private int mapActivityId(AuditLogEntry entry) {
        if (entry.getEventType() == null) {
            return 0;
        }
        return switch (entry.getEventType()) {
            case LOGIN -> 1;
            case LOGOUT -> 2;
            case CREATE, BATCH_CREATE -> 1;
            case UPDATE, BATCH_UPDATE -> 2;
            case DELETE, BATCH_DELETE -> 3;
            default -> 0;
        };
    }

    private long computeTypeUid(AuditLogEntry entry) {
        int classUid = mapClassUid(entry.getEventType());
        int activityId = mapActivityId(entry);
        return (long) classUid * 100 + activityId;
    }

    private String buildMessage(AuditLogEntry entry) {
        StringBuilder sb = new StringBuilder();
        if (entry.getEventType() != null) {
            sb.append(entry.getEventType().getDescription());
        }
        if (entry.getResource() != null) {
            sb.append(" - ").append(entry.getResource());
        }
        if (entry.getResult() != null) {
            sb.append(" [").append(entry.getResult()).append("]");
        }
        return sb.toString();
    }
}
