package com.basebackend.ai.exception;

/**
 * Token 超限异常
 */
public class AiTokenLimitException extends AiException {

    private final int requestedTokens;
    private final int maxTokens;

    public AiTokenLimitException(int requestedTokens, int maxTokens) {
        super("Token 数量超出限制: 请求 %d, 最大 %d".formatted(requestedTokens, maxTokens));
        this.requestedTokens = requestedTokens;
        this.maxTokens = maxTokens;
    }

    public int getRequestedTokens() {
        return requestedTokens;
    }

    public int getMaxTokens() {
        return maxTokens;
    }
}
