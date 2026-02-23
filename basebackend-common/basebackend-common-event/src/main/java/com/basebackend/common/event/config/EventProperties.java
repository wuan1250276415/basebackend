package com.basebackend.common.event.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("basebackend.common.event")
public class EventProperties {

    private boolean enabled = true;

    /**
     * 事件存储配置
     */
    private Store store = new Store();

    /**
     * 重试配置
     */
    private Retry retry = new Retry();

    /**
     * 过期清理配置
     */
    private Cleanup cleanup = new Cleanup();

    @Data
    public static class Store {
        /**
         * 存储类型：memory / jdbc
         */
        private String type = "memory";
    }

    @Data
    public static class Retry {
        /**
         * 是否启用重试
         */
        private boolean enabled = true;

        /**
         * 重试扫描间隔（秒）
         */
        private int intervalSeconds = 30;

        /**
         * 默认最大重试次数
         */
        private int maxRetries = 3;

        /**
         * 每次扫描的批量大小
         */
        private int batchSize = 100;
    }

    @Data
    public static class Cleanup {
        /**
         * 是否启用过期清理
         */
        private boolean enabled = true;

        /**
         * 事件过期天数
         */
        private int expiredDays = 7;
    }
}
