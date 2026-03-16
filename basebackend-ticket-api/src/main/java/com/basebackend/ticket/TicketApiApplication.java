package com.basebackend.ticket;

import com.basebackend.ticket.config.TicketApiNativeHints;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * 工单微服务启动类
 *
 * @author BaseBackend Team
 */
@SpringBootApplication(scanBasePackages = {
        "com.basebackend.ticket",
        "com.basebackend.common",
        "com.basebackend.security",
})
@MapperScan({
    "com.basebackend.ticket.mapper",
})
@EnableDiscoveryClient
@EnableAspectJAutoProxy
@ImportRuntimeHints(TicketApiNativeHints.class)
public class TicketApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketApiApplication.class, args);
    }
}
