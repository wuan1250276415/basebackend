package com.basebackend.nacos.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Nacos 配置属性
 * <p>
 * 支持多维度配置隔离和动态配置刷新功能。
 * </p>
 */
@Data
@RefreshScope
@ConfigurationProperties(prefix = "nacos")
@Validated
public class NacosConfigProperties {

    /**
     * 环境名称（用于环境隔离）
     */
    @NotBlank(message = "环境配置不能为空")
    private String environment = "dev";

    /**
     * 租户ID（用于租户隔离）
     */
    private String tenantId = "public";

    /**
     * 应用ID（用于应用隔离）
     */
    private Long appId;

    /**
     * 配置中心配置
     */
    @Valid
    private Config config = new Config();

    /**
     * 服务发现配置
     */
    @Valid
    private Discovery discovery = new Discovery();

    /**
     * 配置中心配置
     */
    @Data
    public static class Config {
        /**
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * Nacos 服务地址
         */
        @NotBlank(message = "nacos.config.server-addr 不能为空")
        private String serverAddr = "127.0.0.1:8848";

        /**
         * 命名空间
         */
        private String namespace = "public";

        /**
         * 分组
         */
        private String group = "DEFAULT_GROUP";

        /**
         * 文件扩展名
         */
        private String fileExtension = "yml";

        /**
         * 是否启用导入检查
         */
        private boolean importCheckEnabled = true;

        /**
         * 动态刷新开关（自动检测）
         */
        private Boolean refreshEnabled;

        /**
         * 用户名
         */
        private String username = "nacos";

        /**
         * 密码
         */
        private String password = "nacos";

        /**
         * 共享配置列表（所有服务共享）
         */
        @Valid
        private List<SharedConfig> sharedConfigs = new ArrayList<>();

        /**
         * 扩展配置列表（服务特定配置）
         */
        @Valid
        private List<ExtensionConfig> extensionConfigs = new ArrayList<>();

        /**
         * 共享配置
         */
        @Data
        public static class SharedConfig {
            /**
             * 数据ID
             */
            @NotBlank
            private String dataId;

            /**
             * 是否刷新
             */
            private boolean refresh = true;

            /**
             * 分组
             */
            private String group = "DEFAULT_GROUP";
        }

        /**
         * 扩展配置
         */
        @Data
        public static class ExtensionConfig {
            /**
             * 数据ID
             */
            @NotBlank
            private String dataId;

            /**
             * 是否刷新
             */
            private boolean refresh = true;

            /**
             * 分组
             */
            private String group = "DEFAULT_GROUP";
        }
    }

    /**
     * 服务发现配置
     */
    @Data
    public static class Discovery {
        /**
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * Nacos 服务地址
         */
        @NotBlank(message = "nacos.discovery.server-addr 不能为空")
        private String serverAddr = "127.0.0.1:8848";

        /**
         * 命名空间
         */
        private String namespace = "public";

        /**
         * 分组
         */
        private String group = "DEFAULT_GROUP";

        /**
         * 服务名
         */
        private String serviceName;

        /**
         * 权重
         */
        private double weight = 1.0;

        /**
         * 集群名
         */
        private String cluster = "DEFAULT";

        /**
         * 用户名
         */
        private String username = "nacos";

        /**
         * 密码
         */
        private String password = "nacos";

        /**
         * 实例元数据（用于灰度发布）
         */
        private Map<String, String> metadata = new HashMap<>();
    }
}
