package com.basebackend.ai.client;

/**
 * 统一 AI 客户端接口
 * <p>
 * 所有 LLM Provider（OpenAI、DeepSeek、通义千问等）实现此接口，
 * 业务方通过此接口调用，不关心底层 Provider。
 */
public interface AiClient {

    /**
     * 同步聊天
     *
     * @param request AI 请求
     * @return AI 响应
     */
    AiResponse chat(AiRequest request);

    /**
     * 流式聊天（SSE）
     *
     * @param request  AI 请求
     * @param callback 流式回调
     */
    void streamChat(AiRequest request, AiStreamCallback callback);

    /**
     * 获取 Provider 名称
     *
     * @return provider 标识（如 openai、deepseek、qianwen）
     */
    String getProvider();

    /**
     * 快捷方式：单轮对话
     */
    default AiResponse chat(String userMessage) {
        return chat(AiRequest.of(userMessage));
    }

    /**
     * 快捷方式：带系统提示的单轮对话
     */
    default AiResponse chat(String systemPrompt, String userMessage) {
        return chat(AiRequest.of(systemPrompt, userMessage));
    }
}
