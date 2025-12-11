package com.basebackend.file.audit;

import com.basebackend.file.mapper.FileShareAuditLogMapper;
import com.basebackend.file.security.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件分享审计服务
 *
 * 异步记录所有文件分享相关的安全和管理操作
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final FileShareAuditLogMapper auditLogMapper;

    /**
     * 异步记录审计日志
     *
     * @param action 操作类型
     * @param outcome 操作结果
     * @param shareCode 分享码
     */
    @Async("auditExecutor")
    public void record(AuditAction action, AuditOutcome outcome, String shareCode) {
        recordInternal(action, outcome, shareCode, null, null, null);
    }

    /**
     * 异步记录审计日志（含错误信息）
     *
     * @param action 操作类型
     * @param outcome 操作结果
     * @param shareCode 分享码
     * @param errorCode 错误码
     * @param errorMessage 错误信息
     */
    @Async("auditExecutor")
    public void record(AuditAction action, AuditOutcome outcome, String shareCode,
                      String errorCode, String errorMessage) {
        recordInternal(action, outcome, shareCode, errorCode, errorMessage, null);
    }

    /**
     * 异步记录审计日志（含详细信息）
     *
     * @param action 操作类型
     * @param outcome 操作结果
     * @param shareCode 分享码
     * @param details 详细信息（Map会自动转为JSON）
     */
    @Async("auditExecutor")
    public void record(AuditAction action, AuditOutcome outcome, String shareCode,
                      Map<String, Object> details) {
        String detailsJson = convertDetailsToJson(details);
        recordInternal(action, outcome, shareCode, null, null, detailsJson);
    }

    /**
     * 异步记录审计日志（含错误和详细信息）
     *
     * @param action 操作类型
     * @param outcome 操作结果
     * @param shareCode 分享码
     * @param errorCode 错误码
     * @param errorMessage 错误信息
     * @param details 详细信息（Map会自动转为JSON）
     */
    @Async("auditExecutor")
    public void record(AuditAction action, AuditOutcome outcome, String shareCode,
                      String errorCode, String errorMessage, Map<String, Object> details) {
        String detailsJson = convertDetailsToJson(details);
        recordInternal(action, outcome, shareCode, errorCode, errorMessage, detailsJson);
    }

    /**
     * 内部记录方法
     */
    private void recordInternal(AuditAction action, AuditOutcome outcome, String shareCode,
                               String errorCode, String errorMessage, String detailsJson) {
        try {
            // 获取当前用户上下文
            var userContext = UserContextHolder.getContext();

            // 创建审计日志实体
            FileShareAuditLog auditLog = new FileShareAuditLog();
            auditLog.setCreatedAt(LocalDateTime.now());
            auditLog.setTraceId(getOrCreateTraceId());
            auditLog.setAction(action);
            auditLog.setOutcome(outcome);
            auditLog.setShareCode(shareCode);

            // 设置错误信息
            if (AuditOutcome.FAIL.equals(outcome)) {
                auditLog.setErrorCode(errorCode);
                auditLog.setErrorMessage(errorMessage);
            }

            // 设置用户信息
            if (userContext != null) {
                auditLog.setUserId(userContext.getUserId());
                auditLog.setTenantId(userContext.getTenantId());
                auditLog.setClientIp(userContext.getClientIp());
                auditLog.setUserAgent(userContext.getUserAgent());
            }

            // 设置详细信息
            if (detailsJson != null) {
                auditLog.setDetails(detailsJson);
            }

            // 写入数据库
            auditLogMapper.insert(auditLog);

            // 记录审计日志（审计本身的日志，用于追踪）
            log.debug("审计记录: action={}, outcome={}, shareCode={}",
                    action.getCode(), outcome.getCode(), shareCode);

        } catch (Exception e) {
            // 审计写入失败不应影响主流程，只记录 warn
            log.warn("审计日志写入失败: action={}, shareCode={}, error={}",
                    action, shareCode, e.getMessage(), e);
        }
    }

    /**
     * 获取或创建追踪ID
     */
    private String getOrCreateTraceId() {
        // 这里简化处理，实际项目中应从上下文或MDC获取
        // 可以使用 UUID.randomUUID().toString() 或链路追踪工具（如Zipkin、Jaeger）
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 将详细信息Map转为JSON字符串
     */
    private String convertDetailsToJson(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        try {
            // 使用简单的字符串拼接方式，避免引入额外依赖
            // 生产环境建议使用 Jackson 或 Gson
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : details.entrySet()) {
                if (!first) {
                    json.append(",");
                }
                json.append("\"").append(entry.getKey()).append("\":");
                Object value = entry.getValue();
                if (value instanceof String) {
                    json.append("\"").append(escapeJsonString((String) value)).append("\"");
                } else if (value instanceof Number || value instanceof Boolean) {
                    json.append(value);
                } else {
                    json.append("\"").append(escapeJsonString(value.toString())).append("\"");
                }
                first = false;
            }
            json.append("}");
            return json.toString();
        } catch (Exception e) {
            log.warn("转换详细信息为JSON失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 转义JSON字符串中的特殊字符
     */
    private String escapeJsonString(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * 创建详细信息Map的构建器
     *
     * @return 详细信息构建器
     */
    public static DetailsBuilder detailsBuilder() {
        return new DetailsBuilder();
    }

    /**
     * 详细信息构建器
     */
    public static class DetailsBuilder {
        private final Map<String, Object> details = new HashMap<>();

        public DetailsBuilder put(String key, Object value) {
            details.put(key, value);
            return this;
        }

        public DetailsBuilder limitRemaining(Integer limitRemaining) {
            details.put("limitRemaining", limitRemaining);
            return this;
        }

        public DetailsBuilder downloadCountBefore(Integer before) {
            details.put("downloadCountBefore", before);
            return this;
        }

        public DetailsBuilder downloadCountAfter(Integer after) {
            details.put("downloadCountAfter", after);
            return this;
        }

        public DetailsBuilder cooldownUntil(LocalDateTime cooldownUntil) {
            details.put("cooldownUntil", cooldownUntil);
            return this;
        }

        public DetailsBuilder rateLimitInfo(String info) {
            details.put("rateLimitInfo", info);
            return this;
        }

        public Map<String, Object> build() {
            return new HashMap<>(details);
        }
    }
}
