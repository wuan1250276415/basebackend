package com.basebackend.search.config;

import com.basebackend.search.client.RestClientSearchClient;
import com.basebackend.search.client.SearchClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 全文搜索自动配置
 * <p>
 * 需要显式启用：{@code basebackend.search.enabled=true}
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(SearchProperties.class)
@ConditionalOnProperty(prefix = "basebackend.search", name = "enabled", havingValue = "true")
public class SearchAutoConfiguration {

    public SearchAutoConfiguration(SearchProperties properties) {
        log.info("全文搜索模块已启用");
        log.info("  - ES 地址: {}", properties.getUris());
        log.info("  - 索引前缀: {}", properties.getIndexPrefix().isEmpty() ? "(无)" : properties.getIndexPrefix());
        log.info("  - 默认分词器: {}", properties.getDefaultAnalyzer());
    }

    @Bean
    @ConditionalOnMissingBean
    public SearchClient searchClient(SearchProperties properties,
                                     ObjectProvider<ObjectMapper> objectMapperProvider) {
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        log.info("注册 SearchClient: uris={}", properties.getUris());
        return new RestClientSearchClient(properties, objectMapper);
    }
}
