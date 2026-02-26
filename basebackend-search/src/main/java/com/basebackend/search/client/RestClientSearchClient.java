package com.basebackend.search.client;

import com.basebackend.search.model.IndexDefinition;
import com.basebackend.search.model.SearchResult;
import com.basebackend.search.query.SearchQuery;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 基于 RestClient 的 Elasticsearch 搜索客户端实现
 * <p>
 * 通过 REST API 与 ES 交互，不依赖 ES Java Client 库。
 * 支持 Elasticsearch 7.x / 8.x 和兼容的 OpenSearch。
 */
@Slf4j
public class RestClientSearchClient implements SearchClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String indexPrefix;

    public RestClientSearchClient(String baseUrl, String username, String password, String indexPrefix) {
        this.indexPrefix = indexPrefix != null ? indexPrefix : "";
        this.objectMapper = new ObjectMapper();

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        if (username != null && !username.isBlank()) {
            String credentials = Base64.getEncoder().encodeToString(
                    (username + ":" + password).getBytes(StandardCharsets.UTF_8));
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + credentials);
        }

        this.restClient = builder.build();
    }

    @Override
    public boolean createIndex(IndexDefinition definition) {
        String indexName = fullName(definition.getIndexName());

        Map<String, Object> body = buildCreateIndexBody(definition);

        try {
            restClient.put()
                    .uri("/{index}", indexName)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("索引创建成功: {}", indexName);
            return true;
        } catch (Exception e) {
            log.error("索引创建失败: {}, error={}", indexName, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean deleteIndex(String indexName) {
        try {
            restClient.delete()
                    .uri("/{index}", fullName(indexName))
                    .retrieve()
                    .toBodilessEntity();
            log.info("索引删除成功: {}", indexName);
            return true;
        } catch (Exception e) {
            log.error("索引删除失败: {}, error={}", indexName, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean indexExists(String indexName) {
        try {
            restClient.head()
                    .uri("/{index}", fullName(indexName))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public <T> boolean index(String indexName, String id, T document) {
        try {
            restClient.put()
                    .uri("/{index}/_doc/{id}", fullName(indexName), id)
                    .body(document)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.error("文档索引失败: index={}, id={}, error={}", indexName, id, e.getMessage());
            return false;
        }
    }

    @Override
    public <T> int bulkIndex(String indexName, Map<String, T> documents) {
        if (documents == null || documents.isEmpty()) return 0;

        StringBuilder bulkBody = new StringBuilder();
        String fullIndex = fullName(indexName);

        for (Map.Entry<String, T> entry : documents.entrySet()) {
            try {
                bulkBody.append("""
                        {"index":{"_index":"%s","_id":"%s"}}""".formatted(fullIndex, entry.getKey()));
                bulkBody.append("\n");
                bulkBody.append(objectMapper.writeValueAsString(entry.getValue()));
                bulkBody.append("\n");
            } catch (JsonProcessingException e) {
                log.warn("批量索引序列化失败: id={}", entry.getKey(), e);
            }
        }

        try {
            restClient.post()
                    .uri("/_bulk")
                    .contentType(MediaType.valueOf("application/x-ndjson"))
                    .body(bulkBody.toString())
                    .retrieve()
                    .toBodilessEntity();
            log.info("批量索引完成: index={}, count={}", indexName, documents.size());
            return documents.size();
        } catch (Exception e) {
            log.error("批量索引失败: index={}, error={}", indexName, e.getMessage());
            return 0;
        }
    }

    @Override
    public <T> T get(String indexName, String id, Class<T> clazz) {
        try {
            String responseBody = restClient.get()
                    .uri("/{index}/_doc/{id}", fullName(indexName), id)
                    .retrieve()
                    .body(String.class);

            var root = objectMapper.readTree(responseBody);
            if (root.path("found").asBoolean(false)) {
                return objectMapper.treeToValue(root.path("_source"), clazz);
            }
            return null;
        } catch (Exception e) {
            log.debug("文档获取失败: index={}, id={}, error={}", indexName, id, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean delete(String indexName, String id) {
        try {
            restClient.delete()
                    .uri("/{index}/_doc/{id}", fullName(indexName), id)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.error("文档删除失败: index={}, id={}, error={}", indexName, id, e.getMessage());
            return false;
        }
    }

    @Override
    public int bulkDelete(String indexName, List<String> ids) {
        if (ids == null || ids.isEmpty()) return 0;

        StringBuilder bulkBody = new StringBuilder();
        String fullIndex = fullName(indexName);

        for (String id : ids) {
            bulkBody.append("""
                    {"delete":{"_index":"%s","_id":"%s"}}""".formatted(fullIndex, id));
            bulkBody.append("\n");
        }

        try {
            restClient.post()
                    .uri("/_bulk")
                    .contentType(MediaType.valueOf("application/x-ndjson"))
                    .body(bulkBody.toString())
                    .retrieve()
                    .toBodilessEntity();
            return ids.size();
        } catch (Exception e) {
            log.error("批量删除失败: index={}, error={}", indexName, e.getMessage());
            return 0;
        }
    }

    @Override
    public <T> SearchResult<T> search(SearchQuery query, Class<T> clazz) {
        long startTime = System.currentTimeMillis();
        String fullIndex = fullName(query.getIndexName());

        Map<String, Object> body = buildSearchBody(query);

        try {
            String responseBody = restClient.post()
                    .uri("/{index}/_search", fullIndex)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return parseSearchResponse(responseBody, clazz, System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("搜索失败: index={}, error={}", query.getIndexName(), e.getMessage());
            return SearchResult.empty();
        }
    }

    @Override
    public long count(String indexName) {
        try {
            String responseBody = restClient.get()
                    .uri("/{index}/_count", fullName(indexName))
                    .retrieve()
                    .body(String.class);

            var root = objectMapper.readTree(responseBody);
            return root.path("count").asLong(0);
        } catch (Exception e) {
            log.error("计数失败: index={}, error={}", indexName, e.getMessage());
            return 0;
        }
    }

    // --- 内部方法 ---

    private String fullName(String indexName) {
        return indexPrefix + indexName;
    }

    private Map<String, Object> buildCreateIndexBody(IndexDefinition definition) {
        Map<String, Object> body = new LinkedHashMap<>();

        // settings
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("number_of_shards", definition.getNumberOfShards());
        settings.put("number_of_replicas", definition.getNumberOfReplicas());
        body.put("settings", settings);

        // mappings
        Map<String, Object> properties = new LinkedHashMap<>();
        for (var entry : definition.getFields().entrySet()) {
            var field = entry.getValue();
            Map<String, Object> fieldMapping = new LinkedHashMap<>();
            fieldMapping.put("type", field.type().name().toLowerCase());
            if (field.analyzer() != null) {
                fieldMapping.put("analyzer", field.analyzer());
            }
            if (field.searchAnalyzer() != null) {
                fieldMapping.put("search_analyzer", field.searchAnalyzer());
            }
            properties.put(field.name(), fieldMapping);
        }
        body.put("mappings", Map.of("properties", properties));

        return body;
    }

    private Map<String, Object> buildSearchBody(SearchQuery query) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from", query.getFrom());
        body.put("size", query.getSize());

        // bool 查询
        if (query.hasConditions()) {
            Map<String, Object> boolQuery = new LinkedHashMap<>();

            if (!query.getMustConditions().isEmpty()) {
                boolQuery.put("must", query.getMustConditions().stream()
                        .map(this::buildCondition).toList());
            }
            if (!query.getShouldConditions().isEmpty()) {
                boolQuery.put("should", query.getShouldConditions().stream()
                        .map(this::buildCondition).toList());
            }
            if (!query.getMustNotConditions().isEmpty()) {
                boolQuery.put("must_not", query.getMustNotConditions().stream()
                        .map(this::buildCondition).toList());
            }
            if (!query.getFilterConditions().isEmpty()) {
                boolQuery.put("filter", query.getFilterConditions().stream()
                        .map(this::buildCondition).toList());
            }
            if (query.getMinimumShouldMatch() != null) {
                boolQuery.put("minimum_should_match", query.getMinimumShouldMatch());
            }

            body.put("query", Map.of("bool", boolQuery));
        }

        // 排序
        if (!query.getSortFields().isEmpty()) {
            body.put("sort", query.getSortFields().stream()
                    .map(s -> Map.of(s.field(), Map.of("order", s.order().name().toLowerCase())))
                    .toList());
        }

        // 高亮
        if (!query.getHighlightFields().isEmpty()) {
            Map<String, Object> highlightFields = new LinkedHashMap<>();
            for (String field : query.getHighlightFields()) {
                highlightFields.put(field, Map.of());
            }
            body.put("highlight", Map.of("fields", highlightFields));
        }

        // source 过滤
        if (!query.getSourceIncludes().isEmpty() || !query.getSourceExcludes().isEmpty()) {
            Map<String, Object> source = new LinkedHashMap<>();
            if (!query.getSourceIncludes().isEmpty()) {
                source.put("includes", query.getSourceIncludes());
            }
            if (!query.getSourceExcludes().isEmpty()) {
                source.put("excludes", query.getSourceExcludes());
            }
            body.put("_source", source);
        }

        return body;
    }

    private Map<String, Object> buildCondition(SearchQuery.Condition condition) {
        return switch (condition.type()) {
            case MATCH -> Map.of("match", Map.of(condition.field(), condition.value()));
            case MULTI_MATCH -> throw new UnsupportedOperationException("MULTI_MATCH not supported yet");
            case MATCH_PHRASE -> Map.of("match_phrase", Map.of(condition.field(), condition.value()));
            case TERM -> Map.of("term", Map.of(condition.field(), condition.value()));
            case TERMS -> Map.of("terms", Map.of(condition.field(), condition.value()));
            case PREFIX -> Map.of("prefix", Map.of(condition.field(), condition.value()));
            case WILDCARD -> Map.of("wildcard", Map.of(condition.field(), condition.value()));
            case EXISTS -> Map.of("exists", Map.of("field", condition.field()));
            case RANGE -> {
                Map<String, Object> rangeCondition = new LinkedHashMap<>();
                if (condition.value() != null) rangeCondition.put("gte", condition.value());
                if (condition.value2() != null) rangeCondition.put("lte", condition.value2());
                yield Map.of("range", Map.of(condition.field(), rangeCondition));
            }
        };
    }

    private <T> SearchResult<T> parseSearchResponse(String responseBody, Class<T> clazz, long tookMs) {
        try {
            var root = objectMapper.readTree(responseBody);
            var hitsNode = root.path("hits");
            long totalHits = hitsNode.path("total").path("value").asLong(0);

            List<SearchResult.SearchHit<T>> hits = new ArrayList<>();
            for (var hitNode : hitsNode.path("hits")) {
                String id = hitNode.path("_id").asText();
                double score = hitNode.path("_score").asDouble(0);
                T source = objectMapper.treeToValue(hitNode.path("_source"), clazz);

                Map<String, List<String>> highlights = new LinkedHashMap<>();
                var highlightNode = hitNode.path("highlight");
                if (!highlightNode.isMissingNode()) {
                    var fieldIter = highlightNode.fields();
                    while (fieldIter.hasNext()) {
                        var entry = fieldIter.next();
                        List<String> fragments = new ArrayList<>();
                        for (var fragment : entry.getValue()) {
                            fragments.add(fragment.asText());
                        }
                        highlights.put(entry.getKey(), fragments);
                    }
                }

                hits.add(new SearchResult.SearchHit<>(id, score, source, highlights));
            }

            return new SearchResult<>(hits, totalHits, tookMs, Map.of());
        } catch (Exception e) {
            log.error("解析搜索响应失败: {}", e.getMessage());
            return SearchResult.empty();
        }
    }
}
