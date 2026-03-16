package com.basebackend.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * AI 模块配置属性
 *
 * <pre>
 * basebackend:
 *   ai:
 *     enabled: true
 *     default-provider: openai
 *     providers:
 *       openai:
 *         enabled: true
 *         api-key: ${OPENAI_API_KEY:}
 *         base-url: https://api.openai.com/v1
 *         default-model: gpt-4o
 *       deepseek:
 *         enabled: true
 *         api-key: ${DEEPSEEK_API_KEY:}
 *         base-url: https://api.deepseek.com/v1
 *         default-model: deepseek-chat
 *       qianwen:
 *         enabled: true
 *         api-key: ${QIANWEN_API_KEY:}
 *         base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
 *         default-model: qwen-plus
 *     conversation:
 *       store-type: memory
 *       max-history: 20
 *       ttl: 1h
 *     token:
 *       tracking-enabled: true
 *     rag:
 *       chunk-size: 500
 *       chunk-overlap: 50
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "basebackend.ai")
public class AiProperties {

    /** 是否启用 AI 模块 */
    private boolean enabled = false;

    /** 默认 Provider */
    private String defaultProvider = "openai";

    /** Provider 解析严格模式（true=找不到即失败，false=允许回退） */
    private boolean strictProviderResolution = false;

    /** Provider 配置 */
    private Map<String, ProviderConfig> providers = new HashMap<>();

    /** 对话管理配置 */
    private ConversationConfig conversation = new ConversationConfig();

    /** Token 追踪配置 */
    private TokenConfig token = new TokenConfig();

    /** RAG 配置 */
    private RagConfig rag = new RagConfig();

    @Data
    public static class ProviderConfig {
        /** 是否启用 */
        private boolean enabled = true;
        /** API Key */
        private String apiKey;
        /** API Base URL */
        private String baseUrl;
        /** 默认模型 */
        private String defaultModel;
        /** 请求超时 */
        private Duration timeout = Duration.ofSeconds(60);
        /** 最大重试次数 */
        private int maxRetries = 3;
    }

    @Data
    public static class ConversationConfig {
        /** 存储类型：memory / redis */
        private String storeType = "memory";
        /** 最大历史消息数 */
        private int maxHistory = 20;
        /** 对话过期时间 */
        private Duration ttl = Duration.ofHours(1);
    }

    @Data
    public static class TokenConfig {
        /** 是否启用 Token 追踪 */
        private boolean trackingEnabled = true;
    }

    @Data
    public static class RagConfig {
        /** 文本分块大小（字符数） */
        private int chunkSize = 500;
        /** 分块重叠大小 */
        private int chunkOverlap = 50;
        /** 检索 Top-K */
        private int topK = 5;
        /** 相似度阈值 */
        private double similarityThreshold = 0.7;
    }
}
