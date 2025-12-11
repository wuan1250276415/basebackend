package com.basebackend.featuretoggle.audit;

import com.basebackend.featuretoggle.model.FeatureContext;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * 特性开关审计服务
 * <p>
 * 用于记录特性开关的访问日志和审计信息。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class FeatureToggleAuditService {

    /**
     * 最大记录数
     */
    private static final int MAX_RECORDS = 10000;

    /**
     * 审计记录存储
     */
    private final ConcurrentLinkedDeque<AuditRecord> records = new ConcurrentLinkedDeque<>();

    /**
     * 记录特性开关访问
     *
     * @param featureName   特性名称
     * @param operationType 操作类型（如 FEATURE_TOGGLE, GRADUAL_ROLLOUT, AB_TEST）
     * @param result        访问结果（是否启用）
     * @param context       特性上下文
     */
    public void recordAccess(String featureName, String operationType, boolean result, FeatureContext context) {
        AuditRecord record = new AuditRecord();
        record.setFeatureName(featureName);
        record.setOperationType(operationType);
        record.setResult(result);
        record.setTimestamp(LocalDateTime.now());

        if (context != null) {
            record.setUserId(context.getUserId());
            record.setSessionId(context.getSessionId());
            record.setIpAddress(context.getIpAddress());
        }

        addRecord(record);
        log.debug("Recorded access: feature={}, type={}, result={}, user={}",
                featureName, operationType, result, record.getUserId());
    }

    /**
     * 记录带变体信息的特性开关访问
     *
     * @param featureName   特性名称
     * @param operationType 操作类型
     * @param result        访问结果
     * @param context       特性上下文
     * @param variantName   变体名称
     */
    public void recordAccessWithVariant(String featureName, String operationType, boolean result,
            FeatureContext context, String variantName) {
        AuditRecord record = new AuditRecord();
        record.setFeatureName(featureName);
        record.setOperationType(operationType);
        record.setResult(result);
        record.setVariantName(variantName);
        record.setTimestamp(LocalDateTime.now());

        if (context != null) {
            record.setUserId(context.getUserId());
            record.setSessionId(context.getSessionId());
            record.setIpAddress(context.getIpAddress());
        }

        addRecord(record);
        log.debug("Recorded access with variant: feature={}, type={}, result={}, variant={}, user={}",
                featureName, operationType, result, variantName, record.getUserId());
    }

    /**
     * 记录配置变更
     *
     * @param featureName 特性名称
     * @param changeType  变更类型
     * @param oldValue    旧值
     * @param newValue    新值
     * @param operator    操作人
     */
    public void recordConfigChange(String featureName, String changeType, String oldValue,
            String newValue, String operator) {
        AuditRecord record = new AuditRecord();
        record.setFeatureName(featureName);
        record.setOperationType("CONFIG_CHANGE");
        record.setChangeType(changeType);
        record.setOldValue(oldValue);
        record.setNewValue(newValue);
        record.setOperator(operator);
        record.setTimestamp(LocalDateTime.now());

        addRecord(record);
        log.info("Recorded config change: feature={}, changeType={}, oldValue={}, newValue={}, operator={}",
                featureName, changeType, oldValue, newValue, operator);
    }

    /**
     * 获取最近的审计记录
     *
     * @param limit 限制数量
     * @return 审计记录列表
     */
    public List<AuditRecord> getRecentRecords(int limit) {
        return records.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 按特性名称获取审计记录
     *
     * @param featureName 特性名称
     * @param limit       限制数量
     * @return 审计记录列表
     */
    public List<AuditRecord> getRecordsByFeature(String featureName, int limit) {
        return records.stream()
                .filter(r -> featureName.equals(r.getFeatureName()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 获取统计信息
     *
     * @return 审计统计信息
     */
    public AuditStatistics getStatistics() {
        AuditStatistics stats = new AuditStatistics();

        List<AuditRecord> allRecords = new ArrayList<>(records);
        stats.setTotalRecords(allRecords.size());

        stats.setEnabledCount((int) allRecords.stream().filter(AuditRecord::isResult).count());
        stats.setDisabledCount((int) allRecords.stream().filter(r -> !r.isResult()).count());

        stats.setFeatureToggleCount((int) allRecords.stream()
                .filter(r -> "FEATURE_TOGGLE".equals(r.getOperationType())).count());
        stats.setGradualRolloutCount((int) allRecords.stream()
                .filter(r -> "GRADUAL_ROLLOUT".equals(r.getOperationType())).count());
        stats.setAbTestCount((int) allRecords.stream()
                .filter(r -> "AB_TEST".equals(r.getOperationType())).count());
        stats.setConfigChangeCount((int) allRecords.stream()
                .filter(r -> "CONFIG_CHANGE".equals(r.getOperationType())).count());

        return stats;
    }

    /**
     * 清空所有记录
     */
    public void clearRecords() {
        records.clear();
        log.info("Audit records cleared");
    }

    /**
     * 添加记录（内部方法）
     */
    private void addRecord(AuditRecord record) {
        records.addFirst(record);

        // 限制最大记录数
        while (records.size() > MAX_RECORDS) {
            records.removeLast();
        }
    }

    /**
     * 审计记录
     */
    @Data
    public static class AuditRecord {
        /**
         * 特性名称
         */
        private String featureName;

        /**
         * 操作类型
         */
        private String operationType;

        /**
         * 结果（是否启用）
         */
        private boolean result;

        /**
         * 变体名称（A/B测试）
         */
        private String variantName;

        /**
         * 用户ID
         */
        private String userId;

        /**
         * 会话ID
         */
        private String sessionId;

        /**
         * IP地址
         */
        private String ipAddress;

        /**
         * 变更类型（配置变更时）
         */
        private String changeType;

        /**
         * 旧值
         */
        private String oldValue;

        /**
         * 新值
         */
        private String newValue;

        /**
         * 操作人
         */
        private String operator;

        /**
         * 时间戳
         */
        private LocalDateTime timestamp;
    }

    /**
     * 审计统计信息
     */
    @Data
    public static class AuditStatistics {
        /**
         * 总记录数
         */
        private int totalRecords;

        /**
         * 启用次数
         */
        private int enabledCount;

        /**
         * 禁用次数
         */
        private int disabledCount;

        /**
         * 特性开关调用次数
         */
        private int featureToggleCount;

        /**
         * 灰度发布调用次数
         */
        private int gradualRolloutCount;

        /**
         * A/B测试调用次数
         */
        private int abTestCount;

        /**
         * 配置变更次数
         */
        private int configChangeCount;
    }
}
