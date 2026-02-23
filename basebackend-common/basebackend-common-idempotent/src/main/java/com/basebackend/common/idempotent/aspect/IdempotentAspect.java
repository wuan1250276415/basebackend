package com.basebackend.common.idempotent.aspect;

import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.idempotent.annotation.Idempotent;
import com.basebackend.common.idempotent.config.IdempotentProperties;
import com.basebackend.common.idempotent.enums.IdempotentStrategy;
import com.basebackend.common.idempotent.exception.IdempotentException;
import com.basebackend.common.idempotent.store.IdempotentStore;
import com.basebackend.common.idempotent.token.IdempotentTokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * 幂等性 AOP 切面
 * <p>
 * 根据 {@link Idempotent} 注解的策略计算幂等 key，
 * 通过 {@link IdempotentStore} 进行幂等性检查。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class IdempotentAspect {

    private final IdempotentStore idempotentStore;
    private final IdempotentProperties properties;
    private final IdempotentTokenService idempotentTokenService;

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();
    private static final String IDEMPOTENT_KEY_PREFIX = "idempotent:";

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        if (!properties.isEnabled()) {
            return joinPoint.proceed();
        }

        String idempotentKey = buildIdempotentKey(joinPoint, idempotent);
        long timeout = idempotent.timeout() > 0 ? idempotent.timeout() : properties.getDefaultTimeout();

        boolean acquired = idempotentStore.tryAcquire(idempotentKey, timeout, idempotent.timeUnit());
        if (!acquired) {
            log.warn("幂等性检查失败, key={}", idempotentKey);
            throw new IdempotentException(idempotent.message());
        }

        log.debug("幂等性检查通过, key={}", idempotentKey);
        return joinPoint.proceed();
    }

    private String buildIdempotentKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        return switch (idempotent.strategy()) {
            case TOKEN -> buildTokenKey(idempotent);
            case PARAM -> buildParamKey(joinPoint);
            case SPEL -> buildSpelKey(joinPoint, idempotent);
        };
    }

    /**
     * TOKEN 策略：从请求 Header 中获取幂等 Token 并验证
     */
    private String buildTokenKey(Idempotent idempotent) {
        HttpServletRequest request = getHttpServletRequest();
        String token = request.getHeader(properties.getTokenHeader());
        if (token == null || token.isEmpty()) {
            throw new IdempotentException("缺少幂等Token，请先获取Token");
        }
        if (idempotentTokenService != null && !idempotentTokenService.validateAndConsume(token)) {
            throw new IdempotentException(idempotent.message());
        }
        return IDEMPOTENT_KEY_PREFIX + "token:" + token;
    }

    /**
     * PARAM 策略：基于请求参数 MD5 + 用户ID
     */
    private String buildParamKey(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringTypeName();
        String methodName = signature.getName();
        String argsHash = md5(Arrays.deepToString(joinPoint.getArgs()));
        Long userId = UserContextHolder.getUserId();
        String userPart = userId != null ? String.valueOf(userId) : "anonymous";

        return IDEMPOTENT_KEY_PREFIX + "param:" + className + "#" + methodName
                + ":" + userPart + ":" + argsHash;
    }

    /**
     * SPEL 策略：基于 SpEL 表达式
     */
    private String buildSpelKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        String spelExpression = idempotent.key();
        if (spelExpression.isEmpty()) {
            return buildParamKey(joinPoint);
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                joinPoint.getTarget(), method, joinPoint.getArgs(), NAME_DISCOVERER
        );

        Object value = PARSER.parseExpression(spelExpression).getValue(context);
        return IDEMPOTENT_KEY_PREFIX + "spel:" + (value != null ? value.toString() : "null");
    }

    private HttpServletRequest getHttpServletRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new IdempotentException("无法获取HTTP请求上下文");
        }
        return attrs.getRequest();
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // MD5 is always available in JDK
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }
}
