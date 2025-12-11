package com.basebackend.featuretoggle.abtest;

import com.basebackend.featuretoggle.model.FeatureContext;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 渐进式发布计算器
 * <p>
 * 支持基于时间、用户权重、地理区域等多种策略的渐进式发布。
 * 提供平滑的发布控制，避免一次性全量发布带来的风险。
 * </p>
 *
 * <h3>发布策略：</h3>
 * <ul>
 *   <li>线性增长 - 按时间线性增加用户比例</li>
 *   <li>指数增长 - 按指数曲线快速扩展用户群体</li>
 *   <li>固定比例 - 维持固定比例的用户启用新特性</li>
 *   <li>按地理发布 - 基于地理位置分阶段发布</li>
 *   <li>按用户标签 - 基于用户属性分阶段发布</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RolloutCalculator {

    /**
     * 渐进式发布配置
     */
    public static class RolloutConfig {
        private final double startPercentage;
        private final double endPercentage;
        private final long durationMinutes;
        private final RolloutStrategy strategy;
        private final Date startTime;
        private final Set<String> allowedRegions;
        private final Set<String> allowedUserTags;

        private RolloutConfig(Builder builder) {
            this.startPercentage = builder.startPercentage;
            this.endPercentage = builder.endPercentage;
            this.durationMinutes = builder.durationMinutes;
            this.strategy = builder.strategy;
            this.startTime = builder.startTime;
            this.allowedRegions = builder.allowedRegions;
            this.allowedUserTags = builder.allowedUserTags;
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public double getStartPercentage() {
            return startPercentage;
        }

        public double getEndPercentage() {
            return endPercentage;
        }

        public long getDurationMinutes() {
            return durationMinutes;
        }

        public RolloutStrategy getStrategy() {
            return strategy;
        }

        public Date getStartTime() {
            return startTime;
        }

        public Set<String> getAllowedRegions() {
            return allowedRegions;
        }

        public Set<String> getAllowedUserTags() {
            return allowedUserTags;
        }

        /**
         * 构建器
         */
        public static class Builder {
            private double startPercentage = 0;
            private double endPercentage = 100;
            private long durationMinutes = 60;
            private RolloutStrategy strategy = RolloutStrategy.LINEAR;
            private Date startTime = new Date();
            private Set<String> allowedRegions = new HashSet<>();
            private Set<String> allowedUserTags = new HashSet<>();

            /**
             * 设置起始百分比
             *
             * @param startPercentage 起始百分比 (0-100)
             * @return Builder
             */
            public Builder startPercentage(double startPercentage) {
                if (startPercentage < 0 || startPercentage > 100) {
                    throw new IllegalArgumentException("Start percentage must be between 0 and 100");
                }
                this.startPercentage = startPercentage;
                return this;
            }

            /**
             * 设置目标百分比
             *
             * @param endPercentage 目标百分比 (0-100)
             * @return Builder
             */
            public Builder endPercentage(double endPercentage) {
                if (endPercentage < 0 || endPercentage > 100) {
                    throw new IllegalArgumentException("End percentage must be between 0 and 100");
                }
                this.endPercentage = endPercentage;
                return this;
            }

            /**
             * 设置发布持续时间
             *
             * @param durationMinutes 持续时间（分钟）
             * @return Builder
             */
            public Builder durationMinutes(long durationMinutes) {
                if (durationMinutes <= 0) {
                    throw new IllegalArgumentException("Duration must be positive");
                }
                this.durationMinutes = durationMinutes;
                return this;
            }

            /**
             * 设置发布策略
             *
             * @param strategy 发布策略
             * @return Builder
             */
            public Builder strategy(RolloutStrategy strategy) {
                this.strategy = strategy;
                return this;
            }

            /**
             * 设置开始时间
             *
             * @param startTime 开始时间
             * @return Builder
             */
            public Builder startTime(Date startTime) {
                this.startTime = startTime != null ? startTime : new Date();
                return this;
            }

            /**
             * 设置允许的地理区域
             *
             * @param regions 地理区域列表
             * @return Builder
             */
            public Builder allowedRegions(String... regions) {
                this.allowedRegions.addAll(Arrays.asList(regions));
                return this;
            }

            /**
             * 设置允许的用户标签
             *
             * @param tags 用户标签列表
             * @return Builder
             */
            public Builder allowedUserTags(String... tags) {
                this.allowedUserTags.addAll(Arrays.asList(tags));
                return this;
            }

            /**
             * 构建配置对象
             *
             * @return RolloutConfig
             */
            public RolloutConfig build() {
                if (startPercentage > endPercentage) {
                    throw new IllegalArgumentException("Start percentage cannot be greater than end percentage");
                }
                return new RolloutConfig(this);
            }
        }
    }

    /**
     * 发布策略枚举
     */
    public enum RolloutStrategy {
        /**
         * 线性增长 - 推荐用于平滑发布
         */
        LINEAR {
            @Override
            public double calculatePercentage(double start, double end, double progress) {
                return start + (end - start) * progress;
            }
        },

        /**
         * 指数增长 - 快速扩大用户群体
         */
        EXPONENTIAL {
            @Override
            public double calculatePercentage(double start, double end, double progress) {
                // 使用指数曲线：percentage = start + (end - start) * (1 - e^(-k*progress)) / (1 - e^(-k))
                // 其中 k = 3 调整曲线陡峭度
                double k = 3.0;
                double numerator = 1 - Math.exp(-k * progress);
                double denominator = 1 - Math.exp(-k);
                return start + (end - start) * numerator / denominator;
            }
        },

        /**
         * 固定比例 - 维持固定比例
         */
        FIXED {
            @Override
            public double calculatePercentage(double start, double end, double progress) {
                return start; // 固定在起始比例
            }
        },

        /**
         * 突发增长 - 前中期缓慢，后期快速
         */
        SUDDEN {
            @Override
            public double calculatePercentage(double start, double end, double progress) {
                // 使用S形曲线：percentage = start + (end - start) * (1 / (1 + e^(-k*(progress-0.5))))
                double k = 10.0;
                double sigmoid = 1.0 / (1.0 + Math.exp(-k * (progress - 0.5)));
                return start + (end - start) * sigmoid;
            }
        };

        /**
         * 计算当前百分比
         *
         * @param start 起始百分比
         * @param end 目标百分比
         * @param progress 进度 (0.0-1.0)
         * @return 当前百分比
         */
        public abstract double calculatePercentage(double start, double end, double progress);
    }

    /**
     * 计算当前应该启用的用户百分比
     *
     * @param config 发布配置
     * @return 当前百分比 (0-100)
     */
    public static double calculateCurrentPercentage(RolloutConfig config) {
        if (config == null) {
            return 0.0;
        }

        // 计算时间进度
        double progress = calculateTimeProgress(config);
        log.debug("Rollout progress: {:.2%}", progress);

        // 根据策略计算当前百分比
        double currentPercentage = config.getStrategy()
                .calculatePercentage(config.getStartPercentage(), config.getEndPercentage(), progress);

        log.debug("Calculated rollout percentage: {:.2f}%", currentPercentage);
        return currentPercentage;
    }

    /**
     * 计算用户是否应该被包含在当前发布中
     * <p>
     * 考虑时间进度、地理区域、用户标签等多个因素。
     * </p>
     *
     * @param featureName 特性名称
     * @param context 用户上下文
     * @param config 发布配置
     * @return 是否应该启用该特性
     */
    public static boolean shouldEnableFeature(String featureName, FeatureContext context, RolloutConfig config) {
        if (config == null) {
            return false;
        }

        // 检查地理区域限制
        if (!config.getAllowedRegions().isEmpty()) {
            String region = extractRegionFromContext(context);
            if (!config.getAllowedRegions().contains(region)) {
                log.debug("Feature '{}' not enabled for region: {}", featureName, region);
                return false;
            }
        }

        // 检查用户标签限制
        if (!config.getAllowedUserTags().isEmpty()) {
            Set<String> userTags = extractUserTagsFromContext(context);
            boolean hasAllowedTag = config.getAllowedUserTags().stream()
                    .anyMatch(userTags::contains);
            if (!hasAllowedTag) {
                log.debug("Feature '{}' not enabled for user tags: {}", featureName, userTags);
                return false;
            }
        }

        // 计算当前发布百分比
        double currentPercentage = calculateCurrentPercentage(config);
        if (currentPercentage <= 0) {
            return false;
        }

        if (currentPercentage >= 100) {
            return true;
        }

        // 基于用户一致性哈希判断是否命中
        String hashKey = generateRolloutHashKey(featureName, context);
        long hash = HashAlgorithm.murmur3_32(hashKey);
        int bucket = HashAlgorithm.toPercentileBucket(hash);

        boolean enabled = bucket < currentPercentage;
        log.debug("Feature '{}' rollout: hash={}, bucket={}, percentage={:.2f}, enabled={}",
                featureName, hash, bucket, currentPercentage, enabled);

        return enabled;
    }

    /**
     * 计算发布进度百分比
     *
     * @param config 发布配置
     * @return 进度 (0.0-1.0)
     */
    public static double calculateProgress(RolloutConfig config) {
        return calculateTimeProgress(config);
    }

    /**
     * 检查发布是否完成
     *
     * @param config 发布配置
     * @return 是否完成
     */
    public static boolean isRolloutComplete(RolloutConfig config) {
        if (config == null) {
            return true;
        }
        return calculateTimeProgress(config) >= 1.0;
    }

    /**
     * 检查发布是否开始
     *
     * @param config 发布配置
     * @return 是否开始
     */
    public static boolean isRolloutStarted(RolloutConfig config) {
        if (config == null) {
            return true;
        }
        Date now = new Date();
        return !now.before(config.getStartTime());
    }

    /**
     * 计算时间进度
     */
    private static double calculateTimeProgress(RolloutConfig config) {
        Date now = new Date();
        Date startTime = config.getStartTime();
        long durationMillis = config.getDurationMinutes() * 60 * 1000L;
        long elapsedMillis = now.getTime() - startTime.getTime();

        if (elapsedMillis <= 0) {
            return 0.0; // 尚未开始
        }

        if (elapsedMillis >= durationMillis) {
            return 1.0; // 已经完成
        }

        return (double) elapsedMillis / durationMillis;
    }

    /**
     * 从上下文提取地理区域
     */
    private static String extractRegionFromContext(FeatureContext context) {
        if (context == null) {
            return "unknown";
        }

        // 从自定义属性中查找区域信息
        if (context.getProperties() != null) {
            String region = context.getProperties().get("region");
            if (region != null) {
                return region;
            }
        }

        // 从IP推断区域（简单实现）
        String ip = context.getIpAddress();
        if (ip != null) {
            // 实际实现可以调用地理位置API
            if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                return "internal";
            }
            return "external";
        }

        return "unknown";
    }

    /**
     * 从上下文提取用户标签
     */
    private static Set<String> extractUserTagsFromContext(FeatureContext context) {
        Set<String> tags = new HashSet<>();

        if (context == null) {
            return tags;
        }

        // 从自定义属性中查找标签
        if (context.getProperties() != null) {
            String tagsStr = context.getProperties().get("user_tags");
            if (tagsStr != null && !tagsStr.trim().isEmpty()) {
                String[] tagArray = tagsStr.split(",");
                for (String tag : tagArray) {
                    String trimmedTag = tag.trim();
                    if (!trimmedTag.isEmpty()) {
                        tags.add(trimmedTag);
                    }
                }
            }

            // 单独提取的标签
            String userType = context.getProperties().get("user_type");
            if (userType != null) {
                tags.add("type:" + userType);
            }

            String membershipLevel = context.getProperties().get("membership_level");
            if (membershipLevel != null) {
                tags.add("membership:" + membershipLevel);
            }
        }

        // 从环境信息推断标签
        if (context.getEnvironment() != null) {
            tags.add("env:" + context.getEnvironment());
        }

        return tags;
    }

    /**
     * 生成发布相关的哈希键
     */
    private static String generateRolloutHashKey(String featureName, FeatureContext context) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("rollout:").append(featureName);

        if (context != null) {
            if (context.getUserId() != null) {
                keyBuilder.append(":user:").append(context.getUserId());
            } else if (context.getTenantId() != null) {
                keyBuilder.append(":tenant:").append(context.getTenantId());
            } else if (context.getIpAddress() != null) {
                keyBuilder.append(":ip:").append(context.getIpAddress());
            }
        }

        return keyBuilder.toString();
    }
}
