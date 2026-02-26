package com.basebackend.ai.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 请求（支持 Builder 模式）
 */
public class AiRequest {

    private String model;
    private List<AiMessage> messages;
    private Double temperature;
    private Integer maxTokens;
    private Double topP;
    private Boolean stream;
    private Map<String, Object> extraParams;

    private AiRequest() {
        this.messages = new ArrayList<>();
        this.extraParams = new HashMap<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** 快捷方式：单轮对话 */
    public static AiRequest of(String userMessage) {
        return builder().addMessage(AiMessage.user(userMessage)).build();
    }

    /** 快捷方式：带系统提示的单轮对话 */
    public static AiRequest of(String systemPrompt, String userMessage) {
        return builder()
                .addMessage(AiMessage.system(systemPrompt))
                .addMessage(AiMessage.user(userMessage))
                .build();
    }

    // --- Getters ---

    public String getModel() { return model; }
    public List<AiMessage> getMessages() { return messages; }
    public Double getTemperature() { return temperature; }
    public Integer getMaxTokens() { return maxTokens; }
    public Double getTopP() { return topP; }
    public Boolean getStream() { return stream; }
    public Map<String, Object> getExtraParams() { return extraParams; }

    // --- Builder ---

    public static class Builder {
        private final AiRequest request = new AiRequest();

        public Builder model(String model) {
            request.model = model;
            return this;
        }

        public Builder messages(List<AiMessage> messages) {
            request.messages = new ArrayList<>(messages);
            return this;
        }

        public Builder addMessage(AiMessage message) {
            request.messages.add(message);
            return this;
        }

        public Builder temperature(double temperature) {
            request.temperature = temperature;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            request.maxTokens = maxTokens;
            return this;
        }

        public Builder topP(double topP) {
            request.topP = topP;
            return this;
        }

        public Builder stream(boolean stream) {
            request.stream = stream;
            return this;
        }

        public Builder extraParam(String key, Object value) {
            request.extraParams.put(key, value);
            return this;
        }

        public AiRequest build() {
            return request;
        }
    }
}
