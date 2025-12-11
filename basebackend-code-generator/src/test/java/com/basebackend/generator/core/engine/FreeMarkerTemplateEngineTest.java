package com.basebackend.generator.core.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FreeMarker模板引擎测试
 */
@DisplayName("FreeMarkerTemplateEngine 测试")
class FreeMarkerTemplateEngineTest {

    private FreeMarkerTemplateEngine engine;

    @BeforeEach
    void setUp() {
        engine = new FreeMarkerTemplateEngine();
    }

    @Nested
    @DisplayName("基础渲染测试")
    class BasicRenderTests {

        @Test
        @DisplayName("简单变量替换")
        void shouldRenderSimpleVariables() {
            String template = "Hello, ${name}!";
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("name", "World");

            String result = engine.render(template, dataModel);
            assertEquals("Hello, World!", result);
        }

        @Test
        @DisplayName("多个变量替换")
        void shouldRenderMultipleVariables() {
            String template = "package ${packageName};\n\npublic class ${className} {}";
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("packageName", "com.example.entity");
            dataModel.put("className", "User");

            String result = engine.render(template, dataModel);
            assertTrue(result.contains("com.example.entity"));
            assertTrue(result.contains("User"));
        }

        @Test
        @DisplayName("处理null值")
        void shouldHandleNullValues() {
            String template = "Value: ${value!'default'}";
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("value", null);

            String result = engine.render(template, dataModel);
            assertEquals("Value: default", result);
        }
    }

    @Nested
    @DisplayName("列表渲染测试")
    class ListRenderTests {

        @Test
        @DisplayName("遍历列表")
        void shouldRenderList() {
            String template = """
                    <#list columns as col>
                    private ${col.type} ${col.name};
                    </#list>""";

            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("columns", java.util.List.of(
                    Map.of("type", "String", "name", "username"),
                    Map.of("type", "Integer", "name", "age")));

            String result = engine.render(template, dataModel);
            assertTrue(result.contains("String username"));
            assertTrue(result.contains("Integer age"));
        }

        @Test
        @DisplayName("空列表处理")
        void shouldHandleEmptyList() {
            String template = """
                    <#list columns as col>
                    ${col.name}
                    <#else>
                    No columns
                    </#list>""";

            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("columns", java.util.Collections.emptyList());

            String result = engine.render(template, dataModel);
            assertTrue(result.contains("No columns"));
        }
    }

    @Nested
    @DisplayName("条件渲染测试")
    class ConditionalRenderTests {

        @Test
        @DisplayName("if条件判断")
        void shouldRenderConditional() {
            String template = """
                    <#if hasDateTime>
                    import java.time.LocalDateTime;
                    </#if>
                    <#if hasBigDecimal>
                    import java.math.BigDecimal;
                    </#if>""";

            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("hasDateTime", true);
            dataModel.put("hasBigDecimal", false);

            String result = engine.render(template, dataModel);
            assertTrue(result.contains("LocalDateTime"));
            assertFalse(result.contains("BigDecimal"));
        }

        @Test
        @DisplayName("if-else条件判断")
        void shouldRenderIfElse() {
            String template = """
                    <#if isPrimaryKey>
                    @Id
                    <#else>
                    @Column
                    </#if>""";

            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("isPrimaryKey", true);

            String result = engine.render(template, dataModel);
            assertTrue(result.contains("@Id"));
            assertFalse(result.contains("@Column"));
        }
    }

    @Nested
    @DisplayName("复杂模板测试")
    class ComplexTemplateTests {

        @Test
        @DisplayName("完整的Entity模板渲染")
        void shouldRenderCompleteEntityTemplate() {
            String template = """
                    package ${packageName}.entity;

                    <#if hasDateTime>
                    import java.time.LocalDateTime;
                    </#if>
                    <#if hasBigDecimal>
                    import java.math.BigDecimal;
                    </#if>

                    /**
                     * ${tableComment}
                     * @author ${author}
                     */
                    public class ${className} {

                    <#list columns as col>
                        /** ${col.comment} */
                        private ${col.javaType} ${col.javaField};

                    </#list>
                    }
                    """;

            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("packageName", "com.example");
            dataModel.put("hasDateTime", true);
            dataModel.put("hasBigDecimal", false);
            dataModel.put("tableComment", "用户表");
            dataModel.put("author", "Generator");
            dataModel.put("className", "User");
            dataModel.put("columns", java.util.List.of(
                    Map.of("comment", "用户ID", "javaType", "Long", "javaField", "id"),
                    Map.of("comment", "用户名", "javaType", "String", "javaField", "username")));

            String result = engine.render(template, dataModel);

            assertTrue(result.contains("package com.example.entity;"));
            assertTrue(result.contains("import java.time.LocalDateTime;"));
            assertFalse(result.contains("import java.math.BigDecimal;"));
            assertTrue(result.contains("用户表"));
            assertTrue(result.contains("public class User"));
            assertTrue(result.contains("private Long id;"));
            assertTrue(result.contains("private String username;"));
        }
    }

    @Nested
    @DisplayName("模板验证测试")
    class ValidationTests {

        @Test
        @DisplayName("有效模板应通过验证")
        void validTemplateShouldPassValidation() {
            String template = "Hello, ${name}!";
            assertDoesNotThrow(() -> engine.validate(template));
        }

        @Test
        @DisplayName("无效模板应抛出异常")
        void invalidTemplateShouldThrowException() {
            String template = "Hello, ${name"; // 缺少闭合括号
            assertThrows(Exception.class, () -> engine.validate(template));
        }

        @Test
        @DisplayName("未闭合的if标签应抛出异常")
        void unclosedIfShouldThrowException() {
            String template = "<#if true>content"; // 缺少</#if>
            assertThrows(Exception.class, () -> engine.validate(template));
        }
    }

    @Nested
    @DisplayName("引擎类型测试")
    class EngineTypeTests {

        @Test
        @DisplayName("应返回正确的引擎类型")
        void shouldReturnCorrectEngineType() {
            assertEquals(com.basebackend.generator.entity.EngineType.FREEMARKER, engine.getType());
        }
    }
}
