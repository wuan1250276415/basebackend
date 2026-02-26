package com.basebackend.file.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * 文件服务自动配置
 *
 * @author BaseBackend
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "file", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({
        FileProperties.class,
        FileStorageProperties.class,
        FileSecurityProperties.class,
        MinioProperties.class,
        OssProperties.class,
        S3Properties.class
})
@Import({
        MinioConfig.class,
        AsyncConfiguration.class,
        PasswordEncoderConfig.class,
        RateLimiterConfig.class
})
public class FileAutoConfiguration {
}
