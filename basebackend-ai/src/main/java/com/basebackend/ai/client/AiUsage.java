package com.basebackend.ai.client;

/**
 * AI 用量统计
 *
 * @param promptTokens     输入 Token 数
 * @param completionTokens 输出 Token 数
 * @param totalTokens      总 Token 数
 */
public record AiUsage(int promptTokens, int completionTokens, int totalTokens) {

    public static AiUsage of(int prompt, int completion) {
        return new AiUsage(prompt, completion, prompt + completion);
    }

    public static AiUsage empty() {
        return new AiUsage(0, 0, 0);
    }
}
