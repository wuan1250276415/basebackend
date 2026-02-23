package com.basebackend.common.event.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("basebackend.common.event")
public class EventProperties {
    private boolean enabled = true;
}
