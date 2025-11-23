package com.basebackend.observability.slo.aspect;

import com.basebackend.observability.slo.annotation.SloMonitored;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
 * 拦截标记了 {@link SloMonitored} 注解的方法，自动采集 SLI 指标：
 * <ul>
 *     <li><b>sli_requests_total</b>：请求总数计数器
 *         <ul>
 *             <li>outcome=total：所有请求</li>
 *             <li>outcome=success：成功请求</li>
 *             <li>outcome=error：失败请求</li>
 *         </ul>
 *     </li>
 *     <li><b>sli_latency</b>：请求延迟计时器（纳秒）</li>
 * </ul>
 * </p>
 * <p>
 * 所有指标包含以下标签：
 * <ul>
 *     <li>service：服务名</li>
 *     <li>method：方法名（格式：类名.方法名）</li>
 *     <li>slo：SLO 名称</li>
 * </ul>
 * </p>
 * <p>
 * 切面保证监控失败不会影响业务逻辑执行，所有监控异常会被捕获并记录到日志。
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

    private final MeterRegistry meterRegistry;
    private final String applicationName;

    /**
     * 构造函数
     *
     * @param meterRegistry   Micrometer 指标注册表
     * @param applicationName 应用名称（从 spring.application.name 读取）
     */
    public SloMonitoringAspect(
            MeterRegistry meterRegistry,
            @Value("${spring.application.name:application}") String applicationName) {
        this.meterRegistry = meterRegistry;
        this.applicationName = applicationName;
        log.info("SLO 监控切面已启用: applicationName={}", applicationName);
    }

    /**
     * 环绕通知：监控标记了 @SloMonitored 的方法
     * <p>
     * 执行流程：
     * <ol>
     *     <li>解析注解参数（service, method, sloName）</li>
     *     <li>启动延迟计时（如果 recordLatency=true）</li>
     *     <li>增加 total 计数器</li>
     *     <li>执行目标方法</li>
     *     <li>根据结果增加 success 或 error 计数器</li>
     *     <li>停止延迟计时并记录</li>
     * </ol>
     * </p>
     *
     * @param joinPoint    连接点
     * @param sloMonitored SloMonitored 注解实例
     * @return 目标方法的返回值
     * @throws Throwable 目标方法抛出的异常
     */
    @Around("@annotation(sloMonitored)")
    public Object monitor(ProceedingJoinPoint joinPoint, SloMonitored sloMonitored) throws Throwable {
        // 1. 解析方法和 SLO 信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = resolveMethodName(signature);
        String sloName = resolveSloName(sloMonitored, methodName);
        String service = resolveService(sloMonitored);

        log.debug("开始 SLO 监控: service={}, method={}, slo={}", service, methodName, sloName);

        // 2. 启动延迟计时
        Timer.Sample sample = startTimerIfRequired(sloMonitored);

        // 3. 增加总请求计数
        incrementCounterSafely(service, methodName, sloName, "total");

        try {
            // 4. 执行目标方法
            Object result = joinPoint.proceed();

            // 5. 记录成功
            if (sloMonitored.recordSuccess()) {
                incrementCounterSafely(service, methodName, sloName, "success");
                log.debug("记录成功请求: service={}, method={}, slo={}", service, methodName, sloName);
            }

            return result;
        } catch (Throwable ex) {
            // 6. 记录错误
            if (sloMonitored.recordError()) {
                incrementCounterSafely(service, methodName, sloName, "error");
                log.debug("记录错误请求: service={}, method={}, slo={}, error={}",
                        service, methodName, sloName, ex.getClass().getSimpleName());
            }
            throw ex;
        } finally {
            // 7. 停止延迟计时
            stopTimerIfRequired(sample, service, methodName, sloName);
        }
    }

    /**
     * 启动延迟计时（如果启用）
     *
     * @param sloMonitored SloMonitored 注解
     * @return Timer.Sample 实例，如果未启用则返回 null
     */
    private Timer.Sample startTimerIfRequired(SloMonitored sloMonitored) {
        if (sloMonitored.recordLatency()) {
            try {
                return Timer.start(meterRegistry);
            } catch (Exception ex) {
                log.debug("启动 SLO 延迟计时失败", ex);
            }
        }
        return null;
    }

    /**
     * 停止延迟计时并记录（如果启用）
     *
     * @param sample  Timer.Sample 实例
     * @param service 服务名
     * @param method  方法名
     * @param sloName SLO 名称
     */
    private void stopTimerIfRequired(Timer.Sample sample, String service, String method, String sloName) {
        if (sample == null) {
            return;
        }
        try {
            Timer timer = Timer.builder("sli_latency")
                    .description("SLI 延迟计时器（纳秒）")
                    .tags("service", service, "method", method, "slo", sloName)
                    .register(meterRegistry);
            sample.stop(timer);
            log.trace("记录 SLO 延迟: service={}, method={}, slo={}", service, method, sloName);
        } catch (Exception ex) {
            log.debug("记录 SLO 延迟失败: service={}, method={}, slo={}", service, method, sloName, ex);
        }
    }

    /**
     * 安全地增加计数器
     * <p>
     * 捕获所有异常以避免影响业务逻辑。
     * </p>
     *
     * @param service 服务名
     * @param method  方法名
     * @param sloName SLO 名称
     * @param outcome 结果类型（total/success/error）
     */
    private void incrementCounterSafely(String service, String method, String sloName, String outcome) {
        try {
            Counter counter = Counter.builder("sli_requests_total")
                    .description("SLI 请求总数计数器")
                    .tags("service", service, "method", method, "slo", sloName, "outcome", outcome)
                    .register(meterRegistry);
            counter.increment();
            log.trace("记录 SLO 计数: service={}, method={}, slo={}, outcome={}",
                    service, method, sloName, outcome);
        } catch (Exception ex) {
            log.debug("记录 SLO 计数失败: service={}, method={}, slo={}, outcome={}",
                    service, method, sloName, outcome, ex);
        }
    }

    /**
     * 解析方法名（格式：类名.方法名）
     *
     * @param signature 方法签名
     * @return 方法名
     */
    private String resolveMethodName(MethodSignature signature) {
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }

    /**
     * 解析 SLO 名称
     * <p>
     * 如果注解中未指定 sloName，则使用方法名作为默认值。
     * </p>
     *
     * @param sloMonitored SloMonitored 注解
     * @param methodName   方法名
     * @return SLO 名称
     */
    private String resolveSloName(SloMonitored sloMonitored, String methodName) {
        if (sloMonitored == null) {
            return methodName;
        }
        String sloName = sloMonitored.sloName();
        return sloName == null || sloName.trim().isEmpty() ? methodName : sloName.trim();
    }

    /**
     * 解析服务名
     * <p>
     * 处理逻辑：
     * <ol>
     *     <li>如果注解中未指定 service，使用 applicationName</li>
     *     <li>如果 service 为占位符 ${spring.application.name}，使用 applicationName</li>
     *     <li>否则使用注解中指定的 service</li>
     * </ol>
     * </p>
     *
     * @param sloMonitored SloMonitored 注解
     * @return 服务名
     */
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
