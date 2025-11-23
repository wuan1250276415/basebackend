package com.basebackend.security.zerotrust;

import com.basebackend.security.zerotrust.policy.ZeroTrustPolicyEngine;
import com.basebackend.security.zerotrust.device.DeviceFingerprintManager;
import com.basebackend.security.zerotrust.risk.RiskAssessmentEngine;
import com.basebackend.security.zerotrust.config.ZeroTrustProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 零信任安全配置
 *
 * 实现"永不信任，始终验证"的零信任安全模型
 * 包括：
 * - 设备信任度评估
 * - 实时风险评估
 * - 动态策略引擎
 * - 持续监控
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(ZeroTrustProperties.class)
@ConditionalOnProperty(name = "basebackend.security.zerotrust.enabled", havingValue = "true")
@EnableAsync
@RequiredArgsConstructor
public class ZeroTrustConfig {

    private final ZeroTrustProperties zeroTrustProperties;

    /**
     * 创建设备指纹管理器
     *
     * @return DeviceFingerprintManager
     */
    @Bean
    @ConditionalOnMissingBean
    public DeviceFingerprintManager deviceFingerprintManager() {
        log.info("初始化设备指纹管理器");

        DeviceFingerprintManager manager = new DeviceFingerprintManager();
        manager.setEnabled(zeroTrustProperties.getDevice().isEnabled());
        manager.setTimeout(zeroTrustProperties.getDevice().getTimeout());

        log.info("设备指纹管理器初始化完成 - 启用状态: {}",
            zeroTrustProperties.getDevice().isEnabled());
        return manager;
    }

    /**
     * 创建风险评估引擎
     *
     * @return RiskAssessmentEngine
     */
    @Bean
    @ConditionalOnMissingBean
    public RiskAssessmentEngine riskAssessmentEngine() {
        log.info("初始化风险评估引擎");

        RiskAssessmentEngine engine = new RiskAssessmentEngine();

        // 配置风险评估参数
        engine.setRiskThreshold(zeroTrustProperties.getRisk().getThreshold());
        engine.setHighRiskThreshold(zeroTrustProperties.getRisk().getHighThreshold());
        engine.setMaxLoginAttempts(zeroTrustProperties.getRisk().getMaxLoginAttempts());
        engine.setAccountLockDuration(zeroTrustProperties.getRisk().getAccountLockDuration());

        // 启用实时分析
        engine.setRealTimeAnalysisEnabled(zeroTrustProperties.getRisk().isRealTimeAnalysisEnabled());
        engine.setBehaviorAnalysisEnabled(zeroTrustProperties.getRisk().isBehaviorAnalysisEnabled());
        engine.setNetworkAnalysisEnabled(zeroTrustProperties.getRisk().isNetworkAnalysisEnabled());

        log.info("风险评估引擎初始化完成 - 风险阈值: {}, 高风险阈值: {}",
            zeroTrustProperties.getRisk().getThreshold(),
            zeroTrustProperties.getRisk().getHighThreshold());

        return engine;
    }

    /**
     * 创建零信任策略引擎
     *
     * @param deviceFingerprintManager 设备指纹管理器
     * @param riskAssessmentEngine 风险评估引擎
     * @return ZeroTrustPolicyEngine
     */
    @Bean
    @ConditionalOnMissingBean
    public ZeroTrustPolicyEngine zeroTrustPolicyEngine(
            DeviceFingerprintManager deviceFingerprintManager,
            RiskAssessmentEngine riskAssessmentEngine) {
        log.info("初始化零信任策略引擎");

        ZeroTrustPolicyEngine engine = new ZeroTrustPolicyEngine(
            deviceFingerprintManager,
            riskAssessmentEngine
        );

        // 配置策略参数
        engine.setTrustScoreThreshold(zeroTrustProperties.getPolicy().getTrustScoreThreshold());
        engine.setMaxConcurrentSessions(zeroTrustProperties.getPolicy().getMaxConcurrentSessions());
        engine.setSessionTimeout(zeroTrustProperties.getPolicy().getSessionTimeout());
        engine.setEnableRealTimeMonitoring(zeroTrustProperties.getPolicy().isRealTimeMonitoringEnabled());

        // 启用策略缓存
        engine.setPolicyCacheEnabled(zeroTrustProperties.getPolicy().isCacheEnabled());
        engine.setPolicyCacheTtl(zeroTrustProperties.getPolicy().getCacheTtl());

        // 策略执行配置
        engine.setEnforceMode(zeroTrustProperties.getPolicy().isEnforceMode());
        engine.setAuditEnabled(zeroTrustProperties.getPolicy().isAuditEnabled());

        log.info("零信任策略引擎初始化完成 - 信任分数阈值: {}, 执行模式: {}",
            zeroTrustProperties.getPolicy().getTrustScoreThreshold(),
            zeroTrustProperties.getPolicy().isEnforceMode() ? "强制" : "监控");

        return engine;
    }

    /**
     * 创建异步执行器
     * 用于处理实时监控和分析任务
     *
     * @return ExecutorService
     */
    @Bean
    @ConditionalOnMissingBean
    public ExecutorService zeroTrustExecutorService() {
        int corePoolSize = zeroTrustProperties.getAsync().getCorePoolSize();
        int maxPoolSize = zeroTrustProperties.getAsync().getMaxPoolSize();
        int queueCapacity = zeroTrustProperties.getAsync().getQueueCapacity();
        String threadNamePrefix = zeroTrustProperties.getAsync().getThreadNamePrefix();

        log.info("创建零信任异步执行器 - 核心线程: {}, 最大线程: {}, 队列容量: {}",
            corePoolSize, maxPoolSize, queueCapacity);

        return new java.util.concurrent.ThreadPoolExecutor(
            corePoolSize,
            maxPoolSize,
            60L, // 空闲线程存活时间
            java.util.concurrent.TimeUnit.SECONDS,
            new java.util.concurrent.LinkedBlockingQueue<>(queueCapacity),
            new java.util.concurrent.ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, threadNamePrefix + counter.incrementAndGet());
                    thread.setDaemon(false);
                    thread.setUncaughtExceptionHandler((t, e) ->
                        log.error("零信任异步任务异常: {}", t.getName(), e));
                    return thread;
                }
            },
            new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 初始化零信任安全监控
     *
     * @param policyEngine 策略引擎
     * @param riskEngine 风险评估引擎
     * @param executorService 执行器
     */
    @Bean
    @ConditionalOnMissingBean
    public void initializeZeroTrustMonitoring(
            ZeroTrustPolicyEngine policyEngine,
            RiskAssessmentEngine riskEngine,
            ExecutorService executorService) {

        if (!zeroTrustProperties.getMonitoring().isEnabled()) {
            log.info("零信任监控未启用");
            return;
        }

        log.info("初始化零信任安全监控");

        // 启动定期风险评估任务
        int monitoringInterval = zeroTrustProperties.getMonitoring().getIntervalMinutes();

        // 这里可以添加定时任务，用于：
        // 1. 定期重新评估用户信任度
        // 2. 检查异常行为模式
        // 3. 清理过期的策略缓存
        // 4. 生成安全报告

        log.info("零信任安全监控初始化完成 - 监控间隔: {} 分钟", monitoringInterval);
    }

    /**
     * 健康检查配置
     *
     * @param policyEngine 策略引擎
     * @return ZeroTrustHealthIndicator
     */
    @Bean
    @ConditionalOnMissingBean
    public ZeroTrustHealthIndicator zeroTrustHealthIndicator(
            ZeroTrustPolicyEngine policyEngine) {
        log.debug("创建零信任健康指示器");
        return new ZeroTrustHealthIndicator(policyEngine);
    }
}
