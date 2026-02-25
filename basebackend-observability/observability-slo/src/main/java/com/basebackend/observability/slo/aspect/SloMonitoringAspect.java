package com.basebackend.observability.slo.aspect;

import com.basebackend.observability.slo.annotation.SloMonitored;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * SLO 监控 AOP 切面
 * <p>
 * 使用 Observation API 拦截标记了 {@link SloMonitored} 注解的方法，
 * 自动采集 SLI 指标（延迟、请求计数、成功/失败统计）。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 * @see SloMonitored
 */
@Aspect
@Component
@ConditionalOnProperty(prefix = "observability.slo", name = "enabled", havingValue = "true")
public class SloMonitoringAspect {

    private static final Logger log = LoggerFactory.getLogger(SloMonitoringAspect.class);
    private static final String DEFAULT_SERVICE_PLACEHOLDER = "${spring.application.name}";

    private final ObservationRegistry observationRegistry;
    private final String applicationName;

    public SloMonitoringAspect(
            ObservationRegistry observationRegistry,
            @Value("${spring.application.name:application}") String applicationName) {
        this.observationRegistry = observationRegistry;
        this.applicationName = applicationName;
        log.info("SLO 监控切面已启用: applicationName={}", applicationName);
    }

    @Around("@annotation(sloMonitored)")
    public Object monitor(ProceedingJoinPoint joinPoint, SloMonitored sloMonitored) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = resolveMethodName(signature);
        String sloName = resolveSloName(sloMonitored, methodName);
        String service = resolveService(sloMonitored);

        log.debug("开始 SLO 监控: service={}, method={}, slo={}", service, methodName, sloName);

        Observation observation = Observation.createNotStarted("sli", observationRegistry)
                .lowCardinalityKeyValue("service", service)
                .lowCardinalityKeyValue("method", methodName)
                .lowCardinalityKeyValue("slo", sloName)
                .contextualName("sli." + sloName);

        return observation.observeChecked(() -> joinPoint.proceed());
    }

    private String resolveMethodName(MethodSignature signature) {
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }

    private String resolveSloName(SloMonitored sloMonitored, String methodName) {
        if (sloMonitored == null) {
            return methodName;
        }
        String sloName = sloMonitored.sloName();
        return sloName == null || sloName.trim().isEmpty() ? methodName : sloName.trim();
    }

    private String resolveService(SloMonitored sloMonitored) {
        if (sloMonitored == null) {
            return applicationName;
        }
        String service = sloMonitored.service();
        if (service == null || service.trim().isEmpty() || DEFAULT_SERVICE_PLACEHOLDER.equals(service)) {
            return applicationName;
        }
        return service.trim();
    }
}
