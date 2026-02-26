package com.basebackend.search.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 全文搜索配置属性
 *
 * <pre>
 * basebackend:
 *   search:
 *     enabled: true
 *     uris:
 *       - http://localhost:9200
 *     username: elastic
 *     password: changeme
 *     connect-timeout: 5s
 *     socket-timeout: 30s
 *     index-prefix: ""
 *     default-analyzer: ik_max_word
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "basebackend.search")
public class SearchProperties {

    /** 是否启用搜索模块 */
    private boolean enabled = false;

    /** ES/OpenSearch 地址列表 */
    private List<String> uris = new ArrayList<>(List.of("http://localhost:9200"));

    /** 用户名 */
    private String username;

    /** 密码 */
    private String password;

    /** 连接超时 */
    private Duration connectTimeout = Duration.ofSeconds(5);

    /** Socket 超时 */
    private Duration socketTimeout = Duration.ofSeconds(30);

    /** 索引名前缀（多环境隔离） */
    private String indexPrefix = "";

    /** 默认分词器 */
    private String defaultAnalyzer = "ik_max_word";

    /** 默认搜索分词器 */
    private String defaultSearchAnalyzer = "ik_smart";

    /** 默认分页大小 */
    private int defaultPageSize = 20;

    /** 最大分页大小 */
    private int maxPageSize = 1000;

    /** 高亮前标签 */
    private String highlightPreTag = "<em>";

    /** 高亮后标签 */
    private String highlightPostTag = "</em>";

    /**
     * 获取完整索引名（带前缀）
     */
    public String getFullIndexName(String indexName) {
        if (indexPrefix == null || indexPrefix.isBlank()) {
            return indexName;
        }
        return indexPrefix + indexName;
    }
}
