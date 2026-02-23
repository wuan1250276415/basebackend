package com.basebackend.common.export.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("basebackend.common.export")
public class ExportProperties {

    private boolean enabled = true;
}
