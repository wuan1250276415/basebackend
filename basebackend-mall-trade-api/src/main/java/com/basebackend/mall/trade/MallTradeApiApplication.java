package com.basebackend.mall.trade;

import com.basebackend.mall.trade.config.MallTradeApiNativeHints;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 商城交易服务启动类
 */
@SpringBootApplication(scanBasePackages = {
        "com.basebackend.mall.trade",
        "com.basebackend.common",
        "com.basebackend.security",
})
@MapperScan({
        "com.basebackend.mall.trade.mapper",
})
@EnableDiscoveryClient
@EnableAspectJAutoProxy
@EnableScheduling
@ImportRuntimeHints(MallTradeApiNativeHints.class)
public class MallTradeApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallTradeApiApplication.class, args);
    }
}
