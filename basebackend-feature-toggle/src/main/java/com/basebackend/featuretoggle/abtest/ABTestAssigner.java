package com.basebackend.featuretoggle.abtest;

import com.basebackend.featuretoggle.model.FeatureContext;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AB测试分组分配器
 * <p>
 * 负责将用户分配到不同的AB测试分组中，确保：
 * <ul>
 *   <li>一致性 - 同一用户始终分配到相同分组</li>
 *   <li>权重控制 - 支持按百分比分配用户</li>
 *   <li>实验隔离 - 不同实验互不影响</li>
 *   <li>动态调整 - 支持运行时调整权重</li>
 * </ul>
 * </p>
 *
 * <h3>分配策略：</h3>
 * <ul>
 *   <li>单用户单实验 - 每个用户在单次实验中只能分配到一个分组</li>
 *   <li>权重随机 - 基于用户ID哈希的伪随机分配</li>
 *   <li>持久化 - 用户分组记录可持久化以保证一致性</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ABTestAssigner {

    /**
     * AB测试分组配置
     */
    public static class Group {
        private final String name;
        private final double weight;
        private final Map<String, Object> properties;

        private Group(String name, double weight, Map<String, Object> properties) {
            this.name = name;
            this.weight = weight;
            this.properties = properties != null ? properties : Collections.emptyMap();
        }

        public static Group of(String name, double weight) {
            return new Group(name, weight, null);
        }

        public static Group of(String name, double weight, Map<String, Object> properties) {
            return new Group(name, weight, properties);
        }

        public String getName() {
            return name;
        }

        public double getWeight() {
            return weight;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        @Override
        public String toString() {
            return String.format("Group{name='%s', weight=%.2f}", name, weight);
        }
    }

    /**
     * 分配结果
     */
    public static class Assignment {
        private final String groupName;
        private final double weight;
        private final long hash;
        private final int percentileBucket;

        private Assignment(String groupName, double weight, long hash, int percentileBucket) {
            this.groupName = groupName;
            this.weight = weight;
            this.hash = hash;
            this.percentileBucket = percentileBucket;
        }

        public static Assignment of(String groupName, double weight, long hash, int percentileBucket) {
            return new Assignment(groupName, weight, hash, percentileBucket);
        }

        public String getGroupName() {
            return groupName;
        }

        public double getWeight() {
            return weight;
        }

        public long getHash() {
            return hash;
        }

        public int getPercentileBucket() {
            return percentileBucket;
        }

        @Override
        public String toString() {
            return String.format("Assignment{group='%s', weight=%.2f, hash=%d, bucket=%d}",
                    groupName, weight, hash, percentileBucket);
        }
    }

    /**
     * 为用户分配AB测试分组
     * <p>
     * 使用MurmurHash3算法进行一致性分配。
     * </p>
     *
     * @param featureName 特性名称（实验ID）
     * @param context 用户上下文
     * @param groups 分组列表
     * @return 分配结果
     */
    public static Assignment assignGroup(String featureName, FeatureContext context, List<Group> groups) {
        if (featureName == null || featureName.trim().isEmpty()) {
            throw new IllegalArgumentException("Feature name cannot be null or empty");
        }

        if (groups == null || groups.isEmpty()) {
            throw new IllegalArgumentException("Groups cannot be null or empty");
        }

        // 验证权重总和
        double totalWeight = groups.stream().mapToDouble(Group::getWeight).sum();
        if (Math.abs(totalWeight - 100.0) > 0.01) {
            log.warn("Total weight is {}%, not 100%. Proceeding with normalization.", totalWeight);
        }

        // 标准化权重
        Map<String, Double> normalizedWeights = normalizeWeights(groups);
        log.debug("Normalized weights: {}", normalizedWeights);

        // 生成一致性哈希键
        String hashKey = generateHashKey(featureName, context);
        long hash = HashAlgorithm.murmur3_32(hashKey);
        int bucket = HashAlgorithm.toPercentileBucket(hash);

        log.debug("Assignment for feature '{}', context: {}, hash: {}, bucket: {}",
                featureName, context, hash, bucket);

        // 根据桶索引分配分组
        String assignedGroup = assignByBucket(bucket, normalizedWeights);
        double weight = normalizedWeights.getOrDefault(assignedGroup, 0.0);

        return Assignment.of(assignedGroup, weight, hash, bucket);
    }

    /**
     * 分配对照组分组
     * <p>
     * 特殊处理：对照组权重表示"不启用新特性"的用户比例。
     * 实验组权重表示"启用新特性"的用户比例。
     * </p>
     *
     * @param featureName 特性名称
     * @param context 用户上下文
     * @param controlWeight 对照组权重百分比 (0-100)
     * @return true-启用新特性，false-使用对照组
     */
    public static boolean assignToVariant(String featureName, FeatureContext context, double controlWeight) {
        if (controlWeight < 0 || controlWeight > 100) {
            throw new IllegalArgumentException("Control weight must be between 0 and 100");
        }

        String hashKey = generateHashKey(featureName, context);
        long hash = HashAlgorithm.murmur3_32(hashKey);
        int bucket = HashAlgorithm.toPercentileBucket(hash);

        // 桶索引 < 对照组权重 -> 使用对照组
        boolean isControlGroup = bucket < controlWeight;

        log.debug("Variant assignment for feature '{}': {}, bucket={}, controlWeight={}",
                featureName, isControlGroup ? "control" : "variant", bucket, controlWeight);

        return !isControlGroup; // 返回是否启用新特性
    }

    /**
     * 检查用户是否在指定分组中
     * <p>
     * 用于快速判断当前用户是否属于某个特定分组。
     * </p>
     *
     * @param featureName 特性名称
     * @param context 用户上下文
     * @param groupName 分组名称
     * @param groups 所有分组
     * @return 是否在指定分组
     */
    public static boolean isInGroup(String featureName, FeatureContext context,
                                   String groupName, List<Group> groups) {
        Assignment assignment = assignGroup(featureName, context, groups);
        return groupName.equals(assignment.getGroupName());
    }

    /**
     * 检查用户是否启用新特性（对照/实验）
     * <p>
     * 简化版的对照/实验分配。
     * </p>
     *
     * @param featureName 特性名称
     * @param context 用户上下文
     * @param variantWeight 实验组权重百分比
     * @return 是否在实验组
     */
    public static boolean isInVariant(String featureName, FeatureContext context, double variantWeight) {
        return assignToVariant(featureName, context, 100 - variantWeight);
    }

    /**
     * 标准化权重
     */
    private static Map<String, Double> normalizeWeights(List<Group> groups) {
        double totalWeight = groups.stream().mapToDouble(Group::getWeight).sum();
        if (totalWeight <= 0) {
            throw new IllegalArgumentException("Total weight must be greater than 0");
        }

        return groups.stream()
                .collect(Collectors.toMap(
                        Group::getName,
                        g -> g.getWeight() / totalWeight * 100.0
                ));
    }

    /**
     * 生成一致性哈希键
     */
    private static String generateHashKey(String featureName, FeatureContext context) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(featureName);

        // 使用用户ID作为主要标识
        if (context != null && context.getUserId() != null) {
            keyBuilder.append(":").append(context.getUserId());
        } else if (context != null && context.getTenantId() != null) {
            // 租户级别分配
            keyBuilder.append(":tenant:").append(context.getTenantId());
        } else if (context != null && context.getSessionId() != null) {
            // 会话级别分配
            keyBuilder.append(":session:").append(context.getSessionId());
        } else {
            // IP级别分配（最低优先级）
            keyBuilder.append(":ip:").append(
                    context != null && context.getIpAddress() != null
                            ? context.getIpAddress() : "anonymous"
            );
        }

        // 添加环境标识，确保不同环境隔离
        if (context != null && context.getEnvironment() != null) {
            keyBuilder.append(":env:").append(context.getEnvironment());
        }

        String hashKey = keyBuilder.toString();
        log.trace("Generated hash key: {}", hashKey);
        return hashKey;
    }

    /**
     * 根据桶索引分配分组
     */
    private static String assignByBucket(int bucket, Map<String, Double> weights) {
        double cumulativeWeight = 0.0;

        // 按权重从小到大排序
        List<Map.Entry<String, Double>> sortedGroups = weights.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toList());

        for (Map.Entry<String, Double> entry : sortedGroups) {
            cumulativeWeight += entry.getValue();
            if (bucket < cumulativeWeight) {
                return entry.getKey();
            }
        }

        // 兜底：返回权重最大的分组
        return sortedGroups.get(sortedGroups.size() - 1).getKey();
    }

    /**
     * 验证分组配置的合法性
     *
     * @param groups 分组列表
     */
    public static void validateGroups(List<Group> groups) {
        if (groups == null || groups.isEmpty()) {
            throw new IllegalArgumentException("Groups cannot be null or empty");
        }

        // 检查分组名称唯一性
        Set<String> names = new HashSet<>();
        for (Group group : groups) {
            if (group.getName() == null || group.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Group name cannot be null or empty");
            }
            if (names.contains(group.getName())) {
                throw new IllegalArgumentException("Duplicate group name: " + group.getName());
            }
            names.add(group.getName());

            if (group.getWeight() < 0) {
                throw new IllegalArgumentException("Group weight cannot be negative: " + group.getName());
            }
        }

        // 检查权重总和
        double totalWeight = groups.stream().mapToDouble(Group::getWeight).sum();
        if (totalWeight <= 0) {
            throw new IllegalArgumentException("Total weight must be greater than 0");
        }
    }
}
