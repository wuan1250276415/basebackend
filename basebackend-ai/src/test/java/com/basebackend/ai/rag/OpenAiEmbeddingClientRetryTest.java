package com.basebackend.ai.rag;

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

@DisplayName("OpenAiEmbeddingClient 重试测试")
class OpenAiEmbeddingClientRetryTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("embed 在 5xx 后可重试并成功")
    void embedRetryOnServerError() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        startServer("/embeddings", exchange -> {
            int current = counter.incrementAndGet();
            if (current < 2) {
                writeResponse(exchange, 500, "{\"error\":\"temporary\"}");
                return;
            }
            writeResponse(exchange, 200, "{\"data\":[{\"embedding\":[0.1,0.2,0.3]}]}");
        });

        OpenAiEmbeddingClient client = new OpenAiEmbeddingClient(buildConfig(1), "text-embedding-3-small", 3);
        float[] vector = client.embed("hello");

        assertThat(vector).containsExactly(0.1f, 0.2f, 0.3f);
        assertThat(counter.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("embed 超过重试次数后抛异常")
    void embedShouldFailWhenRetryExhausted() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        startServer("/embeddings", exchange -> {
            counter.incrementAndGet();
            writeResponse(exchange, 500, "{\"error\":\"always-fail\"}");
        });

        OpenAiEmbeddingClient client = new OpenAiEmbeddingClient(buildConfig(1), "text-embedding-3-small", 3);

        assertThatThrownBy(() -> client.embed("hello"))
                .isInstanceOf(AiException.class)
                .hasMessageContaining("Embedding 请求失败");
        assertThat(counter.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("embed 在 200 但 data 结构错误时抛解析异常")
    void embedShouldFailWhenDataStructureInvalid() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        startServer("/embeddings", exchange -> {
            counter.incrementAndGet();
            writeResponse(exchange, 200, "{\"data\":[]}");
        });

        OpenAiEmbeddingClient client = new OpenAiEmbeddingClient(buildConfig(0), "text-embedding-3-small", 3);

        assertThatThrownBy(() -> client.embed("hello"))
                .isInstanceOf(AiException.class)
                .hasMessageContaining("Embedding 响应解析失败");
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("embed 在 200 但 embedding 为空时抛解析异常")
    void embedShouldFailWhenEmbeddingEmpty() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        startServer("/embeddings", exchange -> {
            counter.incrementAndGet();
            writeResponse(exchange, 200, "{\"data\":[{\"embedding\":[]}]}");
        });

        OpenAiEmbeddingClient client = new OpenAiEmbeddingClient(buildConfig(0), "text-embedding-3-small", 3);

        assertThatThrownBy(() -> client.embed("hello"))
                .isInstanceOf(AiException.class)
                .hasMessageContaining("Embedding 响应解析失败");
        assertThat(counter.get()).isEqualTo(1);
    }

    private AiProperties.ProviderConfig buildConfig(int maxRetries) {
        AiProperties.ProviderConfig config = new AiProperties.ProviderConfig();
        config.setApiKey("test-key");
        config.setBaseUrl("http://localhost:" + server.getAddress().getPort());
        config.setDefaultModel("text-embedding-3-small");
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
