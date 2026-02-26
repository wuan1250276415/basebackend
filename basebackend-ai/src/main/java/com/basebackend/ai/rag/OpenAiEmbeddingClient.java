package com.basebackend.ai.rag;

import com.basebackend.ai.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 基于 OpenAI 兼容 API 的向量嵌入客户端
 * <p>
 * 支持 OpenAI、DeepSeek、通义千问等兼容 Embedding API 的服务。
 */
@Slf4j
public class OpenAiEmbeddingClient implements EmbeddingClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final int dimension;

    public OpenAiEmbeddingClient(AiProperties.ProviderConfig config, String model, int dimension) {
        this.model = model;
        this.dimension = dimension;
        this.objectMapper = new ObjectMapper();
        this.restClient = RestClient.builder()
                .baseUrl(config.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public OpenAiEmbeddingClient(AiProperties.ProviderConfig config) {
        this(config, "text-embedding-3-small", 1536);
    }

    @Override
    public float[] embed(String text) {
        Map<String, Object> body = Map.of(
                "model", model,
                "input", text
        );

        try {
            String responseBody = restClient.post()
                    .uri("/embeddings")
                    .body(body)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (req, resp) -> {
                        String errorBody = new String(resp.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        throw new RuntimeException("Embedding 请求失败: " + errorBody);
                    })
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode embeddingNode = root.path("data").path(0).path("embedding");

            float[] vector = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                vector[i] = (float) embeddingNode.get(i).asDouble();
            }

            return vector;
        } catch (Exception e) {
            log.error("Embedding 请求异常, model={}", model, e);
            throw new RuntimeException("Embedding 请求异常: " + e.getMessage(), e);
        }
    }

    @Override
    public int getDimension() {
        return dimension;
    }
}
