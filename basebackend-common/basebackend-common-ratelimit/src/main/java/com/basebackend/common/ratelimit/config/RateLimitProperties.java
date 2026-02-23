package com.basebackend.common.ratelimit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("basebackend.common.ratelimit")
public class RateLimitProperties {

    private boolean enabled = true;
}
