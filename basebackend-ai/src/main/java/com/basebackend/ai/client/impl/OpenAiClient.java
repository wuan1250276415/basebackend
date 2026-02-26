package com.basebackend.ai.client.impl;

import com.basebackend.ai.client.*;
import com.basebackend.ai.config.AiProperties;
import com.basebackend.ai.exception.AiException;
import com.basebackend.ai.exception.AiRateLimitException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * OpenAI 兼容客户端实现
 * <p>
 * 支持所有兼容 OpenAI API 格式的 Provider（OpenAI、DeepSeek、通义千问等）。
 * 各 Provider 子类只需覆盖 {@link #getProvider()} 即可。
 */
@Slf4j
public class OpenAiClient implements AiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AiProperties.ProviderConfig config;
    private final String providerName;

    public OpenAiClient(AiProperties.ProviderConfig config, String providerName) {
        this.config = config;
        this.providerName = providerName;
        this.objectMapper = new ObjectMapper();
        this.restClient = RestClient.builder()
                .baseUrl(config.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public AiResponse chat(AiRequest request) {
        long startTime = System.currentTimeMillis();
        String model = resolveModel(request);

        Map<String, Object> body = buildRequestBody(request, model, false);

        log.debug("[{}] 发起聊天请求, model={}, messages={}", providerName, model, request.getMessages().size());

        try {
            String responseBody = restClient.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .onStatus(status -> status.value() == 429, (req, resp) -> {
                        throw new AiRateLimitException(providerName, parseRetryAfter(resp.getHeaders()));
                    })
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (req, resp) -> {
                        String errorBody = new String(resp.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        throw new AiException("AI 请求失败: " + errorBody, providerName, resp.getStatusCode().value());
                    })
                    .body(String.class);

            return parseResponse(responseBody, model, startTime);
        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("[{}] 聊天请求异常, model={}, 耗时={}ms", providerName, model, latency, e);
            throw new AiException("AI 请求异常: " + e.getMessage(), providerName, 0, e);
        }
    }

    @Override
    public void streamChat(AiRequest request, AiStreamCallback callback) {
        long startTime = System.currentTimeMillis();
        String model = resolveModel(request);

        Map<String, Object> body = buildRequestBody(request, model, true);

        log.debug("[{}] 发起流式请求, model={}", providerName, model);

        try {
            restClient.post()
                    .uri("/chat/completions")
                    .body(body)
                    .exchange((req, resp) -> {
                        if (resp.getStatusCode().is2xxSuccessful()) {
                            processStream(resp.getBody(), callback, model, startTime);
                        } else {
                            String errorBody = new String(resp.getBody().readAllBytes(), StandardCharsets.UTF_8);
                            callback.onError(new AiException("流式请求失败: " + errorBody, providerName, resp.getStatusCode().value()));
                        }
                        return null;
                    });
        } catch (AiException e) {
            callback.onError(e);
        } catch (Exception e) {
            log.error("[{}] 流式请求异常, model={}", providerName, model, e);
            callback.onError(new AiException("流式请求异常: " + e.getMessage(), providerName, 0, e));
        }
    }

    @Override
    public String getProvider() {
        return providerName;
    }

    // --- 内部方法 ---

    private String resolveModel(AiRequest request) {
        return request.getModel() != null ? request.getModel() : config.getDefaultModel();
    }

    private Map<String, Object> buildRequestBody(AiRequest request, String model, boolean stream) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", request.getMessages().stream()
                .map(m -> Map.of("role", m.role(), "content", m.content()))
                .toList());

        if (stream) {
            body.put("stream", true);
        }
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        }
        if (request.getTopP() != null) {
            body.put("top_p", request.getTopP());
        }
        if (request.getExtraParams() != null) {
            body.putAll(request.getExtraParams());
        }
        return body;
    }

    private AiResponse parseResponse(String responseBody, String model, long startTime) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choice = root.path("choices").path(0);
            String content = choice.path("message").path("content").asText("");
            String finishReason = choice.path("finish_reason").asText("stop");

            JsonNode usageNode = root.path("usage");
            AiUsage usage = new AiUsage(
                    usageNode.path("prompt_tokens").asInt(0),
                    usageNode.path("completion_tokens").asInt(0),
                    usageNode.path("total_tokens").asInt(0)
            );

            String actualModel = root.path("model").asText(model);
            long latency = System.currentTimeMillis() - startTime;

            log.debug("[{}] 请求完成, model={}, tokens={}, 耗时={}ms",
                    providerName, actualModel, usage.totalTokens(), latency);

            return AiResponse.of(content, actualModel, usage, finishReason, latency);
        } catch (JsonProcessingException e) {
            throw new AiException("解析响应失败: " + e.getMessage(), providerName, 0, e);
        }
    }

    private void processStream(java.io.InputStream inputStream, AiStreamCallback callback,
                               String model, long startTime) {
        StringBuilder fullContent = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || !line.startsWith("data: ")) {
                    continue;
                }

                String data = line.substring(6).trim();
                if ("[DONE]".equals(data)) {
                    break;
                }

                try {
                    JsonNode root = objectMapper.readTree(data);
                    JsonNode delta = root.path("choices").path(0).path("delta");
                    String content = delta.path("content").asText("");

                    if (!content.isEmpty()) {
                        fullContent.append(content);
                        callback.onToken(content);
                    }
                } catch (JsonProcessingException e) {
                    log.warn("[{}] 解析流式数据失败: {}", providerName, data, e);
                }
            }

            long latency = System.currentTimeMillis() - startTime;
            AiResponse response = AiResponse.of(
                    fullContent.toString(), model, AiUsage.empty(), "stop", latency
            );
            callback.onComplete(response);

        } catch (IOException e) {
            callback.onError(new AiException("流式读取异常: " + e.getMessage(), providerName, 0, e));
        }
    }

    private long parseRetryAfter(HttpHeaders headers) {
        String retryAfter = headers.getFirst("Retry-After");
        if (retryAfter != null) {
            try {
                return Long.parseLong(retryAfter) * 1000;
            } catch (NumberFormatException ignored) {
            }
        }
        return 60_000; // 默认 60 秒
    }
}
