package com.basebackend.ai.config;

import com.basebackend.ai.client.AiClient;
import com.basebackend.ai.client.AiRequest;
import com.basebackend.ai.client.AiResponse;
import com.basebackend.ai.client.AiUsage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AiAutoConfiguration 测试")
class AiAutoConfigurationTest {

    private final AiAutoConfiguration configuration = new AiAutoConfiguration();

    @Test
    @DisplayName("严格模式下默认 Provider 不存在应 fail-fast")
    void strictDefaultProviderShouldFailFast() {
        AiProperties properties = new AiProperties();
        properties.setDefaultProvider("missing");
        properties.setStrictProviderResolution(true);

        Map<String, AiClient> allClients = Map.of("openAiClient", createClient("openai"));

        assertThatThrownBy(() -> configuration.defaultAiClient(properties, allClients))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("严格模式下默认 Provider");
    }

    @Test
    @DisplayName("非严格模式下默认 Provider 不存在可回退")
    void nonStrictDefaultProviderCanFallback() {
        AiProperties properties = new AiProperties();
        properties.setDefaultProvider("missing");
        properties.setStrictProviderResolution(false);

        Map<String, AiClient> allClients = Map.of("openAiClient", createClient("openai"));

        AiClient client = configuration.defaultAiClient(properties, allClients);
        assertThat(client.getProvider()).isEqualTo("openai");
    }

    @Test
    @DisplayName("Provider 缺少关键配置应失败")
    void providerWithoutApiKeyShouldFail() {
        AiProperties properties = new AiProperties();
        AiProperties.ProviderConfig providerConfig = new AiProperties.ProviderConfig();
        providerConfig.setApiKey("");
        providerConfig.setBaseUrl("https://api.openai.com/v1");
        providerConfig.setDefaultModel("gpt-4o");
        properties.getProviders().put("openai", providerConfig);

        assertThatThrownBy(() -> configuration.openAiClient(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("api-key");
    }

    @Test
    @DisplayName("RAG topK 非法应失败")
    void invalidTopKShouldFail() {
        AiProperties properties = new AiProperties();
        properties.getRag().setTopK(0);

        assertThatThrownBy(() -> configuration.textSplitter(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("top-k");
    }

    @Test
    @DisplayName("Provider timeout 非法应失败")
    void invalidTimeoutShouldFail() {
        AiProperties properties = new AiProperties();
        AiProperties.ProviderConfig providerConfig = new AiProperties.ProviderConfig();
        providerConfig.setApiKey("test-key");
        providerConfig.setBaseUrl("https://api.openai.com/v1");
        providerConfig.setDefaultModel("gpt-4o");
        providerConfig.setTimeout(Duration.ZERO);
        properties.getProviders().put("openai", providerConfig);

        assertThatThrownBy(() -> configuration.openAiClient(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("timeout");
    }

    private AiClient createClient(String provider) {
        return new AiClient() {
            @Override
            public AiResponse chat(AiRequest request) {
                return AiResponse.of("ok", "model", AiUsage.empty(), "stop", 1);
            }

            @Override
            public void streamChat(AiRequest request, com.basebackend.ai.client.AiStreamCallback callback) {
                callback.onComplete(AiResponse.of("ok", "model", AiUsage.empty(), "stop", 1));
            }

            @Override
            public String getProvider() {
                return provider;
            }
        };
    }
}
