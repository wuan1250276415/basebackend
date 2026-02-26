package com.basebackend.ai.prompt;

import com.basebackend.ai.client.AiMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 链式 Prompt 构建器
 * <p>
 * 支持模板渲染 + 消息列表构建。
 *
 * <pre>
 * List&lt;AiMessage&gt; messages = PromptBuilder.create()
 *     .system("你是一个{{role}}专家")
 *     .variable("role", "Java")
 *     .user("请解释虚拟线程")
 *     .build();
 * </pre>
 */
public class PromptBuilder {

    private final List<MessageEntry> entries = new ArrayList<>();
    private final Map<String, Object> variables = new HashMap<>();

    private PromptBuilder() {}

    public static PromptBuilder create() {
        return new PromptBuilder();
    }

    /** 添加系统消息 */
    public PromptBuilder system(String content) {
        entries.add(new MessageEntry(AiMessage.ROLE_SYSTEM, content));
        return this;
    }

    /** 添加用户消息 */
    public PromptBuilder user(String content) {
        entries.add(new MessageEntry(AiMessage.ROLE_USER, content));
        return this;
    }

    /** 添加助手消息 */
    public PromptBuilder assistant(String content) {
        entries.add(new MessageEntry(AiMessage.ROLE_ASSISTANT, content));
        return this;
    }

    /** 设置变量 */
    public PromptBuilder variable(String key, Object value) {
        variables.put(key, value);
        return this;
    }

    /** 批量设置变量 */
    public PromptBuilder variables(Map<String, Object> vars) {
        variables.putAll(vars);
        return this;
    }

    /** 构建消息列表（渲染模板变量） */
    public List<AiMessage> build() {
        return entries.stream()
                .map(e -> new AiMessage(e.role, renderTemplate(e.content)))
                .toList();
    }

    private String renderTemplate(String content) {
        if (variables.isEmpty()) {
            return content;
        }
        PromptTemplate template = new PromptTemplate("inline", content);
        return template.render(variables);
    }

    private record MessageEntry(String role, String content) {}
}
