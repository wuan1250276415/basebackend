package com.basebackend.ai.client.impl;

import com.basebackend.ai.client.AiClient;
import com.basebackend.ai.client.AiRequest;
import com.basebackend.ai.client.AiResponse;
import com.basebackend.ai.client.AiStreamCallback;
import com.basebackend.ai.client.AiUsage;
import com.basebackend.ai.config.AiProperties;
import com.basebackend.ai.exception.AiException;
import com.basebackend.ai.exception.AiRateLimitException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OpenAI 兼容客户端实现
 * <p>
 * 支持所有兼容 OpenAI API 格式的 Provider（OpenAI、DeepSeek、通义千问等）。
 * 各 Provider 子类只需覆盖 {@link #getProvider()} 即可。
 */
@Slf4j
public class OpenAiClient implements AiClient {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);
    private static final long BASE_RETRY_DELAY_MS = 300;
    private static final long MAX_RETRY_DELAY_MS = 5_000;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AiProperties.ProviderConfig config;
    private final String providerName;
    private final Duration timeout;
    private final int maxRetries;

    public OpenAiClient(AiProperties.ProviderConfig config, String providerName) {
        this.config = config;
        this.providerName = providerName;
        this.objectMapper = new ObjectMapper();
        this.timeout = normalizeTimeout(config.getTimeout());
        this.maxRetries = Math.max(config.getMaxRetries(), 0);
        this.restClient = buildRestClient(config);
    }

    @Override
    public AiResponse chat(AiRequest request) {
        long startTime = System.currentTimeMillis();
        String model = resolveModel(request);

        log.debug("[{}] 发起聊天请求, model={}, messages={}", providerName, model, request.getMessages().size());

        try {
            return executeWithRetry(() -> doChat(request, model, startTime), "聊天请求", model);
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

        log.debug("[{}] 发起流式请求, model={}", providerName, model);

        try {
            executeWithRetry(() -> {
                doStreamChat(request, callback, model, startTime);
                return null;
            }, "流式请求", model);
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

    private RestClient buildRestClient(AiProperties.ProviderConfig providerConfig) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMs = toTimeoutMillis(timeout);
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);

        return RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(providerConfig.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + providerConfig.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private String resolveModel(AiRequest request) {
        return request.getModel() != null ? request.getModel() : config.getDefaultModel();
    }

    private AiResponse doChat(AiRequest request, String model, long startTime) {
        Map<String, Object> body = buildRequestBody(request, model, false);
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
    }

    private void doStreamChat(AiRequest request, AiStreamCallback callback, String model, long startTime) {
        Map<String, Object> body = buildRequestBody(request, model, true);

        restClient.post()
                .uri("/chat/completions")
                .body(body)
                .exchange((req, resp) -> {
                    if (resp.getStatusCode().is2xxSuccessful()) {
                        processStream(resp.getBody(), callback, model, startTime);
                        return null;
                    }

                    if (resp.getStatusCode().value() == 429) {
                        throw new AiRateLimitException(providerName, parseRetryAfter(resp.getHeaders()));
                    }

                    String errorBody = new String(resp.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    throw new AiException("流式请求失败: " + errorBody, providerName, resp.getStatusCode().value());
                });
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
            throw new AiException("流式读取异常: " + e.getMessage(), providerName, 499, e);
        }
    }

    private <T> T executeWithRetry(RetryOperation<T> operation, String action, String model) throws Exception {
        int maxAttempts = Math.max(1, maxRetries + 1);
        long retryDelayMs = BASE_RETRY_DELAY_MS;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.execute();
            } catch (Exception ex) {
                if (attempt >= maxAttempts || !isRetryable(ex)) {
                    throw ex;
                }

                long currentDelay = resolveRetryDelayMs(ex, retryDelayMs);
                log.warn("[{}] {}失败，将重试: attempt={}/{}, model={}, delay={}ms",
                        providerName, action, attempt, maxAttempts, model, currentDelay, ex);

                sleepBeforeRetry(currentDelay);
                retryDelayMs = Math.min(retryDelayMs * 2, MAX_RETRY_DELAY_MS);
            }
        }

        throw new IllegalStateException("重试流程异常终止");
    }

    private boolean isRetryable(Exception ex) {
        if (ex instanceof AiRateLimitException) {
            return true;
        }

        if (ex instanceof AiException aiException) {
            int code = aiException.getStatusCode();
            return code == 0 || code == 408 || code == 429 || code >= 500;
        }

        return !(ex instanceof IllegalArgumentException);
    }

    private long resolveRetryDelayMs(Exception ex, long defaultDelayMs) {
        if (ex instanceof AiRateLimitException rateLimitException) {
            long retryAfterMs = rateLimitException.getRetryAfterMs();
            if (retryAfterMs > 0) {
                return Math.min(Math.max(retryAfterMs, defaultDelayMs), 30_000);
            }
        }
        return defaultDelayMs;
    }

    private void sleepBeforeRetry(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException("重试等待被中断", providerName, 0, e);
        }
    }

    private int toTimeoutMillis(Duration timeoutValue) {
        long millis = timeoutValue.toMillis();
        if (millis > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) millis;
    }

    private Duration normalizeTimeout(Duration configuredTimeout) {
        if (configuredTimeout == null || configuredTimeout.isZero() || configuredTimeout.isNegative()) {
            return DEFAULT_TIMEOUT;
        }
        return configuredTimeout;
    }

    private long parseRetryAfter(HttpHeaders headers) {
        String retryAfter = headers.getFirst("Retry-After");
        if (retryAfter != null) {
            try {
                return Long.parseLong(retryAfter) * 1000;
            } catch (NumberFormatException ignored) {
                try {
                    ZonedDateTime retryAt = ZonedDateTime.parse(retryAfter, DateTimeFormatter.RFC_1123_DATE_TIME);
                    long delay = Duration.between(ZonedDateTime.now(retryAt.getZone()), retryAt).toMillis();
                    return Math.max(delay, 0L);
                } catch (Exception ignoredDateFormat) {
                    // ignore
                }
            }
        }
        return 60_000; // 默认 60 秒
    }

    @FunctionalInterface
    private interface RetryOperation<T> {
        T execute() throws Exception;
    }
}
