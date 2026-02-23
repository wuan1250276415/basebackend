package com.basebackend.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "gateway.request-size-limit")
public class RequestSizeLimitProperties {

    private boolean enabled = true;

    private DataSize maxBodySize = DataSize.ofMegabytes(10);

    private List<String> excludePaths = List.of("/api/files/**");
}
