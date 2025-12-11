package com.basebackend.generator.util;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.JavaFormatterOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 代码格式化工具
 * 
 * 使用Google Java Format对生成的Java代码进行格式化
 * 确保生成的代码符合统一的代码规范
 */
@Slf4j
@Component
public class CodeFormatter {

    private final Formatter javaFormatter;
    private final boolean formattingEnabled;

    public CodeFormatter() {
        Formatter tempFormatter = null;
        boolean enabled = true;

        try {
            // 创建Java格式化器，使用Google风格
            // 注意：在Java 17+上可能需要额外的JVM参数：
            // --add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
            // --add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
            // --add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
            // --add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
            // --add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
            JavaFormatterOptions options = JavaFormatterOptions.builder()
                    .style(JavaFormatterOptions.Style.GOOGLE)
                    .build();
            tempFormatter = new Formatter(options);
            log.info("Google Java Format 初始化成功");
        } catch (Throwable e) {
            // 捕获所有Throwable，包括Error（如IllegalAccessError）
            log.warn("Google Java Format 初始化失败，代码格式化功能已禁用: {}", e.getMessage());
            enabled = false;
        }

        this.javaFormatter = tempFormatter;
        this.formattingEnabled = enabled;
    }

    /**
     * 格式化Java代码
     *
     * @param code 原始Java代码
     * @return 格式化后的代码，如果格式化失败返回原始代码
     */
    public String formatJava(String code) {
        if (!formattingEnabled || code == null || code.isBlank()) {
            return code;
        }

        try {
            return javaFormatter.formatSource(code);
        } catch (Exception e) {
            // 捕获所有异常（包括FormatterException和其他运行时异常）
            log.warn("Java代码格式化失败，返回原始代码: {}", e.getMessage());
            return code;
        }
    }

    /**
     * 格式化代码（根据文件类型自动选择格式化器）
     *
     * @param code     代码内容
     * @param filePath 文件路径（用于判断文件类型）
     * @return 格式化后的代码
     */
    public String format(String code, String filePath) {
        if (code == null || code.isBlank() || filePath == null) {
            return code;
        }

        String lowerPath = filePath.toLowerCase();

        if (lowerPath.endsWith(".java")) {
            return formatJava(code);
        } else if (lowerPath.endsWith(".xml")) {
            return formatXml(code);
        } else if (lowerPath.endsWith(".json")) {
            return formatJson(code);
        }

        // 其他类型不格式化
        return code;
    }

    /**
     * 格式化XML代码
     * 简单的XML缩进格式化
     *
     * @param xml XML代码
     * @return 格式化后的XML
     */
    public String formatXml(String xml) {
        if (xml == null || xml.isBlank()) {
            return xml;
        }

        try {
            // 简单的XML格式化：移除多余空格和换行，重新缩进
            StringBuilder formatted = new StringBuilder();
            int indent = 0;
            StringBuilder current = new StringBuilder();

            for (char c : xml.toCharArray()) {
                if (c == '<') {
                    // 输出之前的内容
                    String content = current.toString().trim();
                    if (!content.isEmpty()) {
                        formatted.append(content);
                    }
                    current = new StringBuilder();
                    current.append(c);
                } else if (c == '>') {
                    current.append(c);
                    String tag = current.toString();

                    // 判断标签类型
                    boolean isClosingTag = tag.startsWith("</");
                    boolean isSelfClosing = tag.endsWith("/>");
                    boolean isOpeningTag = !isClosingTag && !isSelfClosing && !tag.startsWith("<?")
                            && !tag.startsWith("<!");

                    if (isClosingTag) {
                        indent = Math.max(0, indent - 1);
                    }

                    if (!formatted.isEmpty() && formatted.charAt(formatted.length() - 1) != '\n') {
                        formatted.append("\n");
                    }
                    formatted.append("    ".repeat(indent)).append(tag);

                    if (isOpeningTag) {
                        indent++;
                    }

                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }

            return formatted.toString();
        } catch (Exception e) {
            log.warn("XML格式化失败，返回原始代码: {}", e.getMessage());
            return xml;
        }
    }

    /**
     * 格式化JSON代码
     * 简单的JSON缩进格式化
     *
     * @param json JSON代码
     * @return 格式化后的JSON
     */
    public String formatJson(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }

        try {
            StringBuilder formatted = new StringBuilder();
            int indent = 0;
            boolean inString = false;
            char prevChar = 0;

            for (char c : json.toCharArray()) {
                if (c == '"' && prevChar != '\\') {
                    inString = !inString;
                    formatted.append(c);
                } else if (!inString) {
                    switch (c) {
                        case '{', '[' -> {
                            formatted.append(c);
                            formatted.append('\n');
                            indent++;
                            formatted.append("  ".repeat(indent));
                        }
                        case '}', ']' -> {
                            formatted.append('\n');
                            indent = Math.max(0, indent - 1);
                            formatted.append("  ".repeat(indent));
                            formatted.append(c);
                        }
                        case ',' -> {
                            formatted.append(c);
                            formatted.append('\n');
                            formatted.append("  ".repeat(indent));
                        }
                        case ':' -> formatted.append(": ");
                        case ' ', '\n', '\r', '\t' -> {
                            // 忽略空白字符
                        }
                        default -> formatted.append(c);
                    }
                } else {
                    formatted.append(c);
                }
                prevChar = c;
            }

            return formatted.toString();
        } catch (Exception e) {
            log.warn("JSON格式化失败，返回原始代码: {}", e.getMessage());
            return json;
        }
    }

    /**
     * 检查格式化功能是否可用
     *
     * @return true表示Java格式化可用
     */
    public boolean isFormattingEnabled() {
        return formattingEnabled;
    }
}
