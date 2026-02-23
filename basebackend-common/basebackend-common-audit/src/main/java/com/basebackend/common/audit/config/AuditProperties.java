package com.basebackend.common.audit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("basebackend.common.audit")
public class AuditProperties {

    private boolean enabled = true;
}
