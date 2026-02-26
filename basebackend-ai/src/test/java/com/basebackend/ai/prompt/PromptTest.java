package com.basebackend.ai.prompt;

import com.basebackend.ai.client.AiMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Prompt 模块测试")
class PromptTest {

    // ==================== PromptTemplate ====================

    @Nested
    @DisplayName("PromptTemplate 测试")
    class PromptTemplateTest {

        @Test
        @DisplayName("渲染单个变量")
        void renderSingleVariable() {
            PromptTemplate t = new PromptTemplate("test", "你好, {{name}}!");
            String result = t.render("name", "小吴");
            assertThat(result).isEqualTo("你好, 小吴!");
        }

        @Test
        @DisplayName("渲染多个变量")
        void renderMultipleVariables() {
            PromptTemplate t = new PromptTemplate("test", "将{{text}}翻译为{{lang}}");
            String result = t.render(Map.of("text", "hello", "lang", "中文"));
            assertThat(result).isEqualTo("将hello翻译为中文");
        }

        @Test
        @DisplayName("未匹配的变量保留原始占位符")
        void unmatchedVariablePreserved() {
            PromptTemplate t = new PromptTemplate("test", "{{name}} + {{unknown}}");
            String result = t.render("name", "test");
            assertThat(result).isEqualTo("test + {{unknown}}");
        }

        @Test
        @DisplayName("无变量模板原样返回")
        void noVariablesReturnsOriginal() {
            PromptTemplate t = new PromptTemplate("test", "纯文本内容");
            String result = t.render(Map.of());
            assertThat(result).isEqualTo("纯文本内容");
        }

        @Test
        @DisplayName("空 Map 渲染不报错")
        void emptyMapRender() {
            PromptTemplate t = new PromptTemplate("test", "{{a}} {{b}}");
            String result = t.render(Map.of());
            assertThat(result).isEqualTo("{{a}} {{b}}");
        }

        @Test
        @DisplayName("Record 字段正确")
        void recordFields() {
            PromptTemplate t = new PromptTemplate("myTemplate", "content");
            assertThat(t.name()).isEqualTo("myTemplate");
            assertThat(t.template()).isEqualTo("content");
        }

        @Test
        @DisplayName("变量值包含特殊字符（$、\\）")
        void specialCharactersInValue() {
            PromptTemplate t = new PromptTemplate("test", "路径: {{path}}");
            String result = t.render("path", "C:\\Users\\test$dir");
            assertThat(result).isEqualTo("路径: C:\\Users\\test$dir");
        }
    }

    // ==================== PromptTemplateRegistry ====================

    @Nested
    @DisplayName("PromptTemplateRegistry 测试")
    class PromptTemplateRegistryTest {

        @Test
        @DisplayName("注册和获取模板")
        void registerAndGet() {
            PromptTemplateRegistry registry = new PromptTemplateRegistry();
            registry.register("greeting", "你好, {{name}}");
            PromptTemplate t = registry.get("greeting");
            assertThat(t.name()).isEqualTo("greeting");
            assertThat(t.render("name", "AI")).isEqualTo("你好, AI");
        }

        @Test
        @DisplayName("注册 PromptTemplate 对象")
        void registerTemplateObject() {
            PromptTemplateRegistry registry = new PromptTemplateRegistry();
            registry.register(new PromptTemplate("t1", "内容"));
            assertThat(registry.contains("t1")).isTrue();
        }

        @Test
        @DisplayName("获取不存在的模板抛异常")
        void getNotFoundThrows() {
            PromptTemplateRegistry registry = new PromptTemplateRegistry();
            assertThatThrownBy(() -> registry.get("nonexistent"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("不存在");
        }

        @Test
        @DisplayName("render 快捷方法")
        void renderShortcut() {
            PromptTemplateRegistry registry = new PromptTemplateRegistry();
            registry.register("translate", "翻译: {{text}}");
            String result = registry.render("translate", Map.of("text", "hello"));
            assertThat(result).isEqualTo("翻译: hello");
        }

        @Test
        @DisplayName("contains 判断")
        void containsCheck() {
            PromptTemplateRegistry registry = new PromptTemplateRegistry();
            assertThat(registry.contains("x")).isFalse();
            registry.register("x", "content");
            assertThat(registry.contains("x")).isTrue();
        }

        @Test
        @DisplayName("remove 删除模板")
        void removeTemplate() {
            PromptTemplateRegistry registry = new PromptTemplateRegistry();
            registry.register("x", "content");
            registry.remove("x");
            assertThat(registry.contains("x")).isFalse();
        }

        @Test
        @DisplayName("getTemplateNames 返回所有名称")
        void getTemplateNames() {
            PromptTemplateRegistry registry = new PromptTemplateRegistry();
            registry.register("a", "1");
            registry.register("b", "2");
            assertThat(registry.getTemplateNames()).containsExactlyInAnyOrder("a", "b");
        }

        @Test
        @DisplayName("覆盖注册同名模板")
        void overrideRegistration() {
            PromptTemplateRegistry registry = new PromptTemplateRegistry();
            registry.register("x", "old");
            registry.register("x", "new");
            assertThat(registry.get("x").template()).isEqualTo("new");
        }
    }

    // ==================== PromptBuilder ====================

    @Nested
    @DisplayName("PromptBuilder 测试")
    class PromptBuilderTest {

        @Test
        @DisplayName("构建多角色消息列表")
        void buildMultiRoleMessages() {
            List<AiMessage> messages = PromptBuilder.create()
                    .system("你是助手")
                    .user("你好")
                    .assistant("你好！有什么能帮你的？")
                    .user("解释虚拟线程")
                    .build();

            assertThat(messages).hasSize(4);
            assertThat(messages.get(0).role()).isEqualTo("system");
            assertThat(messages.get(1).role()).isEqualTo("user");
            assertThat(messages.get(2).role()).isEqualTo("assistant");
            assertThat(messages.get(3).role()).isEqualTo("user");
        }

        @Test
        @DisplayName("变量注入渲染")
        void variableInjection() {
            List<AiMessage> messages = PromptBuilder.create()
                    .system("你是一个{{role}}专家")
                    .user("{{question}}")
                    .variable("role", "Java")
                    .variable("question", "什么是Record？")
                    .build();

            assertThat(messages.get(0).content()).isEqualTo("你是一个Java专家");
            assertThat(messages.get(1).content()).isEqualTo("什么是Record？");
        }

        @Test
        @DisplayName("批量设置变量")
        void batchVariables() {
            List<AiMessage> messages = PromptBuilder.create()
                    .user("{{a}} + {{b}} = ?")
                    .variables(Map.of("a", "1", "b", "2"))
                    .build();

            assertThat(messages.getFirst().content()).isEqualTo("1 + 2 = ?");
        }

        @Test
        @DisplayName("无变量时不渲染")
        void noVariablesNoRendering() {
            List<AiMessage> messages = PromptBuilder.create()
                    .user("纯文本")
                    .build();

            assertThat(messages.getFirst().content()).isEqualTo("纯文本");
        }

        @Test
        @DisplayName("空构建")
        void emptyBuild() {
            List<AiMessage> messages = PromptBuilder.create().build();
            assertThat(messages).isEmpty();
        }
    }
}
