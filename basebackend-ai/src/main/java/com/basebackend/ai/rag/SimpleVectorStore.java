package com.basebackend.ai.rag;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存的向量存储
 * <p>
 * 使用余弦相似度进行检索，适用于小规模数据场景。
 * 生产环境建议替换为 Milvus / Pinecone / Elasticsearch 实现。
 */
public class SimpleVectorStore implements VectorStore {

    private final Map<String, VectorEntry> store = new ConcurrentHashMap<>();

    @Override
    public void store(String id, String content, float[] vector) {
        store.put(id, new VectorEntry(id, content, vector));
    }

    @Override
    public List<SearchResult> search(float[] queryVector, int topK, double threshold) {
        return store.values().stream()
                .map(entry -> new SearchResult(
                        entry.id(),
                        entry.content(),
                        cosineSimilarity(queryVector, entry.vector())
                ))
                .filter(r -> r.score() >= threshold)
                .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
                .limit(topK)
                .toList();
    }

    @Override
    public void delete(String id) {
        store.remove(id);
    }

    @Override
    public void clear() {
        store.clear();
    }

    /** 当前存储的文档块数量 */
    public int size() {
        return store.size();
    }

    /**
     * 余弦相似度
     */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("向量维度不匹配: %d vs %d".formatted(a.length, b.length));
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        return denominator == 0 ? 0 : dotProduct / denominator;
    }

    private record VectorEntry(String id, String content, float[] vector) {}
}
