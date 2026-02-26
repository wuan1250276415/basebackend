package com.basebackend.ai.token;

/**
 * Token 计数器接口
 */
public interface TokenCounter {

    /**
     * 估算文本的 Token 数量
     *
     * @param text 文本
     * @return 估算的 Token 数
     */
    int countTokens(String text);

    /**
     * 估算文本的 Token 数量（指定模型）
     *
     * @param text  文本
     * @param model 模型名称
     * @return 估算的 Token 数
     */
    default int countTokens(String text, String model) {
        return countTokens(text);
    }
}
