package com.basebackend.search.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("搜索模型测试")
class SearchModelTest {

    // ==================== SearchResult ====================

    @Nested
    @DisplayName("SearchResult 测试")
    class SearchResultTest {

        @Test
        @DisplayName("empty() 返回空结果")
        void emptyResult() {
            SearchResult<String> result = SearchResult.empty();
            assertThat(result.totalHits()).isZero();
            assertThat(result.hits()).isEmpty();
            assertThat(result.tookMs()).isZero();
            assertThat(result.hasHits()).isFalse();
        }

        @Test
        @DisplayName("有结果时 hasHits 返回 true")
        void hasHitsTrue() {
            var hit = new SearchResult.SearchHit<>("1", 1.0, "doc", Map.of());
            SearchResult<String> result = new SearchResult<>(List.of(hit), 1, 10, Map.of());
            assertThat(result.hasHits()).isTrue();
            assertThat(result.totalHits()).isEqualTo(1);
        }

        @Test
        @DisplayName("SearchHit Record 字段")
        void searchHitFields() {
            var hit = new SearchResult.SearchHit<>("doc1", 0.95, "内容",
                    Map.of("title", List.of("<em>Java</em> 虚拟线程")));
            assertThat(hit.id()).isEqualTo("doc1");
            assertThat(hit.score()).isEqualTo(0.95);
            assertThat(hit.source()).isEqualTo("内容");
            assertThat(hit.highlights()).containsKey("title");
        }

        @Test
        @DisplayName("AggregationBucket Record")
        void aggregationBucket() {
            var bucket = new SearchResult.AggregationBucket("java", 42);
            assertThat(bucket.key()).isEqualTo("java");
            assertThat(bucket.docCount()).isEqualTo(42);
        }
    }

    // ==================== IndexDefinition ====================

    @Nested
    @DisplayName("IndexDefinition 测试")
    class IndexDefinitionTest {

        @Test
        @DisplayName("Builder 基本构建")
        void basicBuild() {
            IndexDefinition def = IndexDefinition.builder("articles")
                    .textField("title", "ik_max_word")
                    .keywordField("status")
                    .dateField("createTime")
                    .integerField("viewCount")
                    .longField("id")
                    .booleanField("published")
                    .shards(3)
                    .replicas(2)
                    .defaultAnalyzer("ik_smart")
                    .build();

            assertThat(def.getIndexName()).isEqualTo("articles");
            assertThat(def.getFields()).hasSize(6);
            assertThat(def.getNumberOfShards()).isEqualTo(3);
            assertThat(def.getNumberOfReplicas()).isEqualTo(2);
            assertThat(def.getDefaultAnalyzer()).isEqualTo("ik_smart");
        }

        @Test
        @DisplayName("默认分片和副本")
        void defaultShardsAndReplicas() {
            IndexDefinition def = IndexDefinition.builder("test").build();
            assertThat(def.getNumberOfShards()).isEqualTo(1);
            assertThat(def.getNumberOfReplicas()).isEqualTo(1);
        }

        @Test
        @DisplayName("FieldMapping.text 工厂方法")
        void fieldMappingText() {
            var field = IndexDefinition.FieldMapping.text("title", "ik_max_word");
            assertThat(field.name()).isEqualTo("title");
            assertThat(field.type()).isEqualTo(IndexDefinition.FieldType.TEXT);
            assertThat(field.analyzer()).isEqualTo("ik_max_word");
            assertThat(field.index()).isTrue();
        }

        @Test
        @DisplayName("FieldMapping.keyword 工厂方法")
        void fieldMappingKeyword() {
            var field = IndexDefinition.FieldMapping.keyword("status");
            assertThat(field.type()).isEqualTo(IndexDefinition.FieldType.KEYWORD);
            assertThat(field.analyzer()).isNull();
        }

        @Test
        @DisplayName("FieldMapping.date/integer/long/bool/object 工厂方法")
        void fieldMappingOtherTypes() {
            assertThat(IndexDefinition.FieldMapping.date("d").type()).isEqualTo(IndexDefinition.FieldType.DATE);
            assertThat(IndexDefinition.FieldMapping.integer("i").type()).isEqualTo(IndexDefinition.FieldType.INTEGER);
            assertThat(IndexDefinition.FieldMapping.longType("l").type()).isEqualTo(IndexDefinition.FieldType.LONG);
            assertThat(IndexDefinition.FieldMapping.bool("b").type()).isEqualTo(IndexDefinition.FieldType.BOOLEAN);
            assertThat(IndexDefinition.FieldMapping.object("o").type()).isEqualTo(IndexDefinition.FieldType.OBJECT);
        }

        @Test
        @DisplayName("FieldType 枚举完整")
        void fieldTypeEnum() {
            assertThat(IndexDefinition.FieldType.values()).containsExactlyInAnyOrder(
                    IndexDefinition.FieldType.TEXT,
                    IndexDefinition.FieldType.KEYWORD,
                    IndexDefinition.FieldType.INTEGER,
                    IndexDefinition.FieldType.LONG,
                    IndexDefinition.FieldType.FLOAT,
                    IndexDefinition.FieldType.DOUBLE,
                    IndexDefinition.FieldType.BOOLEAN,
                    IndexDefinition.FieldType.DATE,
                    IndexDefinition.FieldType.OBJECT,
                    IndexDefinition.FieldType.NESTED
            );
        }

        @Test
        @DisplayName("自定义 FieldMapping")
        void customFieldMapping() {
            var field = IndexDefinition.builder("test")
                    .field(new IndexDefinition.FieldMapping("custom", IndexDefinition.FieldType.TEXT,
                            "ik_max_word", "ik_smart", true, true))
                    .build();

            var customField = field.getFields().get("custom");
            assertThat(customField.analyzer()).isEqualTo("ik_max_word");
            assertThat(customField.searchAnalyzer()).isEqualTo("ik_smart");
            assertThat(customField.store()).isTrue();
        }
    }

    // ==================== SearchProperties ====================

    @Nested
    @DisplayName("SearchProperties 测试")
    class SearchPropertiesTest {

        @Test
        @DisplayName("getFullIndexName 无前缀")
        void fullIndexNameNoPrefix() {
            var props = new com.basebackend.search.config.SearchProperties();
            assertThat(props.getFullIndexName("articles")).isEqualTo("articles");
        }

        @Test
        @DisplayName("getFullIndexName 有前缀")
        void fullIndexNameWithPrefix() {
            var props = new com.basebackend.search.config.SearchProperties();
            props.setIndexPrefix("prod_");
            assertThat(props.getFullIndexName("articles")).isEqualTo("prod_articles");
        }

        @Test
        @DisplayName("默认值正确")
        void defaults() {
            var props = new com.basebackend.search.config.SearchProperties();
            assertThat(props.isEnabled()).isFalse();
            assertThat(props.getUris()).containsExactly("http://localhost:9200");
            assertThat(props.getDefaultAnalyzer()).isEqualTo("ik_max_word");
            assertThat(props.getDefaultSearchAnalyzer()).isEqualTo("ik_smart");
            assertThat(props.getDefaultPageSize()).isEqualTo(20);
            assertThat(props.getMaxPageSize()).isEqualTo(1000);
            assertThat(props.getHighlightPreTag()).isEqualTo("<em>");
            assertThat(props.getHighlightPostTag()).isEqualTo("</em>");
        }
    }
}
