package com.basebackend.ai.client;

/**
 * AI 响应
 *
 * @param content      生成的内容
 * @param model        使用的模型
 * @param usage        Token 用量
 * @param finishReason 结束原因（stop / length / content_filter）
 * @param latencyMs    请求耗时（毫秒）
 */
public record AiResponse(
        String content,
        String model,
        AiUsage usage,
        String finishReason,
        long latencyMs
) {
    public static AiResponse of(String content, String model, AiUsage usage, String finishReason, long latencyMs) {
        return new AiResponse(content, model, usage, finishReason, latencyMs);
    }
}
