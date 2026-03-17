package com.basebackend.search.client;

import com.basebackend.search.config.SearchProperties;
import com.basebackend.search.model.IndexDefinition;
import com.basebackend.search.model.SearchResult;
import com.basebackend.search.query.SearchQuery;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("RestClientSearchClient 测试")
class RestClientSearchClientTest {

    private MockRestServiceServer mockServer;
    private RestClientSearchClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        SearchProperties properties = new SearchProperties();
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:9200");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new RestClientSearchClient(builder, properties, objectMapper);
    }

    @AfterEach
    void verify() {
        mockServer.verify();
    }

    // ==================== 索引管理 ====================

    @Nested
    @DisplayName("索引管理")
    class IndexManagementTest {

        @Test
        @DisplayName("createIndex — 成功")
        void createIndexSuccess() {
            mockServer.expect(requestTo("http://localhost:9200/articles"))
                    .andExpect(method(HttpMethod.PUT))
                    .andExpect(content().string(containsString("number_of_shards")))
                    .andExpect(content().string(containsString("properties")))
                    .andRespond(withSuccess("{\"acknowledged\":true}", MediaType.APPLICATION_JSON));

            IndexDefinition def = IndexDefinition.builder("articles")
                    .textField("title", "ik_max_word")
                    .keywordField("status")
                    .shards(1).replicas(1)
                    .build();

            assertThat(client.createIndex(def)).isTrue();
        }

        @Test
        @DisplayName("createIndex — 服务器错误返回 false")
        void createIndexFailure() {
            mockServer.expect(requestTo("http://localhost:9200/articles"))
                    .andExpect(method(HttpMethod.PUT))
                    .andRespond(withServerError());

            assertThat(client.createIndex(IndexDefinition.builder("articles").build())).isFalse();
        }

        @Test
        @DisplayName("createIndex — index/store 属性写入 mapping")
        void createIndexWithIndexAndStoreFields() {
            mockServer.expect(requestTo("http://localhost:9200/test"))
                    .andExpect(method(HttpMethod.PUT))
                    .andExpect(content().string(containsString("\"index\":false")))
                    .andExpect(content().string(containsString("\"store\":true")))
                    .andRespond(withSuccess("{\"acknowledged\":true}", MediaType.APPLICATION_JSON));

            IndexDefinition def = IndexDefinition.builder("test")
                    .field(new IndexDefinition.FieldMapping("raw", IndexDefinition.FieldType.TEXT,
                            null, null, false, true))
                    .build();

            client.createIndex(def);
        }

        @Test
        @DisplayName("deleteIndex — 成功")
        void deleteIndexSuccess() {
            mockServer.expect(requestTo("http://localhost:9200/articles"))
                    .andExpect(method(HttpMethod.DELETE))
                    .andRespond(withSuccess("{\"acknowledged\":true}", MediaType.APPLICATION_JSON));

            assertThat(client.deleteIndex("articles")).isTrue();
        }

        @Test
        @DisplayName("deleteIndex — 失败返回 false")
        void deleteIndexFailure() {
            mockServer.expect(requestTo("http://localhost:9200/articles"))
                    .andExpect(method(HttpMethod.DELETE))
                    .andRespond(withStatus(HttpStatus.NOT_FOUND));

            assertThat(client.deleteIndex("articles")).isFalse();
        }

        @Test
        @DisplayName("indexExists — 索引存在")
        void indexExists() {
            mockServer.expect(requestTo("http://localhost:9200/articles"))
                    .andExpect(method(HttpMethod.HEAD))
                    .andRespond(withSuccess());

            assertThat(client.indexExists("articles")).isTrue();
        }

        @Test
        @DisplayName("indexExists — 索引不存在")
        void indexNotExists() {
            mockServer.expect(requestTo("http://localhost:9200/articles"))
                    .andExpect(method(HttpMethod.HEAD))
                    .andRespond(withStatus(HttpStatus.NOT_FOUND));

            assertThat(client.indexExists("articles")).isFalse();
        }
    }

    // ==================== 文档操作 ====================

    @Nested
    @DisplayName("文档操作")
    class DocumentTest {

        @Test
        @DisplayName("index — 成功")
        void indexDocumentSuccess() {
            mockServer.expect(requestTo("http://localhost:9200/articles/_doc/1"))
                    .andExpect(method(HttpMethod.PUT))
                    .andRespond(withSuccess("{\"result\":\"created\"}", MediaType.APPLICATION_JSON));

            assertThat(client.index("articles", "1", Map.of("title", "Java"))).isTrue();
        }

        @Test
        @DisplayName("index — 失败返回 false")
        void indexDocumentFailure() {
            mockServer.expect(requestTo("http://localhost:9200/articles/_doc/1"))
                    .andExpect(method(HttpMethod.PUT))
                    .andRespond(withServerError());

            assertThat(client.index("articles", "1", Map.of("title", "Java"))).isFalse();
        }

        @Test
        @DisplayName("get — 文档存在时返回 source")
        void getDocumentFound() {
            String response = "{\"found\":true,\"_source\":{\"title\":\"Java虚拟线程\"}}";
            mockServer.expect(requestTo("http://localhost:9200/articles/_doc/1"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

            @SuppressWarnings("unchecked")
            Map<String, Object> doc = client.get("articles", "1", Map.class);
            assertThat(doc).containsEntry("title", "Java虚拟线程");
        }

        @Test
        @DisplayName("get — found=false 返回 null")
        void getDocumentNotFound() {
            mockServer.expect(requestTo("http://localhost:9200/articles/_doc/999"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess("{\"found\":false}", MediaType.APPLICATION_JSON));

            assertThat(client.get("articles", "999", Map.class)).isNull();
        }

        @Test
        @DisplayName("delete — 成功")
        void deleteDocumentSuccess() {
            mockServer.expect(requestTo("http://localhost:9200/articles/_doc/1"))
                    .andExpect(method(HttpMethod.DELETE))
                    .andRespond(withSuccess("{\"result\":\"deleted\"}", MediaType.APPLICATION_JSON));

            assertThat(client.delete("articles", "1")).isTrue();
        }

        @Test
        @DisplayName("count — 返回正确数量")
        void count() {
            mockServer.expect(requestTo("http://localhost:9200/articles/_count"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess("{\"count\":42}", MediaType.APPLICATION_JSON));

            assertThat(client.count("articles")).isEqualTo(42L);
        }
    }

    // ==================== 批量操作 ====================

    @Nested
    @DisplayName("批量操作")
    class BulkTest {

        @Test
        @DisplayName("bulkIndex — errors=false 返回全部成功数")
        void bulkIndexNoErrors() {
            String bulkResponse = """
                    {"errors":false,"items":[{"index":{"status":200}},{"index":{"status":201}}]}
                    """;
            mockServer.expect(requestTo("http://localhost:9200/_bulk"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(bulkResponse, MediaType.APPLICATION_JSON));

            Map<String, Object> docs = Map.of("1", Map.of("title", "doc1"), "2", Map.of("title", "doc2"));
            assertThat(client.bulkIndex("articles", docs)).isEqualTo(2);
        }

        @Test
        @DisplayName("bulkIndex — errors=true 只返回 2xx 条目数")
        void bulkIndexPartialErrors() {
            String bulkResponse = """
                    {"errors":true,"items":[
                      {"index":{"status":200}},
                      {"index":{"status":400,"error":{"reason":"mapping error"}}}
                    ]}
                    """;
            mockServer.expect(requestTo("http://localhost:9200/_bulk"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(bulkResponse, MediaType.APPLICATION_JSON));

            Map<String, Object> docs = Map.of("1", Map.of("v", 1), "2", Map.of("v", 2));
            assertThat(client.bulkIndex("articles", docs)).isEqualTo(1);
        }

        @Test
        @DisplayName("bulkIndex — ID 含引号时正确转义，不产生 JSON 注入")
        void bulkIndexIdWithSpecialChars() {
            String bulkResponse = """
                    {"errors":false,"items":[{"index":{"status":200}}]}
                    """;
            // 验证请求体中不含未转义的引号注入序列
            mockServer.expect(requestTo("http://localhost:9200/_bulk"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().string(not(containsString("\"_id\":id\"with"))))
                    .andRespond(withSuccess(bulkResponse, MediaType.APPLICATION_JSON));

            // id 含双引号，旧代码会导致 NDJSON 非法
            Map<String, Object> docs = Map.of("id\"with\"quotes", Map.of("title", "test"));
            assertThat(client.bulkIndex("articles", docs)).isEqualTo(1);
        }

        @Test
        @DisplayName("bulkIndex — 空文档列表返回 0")
        void bulkIndexEmpty() {
            assertThat(client.bulkIndex("articles", Map.of())).isZero();
        }

        @Test
        @DisplayName("bulkDelete — errors=false 返回全部成功数")
        void bulkDeleteNoErrors() {
            String bulkResponse = """
                    {"errors":false,"items":[{"delete":{"status":200}},{"delete":{"status":200}}]}
                    """;
            mockServer.expect(requestTo("http://localhost:9200/_bulk"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(bulkResponse, MediaType.APPLICATION_JSON));

            assertThat(client.bulkDelete("articles", List.of("1", "2"))).isEqualTo(2);
        }

        @Test
        @DisplayName("bulkDelete — 空列表返回 0")
        void bulkDeleteEmpty() {
            assertThat(client.bulkDelete("articles", List.of())).isZero();
        }

        @Test
        @DisplayName("bulkDelete — ID 含特殊字符时正确转义")
        void bulkDeleteIdWithSpecialChars() {
            String bulkResponse = """
                    {"errors":false,"items":[{"delete":{"status":200}}]}
                    """;
            mockServer.expect(requestTo("http://localhost:9200/_bulk"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().string(containsString("\\\"quoted\\\"")))
                    .andRespond(withSuccess(bulkResponse, MediaType.APPLICATION_JSON));

            assertThat(client.bulkDelete("articles", List.of("id\"quoted\""))).isEqualTo(1);
        }
    }

    // ==================== 搜索 ====================

    @Nested
    @DisplayName("搜索")
    class SearchTest {

        private static final String SIMPLE_RESPONSE = """
                {"hits":{"total":{"value":1},"hits":[
                  {"_id":"1","_score":1.5,"_source":{"title":"Java虚拟线程"}}
                ]}}
                """;

        @Test
        @DisplayName("search — match 查询，正确解析命中结果")
        void searchMatch() {
            mockServer.expect(requestTo("http://localhost:9200/articles/_search"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().string(containsString("\"match\"")))
                    .andRespond(withSuccess(SIMPLE_RESPONSE, MediaType.APPLICATION_JSON));

            SearchQuery query = SearchQuery.builder("articles")
                    .must(SearchQuery.Condition.match("title", "Java"))
                    .build();

            SearchResult<Map> result = client.search(query, Map.class);
            assertThat(result.totalHits()).isEqualTo(1);
            assertThat(result.hits()).hasSize(1);
            assertThat(result.hits().get(0).id()).isEqualTo("1");
            assertThat(result.hits().get(0).score()).isEqualTo(1.5);
        }

        @Test
        @DisplayName("search — match 带 boost 生成完整 match query")
        void searchMatchWithBoost() {
            mockServer.expect(requestTo("http://localhost:9200/articles/_search"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().string(containsString("\"boost\"")))
                    .andExpect(content().string(containsString("\"query\"")))
                    .andRespond(withSuccess(SIMPLE_RESPONSE, MediaType.APPLICATION_JSON));

            SearchQuery query = SearchQuery.builder("articles")
                    .must(SearchQuery.Condition.match("title", "Java", 2.0f))
                    .build();

            client.search(query, Map.class);
        }

        @Test
        @DisplayName("search — multi_match 跨多字段查询")
        void searchMultiMatch() {
            mockServer.expect(requestTo("http://localhost:9200/articles/_search"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().string(containsString("\"multi_match\"")))
                    .andExpect(content().string(containsString("\"fields\"")))
                    .andRespond(withSuccess(SIMPLE_RESPONSE, MediaType.APPLICATION_JSON));

            SearchQuery query = SearchQuery.builder("articles")
                    .must(SearchQuery.Condition.multiMatch("title,content,summary", "Java"))
                    .build();

            client.search(query, Map.class);
        }

        @Test
        @DisplayName("search — range 查询生成 gte/lte")
        void searchRange() {
            mockServer.expect(requestTo("http://localhost:9200/articles/_search"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().string(containsString("\"range\"")))
                    .andExpect(content().string(containsString("\"gte\"")))
                    .andExpect(content().string(containsString("\"lte\"")))
                    .andRespond(withSuccess(SIMPLE_RESPONSE, MediaType.APPLICATION_JSON));

            SearchQuery query = SearchQuery.builder("articles")
                    .filter(SearchQuery.Condition.range("createTime", "2024-01-01", "2024-12-31"))
                    .build();

            client.search(query, Map.class);
        }

        @Test
        @DisplayName("search — term/terms/prefix/wildcard/exists 条件均生成正确 DSL key")
        void searchVariousConditions() {
            mockServer.expect(requestTo("http://localhost:9200/articles/_search"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().string(containsString("\"term\"")))
                    .andExpect(content().string(containsString("\"terms\"")))
                    .andExpect(content().string(containsString("\"prefix\"")))
                    .andExpect(content().string(containsString("\"wildcard\"")))
                    .andExpect(content().string(containsString("\"exists\"")))
                    .andRespond(withSuccess(SIMPLE_RESPONSE, MediaType.APPLICATION_JSON));

            SearchQuery query = SearchQuery.builder("articles")
                    .must(SearchQuery.Condition.term("status", "published"))
                    .must(SearchQuery.Condition.terms("tags", List.of("java", "spring")))
                    .must(SearchQuery.Condition.prefix("title", "Java"))
                    .must(SearchQuery.Condition.wildcard("email", "*@example.com"))
                    .filter(SearchQuery.Condition.exists("avatar"))
                    .build();

            client.search(query, Map.class);
        }

        @Test
        @DisplayName("search — 高亮使用配置的 pre_tags / post_tags")
        void searchHighlightTags() {
            String responseWithHighlight = """
                    {"hits":{"total":{"value":1},"hits":[
                      {"_id":"1","_score":1.0,"_source":{"title":"test"},
                       "highlight":{"title":["<em>test</em>"]}}
                    ]}}
                    """;
            mockServer.expect(requestTo("http://localhost:9200/articles/_search"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().string(containsString("\"pre_tags\"")))
                    .andExpect(content().string(containsString("<em>")))
                    .andExpect(content().string(containsString("\"post_tags\"")))
                    .andRespond(withSuccess(responseWithHighlight, MediaType.APPLICATION_JSON));

            SearchQuery query = SearchQuery.builder("articles")
                    .must(SearchQuery.Condition.match("title", "test"))
                    .highlight("title")
                    .build();

            SearchResult<Map> result = client.search(query, Map.class);
            assertThat(result.hits().get(0).highlights()).containsKey("title");
            assertThat(result.hits().get(0).highlights().get("title")).contains("<em>test</em>");
        }

        @Test
        @DisplayName("search — source 过滤生成 includes/excludes")
        void searchSourceFiltering() {
            mockServer.expect(requestTo("http://localhost:9200/articles/_search"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().string(containsString("\"includes\"")))
                    .andExpect(content().string(containsString("\"excludes\"")))
                    .andRespond(withSuccess(SIMPLE_RESPONSE, MediaType.APPLICATION_JSON));

            SearchQuery query = SearchQuery.builder("articles")
                    .must(SearchQuery.Condition.match("title", "Java"))
                    .includes("title", "author")
                    .excludes("content")
                    .build();

            client.search(query, Map.class);
        }

        @Test
        @DisplayName("search — 排序生成正确 sort DSL")
        void searchSort() {
            mockServer.expect(requestTo("http://localhost:9200/articles/_search"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().string(containsString("\"sort\"")))
                    .andExpect(content().string(containsString("\"desc\"")))
                    .andRespond(withSuccess(SIMPLE_RESPONSE, MediaType.APPLICATION_JSON));

            SearchQuery query = SearchQuery.builder("articles")
                    .must(SearchQuery.Condition.match("title", "Java"))
                    .sortBy("createTime", SearchQuery.SortOrder.DESC)
                    .build();

            client.search(query, Map.class);
        }

        @Test
        @DisplayName("search — 无查询条件时不生成 query 节点")
        void searchNoConditions() {
            mockServer.expect(requestTo("http://localhost:9200/articles/_search"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().string(not(containsString("\"query\""))))
                    .andRespond(withSuccess(SIMPLE_RESPONSE, MediaType.APPLICATION_JSON));

            SearchQuery query = SearchQuery.builder("articles").page(1, 10).build();
            client.search(query, Map.class);
        }

        @Test
        @DisplayName("search — 服务器错误返回空结果")
        void searchServerError() {
            mockServer.expect(requestTo("http://localhost:9200/articles/_search"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withServerError());

            SearchQuery query = SearchQuery.builder("articles")
                    .must(SearchQuery.Condition.match("title", "Java"))
                    .build();

            SearchResult<Map> result = client.search(query, Map.class);
            assertThat(result.hasHits()).isFalse();
            assertThat(result.totalHits()).isZero();
        }

        @Test
        @DisplayName("search — 索引前缀正确附加到 URI")
        void searchWithIndexPrefix() {
            SearchProperties properties = new SearchProperties();
            properties.setIndexPrefix("prod_");

            RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:9200");
            MockRestServiceServer prefixServer = MockRestServiceServer.bindTo(builder).build();
            RestClientSearchClient prefixClient = new RestClientSearchClient(builder, properties, objectMapper);

            prefixServer.expect(requestTo("http://localhost:9200/prod_articles/_search"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(SIMPLE_RESPONSE, MediaType.APPLICATION_JSON));

            SearchQuery query = SearchQuery.builder("articles")
                    .must(SearchQuery.Condition.match("title", "test"))
                    .build();

            prefixClient.search(query, Map.class);
            prefixServer.verify();
        }
    }
}
