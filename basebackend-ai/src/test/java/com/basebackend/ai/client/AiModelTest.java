package com.basebackend.ai.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("AI 客户端模型测试")
class AiModelTest {

    // ==================== AiMessage ====================

    @Nested
    @DisplayName("AiMessage 测试")
    class AiMessageTest {

        @Test
        @DisplayName("system() 创建系统消息")
        void systemMessage() {
            AiMessage msg = AiMessage.system("你是助手");
            assertThat(msg.role()).isEqualTo("system");
            assertThat(msg.content()).isEqualTo("你是助手");
        }

        @Test
        @DisplayName("user() 创建用户消息")
        void userMessage() {
            AiMessage msg = AiMessage.user("你好");
            assertThat(msg.role()).isEqualTo("user");
            assertThat(msg.content()).isEqualTo("你好");
        }

        @Test
        @DisplayName("assistant() 创建助手消息")
        void assistantMessage() {
            AiMessage msg = AiMessage.assistant("我是AI");
            assertThat(msg.role()).isEqualTo("assistant");
            assertThat(msg.content()).isEqualTo("我是AI");
        }

        @Test
        @DisplayName("Record equals 和 hashCode")
        void equalsAndHashCode() {
            AiMessage msg1 = AiMessage.user("hello");
            AiMessage msg2 = AiMessage.user("hello");
            AiMessage msg3 = AiMessage.system("hello");
            assertThat(msg1).isEqualTo(msg2);
            assertThat(msg1).isNotEqualTo(msg3);
            assertThat(msg1.hashCode()).isEqualTo(msg2.hashCode());
        }

        @Test
        @DisplayName("角色常量值正确")
        void roleConstants() {
            assertThat(AiMessage.ROLE_SYSTEM).isEqualTo("system");
            assertThat(AiMessage.ROLE_USER).isEqualTo("user");
            assertThat(AiMessage.ROLE_ASSISTANT).isEqualTo("assistant");
        }
    }

    // ==================== AiUsage ====================

    @Nested
    @DisplayName("AiUsage 测试")
    class AiUsageTest {

        @Test
        @DisplayName("of() 自动计算 totalTokens")
        void ofCalculatesTotal() {
            AiUsage usage = AiUsage.of(100, 50);
            assertThat(usage.promptTokens()).isEqualTo(100);
            assertThat(usage.completionTokens()).isEqualTo(50);
            assertThat(usage.totalTokens()).isEqualTo(150);
        }

        @Test
        @DisplayName("empty() 返回零值")
        void emptyUsage() {
            AiUsage usage = AiUsage.empty();
            assertThat(usage.promptTokens()).isZero();
            assertThat(usage.completionTokens()).isZero();
            assertThat(usage.totalTokens()).isZero();
        }

        @Test
        @DisplayName("直接构造函数")
        void directConstructor() {
            AiUsage usage = new AiUsage(200, 100, 300);
            assertThat(usage.totalTokens()).isEqualTo(300);
        }
    }

    // ==================== AiRequest ====================

    @Nested
    @DisplayName("AiRequest 测试")
    class AiRequestTest {

        @Test
        @DisplayName("of(userMessage) 快捷创建")
        void ofSingleMessage() {
            AiRequest req = AiRequest.of("你好");
            assertThat(req.getMessages()).hasSize(1);
            assertThat(req.getMessages().getFirst().role()).isEqualTo("user");
            assertThat(req.getMessages().getFirst().content()).isEqualTo("你好");
        }

        @Test
        @DisplayName("of(system, user) 快捷创建")
        void ofSystemAndUser() {
            AiRequest req = AiRequest.of("你是助手", "你好");
            assertThat(req.getMessages()).hasSize(2);
            assertThat(req.getMessages().get(0).role()).isEqualTo("system");
            assertThat(req.getMessages().get(1).role()).isEqualTo("user");
        }

        @Test
        @DisplayName("Builder 完整构建")
        void builderFullBuild() {
            AiRequest req = AiRequest.builder()
                    .model("gpt-4o")
                    .addMessage(AiMessage.system("系统提示"))
                    .addMessage(AiMessage.user("用户输入"))
                    .temperature(0.5)
                    .maxTokens(1000)
                    .topP(0.9)
                    .stream(true)
                    .extraParam("frequency_penalty", 0.5)
                    .build();

            assertThat(req.getModel()).isEqualTo("gpt-4o");
            assertThat(req.getMessages()).hasSize(2);
            assertThat(req.getTemperature()).isEqualTo(0.5);
            assertThat(req.getMaxTokens()).isEqualTo(1000);
            assertThat(req.getTopP()).isEqualTo(0.9);
            assertThat(req.getStream()).isTrue();
            assertThat(req.getExtraParams()).containsEntry("frequency_penalty", 0.5);
        }

        @Test
        @DisplayName("Builder 默认值为 null")
        void builderDefaults() {
            AiRequest req = AiRequest.builder()
                    .addMessage(AiMessage.user("test"))
                    .build();

            assertThat(req.getModel()).isNull();
            assertThat(req.getTemperature()).isNull();
            assertThat(req.getMaxTokens()).isNull();
            assertThat(req.getTopP()).isNull();
            assertThat(req.getStream()).isNull();
            assertThat(req.getExtraParams()).isEmpty();
        }

        @Test
        @DisplayName("Builder messages() 替换消息列表")
        void builderSetMessages() {
            List<AiMessage> messages = List.of(
                    AiMessage.user("msg1"),
                    AiMessage.user("msg2")
            );
            AiRequest req = AiRequest.builder()
                    .messages(messages)
                    .build();

            assertThat(req.getMessages()).hasSize(2);
        }
    }

    // ==================== AiResponse ====================

    @Nested
    @DisplayName("AiResponse 测试")
    class AiResponseTest {

        @Test
        @DisplayName("of() 工厂方法")
        void ofFactory() {
            AiUsage usage = AiUsage.of(50, 30);
            AiResponse resp = AiResponse.of("生成内容", "gpt-4o", usage, "stop", 1500);

            assertThat(resp.content()).isEqualTo("生成内容");
            assertThat(resp.model()).isEqualTo("gpt-4o");
            assertThat(resp.usage().totalTokens()).isEqualTo(80);
            assertThat(resp.finishReason()).isEqualTo("stop");
            assertThat(resp.latencyMs()).isEqualTo(1500);
        }

        @Test
        @DisplayName("Record toString 包含所有字段")
        void toStringContainsAllFields() {
            AiResponse resp = AiResponse.of("内容", "model", AiUsage.empty(), "stop", 100);
            String str = resp.toString();
            assertThat(str).contains("内容", "model", "stop");
        }
    }
}
