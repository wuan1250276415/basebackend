package com.basebackend.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "gateway.api-version")
public class ApiVersionProperties {

    private boolean enabled = false;

    private String headerName = "Api-Version";

    private String defaultVersion = "v1";
}
