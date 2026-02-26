package com.basebackend.ai.exception;

/**
 * AI 限流异常（HTTP 429）
 */
public class AiRateLimitException extends AiException {

    private final long retryAfterMs;

    public AiRateLimitException(String provider, long retryAfterMs) {
        super("AI 服务限流，请稍后重试", provider, 429);
        this.retryAfterMs = retryAfterMs;
    }

    public long getRetryAfterMs() {
        return retryAfterMs;
    }
}
