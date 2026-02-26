package com.basebackend.search.client;

import com.basebackend.search.model.IndexDefinition;
import com.basebackend.search.model.SearchResult;
import com.basebackend.search.query.SearchQuery;

import java.util.List;
import java.util.Map;

/**
 * 统一搜索客户端接口
 * <p>
 * 封装 Elasticsearch / OpenSearch 的核心操作，业务方通过此接口调用，不直接依赖 ES 客户端。
 */
public interface SearchClient {

    // ==================== 索引管理 ====================

    /** 创建索引 */
    boolean createIndex(IndexDefinition definition);

    /** 删除索引 */
    boolean deleteIndex(String indexName);

    /** 索引是否存在 */
    boolean indexExists(String indexName);

    // ==================== 文档操作 ====================

    /** 索引文档（新增或更新） */
    <T> boolean index(String indexName, String id, T document);

    /** 批量索引文档 */
    <T> int bulkIndex(String indexName, Map<String, T> documents);

    /** 获取文档 */
    <T> T get(String indexName, String id, Class<T> clazz);

    /** 删除文档 */
    boolean delete(String indexName, String id);

    /** 批量删除文档 */
    int bulkDelete(String indexName, List<String> ids);

    // ==================== 搜索 ====================

    /** 执行搜索 */
    <T> SearchResult<T> search(SearchQuery query, Class<T> clazz);

    /** 统计文档数量 */
    long count(String indexName);
}
