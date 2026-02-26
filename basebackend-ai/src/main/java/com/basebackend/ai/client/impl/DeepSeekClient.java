package com.basebackend.ai.client.impl;

import com.basebackend.ai.config.AiProperties;

/**
 * DeepSeek 客户端
 * <p>
 * DeepSeek 完全兼容 OpenAI API 格式，直接继承 OpenAiClient。
 */
public class DeepSeekClient extends OpenAiClient {

    public DeepSeekClient(AiProperties.ProviderConfig config) {
        super(config, "deepseek");
    }
}
