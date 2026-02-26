package com.basebackend.ai.client;

/**
 * AI 消息（对话中的单条消息）
 *
 * @param role    角色：system / user / assistant
 * @param content 消息内容
 */
public record AiMessage(String role, String content) {

    public static final String ROLE_SYSTEM = "system";
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    /** 创建系统消息 */
    public static AiMessage system(String content) {
        return new AiMessage(ROLE_SYSTEM, content);
    }

    /** 创建用户消息 */
    public static AiMessage user(String content) {
        return new AiMessage(ROLE_USER, content);
    }

    /** 创建助手消息 */
    public static AiMessage assistant(String content) {
        return new AiMessage(ROLE_ASSISTANT, content);
    }
}
