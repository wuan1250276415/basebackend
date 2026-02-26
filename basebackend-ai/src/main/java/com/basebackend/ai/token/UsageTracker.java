package com.basebackend.ai.token;

import com.basebackend.ai.client.AiUsage;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Token 用量追踪器
 * <p>
 * 按 Provider + Model 维度统计 Token 用量和请求次数，用于成本分析。
 */
@Slf4j
public class UsageTracker {

    private final Map<String, ModelUsage> usageMap = new ConcurrentHashMap<>();

    /**
     * 记录一次 AI 调用的用量
     */
    public void track(String provider, String model, AiUsage usage, long latencyMs) {
        String key = provider + ":" + model;
        usageMap.computeIfAbsent(key, k -> new ModelUsage(provider, model))
                .record(usage, latencyMs);

        log.debug("Token 用量追踪: {}:{} prompt={} completion={} total={} latency={}ms",
                provider, model, usage.promptTokens(), usage.completionTokens(),
                usage.totalTokens(), latencyMs);
    }

    /**
     * 获取指定模型的用量统计
     */
    public ModelUsage getUsage(String provider, String model) {
        return usageMap.get(provider + ":" + model);
    }

    /**
     * 获取所有用量统计
     */
    public Map<String, ModelUsage> getAllUsage() {
        return Map.copyOf(usageMap);
    }

    /**
     * 重置所有统计
     */
    public void reset() {
        usageMap.clear();
    }

    /**
     * 获取总 Token 消耗
     */
    public long getTotalTokens() {
        return usageMap.values().stream()
                .mapToLong(u -> u.totalTokens.get())
                .sum();
    }

    /**
     * 模型用量统计
     */
    public static class ModelUsage {
        private final String provider;
        private final String model;
        private final AtomicLong promptTokens = new AtomicLong();
        private final AtomicLong completionTokens = new AtomicLong();
        private final AtomicLong totalTokens = new AtomicLong();
        private final AtomicInteger requestCount = new AtomicInteger();
        private final AtomicLong totalLatencyMs = new AtomicLong();

        ModelUsage(String provider, String model) {
            this.provider = provider;
            this.model = model;
        }

        void record(AiUsage usage, long latencyMs) {
            promptTokens.addAndGet(usage.promptTokens());
            completionTokens.addAndGet(usage.completionTokens());
            totalTokens.addAndGet(usage.totalTokens());
            requestCount.incrementAndGet();
            totalLatencyMs.addAndGet(latencyMs);
        }

        public String getProvider() { return provider; }
        public String getModel() { return model; }
        public long getPromptTokens() { return promptTokens.get(); }
        public long getCompletionTokens() { return completionTokens.get(); }
        public long getTotalTokens() { return totalTokens.get(); }
        public int getRequestCount() { return requestCount.get(); }
        public long getTotalLatencyMs() { return totalLatencyMs.get(); }

        public double getAvgLatencyMs() {
            int count = requestCount.get();
            return count > 0 ? (double) totalLatencyMs.get() / count : 0;
        }

        @Override
        public String toString() {
            return "ModelUsage{provider='%s', model='%s', requests=%d, tokens=%d, avgLatency=%.0fms}"
                    .formatted(provider, model, requestCount.get(), totalTokens.get(), getAvgLatencyMs());
        }
    }
}
