package com.basebackend.ai.client.impl;

import com.basebackend.ai.client.AiRequest;
import com.basebackend.ai.client.AiResponse;
import com.basebackend.ai.config.AiProperties;
import com.basebackend.ai.exception.AiException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OpenAiClient 重试测试")
class OpenAiClientRetryTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("chat 在 5xx 后可按 maxRetries 重试并成功")
    void chatRetryOnServerError() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        startServer("/chat/completions", exchange -> {
            int current = counter.incrementAndGet();
            if (current < 3) {
                writeResponse(exchange, 500, "{\"error\":\"temporary\"}");
                return;
            }
            writeResponse(exchange, 200, """
                    {"model":"gpt-4o","choices":[{"message":{"content":"ok"},"finish_reason":"stop"}],
                    "usage":{"prompt_tokens":1,"completion_tokens":1,"total_tokens":2}}
                    """);
        });

        OpenAiClient client = new OpenAiClient(buildConfig(2), "openai");
        AiResponse response = client.chat(AiRequest.of("hello"));

        assertThat(response.content()).isEqualTo("ok");
        assertThat(counter.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("chat 超过 maxRetries 后抛异常")
    void chatShouldFailWhenRetryExhausted() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        startServer("/chat/completions", exchange -> {
            counter.incrementAndGet();
            writeResponse(exchange, 500, "{\"error\":\"always-fail\"}");
        });

        OpenAiClient client = new OpenAiClient(buildConfig(1), "openai");

        assertThatThrownBy(() -> client.chat(AiRequest.of("hello")))
                .isInstanceOf(AiException.class)
                .hasMessageContaining("AI 请求失败");
        assertThat(counter.get()).isEqualTo(2);
    }

    private AiProperties.ProviderConfig buildConfig(int maxRetries) {
        AiProperties.ProviderConfig config = new AiProperties.ProviderConfig();
        config.setApiKey("test-key");
        config.setBaseUrl("http://localhost:" + server.getAddress().getPort());
        config.setDefaultModel("gpt-4o");
        config.setTimeout(Duration.ofSeconds(2));
        config.setMaxRetries(maxRetries);
        return config;
    }

    private void startServer(String path, HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(path, handler);
        server.start();
    }

    private void writeResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
        exchange.close();
    }
}
