package com.basebackend.scheduler.camunda.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Camunda配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "camunda.bpm")
public class CamundaProperties {

    /**
     * 是否启用Camunda
     */
    private Boolean enabled = true;

    /**
     * 管理员用户信息
     */
    private Admin admin = new Admin();

    /**
     * 历史级别
     */
    private String historyLevel = "full";

    /**
     * 自动部署
     */
    private AutoDeploy autoDeploy = new AutoDeploy();

    /**
     * 作业执行配置
     */
    private JobExecution jobExecution = new JobExecution();

    @Data
    public static class Admin {
        private String id = "admin";
        private String password = "admin";
        private String firstName = "Admin";
        private String lastName = "User";
        private String email = "admin@basebackend.com";
    }

    @Data
    public static class AutoDeploy {
        /**
         * 是否启用自动部署
         */
        private Boolean enabled = true;

        /**
         * 流程文件路径
         */
        private String resourcePattern = "classpath*:processes/**/*.bpmn";
    }

    @Data
    public static class JobExecution {
        /**
         * 是否启用作业执行
         */
        private Boolean enabled = true;

        /**
         * 核心线程数
         */
        private Integer corePoolSize = 5;

        /**
         * 最大线程数
         */
        private Integer maxPoolSize = 20;

        /**
         * 队列容量
         */
        private Integer queueCapacity = 100;

        /**
         * 锁定时间（毫秒）
         */
        private Integer lockTimeInMillis = 300000;
    }
}
