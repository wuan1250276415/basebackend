package com.basebackend.ai.prompt;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prompt 模板
 * <p>
 * 支持 {@code {{variable}}} 占位符语法。
 *
 * @param name     模板名称
 * @param template 模板内容（含占位符）
 */
public record PromptTemplate(String name, String template) {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    /**
     * 渲染模板，替换占位符
     *
     * @param variables 变量映射
     * @return 渲染后的字符串
     */
    public String render(Map<String, Object> variables) {
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = variables.getOrDefault(varName, "{{" + varName + "}}");
            matcher.appendReplacement(result, Matcher.quoteReplacement(String.valueOf(value)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 快捷渲染（单变量）
     */
    public String render(String key, Object value) {
        return render(Map.of(key, value));
    }
}
