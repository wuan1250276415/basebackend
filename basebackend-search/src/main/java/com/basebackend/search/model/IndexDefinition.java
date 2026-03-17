package com.basebackend.search.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 索引定义
 * <p>
 * 描述一个 ES 索引的结构：字段映射、设置、别名等。
 */
public class IndexDefinition {

    private final String indexName;
    private final Map<String, FieldMapping> fields;
    private int numberOfShards = 1;
    private int numberOfReplicas = 1;
    private String defaultAnalyzer;

    private IndexDefinition(String indexName) {
        this.indexName = indexName;
        this.fields = new HashMap<>();
    }

    public static Builder builder(String indexName) {
        return new Builder(indexName);
    }

    public String getIndexName() { return indexName; }
    public Map<String, FieldMapping> getFields() { return Collections.unmodifiableMap(fields); }
    public int getNumberOfShards() { return numberOfShards; }
    public int getNumberOfReplicas() { return numberOfReplicas; }
    public String getDefaultAnalyzer() { return defaultAnalyzer; }

    /**
     * 字段映射
     */
    public record FieldMapping(
            String name,
            FieldType type,
            String analyzer,
            String searchAnalyzer,
            boolean index,
            boolean store
    ) {
        public static FieldMapping text(String name, String analyzer) {
            return new FieldMapping(name, FieldType.TEXT, analyzer, null, true, false);
        }

        public static FieldMapping keyword(String name) {
            return new FieldMapping(name, FieldType.KEYWORD, null, null, true, false);
        }

        public static FieldMapping date(String name) {
            return new FieldMapping(name, FieldType.DATE, null, null, true, false);
        }

        public static FieldMapping integer(String name) {
            return new FieldMapping(name, FieldType.INTEGER, null, null, true, false);
        }

        public static FieldMapping longType(String name) {
            return new FieldMapping(name, FieldType.LONG, null, null, true, false);
        }

        public static FieldMapping bool(String name) {
            return new FieldMapping(name, FieldType.BOOLEAN, null, null, true, false);
        }

        public static FieldMapping object(String name) {
            return new FieldMapping(name, FieldType.OBJECT, null, null, true, false);
        }
    }

    public enum FieldType {
        TEXT, KEYWORD, INTEGER, LONG, FLOAT, DOUBLE, BOOLEAN, DATE, OBJECT, NESTED
    }

    public static class Builder {
        private final IndexDefinition definition;

        Builder(String indexName) {
            this.definition = new IndexDefinition(indexName);
        }

        public Builder field(FieldMapping field) {
            definition.fields.put(field.name(), field);
            return this;
        }

        public Builder textField(String name, String analyzer) {
            return field(FieldMapping.text(name, analyzer));
        }

        public Builder keywordField(String name) {
            return field(FieldMapping.keyword(name));
        }

        public Builder dateField(String name) {
            return field(FieldMapping.date(name));
        }

        public Builder integerField(String name) {
            return field(FieldMapping.integer(name));
        }

        public Builder longField(String name) {
            return field(FieldMapping.longType(name));
        }

        public Builder booleanField(String name) {
            return field(FieldMapping.bool(name));
        }

        public Builder shards(int shards) {
            definition.numberOfShards = shards;
            return this;
        }

        public Builder replicas(int replicas) {
            definition.numberOfReplicas = replicas;
            return this;
        }

        public Builder defaultAnalyzer(String analyzer) {
            definition.defaultAnalyzer = analyzer;
            return this;
        }

        public IndexDefinition build() {
            return definition;
        }
    }
}
