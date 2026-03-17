package com.basebackend.search.client;

import com.basebackend.search.config.SearchProperties;
import com.basebackend.search.model.IndexDefinition;
import com.basebackend.search.model.SearchResult;
import com.basebackend.search.query.SearchQuery;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
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
    private final String highlightPreTag;
    private final String highlightPostTag;

    /**
     * 生产环境构造器 — 根据 {@link SearchProperties} 创建配置完整的 RestClient。
     */
    public RestClientSearchClient(SearchProperties properties, ObjectMapper objectMapper) {
        this(buildRestClientBuilder(properties), properties, objectMapper);
    }

    /**
     * 可测试构造器（package-private）— 接受外部传入的 {@link RestClient.Builder}，便于单元测试绑定 MockRestServiceServer。
     */
    RestClientSearchClient(RestClient.Builder restClientBuilder, SearchProperties properties, ObjectMapper objectMapper) {
        this.indexPrefix = properties.getIndexPrefix() != null ? properties.getIndexPrefix() : "";
        this.highlightPreTag = properties.getHighlightPreTag();
        this.highlightPostTag = properties.getHighlightPostTag();
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
    }

    private static RestClient.Builder buildRestClientBuilder(SearchProperties properties) {
        String baseUrl = properties.getUris().isEmpty()
                ? "http://localhost:9200"
                : properties.getUris().getFirst();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.of(properties.getConnectTimeout()))
                .setResponseTimeout(Timeout.of(properties.getSocketTimeout()))
                .build();

        var httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        String username = properties.getUsername();
        if (username != null && !username.isBlank()) {
            String credentials = Base64.getEncoder().encodeToString(
                    (username + ":" + properties.getPassword()).getBytes(StandardCharsets.UTF_8));
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + credentials);
        }

        return builder;
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
                // 使用 ObjectMapper 序列化 action 元数据，避免 JSON 注入
                Map<String, Object> action = Map.of("index",
                        Map.of("_index", fullIndex, "_id", entry.getKey()));
                bulkBody.append(objectMapper.writeValueAsString(action)).append("\n");
                bulkBody.append(objectMapper.writeValueAsString(entry.getValue())).append("\n");
            } catch (JsonProcessingException e) {
                log.warn("批量索引序列化失败: id={}", entry.getKey(), e);
            }
        }

        if (bulkBody.isEmpty()) return 0;

        try {
            String responseBody = restClient.post()
                    .uri("/_bulk")
                    .contentType(MediaType.valueOf("application/x-ndjson"))
                    .body(bulkBody.toString())
                    .retrieve()
                    .body(String.class);
            int successCount = parseBulkSuccessCount(responseBody);
            log.info("批量索引完成: index={}, total={}, success={}", indexName, documents.size(), successCount);
            return successCount;
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
            try {
                // 使用 ObjectMapper 序列化 action 元数据，避免 JSON 注入
                Map<String, Object> action = Map.of("delete",
                        Map.of("_index", fullIndex, "_id", id));
                bulkBody.append(objectMapper.writeValueAsString(action)).append("\n");
            } catch (JsonProcessingException e) {
                log.warn("批量删除序列化失败: id={}", id, e);
            }
        }

        if (bulkBody.isEmpty()) return 0;

        try {
            String responseBody = restClient.post()
                    .uri("/_bulk")
                    .contentType(MediaType.valueOf("application/x-ndjson"))
                    .body(bulkBody.toString())
                    .retrieve()
                    .body(String.class);
            int successCount = parseBulkSuccessCount(responseBody);
            log.info("批量删除完成: index={}, total={}, success={}", indexName, ids.size(), successCount);
            return successCount;
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

        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("number_of_shards", definition.getNumberOfShards());
        settings.put("number_of_replicas", definition.getNumberOfReplicas());
        body.put("settings", settings);

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
            if (!field.index()) {
                fieldMapping.put("index", false);
            }
            if (field.store()) {
                fieldMapping.put("store", true);
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

        if (!query.getSortFields().isEmpty()) {
            body.put("sort", query.getSortFields().stream()
                    .map(s -> Map.of(s.field(), Map.of("order", s.order().name().toLowerCase())))
                    .toList());
        }

        // 使用配置的 pre/post 高亮标签
        if (!query.getHighlightFields().isEmpty()) {
            Map<String, Object> highlightFields = new LinkedHashMap<>();
            for (String field : query.getHighlightFields()) {
                highlightFields.put(field, Map.of());
            }
            Map<String, Object> highlight = new LinkedHashMap<>();
            highlight.put("pre_tags", List.of(highlightPreTag));
            highlight.put("post_tags", List.of(highlightPostTag));
            highlight.put("fields", highlightFields);
            body.put("highlight", highlight);
        }

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
            case MATCH -> {
                if (condition.boost() != null) {
                    Map<String, Object> matchQuery = new LinkedHashMap<>();
                    matchQuery.put("query", condition.value());
                    matchQuery.put("boost", condition.boost());
                    yield Map.of("match", Map.of(condition.field(), matchQuery));
                }
                yield Map.of("match", Map.of(condition.field(), condition.value()));
            }
            case MULTI_MATCH -> {
                List<String> fields = Arrays.stream(condition.field().split(","))
                        .map(String::trim)
                        .filter(f -> !f.isBlank())
                        .toList();
                Map<String, Object> multiMatchQuery = new LinkedHashMap<>();
                multiMatchQuery.put("query", condition.value());
                multiMatchQuery.put("fields", fields);
                if (condition.boost() != null) {
                    multiMatchQuery.put("boost", condition.boost());
                }
                yield Map.of("multi_match", multiMatchQuery);
            }
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
                        var fieldEntry = fieldIter.next();
                        List<String> fragments = new ArrayList<>();
                        for (var fragment : fieldEntry.getValue()) {
                            fragments.add(fragment.asText());
                        }
                        highlights.put(fieldEntry.getKey(), fragments);
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

    /**
     * 解析 {@code _bulk} 响应，返回实际成功条目数。
     * <p>
     * 当 {@code errors=false} 时，所有 item 均成功；
     * 当 {@code errors=true} 时，逐条检查各 item 的 HTTP status（2xx 为成功）。
     */
    private int parseBulkSuccessCount(String responseBody) {
        try {
            var root = objectMapper.readTree(responseBody);
            if (!root.path("errors").asBoolean(false)) {
                return root.path("items").size();
            }
            int successCount = 0;
            for (var item : root.path("items")) {
                var actionNode = item.fields().next().getValue();
                int status = actionNode.path("status").asInt(0);
                if (status >= 200 && status < 300) {
                    successCount++;
                }
            }
            return successCount;
        } catch (Exception e) {
            log.warn("解析 bulk 响应失败: {}", e.getMessage());
            return 0;
        }
    }
}
