package com.basebackend.ai.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("AI 异常体系测试")
class AiExceptionTest {

    @Test
    @DisplayName("AiException 基础构造")
    void basicException() {
        AiException e = new AiException("测试异常");
        assertThat(e.getMessage()).isEqualTo("测试异常");
        assertThat(e.getProvider()).isNull();
        assertThat(e.getStatusCode()).isZero();
    }

    @Test
    @DisplayName("AiException 带 cause")
    void exceptionWithCause() {
        RuntimeException cause = new RuntimeException("root");
        AiException e = new AiException("包装异常", cause);
        assertThat(e.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("AiException 带 provider 和 statusCode")
    void exceptionWithProviderAndStatus() {
        AiException e = new AiException("请求失败", "openai", 500);
        assertThat(e.getProvider()).isEqualTo("openai");
        assertThat(e.getStatusCode()).isEqualTo(500);
    }

    @Test
    @DisplayName("AiException 全参数构造")
    void exceptionFullConstructor() {
        RuntimeException cause = new RuntimeException("timeout");
        AiException e = new AiException("超时", "deepseek", 408, cause);
        assertThat(e.getMessage()).isEqualTo("超时");
        assertThat(e.getProvider()).isEqualTo("deepseek");
        assertThat(e.getStatusCode()).isEqualTo(408);
        assertThat(e.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("AiRateLimitException 限流异常")
    void rateLimitException() {
        AiRateLimitException e = new AiRateLimitException("openai", 30000);
        assertThat(e.getMessage()).contains("限流");
        assertThat(e.getProvider()).isEqualTo("openai");
        assertThat(e.getStatusCode()).isEqualTo(429);
        assertThat(e.getRetryAfterMs()).isEqualTo(30000);
    }

    @Test
    @DisplayName("AiRateLimitException 是 AiException 子类")
    void rateLimitIsAiException() {
        AiRateLimitException e = new AiRateLimitException("test", 1000);
        assertThat(e).isInstanceOf(AiException.class);
    }

    @Test
    @DisplayName("AiTokenLimitException Token 超限异常")
    void tokenLimitException() {
        AiTokenLimitException e = new AiTokenLimitException(200000, 128000);
        assertThat(e.getMessage()).contains("200000").contains("128000");
        assertThat(e.getRequestedTokens()).isEqualTo(200000);
        assertThat(e.getMaxTokens()).isEqualTo(128000);
    }

    @Test
    @DisplayName("AiTokenLimitException 是 AiException 子类")
    void tokenLimitIsAiException() {
        AiTokenLimitException e = new AiTokenLimitException(100, 50);
        assertThat(e).isInstanceOf(AiException.class);
    }
}
