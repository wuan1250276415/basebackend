package com.basebackend.ai.config;

import com.basebackend.ai.annotation.AIGenerateAspect;
import com.basebackend.ai.client.AiClient;
import com.basebackend.ai.client.impl.DeepSeekClient;
import com.basebackend.ai.client.impl.OpenAiClient;
import com.basebackend.ai.client.impl.QianWenClient;
import com.basebackend.ai.conversation.ConversationManager;
import com.basebackend.ai.conversation.InMemoryConversationManager;
import com.basebackend.ai.conversation.RedisConversationManager;
import com.basebackend.ai.prompt.PromptTemplateRegistry;
import com.basebackend.ai.rag.*;
import com.basebackend.ai.token.SimpleTokenCounter;
import com.basebackend.ai.token.TokenCounter;
import com.basebackend.ai.token.UsageTracker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 模块自动配置
 * <p>
 * 根据配置自动注册 AI 客户端、对话管理器、Prompt 模板、RAG 组件等。
 * 需要显式启用：{@code basebackend.ai.enabled=true}
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(AiProperties.class)
@ConditionalOnProperty(prefix = "basebackend.ai", name = "enabled", havingValue = "true")
public class AiAutoConfiguration {

    public AiAutoConfiguration() {
        log.info("AI 基础设施模块已启用");
    }

    // ==================== Provider 客户端 ====================

    @Bean
    @ConditionalOnMissingBean(name = "openAiClient")
    @ConditionalOnProperty(prefix = "basebackend.ai.providers.openai", name = "enabled", havingValue = "true")
    public AiClient openAiClient(AiProperties properties) {
        AiProperties.ProviderConfig config = properties.getProviders().get("openai");
        log.info("注册 AI Provider: openai, model={}", config.getDefaultModel());
        return new OpenAiClient(config, "openai");
    }

    @Bean
    @ConditionalOnMissingBean(name = "deepSeekClient")
    @ConditionalOnProperty(prefix = "basebackend.ai.providers.deepseek", name = "enabled", havingValue = "true")
    public AiClient deepSeekClient(AiProperties properties) {
        AiProperties.ProviderConfig config = properties.getProviders().get("deepseek");
        log.info("注册 AI Provider: deepseek, model={}", config.getDefaultModel());
        return new DeepSeekClient(config);
    }

    @Bean
    @ConditionalOnMissingBean(name = "qianWenClient")
    @ConditionalOnProperty(prefix = "basebackend.ai.providers.qianwen", name = "enabled", havingValue = "true")
    public AiClient qianWenClient(AiProperties properties) {
        AiProperties.ProviderConfig config = properties.getProviders().get("qianwen");
        log.info("注册 AI Provider: qianwen, model={}", config.getDefaultModel());
        return new QianWenClient(config);
    }

    /**
     * 默认 AI 客户端（根据 default-provider 配置选择）
     */
    @Bean
    @ConditionalOnMissingBean(name = "defaultAiClient")
    public AiClient defaultAiClient(AiProperties properties, Map<String, AiClient> allClients) {
        String defaultProvider = properties.getDefaultProvider();

        // 从已注册的客户端中查找匹配的默认 Provider
        for (Map.Entry<String, AiClient> entry : allClients.entrySet()) {
            if (entry.getValue().getProvider().equals(defaultProvider)) {
                log.info("默认 AI Provider: {}", defaultProvider);
                return entry.getValue();
            }
        }

        // 如果无匹配，使用第一个可用的
        if (!allClients.isEmpty()) {
            AiClient fallback = allClients.values().iterator().next();
            log.warn("默认 Provider '{}' 未找到，使用 '{}'", defaultProvider, fallback.getProvider());
            return fallback;
        }

        throw new IllegalStateException("没有可用的 AI Provider，请检查配置");
    }

    /**
     * AI 客户端映射表（provider name → client）
     */
    @Bean
    @ConditionalOnMissingBean(name = "aiClientMap")
    public Map<String, AiClient> aiClientMap(Map<String, AiClient> allClients) {
        Map<String, AiClient> clientMap = new HashMap<>();
        allClients.forEach((beanName, client) -> {
            if (!"defaultAiClient".equals(beanName)) {
                clientMap.put(client.getProvider(), client);
            }
        });
        return clientMap;
    }

    // ==================== Prompt 模板 ====================

    @Bean
    @ConditionalOnMissingBean
    public PromptTemplateRegistry promptTemplateRegistry() {
        log.info("注册 Prompt 模板引擎");
        return new PromptTemplateRegistry();
    }

    // ==================== 对话管理 ====================

    @Configuration
    @ConditionalOnProperty(prefix = "basebackend.ai.conversation", name = "store-type",
            havingValue = "memory", matchIfMissing = true)
    static class InMemoryConversationConfig {

        @Bean
        @ConditionalOnMissingBean
        public ConversationManager conversationManager(AiProperties properties) {
            AiProperties.ConversationConfig config = properties.getConversation();
            log.info("对话管理: 内存模式, maxHistory={}, ttl={}",
                    config.getMaxHistory(), config.getTtl());
            return new InMemoryConversationManager(
                    config.getMaxHistory(), config.getTtl().toMillis()
            );
        }
    }

    @Configuration
    @ConditionalOnProperty(prefix = "basebackend.ai.conversation", name = "store-type", havingValue = "redis")
    @ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
    static class RedisConversationConfig {

        @Bean
        @ConditionalOnMissingBean
        public ConversationManager conversationManager(StringRedisTemplate redisTemplate,
                                                        AiProperties properties) {
            AiProperties.ConversationConfig config = properties.getConversation();
            log.info("对话管理: Redis 模式, maxHistory={}, ttl={}",
                    config.getMaxHistory(), config.getTtl());
            return new RedisConversationManager(
                    redisTemplate, config.getMaxHistory(), config.getTtl()
            );
        }
    }

    // ==================== Token 计量 ====================

    @Bean
    @ConditionalOnMissingBean
    public TokenCounter tokenCounter() {
        return new SimpleTokenCounter();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "basebackend.ai.token", name = "tracking-enabled",
            havingValue = "true", matchIfMissing = true)
    public UsageTracker usageTracker() {
        log.info("Token 用量追踪已启用");
        return new UsageTracker();
    }

    // ==================== RAG ====================

    @Bean
    @ConditionalOnMissingBean
    public TextSplitter textSplitter(AiProperties properties) {
        AiProperties.RagConfig rag = properties.getRag();
        return new TextSplitter(rag.getChunkSize(), rag.getChunkOverlap());
    }

    @Bean
    @ConditionalOnMissingBean
    public VectorStore vectorStore() {
        log.info("向量存储: 内存模式（生产环境建议替换为 Milvus/ES）");
        return new SimpleVectorStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public RagService ragService(AiClient defaultAiClient,
                                  ObjectProvider<EmbeddingClient> embeddingClientProvider,
                                  VectorStore vectorStore,
                                  TextSplitter textSplitter,
                                  AiProperties properties) {
        EmbeddingClient embeddingClient = embeddingClientProvider.getIfAvailable();
        if (embeddingClient == null) {
            log.info("未配置 EmbeddingClient，RAG 检索功能不可用（可注入自定义实现）");
            return null;
        }
        AiProperties.RagConfig rag = properties.getRag();
        log.info("RAG 服务已启用, topK={}, threshold={}", rag.getTopK(), rag.getSimilarityThreshold());
        return new RagService(defaultAiClient, embeddingClient, vectorStore, textSplitter,
                rag.getTopK(), rag.getSimilarityThreshold());
    }

    // ==================== @AIGenerate 注解切面 ====================

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.aspectj.lang.ProceedingJoinPoint")
    public AIGenerateAspect aiGenerateAspect(Map<String, AiClient> aiClientMap,
                                              AiClient defaultAiClient,
                                              PromptTemplateRegistry templateRegistry) {
        log.info("@AIGenerate 注解切面已启用");
        return new AIGenerateAspect(aiClientMap, defaultAiClient, templateRegistry);
    }
}
