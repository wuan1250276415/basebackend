package com.basebackend.ai.rag;

import com.basebackend.ai.client.AiClient;
import com.basebackend.ai.client.AiMessage;
import com.basebackend.ai.client.AiRequest;
import com.basebackend.ai.client.AiResponse;
import com.basebackend.ai.client.AiStreamCallback;
import com.basebackend.ai.client.AiUsage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RagService 编排测试")
class RagServiceTest {

    @Test
    @DisplayName("同 sourceId 重建索引会清理旧块")
    void reindexShouldRemoveStaleChunks() {
        SimpleVectorStore vectorStore = new SimpleVectorStore();
        RagService ragService = new RagService(
                new NoopAiClient(),
                new LengthEmbeddingClient(),
                vectorStore,
                new TextSplitter(5, 0),
                5,
                0.0
        );

        assertThat(ragService.indexText("1234512345", "doc1")).isEqualTo(2);
        assertThat(ragService.indexText("abcdeabcde", "doc2")).isEqualTo(2);
        assertThat(vectorStore.size()).isEqualTo(4);

        assertThat(ragService.indexText("123", "doc1")).isEqualTo(1);
        assertThat(vectorStore.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("query 会拼装检索上下文并调用 AI")
    void queryShouldComposeContextAndInvokeAi() {
        RecordingAiClient aiClient = new RecordingAiClient();
        RagService ragService = new RagService(
                aiClient,
                new TopicEmbeddingClient(),
                new SimpleVectorStore(),
                new TextSplitter(200, 0),
                3,
                0.5
        );

        ragService.indexText("Java 虚拟线程可以提升并发吞吐。", "java-doc");
        ragService.indexText("Python 协程用于异步并发。", "python-doc");

        String question = "Java 虚拟线程有什么特点？";
        AiResponse response = ragService.query(question);

        assertThat(response.content()).isEqualTo("mock-answer");
        AiRequest request = aiClient.lastRequest();
        assertThat(request).isNotNull();
        assertThat(request.getMessages()).hasSize(2);
        assertThat(request.getMessages().getFirst().role()).isEqualTo(AiMessage.ROLE_SYSTEM);
        assertThat(request.getMessages().getFirst().content())
                .contains("参考资料")
                .contains("Java 虚拟线程可以提升并发吞吐");
        assertThat(request.getMessages().get(1).content()).isEqualTo(question);
    }

    @Test
    @DisplayName("retrieve 返回与问题最相关的文档块")
    void retrieveShouldReturnRelevantChunks() {
        RagService ragService = new RagService(
                new NoopAiClient(),
                new TopicEmbeddingClient(),
                new SimpleVectorStore(),
                new TextSplitter(200, 0),
                3,
                0.5
        );

        ragService.indexText("Java 记录类在 JDK16 引入。", "java-doc");
        ragService.indexText("Python 协程在异步任务中常见。", "python-doc");

        List<VectorStore.SearchResult> results = ragService.retrieve("Python 协程适合什么场景？");
        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().content()).contains("Python 协程");
    }

    private static class RecordingAiClient implements AiClient {
        private AiRequest lastRequest;

        @Override
        public AiResponse chat(AiRequest request) {
            this.lastRequest = request;
            return AiResponse.of("mock-answer", "mock-model", AiUsage.of(10, 20), "stop", 5);
        }

        @Override
        public void streamChat(AiRequest request, AiStreamCallback callback) {
            callback.onComplete(chat(request));
        }

        @Override
        public String getProvider() {
            return "mock";
        }

        public AiRequest lastRequest() {
            return lastRequest;
        }
    }

    private static class NoopAiClient implements AiClient {
        @Override
        public AiResponse chat(AiRequest request) {
            return AiResponse.of("noop", "noop-model", AiUsage.empty(), "stop", 1);
        }

        @Override
        public void streamChat(AiRequest request, AiStreamCallback callback) {
            callback.onComplete(chat(request));
        }

        @Override
        public String getProvider() {
            return "noop";
        }
    }

    private static class LengthEmbeddingClient implements EmbeddingClient {
        @Override
        public float[] embed(String text) {
            return new float[]{text.length(), 1f};
        }

        @Override
        public int getDimension() {
            return 2;
        }
    }

    private static class TopicEmbeddingClient implements EmbeddingClient {
        @Override
        public float[] embed(String text) {
            String lower = text.toLowerCase(Locale.ROOT);
            if (lower.contains("java")) {
                return new float[]{1f, 0f};
            }
            if (lower.contains("python")) {
                return new float[]{0f, 1f};
            }
            return new float[]{0.5f, 0.5f};
        }

        @Override
        public int getDimension() {
            return 2;
        }
    }
}
