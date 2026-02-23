package com.basebackend.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "gateway.access-log")
public class AccessLogProperties {

    private boolean enabled = true;

    private List<String> excludePaths = List.of("/actuator/**");

    private boolean logHeaders = false;

    private boolean logBody = false;
}
