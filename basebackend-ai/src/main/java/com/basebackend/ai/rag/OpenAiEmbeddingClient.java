package com.basebackend.ai.rag;

import com.basebackend.ai.config.AiProperties;
import com.basebackend.ai.exception.AiException;
import com.basebackend.ai.exception.AiRateLimitException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 基于 OpenAI 兼容 API 的向量嵌入客户端
 * <p>
 * 支持 OpenAI、DeepSeek、通义千问等兼容 Embedding API 的服务。
 */
@Slf4j
public class OpenAiEmbeddingClient implements EmbeddingClient {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);
    private static final long BASE_RETRY_DELAY_MS = 300;
    private static final long MAX_RETRY_DELAY_MS = 5_000;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final int dimension;
    private final Duration timeout;
    private final int maxRetries;

    public OpenAiEmbeddingClient(AiProperties.ProviderConfig config, String model, int dimension) {
        this.model = model;
        this.dimension = dimension;
        this.objectMapper = new ObjectMapper();
        this.timeout = normalizeTimeout(config.getTimeout());
        this.maxRetries = Math.max(config.getMaxRetries(), 0);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMs = toTimeoutMillis(timeout);
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
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
            return executeWithRetry(() -> doEmbed(body));
        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Embedding 请求异常, model={}", model, e);
            throw new AiException("Embedding 请求异常: " + e.getMessage(), "embedding", 0, e);
        }
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    private float[] doEmbed(Map<String, Object> body) {
        String responseBody = restClient.post()
                .uri("/embeddings")
                .body(body)
                .retrieve()
                .onStatus(status -> status.value() == 429, (req, resp) -> {
                    throw new AiRateLimitException("embedding", parseRetryAfter(resp.getHeaders()));
                })
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (req, resp) -> {
                    String errorBody = new String(resp.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    throw new AiException("Embedding 请求失败: " + errorBody, "embedding", resp.getStatusCode().value());
                })
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode dataNode = root.path("data");
            if (!dataNode.isArray() || dataNode.isEmpty()) {
                throw new AiException("Embedding 响应解析失败: data 为空", "embedding", 0);
            }

            JsonNode embeddingNode = dataNode.get(0).path("embedding");
            if (!embeddingNode.isArray() || embeddingNode.isEmpty()) {
                throw new AiException("Embedding 响应解析失败: embedding 为空", "embedding", 0);
            }

            float[] vector = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                vector[i] = (float) embeddingNode.get(i).asDouble();
            }
            return vector;
        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            throw new AiException("Embedding 响应解析失败: " + e.getMessage(), "embedding", 0, e);
        }
    }

    private <T> T executeWithRetry(RetryOperation<T> operation) throws Exception {
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
                log.warn("Embedding 请求失败，将重试: attempt={}/{}, model={}, delay={}ms",
                        attempt, maxAttempts, model, currentDelay, ex);

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
            throw new AiException("Embedding 重试等待被中断", "embedding", 0, e);
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
        return 60_000;
    }

    @FunctionalInterface
    private interface RetryOperation<T> {
        T execute() throws Exception;
    }
}
