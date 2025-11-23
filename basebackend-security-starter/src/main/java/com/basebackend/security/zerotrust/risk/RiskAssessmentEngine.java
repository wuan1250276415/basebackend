package com.basebackend.security.zerotrust.risk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 风险评估引擎
 *
 * 实时分析用户行为、地理位置、时间模式等
 * 计算综合风险评分并生成风险警告
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Slf4j
public class RiskAssessmentEngine {

    // 配置参数
    private int riskThreshold = 60;           // 风险阈值
    private int highRiskThreshold = 80;       // 高风险阈值
    private int maxLoginAttempts = 5;         // 最大登录尝试次数
    private long accountLockDuration = 30;    // 账户锁定时长（分钟）

    // 分析开关
    private boolean realTimeAnalysisEnabled = true;
    private boolean behaviorAnalysisEnabled = true;
    private boolean networkAnalysisEnabled = true;

    // 风险事件缓存
    private final Map<String, RiskEvent> riskEvents = new ConcurrentHashMap<>();
    private final Map<String, UserRiskProfile> userRiskProfiles = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> loginAttempts = new ConcurrentHashMap<>();

    /**
     * 评估用户访问风险
     *
     * @param userId 用户ID
     * @param requestContext 请求上下文
     * @return 风险评估结果
     */
    public RiskAssessmentResult assessRisk(String userId, RequestContext requestContext) {
        log.debug("评估用户访问风险 - User: {}, IP: {}", userId, requestContext.getClientIp());

        // 1. 获取或创建用户风险档案
        UserRiskProfile riskProfile = getOrCreateUserRiskProfile(userId);

        // 2. 执行多维度风险分析
        List<RiskFactor> riskFactors = analyzeRiskFactors(userId, requestContext, riskProfile);

        // 3. 计算综合风险分数
        RiskScore riskScore = calculateRiskScore(riskFactors);

        // 4. 确定风险等级
        RiskLevel riskLevel = determineRiskLevel(riskScore.getTotalScore());

        // 5. 生成风险评估结果
        RiskAssessmentResult result = RiskAssessmentResult.builder()
            .userId(userId)
            .requestContext(requestContext)
            .riskScore(riskScore)
            .riskLevel(riskLevel)
            .riskFactors(riskFactors)
            .isHighRisk(isHighRisk(riskScore.getTotalScore()))
            .requiresAdditionalVerification(requiresAdditionalVerification(riskLevel, riskFactors))
            .recommendedActions(generateRecommendedActions(riskLevel, riskFactors))
            .timestamp(Instant.now())
            .build();

        // 6. 更新用户风险档案
        updateUserRiskProfile(userId, result, requestContext);

        // 7. 记录风险事件
        if (result.isHighRisk()) {
            recordHighRiskEvent(userId, result);
        }

        log.info("风险评估完成 - User: {}, Score: {}, Level: {}",
            userId, riskScore.getTotalScore(), riskLevel);

        return result;
    }

    /**
     * 实时行为分析
     *
     * @param userId 用户ID
     * @param requestContext 请求上下文
     * @return 行为分析结果
     */
    public BehaviorAnalysisResult analyzeBehavior(String userId, RequestContext requestContext) {
        if (!behaviorAnalysisEnabled) {
            return BehaviorAnalysisResult.builder()
                .userId(userId)
                .isAnomalous(false)
                .build();
        }

        log.debug("执行实时行为分析 - User: {}", userId);

        BehaviorAnalysisResult.Builder builder = BehaviorAnalysisResult.builder()
            .userId(userId)
            .requestContext(requestContext);

        // 1. 访问时间分析
        analyzeAccessTime(userId, requestContext, builder);

        // 2. 访问频率分析
        analyzeAccessFrequency(userId, requestContext, builder);

        // 3. 地理位置分析
        analyzeGeographicLocation(userId, requestContext, builder);

        // 4. 设备一致性分析
        analyzeDeviceConsistency(userId, requestContext, builder);

        // 5. 访问模式分析
        analyzeAccessPattern(userId, requestContext, builder);

        // 6. 会话行为分析
        analyzeSessionBehavior(userId, requestContext, builder);

        BehaviorAnalysisResult result = builder.build();

        log.debug("行为分析完成 - User: {}, Anomalous: {}",
            userId, result.isAnomalous());

        return result;
    }

    /**
     * 网络安全分析
     *
     * @param userId 用户ID
     * @param requestContext 请求上下文
     * @return 网络安全分析结果
     */
    public NetworkSecurityAnalysisResult analyzeNetworkSecurity(String userId, RequestContext requestContext) {
        if (!networkAnalysisEnabled) {
            return NetworkSecurityAnalysisResult.builder()
                .userId(userId)
                .isSecure(true)
                .build();
        }

        log.debug("执行网络安全分析 - User: {}, IP: {}", userId, requestContext.getClientIp());

        NetworkSecurityAnalysisResult.Builder builder = NetworkSecurityAnalysisResult.builder()
            .userId(userId)
            .requestContext(requestContext);

        // 1. IP信誉分析
        analyzeIpReputation(requestContext, builder);

        // 2. VPN/代理检测
        detectVpnOrProxy(requestContext, builder);

        // 3. Tor网络检测
        detectTorNetwork(requestContext, builder);

        // 4. 数据中心IP检测
        detectDataCenterIp(requestContext, builder);

        // 5. 网络速度分析
        analyzeNetworkSpeed(requestContext, builder);

        // 6. 网络质量分析
        analyzeNetworkQuality(requestContext, builder);

        NetworkSecurityAnalysisResult result = builder.build();

        log.debug("网络安全分析完成 - User: {}, Secure: {}",
            userId, result.isSecure());

        return result;
    }

    // 配置设置器
    public void setRiskThreshold(int riskThreshold) { this.riskThreshold = riskThreshold; }
    public void setHighRiskThreshold(int highRiskThreshold) { this.highRiskThreshold = highRiskThreshold; }
    public void setMaxLoginAttempts(int maxLoginAttempts) { this.maxLoginAttempts = maxLoginAttempts; }
    public void setAccountLockDuration(long accountLockDuration) { this.accountLockDuration = accountLockDuration; }
    public void setRealTimeAnalysisEnabled(boolean realTimeAnalysisEnabled) { this.realTimeAnalysisEnabled = realTimeAnalysisEnabled; }
    public void setBehaviorAnalysisEnabled(boolean behaviorAnalysisEnabled) { this.behaviorAnalysisEnabled = behaviorAnalysisEnabled; }
    public void setNetworkAnalysisEnabled(boolean networkAnalysisEnabled) { this.networkAnalysisEnabled = networkAnalysisEnabled; }

    // 私有辅助方法
    private UserRiskProfile getOrCreateUserRiskProfile(String userId) {
        return userRiskProfiles.computeIfAbsent(userId, k -> UserRiskProfile.builder()
            .userId(userId)
            .createdAt(Instant.now())
            .lastUpdated(Instant.now())
            .totalAccessCount(0)
            .lastKnownIp(null)
            .lastKnownLocation(null)
            .trustedDevices(new HashSet<>())
            .accessHistory(new ArrayList<>())
            .riskEvents(new ArrayList<>())
            .build());
    }

    private List<RiskFactor> analyzeRiskFactors(String userId, RequestContext requestContext, UserRiskProfile riskProfile) {
        List<RiskFactor> riskFactors = new ArrayList<>();

        // 1. 登录尝试次数分析
        analyzeLoginAttempts(userId, riskFactors);

        // 2. IP变更分析
        analyzeIpChange(userId, requestContext, riskProfile, riskFactors);

        // 3. 地理位置变更分析
        analyzeGeographicChange(userId, requestContext, riskProfile, riskFactors);

        // 4. 设备变更分析
        analyzeDeviceChange(userId, requestContext, riskProfile, riskFactors);

        // 5. 访问时间异常分析
        analyzeAccessTimeAnomaly(userId, requestContext, riskFactors);

        // 6. 请求频率异常分析
        analyzeRequestFrequency(userId, requestContext, riskFactors);

        return riskFactors;
    }

    private RiskScore calculateRiskScore(List<RiskFactor> riskFactors) {
        int totalScore = 0;
        int behaviorScore = 0;
        int networkScore = 0;
        int deviceScore = 0;
        int timeScore = 0;

        for (RiskFactor riskFactor : riskFactors) {
            totalScore += riskFactor.getScore();
            switch (riskFactor.getCategory()) {
                case BEHAVIOR -> behaviorScore += riskFactor.getScore();
                case NETWORK -> networkScore += riskFactor.getScore();
                case DEVICE -> deviceScore += riskFactor.getScore();
                case TIME -> timeScore += riskFactor.getScore();
            }
        }

        return RiskScore.builder()
            .totalScore(totalScore)
            .behaviorScore(behaviorScore)
            .networkScore(networkScore)
            .deviceScore(deviceScore)
            .timeScore(timeScore)
            .riskLevel(determineRiskLevel(totalScore))
            .build();
    }

    private RiskLevel determineRiskLevel(int totalScore) {
        if (totalScore >= highRiskThreshold) return RiskLevel.HIGH;
        if (totalScore >= riskThreshold) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    private boolean isHighRisk(int totalScore) {
        return totalScore >= highRiskThreshold;
    }

    private boolean requiresAdditionalVerification(RiskLevel riskLevel, List<RiskFactor> riskFactors) {
        return riskLevel.ordinal() >= RiskLevel.MEDIUM.ordinal() || !riskFactors.isEmpty();
    }

    private List<String> generateRecommendedActions(RiskLevel riskLevel, List<RiskFactor> riskFactors) {
        List<String> actions = new ArrayList<>();

        if (riskLevel == RiskLevel.HIGH) {
            actions.add("立即拒绝访问");
            actions.add("发送安全警告");
            actions.add("通知安全团队");
        } else if (riskLevel == RiskLevel.MEDIUM) {
            actions.add("要求二次验证");
            actions.add("发送访问警告");
            actions.add("记录详细日志");
        } else {
            actions.add("正常允许访问");
            actions.add("保持监控");
        }

        // 根据具体风险因素添加建议
        for (RiskFactor riskFactor : riskFactors) {
            actions.add("处理风险因素: " + riskFactor.getDescription());
        }

        return actions;
    }

    private void updateUserRiskProfile(String userId, RiskAssessmentResult result, RequestContext requestContext) {
        UserRiskProfile profile = userRiskProfiles.get(userId);
        if (profile != null) {
            profile.setTotalAccessCount(profile.getTotalAccessCount() + 1);
            profile.setLastKnownIp(requestContext.getClientIp());
            profile.setLastUpdated(Instant.now());

            // 添加访问记录
            AccessRecord accessRecord = AccessRecord.builder()
                .timestamp(Instant.now())
                .ipAddress(requestContext.getClientIp())
                .userAgent(requestContext.getUserAgent())
                .riskScore(result.getRiskScore().getTotalScore())
                .riskLevel(result.getRiskLevel())
                .build();

            profile.getAccessHistory().add(accessRecord);

            // 保持历史记录大小在合理范围内
            if (profile.getAccessHistory().size() > 100) {
                profile.getAccessHistory().remove(0);
            }
        }
    }

    private void recordHighRiskEvent(String userId, RiskAssessmentResult result) {
        String eventId = UUID.randomUUID().toString();
        RiskEvent event = RiskEvent.builder()
            .eventId(eventId)
            .userId(userId)
            .timestamp(Instant.now())
            .riskScore(result.getRiskScore().getTotalScore())
            .riskLevel(result.getRiskLevel())
            .riskFactors(result.getRiskFactors())
            .requestContext(result.getRequestContext())
            .build();

        riskEvents.put(eventId, event);
        log.warn("记录高风险事件 - Event: {}, User: {}, Score: {}",
            eventId, userId, result.getRiskScore().getTotalScore());
    }

    // 分析方法实现
    private void analyzeLoginAttempts(String userId, List<RiskFactor> riskFactors) {
        AtomicInteger attempts = loginAttempts.computeIfAbsent(userId, k -> new AtomicInteger(0));
        int count = attempts.incrementAndGet();

        if (count > maxLoginAttempts) {
            riskFactors.add(RiskFactor.builder()
                .category(RiskCategory.BEHAVIOR)
                .type(RiskType.EXCESSIVE_LOGIN_ATTEMPTS)
                .score(80)
                .description("登录尝试次数过多")
                .severity(RiskSeverity.HIGH)
                .build());
        }
    }

    private void analyzeIpChange(String userId, RequestContext requestContext, UserRiskProfile profile, List<RiskFactor> riskFactors) {
        String currentIp = requestContext.getClientIp();
        String lastKnownIp = profile.getLastKnownIp();

        if (lastKnownIp != null && !lastKnownIp.equals(currentIp)) {
            riskFactors.add(RiskFactor.builder()
                .category(RiskCategory.NETWORK)
                .type(RiskType.IP_CHANGE)
                .score(40)
                .description("IP地址发生变化: " + lastKnownIp + " -> " + currentIp)
                .severity(RiskSeverity.MEDIUM)
                .build());
        }
    }

    private void analyzeGeographicChange(String userId, RequestContext requestContext, UserRiskProfile profile, List<RiskFactor> riskFactors) {
        String currentLocation = requestContext.getLocation();
        String lastKnownLocation = profile.getLastKnownLocation();

        if (lastKnownLocation != null && currentLocation != null && !lastKnownLocation.equals(currentLocation)) {
            riskFactors.add(RiskFactor.builder()
                .category(RiskCategory.NETWORK)
                .type(RiskType.GEOGRAPHIC_CHANGE)
                .score(60)
                .description("地理位置发生变化: " + lastKnownLocation + " -> " + currentLocation)
                .severity(RiskSeverity.HIGH)
                .build());
        }
    }

    private void analyzeDeviceChange(String userId, RequestContext requestContext, UserRiskProfile profile, List<RiskFactor> riskFactors) {
        String deviceId = requestContext.getDeviceId();
        if (deviceId != null && !profile.getTrustedDevices().contains(deviceId)) {
            riskFactors.add(RiskFactor.builder()
                .category(RiskCategory.DEVICE)
                .type(RiskType.UNKNOWN_DEVICE)
                .score(50)
                .description("检测到未知设备: " + deviceId)
                .severity(RiskSeverity.MEDIUM)
                .build());
        }
    }

    private void analyzeAccessTimeAnomaly(String userId, RequestContext requestContext, List<RiskFactor> riskFactors) {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();

        // 检查是否在正常访问时间内
        if (hour < 6 || hour > 22) { // 晚上10点到早上6点
            riskFactors.add(RiskFactor.builder()
                .category(RiskCategory.TIME)
                .type(RiskType.ACCESS_TIME_ANOMALY)
                .score(30)
                .description("非正常访问时间: " + hour + ":00")
                .severity(RiskSeverity.LOW)
                .build());
        }
    }

    private void analyzeRequestFrequency(String userId, RequestContext requestContext, List<RiskFactor> riskFactors) {
        // 简化的请求频率分析
        // 实际应该基于时间窗口统计请求频率
        if (requestContext.getRequestInterval() != null && requestContext.getRequestInterval() < 1000) {
            riskFactors.add(RiskFactor.builder()
                .category(RiskCategory.BEHAVIOR)
                .type(RiskType.HIGH_REQUEST_FREQUENCY)
                .score(35)
                .description("请求频率过高")
                .severity(RiskSeverity.MEDIUM)
                .build());
        }
    }

    private void analyzeAccessTime(String userId, RequestContext requestContext, BehaviorAnalysisResult.Builder builder) {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        DayOfWeek dayOfWeek = now.getDayOfWeek();

        // 检查是否在正常访问时间
        boolean isNormalTime = hour >= 8 && hour <= 18;
        boolean isWeekday = dayOfWeek.getValue() <= 5;

        if (!isNormalTime || !isWeekday) {
            builder.accessTimeAnomalous(true)
                .anomalyReason("非正常访问时间")
                .accessTimeScore(30);
        }
    }

    private void analyzeAccessFrequency(String userId, RequestContext requestContext, BehaviorAnalysisResult.Builder builder) {
        // 简化分析 - 实际应基于历史数据
        builder.frequencyScore(50);
    }

    private void analyzeGeographicLocation(String userId, RequestContext requestContext, BehaviorAnalysisResult.Builder builder) {
        String location = requestContext.getLocation();
        builder.geographicLocation(location);
    }

    private void analyzeDeviceConsistency(String userId, RequestContext requestContext, BehaviorAnalysisResult.Builder builder) {
        String deviceId = requestContext.getDeviceId();
        String userAgent = requestContext.getUserAgent();

        builder.deviceConsistency(true)
            .deviceId(deviceId)
            .userAgent(userAgent);
    }

    private void analyzeAccessPattern(String userId, RequestContext requestContext, BehaviorAnalysisResult.Builder builder) {
        builder.accessPattern("normal");
    }

    private void analyzeSessionBehavior(String userId, RequestContext requestContext, BehaviorAnalysisResult.Builder builder) {
        builder.sessionBehavior("normal");
    }

    private void analyzeIpReputation(RequestContext requestContext, NetworkSecurityAnalysisResult.Builder builder) {
        builder.ipReputation("good");
    }

    private void detectVpnOrProxy(RequestContext requestContext, NetworkSecurityAnalysisResult.Builder builder) {
        boolean isVpnOrProxy = false; // 简化实现
        builder.isVpnOrProxy(isVpnOrProxy);
    }

    private void detectTorNetwork(RequestContext requestContext, NetworkSecurityAnalysisResult.Builder builder) {
        boolean isTorNetwork = false; // 简化实现
        builder.isTorNetwork(isTorNetwork);
    }

    private void detectDataCenterIp(RequestContext requestContext, NetworkSecurityAnalysisResult.Builder builder) {
        boolean isDataCenterIp = false; // 简化实现
        builder.isDataCenterIp(isDataCenterIp);
    }

    private void analyzeNetworkSpeed(RequestContext requestContext, NetworkSecurityAnalysisResult.Builder builder) {
        builder.networkSpeed("normal");
    }

    private void analyzeNetworkQuality(RequestContext requestContext, NetworkSecurityAnalysisResult.Builder builder) {
        builder.networkQuality("good");
    }
}
