package com.basebackend.ai.rag;

/**
 * 向量嵌入客户端接口
 */
public interface EmbeddingClient {

    /**
     * 将文本转换为向量嵌入
     *
     * @param text 文本
     * @return 向量数组
     */
    float[] embed(String text);

    /**
     * 获取嵌入维度
     *
     * @return 向量维度
     */
    int getDimension();
}
