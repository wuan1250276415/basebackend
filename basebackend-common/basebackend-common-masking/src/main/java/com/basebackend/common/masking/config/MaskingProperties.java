package com.basebackend.common.masking.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("basebackend.common.masking")
public class MaskingProperties {
    private boolean enabled = true;
}
