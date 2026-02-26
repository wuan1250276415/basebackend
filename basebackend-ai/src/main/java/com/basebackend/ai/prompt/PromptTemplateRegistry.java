package com.basebackend.ai.prompt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prompt 模板注册中心
 * <p>
 * 线程安全，支持运行时动态注册模板。
 */
public class PromptTemplateRegistry {

    private final Map<String, PromptTemplate> templates = new ConcurrentHashMap<>();

    /**
     * 注册模板
     */
    public void register(String name, String template) {
        templates.put(name, new PromptTemplate(name, template));
    }

    /**
     * 注册模板
     */
    public void register(PromptTemplate template) {
        templates.put(template.name(), template);
    }

    /**
     * 获取模板
     */
    public PromptTemplate get(String name) {
        PromptTemplate template = templates.get(name);
        if (template == null) {
            throw new IllegalArgumentException("Prompt 模板不存在: " + name);
        }
        return template;
    }

    /**
     * 渲染模板
     */
    public String render(String name, Map<String, Object> variables) {
        return get(name).render(variables);
    }

    /**
     * 是否存在模板
     */
    public boolean contains(String name) {
        return templates.containsKey(name);
    }

    /**
     * 移除模板
     */
    public void remove(String name) {
        templates.remove(name);
    }

    /**
     * 获取所有模板名称
     */
    public java.util.Set<String> getTemplateNames() {
        return java.util.Collections.unmodifiableSet(templates.keySet());
    }
}
