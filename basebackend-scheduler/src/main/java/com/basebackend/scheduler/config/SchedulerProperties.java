package com.basebackend.scheduler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 调度器属性配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "scheduler")
public class SchedulerProperties {

    /**
     * PowerJob配置
     */
    private PowerJob powerjob = new PowerJob();

    /**
     * 延迟任务配置
     */
    private DelayTask delayTask = new DelayTask();

    /**
     * 重试配置
     */
    private Retry retry = new Retry();

    /**
     * 告警配置
     */
    private Alert alert = new Alert();

    @Data
    public static class PowerJob {
        /**
         * PowerJob Server地址
         */
        private String serverAddress = "http://localhost:7700";

        /**
         * 应用名称
         */
        private String appName = "basebackend";

        /**
         * Worker端口
         */
        private Integer port = 27777;

        /**
         * 协议类型: http/akka
         */
        private String protocol = "http";

        /**
         * 健康检查间隔(秒)
         */
        private Integer healthCheckInterval = 30;

        /**
         * 最大任务并发数
         */
        private Integer maxWorkerNum = 200;
    }

    @Data
    public static class DelayTask {
        /**
         * 延迟队列类型: redis/rabbitmq
         */
        private String queueType = "redis";

        /**
         * Redis延迟队列Key前缀
         */
        private String redisKeyPrefix = "delay:task:";

        /**
         * RabbitMQ延迟交换机
         */
        private String rabbitExchange = "delay.task.exchange";

        /**
         * 扫描间隔(毫秒)
         */
        private Long scanInterval = 1000L;
    }

    @Data
    public static class Retry {
        /**
         * 默认最大重试次数
         */
        private Integer maxRetryTimes = 3;

        /**
         * 默认重试间隔(秒)
         */
        private Integer retryInterval = 60;

        /**
         * 是否启用指数退避
         */
        private Boolean exponentialBackoff = true;

        /**
         * 退避倍数
         */
        private Double backoffMultiplier = 2.0;

        /**
         * 最大退避时间(秒)
         */
        private Integer maxBackoffInterval = 3600;
    }

    @Data
    public static class Alert {
        /**
         * 是否启用告警
         */
        private Boolean enabled = true;

        /**
         * 钉钉Webhook
         */
        private String dingTalkWebhook;

        /**
         * 企业微信Webhook
         */
        private String wechatWebhook;

        /**
         * 邮件发送地址
         */
        private String emailFrom;

        /**
         * 邮件接收地址
         */
        private String emailTo;
    }
}
