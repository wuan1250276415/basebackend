package com.basebackend.mall.pay;

import com.basebackend.mall.pay.config.MallPayApiNativeHints;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * 商城支付服务启动类
 */
@SpringBootApplication(scanBasePackages = {
        "com.basebackend.mall.pay",
        "com.basebackend.common",
        "com.basebackend.security",
})
@MapperScan({
        "com.basebackend.mall.pay.mapper",
})
@EnableDiscoveryClient
@EnableAspectJAutoProxy
@ImportRuntimeHints(MallPayApiNativeHints.class)
public class MallPayApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallPayApiApplication.class, args);
    }
}
