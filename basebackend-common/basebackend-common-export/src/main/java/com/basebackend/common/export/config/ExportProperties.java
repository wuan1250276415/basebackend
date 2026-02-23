package com.basebackend.common.export.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("basebackend.common.export")
public class ExportProperties {

    private boolean enabled = true;

    /**
     * 异步导出配置
     */
    private Async async = new Async();

    @Data
    public static class Async {
        /**
         * 异步导出线程池大小
         */
        private int threadPoolSize = 4;

        /**
         * 任务结果保留时间（小时），过期自动清理
         */
        private long taskTtlHours = 24;
    }
}
