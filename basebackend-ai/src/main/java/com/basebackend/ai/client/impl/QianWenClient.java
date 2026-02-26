package com.basebackend.ai.client.impl;

import com.basebackend.ai.config.AiProperties;

/**
 * 通义千问客户端
 * <p>
 * 通义千问 DashScope 兼容 OpenAI API 格式，直接继承 OpenAiClient。
 */
public class QianWenClient extends OpenAiClient {

    public QianWenClient(AiProperties.ProviderConfig config) {
        super(config, "qianwen");
    }
}
