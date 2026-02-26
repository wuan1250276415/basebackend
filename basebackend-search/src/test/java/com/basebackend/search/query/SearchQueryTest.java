package com.basebackend.search.query;

import com.basebackend.search.query.SearchQuery.Condition;
import com.basebackend.search.query.SearchQuery.SortOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SearchQuery DSL Builder 测试")
class SearchQueryTest {

    @Test
    @DisplayName("基本构建")
    void basicBuild() {
        SearchQuery query = SearchQuery.builder("articles")
                .must(Condition.match("title", "Java"))
                .build();

        assertThat(query.getIndexName()).isEqualTo("articles");
        assertThat(query.getMustConditions()).hasSize(1);
        assertThat(query.hasConditions()).isTrue();
    }

    @Test
    @DisplayName("多条件组合查询")
    void multipleConditions() {
        SearchQuery query = SearchQuery.builder("articles")
                .must(Condition.match("title", "Java"))
                .must(Condition.term("status", "published"))
                .should(Condition.match("content", "Spring"))
                .mustNot(Condition.term("deleted", true))
                .filter(Condition.range("createTime", "2024-01-01", "2024-12-31"))
                .build();

        assertThat(query.getMustConditions()).hasSize(2);
        assertThat(query.getShouldConditions()).hasSize(1);
        assertThat(query.getMustNotConditions()).hasSize(1);
        assertThat(query.getFilterConditions()).hasSize(1);
    }

    @Test
    @DisplayName("分页设置")
    void pagination() {
        SearchQuery query = SearchQuery.builder("test")
                .page(3, 20)
                .build();

        assertThat(query.getFrom()).isEqualTo(40); // (3-1)*20
        assertThat(query.getSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("分页参数保护 - 负页码")
    void paginationNegativePage() {
        SearchQuery query = SearchQuery.builder("test")
                .page(-1, 10)
                .build();

        assertThat(query.getFrom()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("排序")
    void sorting() {
        SearchQuery query = SearchQuery.builder("test")
                .sortBy("createTime", SortOrder.DESC)
                .sortByScore()
                .build();

        assertThat(query.getSortFields()).hasSize(2);
        assertThat(query.getSortFields().get(0).field()).isEqualTo("createTime");
        assertThat(query.getSortFields().get(0).order()).isEqualTo(SortOrder.DESC);
        assertThat(query.getSortFields().get(1).field()).isEqualTo("_score");
    }

    @Test
    @DisplayName("高亮字段")
    void highlight() {
        SearchQuery query = SearchQuery.builder("test")
                .highlight("title", "content", "summary")
                .build();

        assertThat(query.getHighlightFields()).containsExactly("title", "content", "summary");
    }

    @Test
    @DisplayName("source 过滤")
    void sourceFiltering() {
        SearchQuery query = SearchQuery.builder("test")
                .includes("title", "author")
                .excludes("content")
                .build();

        assertThat(query.getSourceIncludes()).containsExactly("title", "author");
        assertThat(query.getSourceExcludes()).containsExactly("content");
    }

    @Test
    @DisplayName("minimumShouldMatch")
    void minimumShouldMatch() {
        SearchQuery query = SearchQuery.builder("test")
                .should(Condition.match("a", "1"))
                .should(Condition.match("b", "2"))
                .minimumShouldMatch(1)
                .build();

        assertThat(query.getMinimumShouldMatch()).isEqualTo(1);
    }

    @Test
    @DisplayName("无条件时 hasConditions 返回 false")
    void noConditions() {
        SearchQuery query = SearchQuery.builder("test").build();
        assertThat(query.hasConditions()).isFalse();
    }

    @Test
    @DisplayName("默认分页参数")
    void defaultPagination() {
        SearchQuery query = SearchQuery.builder("test").build();
        assertThat(query.getFrom()).isZero();
        assertThat(query.getSize()).isEqualTo(20);
    }

    // ==================== Condition 工厂方法 ====================

    @Test
    @DisplayName("Condition.match 全文匹配")
    void conditionMatch() {
        Condition c = Condition.match("title", "Java");
        assertThat(c.type()).isEqualTo(Condition.ConditionType.MATCH);
        assertThat(c.field()).isEqualTo("title");
        assertThat(c.value()).isEqualTo("Java");
    }

    @Test
    @DisplayName("Condition.match 带 boost")
    void conditionMatchWithBoost() {
        Condition c = Condition.match("title", "Java", 2.0f);
        assertThat(c.boost()).isEqualTo(2.0f);
    }

    @Test
    @DisplayName("Condition.matchPhrase 短语匹配")
    void conditionMatchPhrase() {
        Condition c = Condition.matchPhrase("title", "Java虚拟线程");
        assertThat(c.type()).isEqualTo(Condition.ConditionType.MATCH_PHRASE);
    }

    @Test
    @DisplayName("Condition.term 精确匹配")
    void conditionTerm() {
        Condition c = Condition.term("status", "active");
        assertThat(c.type()).isEqualTo(Condition.ConditionType.TERM);
        assertThat(c.field()).isEqualTo("status");
    }

    @Test
    @DisplayName("Condition.terms 多值匹配")
    void conditionTerms() {
        Condition c = Condition.terms("tags", java.util.List.of("java", "spring"));
        assertThat(c.type()).isEqualTo(Condition.ConditionType.TERMS);
    }

    @Test
    @DisplayName("Condition.range 范围查询")
    void conditionRange() {
        Condition c = Condition.range("age", 18, 65);
        assertThat(c.type()).isEqualTo(Condition.ConditionType.RANGE);
        assertThat(c.value()).isEqualTo(18);
        assertThat(c.value2()).isEqualTo(65);
    }

    @Test
    @DisplayName("Condition.range 单边范围")
    void conditionRangeOneSided() {
        Condition c = Condition.range("price", 100, null);
        assertThat(c.value()).isEqualTo(100);
        assertThat(c.value2()).isNull();
    }

    @Test
    @DisplayName("Condition.prefix 前缀查询")
    void conditionPrefix() {
        Condition c = Condition.prefix("name", "张");
        assertThat(c.type()).isEqualTo(Condition.ConditionType.PREFIX);
    }

    @Test
    @DisplayName("Condition.wildcard 通配符查询")
    void conditionWildcard() {
        Condition c = Condition.wildcard("email", "*@example.com");
        assertThat(c.type()).isEqualTo(Condition.ConditionType.WILDCARD);
    }

    @Test
    @DisplayName("Condition.exists 字段存在")
    void conditionExists() {
        Condition c = Condition.exists("avatar");
        assertThat(c.type()).isEqualTo(Condition.ConditionType.EXISTS);
        assertThat(c.field()).isEqualTo("avatar");
    }
}
