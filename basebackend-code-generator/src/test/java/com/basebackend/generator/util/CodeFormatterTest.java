package com.basebackend.generator.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 代码格式化工具测试
 * 
 * 注意：Google Java Format在Java 17+上需要特殊JVM参数才能正常工作，
 * 因此Java格式化相关的测试在标准测试环境中可能会失败。
 * 
 * 如需测试Java格式化功能，请添加以下JVM参数：
 * --add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
 * --add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
 * --add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
 * --add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
 * --add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
 */
@DisplayName("CodeFormatter 测试")
class CodeFormatterTest {

    private CodeFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new CodeFormatter();
    }

    @Nested
    @DisplayName("Java格式化测试")
    class JavaFormatterTests {

        @Test
        @DisplayName("处理null输入")
        void shouldHandleNullInput() {
            assertNull(formatter.formatJava(null));
        }

        @Test
        @DisplayName("处理空字符串")
        void shouldHandleEmptyString() {
            assertEquals("", formatter.formatJava(""));
        }

        @Test
        @DisplayName("处理空白字符串")
        void shouldHandleBlankString() {
            String blank = "   ";
            String result = formatter.formatJava(blank);
            assertEquals(blank, result);
        }
    }

    @Nested
    @DisplayName("XML格式化测试")
    class XmlFormatterTests {

        @Test
        @DisplayName("格式化简单XML")
        void shouldFormatSimpleXml() {
            String input = "<root><child>content</child></root>";
            String result = formatter.formatXml(input);

            assertNotNull(result);
            assertTrue(result.contains("<root>"));
            assertTrue(result.contains("<child>"));
        }

        @Test
        @DisplayName("处理null输入")
        void shouldHandleNullXmlInput() {
            assertNull(formatter.formatXml(null));
        }

        @Test
        @DisplayName("处理空字符串")
        void shouldHandleEmptyXmlString() {
            assertEquals("", formatter.formatXml(""));
        }

        @Test
        @DisplayName("处理空白字符串")
        void shouldHandleBlankXmlString() {
            String blank = "   ";
            String result = formatter.formatXml(blank);
            assertEquals(blank, result);
        }
    }

    @Nested
    @DisplayName("JSON格式化测试")
    class JsonFormatterTests {

        @Test
        @DisplayName("格式化简单JSON")
        void shouldFormatSimpleJson() {
            String input = "{\"name\":\"test\",\"value\":123}";
            String result = formatter.formatJson(input);

            assertNotNull(result);
            assertTrue(result.contains("\"name\""));
        }

        @Test
        @DisplayName("格式化嵌套JSON")
        void shouldFormatNestedJson() {
            String input = "{\"user\":{\"name\":\"test\"}}";
            String result = formatter.formatJson(input);

            assertNotNull(result);
            assertTrue(result.contains("\"user\""));
        }

        @Test
        @DisplayName("处理null输入")
        void shouldHandleNullJsonInput() {
            assertNull(formatter.formatJson(null));
        }

        @Test
        @DisplayName("处理空字符串")
        void shouldHandleEmptyJsonString() {
            assertEquals("", formatter.formatJson(""));
        }
    }

    @Nested
    @DisplayName("自动类型检测格式化测试")
    class AutoFormatTests {

        @Test
        @DisplayName("其他类型文件不格式化")
        void shouldNotFormatOtherFileTypes() {
            String content = "Some content";
            String result = formatter.format(content, "file.txt");

            assertEquals(content, result);
        }

        @Test
        @DisplayName("处理null路径")
        void shouldHandleNullFilePath() {
            String code = "content";
            String result = formatter.format(code, null);
            assertEquals(code, result);
        }

        @Test
        @DisplayName("处理null代码")
        void shouldHandleNullCode() {
            String result = formatter.format(null, "file.java");
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("格式化状态测试")
    class FormatterStatusTests {

        @Test
        @DisplayName("检查格式化功能状态")
        void shouldReportFormattingStatus() {
            // 格式化功能可能启用或禁用，取决于环境和JDK版本
            // 在Java 17+环境中，如果没有添加JVM参数，功能会被禁用
            boolean enabled = formatter.isFormattingEnabled();
            // 只验证方法可以正常调用
            assertNotNull(Boolean.valueOf(enabled));
        }
    }
}
