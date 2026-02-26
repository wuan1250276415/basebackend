package com.basebackend.search.model;

import java.util.List;
import java.util.Map;

/**
 * 搜索结果
 *
 * @param <T> 文档类型
 */
public record SearchResult<T>(
        List<SearchHit<T>> hits,
        long totalHits,
        long tookMs,
        Map<String, List<AggregationBucket>> aggregations
) {
    /** 空结果 */
    public static <T> SearchResult<T> empty() {
        return new SearchResult<>(List.of(), 0, 0, Map.of());
    }

    /** 是否有结果 */
    public boolean hasHits() {
        return hits != null && !hits.isEmpty();
    }

    /** 搜索命中 */
    public record SearchHit<T>(
            String id,
            double score,
            T source,
            Map<String, List<String>> highlights
    ) {}

    /** 聚合桶 */
    public record AggregationBucket(String key, long docCount) {}
}
