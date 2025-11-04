package com.basebackend.scheduler;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 调度服务启动类
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication(scanBasePackages = {
        "com.basebackend.scheduler.camunda"
})
@EnableDiscoveryClient
@MapperScan("com.basebackend.scheduler.camunda.mapper")
public class SchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulerApplication.class, args);
    }
}
