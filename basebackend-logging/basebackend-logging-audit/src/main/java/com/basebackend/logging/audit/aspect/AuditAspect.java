package com.basebackend.logging.audit.aspect;

import com.basebackend.logging.audit.AuditEventType;
import com.basebackend.logging.audit.annotation.Auditable;
import com.basebackend.logging.audit.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 审计切面
 *
 * 自动拦截标注了 @Auditable 注解的方法，
 * 记录完整的审计日志。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Slf4j
@Aspect
@Component
@Order(10)
public class AuditAspect {

    private final AuditService auditService;

    public AuditAspect(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * 环绕通知：处理审计注解
     */
    @Around("@annotation(auditable)")
    public Object around(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        AuditEventType eventType = auditable.value();
        String resource = getResource(pjp, auditable);

        // 检查严重级别
        if (eventType.getSeverityLevel() < auditable.minSeverityLevel()) {
            return pjp.proceed();
        }

        long startTime = System.nanoTime();
        Object result = null;
        Exception exception = null;

        try {
            // 执行原方法
            result = pjp.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            // 记录审计日志
            try {
                recordAudit(pjp, eventType, resource, result, exception, startTime, auditable);
            } catch (Exception e) {
                log.error("记录审计日志失败", e);
            }
        }
    }

    /**
     * 记录审计日志
     */
    private void recordAudit(ProceedingJoinPoint pjp,
                            AuditEventType eventType,
                            String resource,
                            Object result,
                            Exception exception,
                            long startTime,
                            Auditable auditable) {
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000L;

        // 获取当前用户信息
        String userId = getCurrentUserId();
        String sessionId = getCurrentSessionId();
        String clientIp = getClientIp();
        String userAgent = getUserAgent();

        // 获取操作结果
        String operationResult = exception != null ? "FAILURE" : "SUCCESS";

        // 构建详细信息
        Map<String, Object> details = new HashMap<>();
        details.put("method", pjp.getSignature().getName());
        details.put("class", pjp.getTarget().getClass().getSimpleName());
        details.put("duration", duration);
        details.put("timestamp", Instant.now().toString());

        // 记录参数（如果启用）
        if (auditable.recordParams()) {
            try {
                Object[] args = pjp.getArgs();
                if (args != null && args.length > 0) {
                    Map<String, Object> params = new HashMap<>();
                    for (int i = 0; i < args.length; i++) {
                        // 过滤敏感参数
                        if (!isSensitiveParam(pjp.getSignature().getName(), i)) {
                            params.put("arg" + i, maskSensitiveData(args[i]));
                        }
                    }
                    details.put("params", params);
                }
            } catch (Exception e) {
                log.debug("记录参数失败", e);
            }
        }

        // 记录异常（如果启用）
        if (auditable.recordException() && exception != null) {
            details.put("error", exception.getClass().getSimpleName());
            details.put("errorMessage", exception.getMessage());
        }

        // 记录返回值（如果启用）
        if (auditable.recordResult() && result != null) {
            details.put("result", maskSensitiveData(result));
        }

        // 实体 ID（从参数中提取）
        String entityId = extractEntityId(pjp.getArgs());

        // 记录审计日志
        auditService.record(
                userId,
                eventType,
                resource,
                operationResult,
                clientIp,
                userAgent,
                pjp.getSignature().getName(),
                entityId,
                sessionId,
                details
        );
    }

    /**
     * 获取资源描述
     */
    private String getResource(ProceedingJoinPoint pjp, Auditable auditable) {
        if (!auditable.resource().isEmpty()) {
            return auditable.resource();
        }

        // 默认使用类名.方法名
        return String.format("%s.%s",
                pjp.getTarget().getClass().getSimpleName(),
                pjp.getSignature().getName());
    }

    /**
     * 获取当前用户 ID
     *
     * <p>优先从 Spring Security {@code SecurityContextHolder} 读取已认证用户名；
     * 若 Spring Security 未引入或上下文中无有效认证信息，则回退为 {@code "anonymous"}。
     */
    private String getCurrentUserId() {
        try {
            // 通过反射调用，避免对 spring-security-core 产生强编译依赖
            Class<?> holderClass = Class.forName(
                    "org.springframework.security.core.context.SecurityContextHolder");
            Object context = holderClass.getMethod("getContext").invoke(null);
            Object authentication = context.getClass().getMethod("getAuthentication").invoke(context);
            if (authentication != null) {
                Object principal = authentication.getClass().getMethod("getPrincipal").invoke(authentication);
                // AnonymousAuthenticationToken 的 principal 是字符串 "anonymousUser"
                if (principal instanceof String principalStr) {
                    return principalStr;
                }
                Object name = authentication.getClass().getMethod("getName").invoke(authentication);
                if (name instanceof String nameStr && !nameStr.isBlank()) {
                    return nameStr;
                }
            }
        } catch (ClassNotFoundException ignored) {
            // spring-security-core 未引入，正常降级
        } catch (Exception e) {
            log.debug("从 SecurityContextHolder 获取用户 ID 失败", e);
        }
        return "anonymous";
    }

    /**
     * 获取当前会话 ID
     *
     * <p>使用 {@code getSession(false)} 以避免为无会话的请求（如 REST API）
     * 无意创建新的 HTTP Session，从而防止内存泄漏与 Session 固化攻击面扩大。
     */
    private String getCurrentSessionId() {
        try {
            HttpServletRequest request = getRequest();
            if (request != null) {
                jakarta.servlet.http.HttpSession session = request.getSession(false);
                return session != null ? session.getId() : null;
            }
        } catch (Exception e) {
            log.debug("获取会话 ID 失败", e);
        }
        return null;
    }

    /**
     * 获取客户端 IP
     */
    private String getClientIp() {
        try {
            HttpServletRequest request = getRequest();
            if (request != null) {
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }

                String xRealIp = request.getHeader("X-Real-IP");
                if (xRealIp != null && !xRealIp.isEmpty()) {
                    return xRealIp;
                }

                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("获取客户端 IP 失败", e);
        }
        return "0.0.0.0";
    }

    /**
     * 获取用户代理
     */
    private String getUserAgent() {
        try {
            HttpServletRequest request = getRequest();
            if (request != null) {
                return request.getHeader("User-Agent");
            }
        } catch (Exception e) {
            log.debug("获取用户代理失败", e);
        }
        return "unknown";
    }

    /**
     * 获取当前请求
     */
    private HttpServletRequest getRequest() {
        try {
            return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 判断参数是否为敏感参数
     */
    private boolean isSensitiveParam(String methodName, int paramIndex) {
        // 过滤密码、令牌等敏感参数
        String lowerMethod = methodName.toLowerCase();
        return lowerMethod.contains("password") ||
               lowerMethod.contains("token") ||
               lowerMethod.contains("secret") ||
               lowerMethod.contains("key");
    }

    /**
     * 脱敏敏感数据
     */
    private Object maskSensitiveData(Object obj) {
        if (obj == null) {
            return null;
        }

        String str = obj.toString();

        // 对于密码、令牌等，直接返回固定值
        if (str.length() > 0 && (str.matches(".*password.*") ||
                                 str.matches(".*token.*") ||
                                 str.matches(".*secret.*"))) {
            return "***";
        }

        // 对于长度超过 100 字符的字符串，截取前 50 和后 50
        if (str.length() > 100) {
            return str.substring(0, 50) + "..." + str.substring(str.length() - 50);
        }

        return obj;
    }

    /**
     * 从参数中提取实体 ID
     */
    private String extractEntityId(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }

        // 尝试从第一个参数中提取 ID
        Object firstArg = args[0];
        if (firstArg != null) {
            try {
                // 通过反射查找 getId() 方法
                java.lang.reflect.Method idMethod = firstArg.getClass().getMethod("getId");
                Object id = idMethod.invoke(firstArg);
                return id != null ? id.toString() : null;
            } catch (Exception e) {
                // 忽略异常
            }
        }

        return null;
    }
}
