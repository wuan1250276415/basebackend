package com.basebackend.search.query;

import java.util.*;

/**
 * 搜索查询 DSL Builder
 * <p>
 * 类似 MyBatis-Plus 的 QueryWrapper，链式构建 ES 搜索查询。
 *
 * <pre>
 * SearchQuery query = SearchQuery.builder("articles")
 *     .must(Condition.match("title", "Java虚拟线程"))
 *     .must(Condition.term("status", "published"))
 *     .should(Condition.match("content", "Spring Boot"))
 *     .filter(Condition.range("createTime", "2024-01-01", null))
 *     .highlight("title", "content")
 *     .page(1, 20)
 *     .sortBy("createTime", SortOrder.DESC)
 *     .build();
 * </pre>
 */
public class SearchQuery {

    private final String indexName;
    private final List<Condition> mustConditions;
    private final List<Condition> shouldConditions;
    private final List<Condition> mustNotConditions;
    private final List<Condition> filterConditions;
    private final List<String> highlightFields;
    private final List<SortField> sortFields;
    private final List<String> sourceIncludes;
    private final List<String> sourceExcludes;
    private int from;
    private int size;
    private Integer minimumShouldMatch;

    private SearchQuery(String indexName) {
        this.indexName = indexName;
        this.mustConditions = new ArrayList<>();
        this.shouldConditions = new ArrayList<>();
        this.mustNotConditions = new ArrayList<>();
        this.filterConditions = new ArrayList<>();
        this.highlightFields = new ArrayList<>();
        this.sortFields = new ArrayList<>();
        this.sourceIncludes = new ArrayList<>();
        this.sourceExcludes = new ArrayList<>();
        this.from = 0;
        this.size = 20;
    }

    public static Builder builder(String indexName) {
        return new Builder(indexName);
    }

    /**
     * 创建带最大分页限制的 Builder。
     * <p>
     * 建议通过 {@code SearchProperties.getMaxPageSize()} 传入配置值，
     * 在服务层统一限制查询规模：
     * <pre>
     * SearchQuery.builder("articles", properties.getMaxPageSize())
     *     .must(Condition.match("title", "Java"))
     *     .build();
     * </pre>
     *
     * @param maxSize 允许的最大 size 值，超出时自动截断
     */
    public static Builder builder(String indexName, int maxSize) {
        return new Builder(indexName, maxSize);
    }

    // --- Getters ---

    public String getIndexName() { return indexName; }
    public List<Condition> getMustConditions() { return Collections.unmodifiableList(mustConditions); }
    public List<Condition> getShouldConditions() { return Collections.unmodifiableList(shouldConditions); }
    public List<Condition> getMustNotConditions() { return Collections.unmodifiableList(mustNotConditions); }
    public List<Condition> getFilterConditions() { return Collections.unmodifiableList(filterConditions); }
    public List<String> getHighlightFields() { return Collections.unmodifiableList(highlightFields); }
    public List<SortField> getSortFields() { return Collections.unmodifiableList(sortFields); }
    public List<String> getSourceIncludes() { return Collections.unmodifiableList(sourceIncludes); }
    public List<String> getSourceExcludes() { return Collections.unmodifiableList(sourceExcludes); }
    public int getFrom() { return from; }
    public int getSize() { return size; }
    public Integer getMinimumShouldMatch() { return minimumShouldMatch; }

    /** 是否有查询条件 */
    public boolean hasConditions() {
        return !mustConditions.isEmpty() || !shouldConditions.isEmpty()
                || !mustNotConditions.isEmpty() || !filterConditions.isEmpty();
    }

    // --- 查询条件 ---

    public record Condition(ConditionType type, String field, Object value, Object value2, Float boost) {

        public enum ConditionType {
            MATCH, MULTI_MATCH, TERM, TERMS, RANGE, PREFIX, WILDCARD, EXISTS, MATCH_PHRASE
        }

        /** 全文匹配 */
        public static Condition match(String field, Object value) {
            return new Condition(ConditionType.MATCH, field, value, null, null);
        }

        public static Condition match(String field, Object value, float boost) {
            return new Condition(ConditionType.MATCH, field, value, null, boost);
        }

        /** 短语匹配 */
        public static Condition matchPhrase(String field, Object value) {
            return new Condition(ConditionType.MATCH_PHRASE, field, value, null, null);
        }

        /**
         * 多字段全文匹配
         *
         * @param fields 逗号分隔的字段列表，支持权重语法，如 {@code "title^2,content,summary"}
         * @param value  查询文本
         */
        public static Condition multiMatch(String fields, Object value) {
            return new Condition(ConditionType.MULTI_MATCH, fields, value, null, null);
        }

        /** 精确匹配 */
        public static Condition term(String field, Object value) {
            return new Condition(ConditionType.TERM, field, value, null, null);
        }

        /** 多值精确匹配 */
        public static Condition terms(String field, List<?> values) {
            return new Condition(ConditionType.TERMS, field, values, null, null);
        }

        /** 范围查询 (gte → lte) */
        public static Condition range(String field, Object from, Object to) {
            return new Condition(ConditionType.RANGE, field, from, to, null);
        }

        /** 前缀查询 */
        public static Condition prefix(String field, String prefix) {
            return new Condition(ConditionType.PREFIX, field, prefix, null, null);
        }

        /** 通配符查询 */
        public static Condition wildcard(String field, String pattern) {
            return new Condition(ConditionType.WILDCARD, field, pattern, null, null);
        }

        /** 字段存在检查 */
        public static Condition exists(String field) {
            return new Condition(ConditionType.EXISTS, field, null, null, null);
        }
    }

    /** 排序字段 */
    public record SortField(String field, SortOrder order) {}

    public enum SortOrder { ASC, DESC }

    // --- Builder ---

    /** 默认最大分页大小，与 ES 默认值保持一致 */
    private static final int DEFAULT_MAX_SIZE = 10000;

    public static class Builder {
        private final SearchQuery query;
        private final int maxSize;

        Builder(String indexName) {
            this(indexName, DEFAULT_MAX_SIZE);
        }

        Builder(String indexName, int maxSize) {
            this.query = new SearchQuery(indexName);
            this.maxSize = maxSize > 0 ? maxSize : DEFAULT_MAX_SIZE;
        }

        public Builder must(Condition condition) {
            query.mustConditions.add(condition);
            return this;
        }

        public Builder should(Condition condition) {
            query.shouldConditions.add(condition);
            return this;
        }

        public Builder mustNot(Condition condition) {
            query.mustNotConditions.add(condition);
            return this;
        }

        public Builder filter(Condition condition) {
            query.filterConditions.add(condition);
            return this;
        }

        public Builder highlight(String... fields) {
            query.highlightFields.addAll(Arrays.asList(fields));
            return this;
        }

        public Builder sortBy(String field, SortOrder order) {
            query.sortFields.add(new SortField(field, order));
            return this;
        }

        public Builder sortByScore() {
            query.sortFields.add(new SortField("_score", SortOrder.DESC));
            return this;
        }

        public Builder page(int page, int size) {
            int effectiveSize = clampSize(size);
            query.from = Math.max(0, (page - 1) * effectiveSize);
            query.size = effectiveSize;
            return this;
        }

        public Builder from(int from) {
            query.from = Math.max(0, from);
            return this;
        }

        public Builder size(int size) {
            query.size = clampSize(size);
            return this;
        }

        private int clampSize(int size) {
            return Math.min(Math.max(1, size), maxSize);
        }

        public Builder includes(String... fields) {
            query.sourceIncludes.addAll(Arrays.asList(fields));
            return this;
        }

        public Builder excludes(String... fields) {
            query.sourceExcludes.addAll(Arrays.asList(fields));
            return this;
        }

        public Builder minimumShouldMatch(int min) {
            query.minimumShouldMatch = min;
            return this;
        }

        public SearchQuery build() {
            return query;
        }
    }
}
