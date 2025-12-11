package com.basebackend.storage.config;

import com.basebackend.storage.provider.LocalStorageProvider;
import com.basebackend.storage.provider.MinioStorageProvider;
import com.basebackend.storage.provider.OssStorageProvider;
import com.basebackend.storage.provider.S3StorageProvider;
import com.basebackend.storage.spi.StorageProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 存储服务自动配置
 * 
 * @author BaseBackend
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageAutoConfiguration {
    
    /**
     * 本地存储 Provider
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(StorageProvider.class)
    @ConditionalOnProperty(name = "basebackend.storage.type", havingValue = "local", matchIfMissing = true)
    public StorageProvider localStorageProvider(StorageProperties properties) {
        log.info("初始化本地存储服务，basePath: {}", properties.getLocal().getBasePath());
        return new LocalStorageProvider(properties);
    }
    
    /**
     * MinIO 存储 Provider
     */
    @Bean
    @ConditionalOnProperty(name = "basebackend.storage.type", havingValue = "minio")
    @ConditionalOnClass(name = "io.minio.MinioClient")
    public StorageProvider minioStorageProvider(StorageProperties properties) {
        log.info("初始化 MinIO 存储服务，endpoint: {}", properties.getMinio().getEndpoint());
        return new MinioStorageProvider(properties);
    }
    
    /**
     * AWS S3 存储 Provider
     */
    @Bean
    @ConditionalOnProperty(name = "basebackend.storage.type", havingValue = "s3")
    @ConditionalOnClass(name = "software.amazon.awssdk.services.s3.S3Client")
    public StorageProvider s3StorageProvider(StorageProperties properties) {
        log.info("初始化 AWS S3 存储服务，region: {}", properties.getS3().getRegion());
        return new S3StorageProvider(properties);
    }
    
    /**
     * 阿里云 OSS 存储 Provider
     */
    @Bean
    @ConditionalOnProperty(name = "basebackend.storage.type", havingValue = "oss")
    @ConditionalOnClass(name = "com.aliyun.oss.OSS")
    public StorageProvider ossStorageProvider(StorageProperties properties) {
        log.info("初始化阿里云 OSS 存储服务，endpoint: {}", properties.getOss().getEndpoint());
        return new OssStorageProvider(properties);
    }
}
