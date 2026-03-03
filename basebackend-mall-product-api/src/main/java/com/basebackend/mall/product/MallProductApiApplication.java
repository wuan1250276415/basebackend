package com.basebackend.mall.product;

import com.basebackend.mall.product.config.MallProductApiNativeHints;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * 商城商品服务启动类
 */
@SpringBootApplication(scanBasePackages = {
        "com.basebackend.mall.product",
        "com.basebackend.common",
        "com.basebackend.security",
})
@MapperScan({
        "com.basebackend.mall.product.mapper",
})
@EnableDiscoveryClient
@EnableAspectJAutoProxy
@ImportRuntimeHints(MallProductApiNativeHints.class)
public class MallProductApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallProductApiApplication.class, args);
    }
}
