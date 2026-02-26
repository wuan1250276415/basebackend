package com.basebackend.ai.rag;

import java.util.List;

/**
 * 向量存储接口
 */
public interface VectorStore {

    /**
     * 存储文档块及其向量
     *
     * @param id      文档块 ID
     * @param content 文本内容
     * @param vector  向量嵌入
     */
    void store(String id, String content, float[] vector);

    /**
     * 相似度检索
     *
     * @param queryVector 查询向量
     * @param topK        返回最相似的 K 个结果
     * @param threshold   相似度阈值（0-1）
     * @return 匹配的文档块列表
     */
    List<SearchResult> search(float[] queryVector, int topK, double threshold);

    /**
     * 删除文档块
     */
    void delete(String id);

    /**
     * 清空所有数据
     */
    void clear();

    /**
     * 检索结果
     */
    record SearchResult(String id, String content, double score) {}
}
