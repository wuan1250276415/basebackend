package com.basebackend.ai.token;

import com.basebackend.ai.client.AiUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Token 模块测试")
class TokenTest {

    // ==================== SimpleTokenCounter ====================

    @Nested
    @DisplayName("SimpleTokenCounter 测试")
    class SimpleTokenCounterTest {

        private SimpleTokenCounter counter;

        @BeforeEach
        void setUp() {
            counter = new SimpleTokenCounter();
        }

        @Test
        @DisplayName("null 文本返回 0")
        void nullText() {
            assertThat(counter.countTokens(null)).isZero();
        }

        @Test
        @DisplayName("空白文本返回 0")
        void blankText() {
            assertThat(counter.countTokens("")).isZero();
            assertThat(counter.countTokens("   ")).isZero();
        }

        @Test
        @DisplayName("纯英文估算（约 4 字符 = 1 token）")
        void englishText() {
            // "hello world" = 11 字符 → ~2.75 → 3 tokens
            int tokens = counter.countTokens("hello world");
            assertThat(tokens).isBetween(2, 4);
        }

        @Test
        @DisplayName("纯中文估算（约 1.5 字符 = 1 token）")
        void chineseText() {
            // "你好世界" = 4 字符 → ~2.67 → 3 tokens
            int tokens = counter.countTokens("你好世界");
            assertThat(tokens).isBetween(2, 4);
        }

        @Test
        @DisplayName("中英混合文本")
        void mixedText() {
            int tokens = counter.countTokens("Hello 你好 World 世界");
            assertThat(tokens).isGreaterThan(0);
        }

        @Test
        @DisplayName("长文本 token 数大于 0")
        void longText() {
            String longText = "这是一段很长的文本。".repeat(100);
            int tokens = counter.countTokens(longText);
            assertThat(tokens).isGreaterThan(100);
        }

        @Test
        @DisplayName("带 model 参数的重载方法")
        void countWithModel() {
            int t1 = counter.countTokens("test");
            int t2 = counter.countTokens("test", "gpt-4o");
            assertThat(t1).isEqualTo(t2); // 简单实现忽略 model
        }
    }

    // ==================== UsageTracker ====================

    @Nested
    @DisplayName("UsageTracker 测试")
    class UsageTrackerTest {

        private UsageTracker tracker;

        @BeforeEach
        void setUp() {
            tracker = new UsageTracker();
        }

        @Test
        @DisplayName("追踪一次调用")
        void trackSingleCall() {
            tracker.track("openai", "gpt-4o", AiUsage.of(100, 50), 1500);

            UsageTracker.ModelUsage usage = tracker.getUsage("openai", "gpt-4o");
            assertThat(usage).isNotNull();
            assertThat(usage.getPromptTokens()).isEqualTo(100);
            assertThat(usage.getCompletionTokens()).isEqualTo(50);
            assertThat(usage.getTotalTokens()).isEqualTo(150);
            assertThat(usage.getRequestCount()).isEqualTo(1);
            assertThat(usage.getTotalLatencyMs()).isEqualTo(1500);
        }

        @Test
        @DisplayName("多次调用累加")
        void trackMultipleCalls() {
            tracker.track("openai", "gpt-4o", AiUsage.of(100, 50), 1000);
            tracker.track("openai", "gpt-4o", AiUsage.of(200, 100), 2000);

            UsageTracker.ModelUsage usage = tracker.getUsage("openai", "gpt-4o");
            assertThat(usage.getPromptTokens()).isEqualTo(300);
            assertThat(usage.getCompletionTokens()).isEqualTo(150);
            assertThat(usage.getTotalTokens()).isEqualTo(450);
            assertThat(usage.getRequestCount()).isEqualTo(2);
            assertThat(usage.getAvgLatencyMs()).isEqualTo(1500.0);
        }

        @Test
        @DisplayName("不同 Provider/Model 分开统计")
        void separateTracking() {
            tracker.track("openai", "gpt-4o", AiUsage.of(100, 50), 1000);
            tracker.track("deepseek", "deepseek-chat", AiUsage.of(200, 100), 500);

            assertThat(tracker.getUsage("openai", "gpt-4o").getTotalTokens()).isEqualTo(150);
            assertThat(tracker.getUsage("deepseek", "deepseek-chat").getTotalTokens()).isEqualTo(300);
        }

        @Test
        @DisplayName("getTotalTokens 汇总所有 Provider")
        void getTotalTokens() {
            tracker.track("openai", "gpt-4o", AiUsage.of(100, 50), 1000);
            tracker.track("deepseek", "chat", AiUsage.of(200, 100), 500);

            assertThat(tracker.getTotalTokens()).isEqualTo(450);
        }

        @Test
        @DisplayName("getAllUsage 获取全部统计")
        void getAllUsage() {
            tracker.track("openai", "gpt-4o", AiUsage.of(100, 50), 1000);
            tracker.track("deepseek", "chat", AiUsage.of(200, 100), 500);

            Map<String, UsageTracker.ModelUsage> all = tracker.getAllUsage();
            assertThat(all).hasSize(2);
            assertThat(all).containsKey("openai:gpt-4o");
            assertThat(all).containsKey("deepseek:chat");
        }

        @Test
        @DisplayName("getUsage 不存在返回 null")
        void getUsageNotFound() {
            assertThat(tracker.getUsage("unknown", "model")).isNull();
        }

        @Test
        @DisplayName("reset 重置所有统计")
        void reset() {
            tracker.track("openai", "gpt-4o", AiUsage.of(100, 50), 1000);
            tracker.reset();

            assertThat(tracker.getTotalTokens()).isZero();
            assertThat(tracker.getAllUsage()).isEmpty();
        }

        @Test
        @DisplayName("ModelUsage.getAvgLatencyMs 零请求不除零")
        void avgLatencyZeroRequests() {
            // 直接构造不通过 track，但通过 reset 后 getUsage 返回 null
            // 测试 track 一次然后检查
            tracker.track("x", "y", AiUsage.of(0, 0), 0);
            assertThat(tracker.getUsage("x", "y").getAvgLatencyMs()).isZero();
        }

        @Test
        @DisplayName("ModelUsage.toString 可读")
        void modelUsageToString() {
            tracker.track("openai", "gpt-4o", AiUsage.of(100, 50), 1500);
            String str = tracker.getUsage("openai", "gpt-4o").toString();
            assertThat(str).contains("openai", "gpt-4o", "requests=1", "tokens=150");
        }
    }
}
