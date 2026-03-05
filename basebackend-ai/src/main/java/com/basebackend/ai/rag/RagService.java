package com.basebackend.ai.rag;

import com.basebackend.ai.client.AiClient;
import com.basebackend.ai.client.AiMessage;
import com.basebackend.ai.client.AiRequest;
import com.basebackend.ai.client.AiResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * RAG（检索增强生成）服务
 * <p>
 * 组合文本分割、向量嵌入、向量检索和 LLM 生成，实现基于文档的智能问答。
 *
 * <pre>
 * // 1. 加载文档
 * ragService.indexText("Java 25 新增了虚拟线程...", "java25-doc");
 *
 * // 2. 提问（自动检索相关上下文 + 生成回答）
 * AiResponse answer = ragService.query("虚拟线程有什么好处？");
 * </pre>
 */
@Slf4j
public class RagService {

    private final AiClient aiClient;
    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;
    private final TextSplitter textSplitter;
    private final int topK;
    private final double similarityThreshold;
    private final Map<String, List<String>> sourceChunkIds = new ConcurrentHashMap<>();
    private final Map<String, Object> sourceLocks = new ConcurrentHashMap<>();

    public RagService(AiClient aiClient, EmbeddingClient embeddingClient,
                      VectorStore vectorStore, TextSplitter textSplitter,
                      int topK, double similarityThreshold) {
        this.aiClient = aiClient;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
        this.textSplitter = textSplitter;
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
    }

    /**
     * 索引文本（分块 → 嵌入 → 存储）
     *
     * @param text     原始文本
     * @param sourceId 来源标识
     * @return 生成的文档块数量
     */
    public int indexText(String text, String sourceId) {
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException("sourceId 不能为空");
        }

        String safeText = text == null ? "" : text;
        List<String> chunks = textSplitter.split(safeText);
        log.info("文本索引: sourceId={}, 原文长度={}, 分块数={}", sourceId, safeText.length(), chunks.size());

        Object lock = sourceLocks.computeIfAbsent(sourceId, id -> new Object());
        synchronized (lock) {
            List<String> previousChunkIds = sourceChunkIds.getOrDefault(sourceId, List.of());
            Set<String> newChunkIds = new LinkedHashSet<>(chunks.size());

            for (int i = 0; i < chunks.size(); i++) {
                String chunkId = sourceId + "#" + i;
                String chunk = chunks.get(i);
                float[] vector = embeddingClient.embed(chunk);
                vectorStore.store(chunkId, chunk, vector);
                newChunkIds.add(chunkId);
            }

            for (String oldChunkId : previousChunkIds) {
                if (!newChunkIds.contains(oldChunkId)) {
                    vectorStore.delete(oldChunkId);
                }
            }

            if (newChunkIds.isEmpty()) {
                sourceChunkIds.remove(sourceId);
            } else {
                sourceChunkIds.put(sourceId, new ArrayList<>(newChunkIds));
            }
        }

        return chunks.size();
    }

    /**
     * RAG 查询（检索相关上下文 + LLM 生成回答）
     *
     * @param question 用户问题
     * @return AI 响应
     */
    public AiResponse query(String question) {
        return query(question, null);
    }

    /**
     * RAG 查询（带自定义系统提示）
     *
     * @param question     用户问题
     * @param systemPrompt 自定义系统提示（null 使用默认）
     * @return AI 响应
     */
    public AiResponse query(String question, String systemPrompt) {
        // 1. 检索相关文档块
        float[] queryVector = embeddingClient.embed(question);
        List<VectorStore.SearchResult> results = vectorStore.search(queryVector, topK, similarityThreshold);

        log.debug("RAG 检索: question='{}', 匹配数={}", question, results.size());

        // 2. 构建上下文
        String context = results.stream()
                .map(r -> r.content())
                .collect(Collectors.joining("\n\n---\n\n"));

        // 3. 构建 Prompt
        String defaultSystemPrompt = """
                你是一个知识问答助手。根据以下参考资料回答用户的问题。
                如果参考资料中没有相关信息，请如实告知。
                不要编造信息，只基于提供的资料回答。
                
                参考资料：
                %s""".formatted(context);

        String finalSystemPrompt = systemPrompt != null
                ? systemPrompt + "\n\n参考资料：\n" + context
                : defaultSystemPrompt;

        // 4. 调用 LLM
        AiRequest request = AiRequest.builder()
                .addMessage(AiMessage.system(finalSystemPrompt))
                .addMessage(AiMessage.user(question))
                .build();

        return aiClient.chat(request);
    }

    /**
     * 仅检索（不调用 LLM）
     *
     * @param question 查询文本
     * @return 匹配的文档块
     */
    public List<VectorStore.SearchResult> retrieve(String question) {
        float[] queryVector = embeddingClient.embed(question);
        return vectorStore.search(queryVector, topK, similarityThreshold);
    }

    /**
     * 清空所有索引数据
     */
    public void clearIndex() {
        vectorStore.clear();
        sourceChunkIds.clear();
        sourceLocks.clear();
        log.info("RAG 索引已清空");
    }
}
