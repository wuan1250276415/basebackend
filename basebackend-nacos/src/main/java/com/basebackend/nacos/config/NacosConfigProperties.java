package com.basebackend.nacos.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * Nacos 配置属性
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "nacos")
public class NacosConfigProperties {

    /**
     * 服务发现配置
     */
    private Discovery discovery = new Discovery();

    /**
     * 配置中心配置
     */
    private Config config = new Config();

    @Data
    public static class Discovery {
        /**
         * Nacos 服务地址
         */
        private String serverAddr = "localhost:28080";

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
         * 是否启用
         */
        private boolean enabled = true;
    }

    @Data
    public static class Config {
        /**
         * Nacos 服务地址
         */
        private String serverAddr = "localhost:28080";

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
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * 是否启用导入检查
         */
        private boolean importCheckEnabled = true;
    }
}
