package com.basebackend.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "gateway.response-cache")
public class ResponseCacheProperties {

    private boolean enabled = false;

    private Duration defaultTtl = Duration.ofSeconds(60);

    private long maxCacheSize = 10000;

    private List<String> cachePaths = new ArrayList<>();

    private List<String> excludePaths = new ArrayList<>();
}
