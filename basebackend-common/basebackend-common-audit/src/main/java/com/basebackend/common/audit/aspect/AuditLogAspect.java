package com.basebackend.common.audit.aspect;

import com.basebackend.common.audit.AuditEvent;
import com.basebackend.common.audit.AuditEventPublisher;
import com.basebackend.common.audit.AuditLog;
import com.basebackend.common.util.JsonUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;

@Aspect
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditEventPublisher auditEventPublisher;
    @Lazy @Nullable
    private final HttpServletRequest httpServletRequest;

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        try {
            result = joinPoint.proceed();
            return result;
        } finally {
            long duration = System.currentTimeMillis() - start;
            publishEvent(joinPoint, auditLog, result, duration);
        }
    }

    private void publishEvent(ProceedingJoinPoint joinPoint, AuditLog auditLog, Object result, long duration) {
        try {
            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            String desc = resolveDescription(auditLog.description(), method, joinPoint.getArgs());
            String operator = resolveOperator();
            String ip = resolveIp();
            String params = auditLog.recordParams() ? serializeSafe(joinPoint.getArgs()) : null;
            String resultJson = auditLog.recordResult() ? serializeSafe(result) : null;

            AuditEvent event = AuditEvent.builder(this)
                    .module(auditLog.module())
                    .action(auditLog.action())
                    .description(desc)
                    .operator(operator)
                    .operatorIp(ip)
                    .params(params)
                    .result(resultJson)
                    .duration(duration)
                    .build();

            auditEventPublisher.publish(event);
        } catch (Exception ignored) {
            // audit failure must not affect business logic
        }
    }

    public String resolveDescription(String template, Method method, Object[] args) {
        if (template == null || template.isEmpty() || !template.contains("#")) {
            return template;
        }
        try {
            EvaluationContext ctx = buildEvaluationContext(method, args);
            return PARSER.parseExpression(template, new org.springframework.expression.common.TemplateParserContext()).getValue(ctx, String.class);
        } catch (Exception e) {
            return template;
        }
    }

    private EvaluationContext buildEvaluationContext(Method method, Object[] args) {
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        String[] paramNames = NAME_DISCOVERER.getParameterNames(method);
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                ctx.setVariable(paramNames[i], args[i]);
            }
        }
        return ctx;
    }

    private String resolveOperator() {
        try {
            Class<?> holderClass = Class.forName("com.basebackend.common.context.UserContextHolder");
            Object username = holderClass.getMethod("getUsername").invoke(null);
            return username != null ? username.toString() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String resolveIp() {
        try {
            if (httpServletRequest != null) {
                String xff = httpServletRequest.getHeader("X-Forwarded-For");
                if (xff != null && !xff.isEmpty()) {
                    return xff.split(",")[0].trim();
                }
                return httpServletRequest.getRemoteAddr();
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }

    private String serializeSafe(Object obj) {
        try {
            return JsonUtils.toJsonString(obj);
        } catch (Exception e) {
            return "serialize_error";
        }
    }
}
