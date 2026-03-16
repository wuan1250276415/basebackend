package com.basebackend.ai.annotation;

import com.basebackend.ai.client.AiClient;
import com.basebackend.ai.client.AiRequest;
import com.basebackend.ai.client.AiResponse;
import com.basebackend.ai.client.AiUsage;
import com.basebackend.ai.prompt.PromptTemplateRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("AIGenerateAspect 测试")
class AIGenerateAspectTest {

    @Test
    @DisplayName("严格模式下未知 Provider 应 fail-fast")
    void strictModeShouldFailOnUnknownProvider() throws Throwable {
        AiClient defaultClient = mock(AiClient.class);
        AIGenerateAspect aspect = new AIGenerateAspect(
                Map.of("openai", defaultClient),
                defaultClient,
                new PromptTemplateRegistry(),
                true
        );

        Invocation invocation = createInvocation(
                "generateWithMissingProvider",
                new Class<?>[]{String.class},
                new Object[]{"hello"}
        );

        assertThatThrownBy(() -> aspect.around(invocation.joinPoint(), invocation.annotation()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("严格模式下指定的 AI Provider");
    }

    @Test
    @DisplayName("非严格模式下未知 Provider 回退默认客户端")
    void nonStrictModeShouldFallbackToDefaultClient() throws Throwable {
        AiClient defaultClient = mock(AiClient.class);
        when(defaultClient.chat(any(AiRequest.class)))
                .thenReturn(AiResponse.of("fallback", "model", AiUsage.empty(), "stop", 1));

        AIGenerateAspect aspect = new AIGenerateAspect(
                Map.of("openai", defaultClient),
                defaultClient,
                new PromptTemplateRegistry(),
                false
        );

        Invocation invocation = createInvocation(
                "generateWithMissingProvider",
                new Class<?>[]{String.class},
                new Object[]{"hello"}
        );
        Object result = aspect.around(invocation.joinPoint(), invocation.annotation());

        assertThat(result).isEqualTo("fallback");
        verify(defaultClient).chat(any(AiRequest.class));
    }

    @Test
    @DisplayName("已注册模板名称应通过注册中心渲染")
    void shouldRenderPromptFromTemplateRegistry() throws Throwable {
        AiClient openAiClient = mock(AiClient.class);
        when(openAiClient.chat(any(AiRequest.class)))
                .thenReturn(AiResponse.of("registry", "model", AiUsage.empty(), "stop", 1));

        PromptTemplateRegistry registry = new PromptTemplateRegistry();
        registry.register("translation-template", "将{{text}}翻译为{{targetLang}}");

        AIGenerateAspect aspect = new AIGenerateAspect(
                Map.of("openai", openAiClient),
                openAiClient,
                registry,
                true
        );

        Invocation invocation = createInvocation(
                "generateByRegisteredTemplate",
                new Class<?>[]{String.class, String.class},
                new Object[]{"hello", "中文"}
        );
        Object result = aspect.around(invocation.joinPoint(), invocation.annotation());

        assertThat(result).isEqualTo("registry");

        AiRequest request = captureSingleRequest(openAiClient);
        assertThat(request.getMessages()).hasSize(1);
        assertThat(request.getMessages().getFirst().content()).isEqualTo("将hello翻译为中文");
    }

    @Test
    @DisplayName("未注册模板应按内联模板渲染")
    void shouldRenderInlinePromptTemplate() throws Throwable {
        AiClient openAiClient = mock(AiClient.class);
        when(openAiClient.chat(any(AiRequest.class)))
                .thenReturn(AiResponse.of("inline", "model", AiUsage.empty(), "stop", 1));

        AIGenerateAspect aspect = new AIGenerateAspect(
                Map.of("openai", openAiClient),
                openAiClient,
                new PromptTemplateRegistry(),
                true
        );

        Invocation invocation = createInvocation(
                "generateByInlineTemplate",
                new Class<?>[]{String.class},
                new Object[]{"Java 25"}
        );
        Object result = aspect.around(invocation.joinPoint(), invocation.annotation());

        assertThat(result).isEqualTo("inline");

        AiRequest request = captureSingleRequest(openAiClient);
        assertThat(request.getMessages()).hasSize(1);
        assertThat(request.getMessages().getFirst().content()).isEqualTo("总结：Java 25");
    }

    @Test
    @DisplayName("方法返回 AiResponse 时应直接返回完整响应")
    void shouldReturnAiResponseWhenMethodReturnTypeIsAiResponse() throws Throwable {
        AiClient openAiClient = mock(AiClient.class);
        AiResponse expected = AiResponse.of("full-response", "model-x", AiUsage.of(3, 5), "stop", 2);
        when(openAiClient.chat(any(AiRequest.class))).thenReturn(expected);

        AIGenerateAspect aspect = new AIGenerateAspect(
                Map.of("openai", openAiClient),
                openAiClient,
                new PromptTemplateRegistry(),
                true
        );

        Invocation invocation = createInvocation(
                "generateAiResponse",
                new Class<?>[]{String.class},
                new Object[]{"hello"}
        );

        Object result = aspect.around(invocation.joinPoint(), invocation.annotation());
        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("方法返回 String 时应返回响应文本")
    void shouldReturnStringWhenMethodReturnTypeIsString() throws Throwable {
        AiClient openAiClient = mock(AiClient.class);
        when(openAiClient.chat(any(AiRequest.class)))
                .thenReturn(AiResponse.of("text-only", "model", AiUsage.empty(), "stop", 1));

        AIGenerateAspect aspect = new AIGenerateAspect(
                Map.of("openai", openAiClient),
                openAiClient,
                new PromptTemplateRegistry(),
                true
        );

        Invocation invocation = createInvocation(
                "generateString",
                new Class<?>[]{String.class},
                new Object[]{"hello"}
        );

        Object result = aspect.around(invocation.joinPoint(), invocation.annotation());
        assertThat(result).isEqualTo("text-only");
    }

    @Test
    @DisplayName("不支持的返回类型应抛出明确异常")
    void shouldFailOnUnsupportedReturnType() throws Throwable {
        AiClient openAiClient = mock(AiClient.class);
        AIGenerateAspect aspect = new AIGenerateAspect(
                Map.of("openai", openAiClient),
                openAiClient,
                new PromptTemplateRegistry(),
                true
        );

        Invocation invocation = createInvocation(
                "generateUnsupportedType",
                new Class<?>[]{String.class},
                new Object[]{"hello"}
        );

        assertThatThrownBy(() -> aspect.around(invocation.joinPoint(), invocation.annotation()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("仅支持返回 String 或 AiResponse");
        verifyNoInteractions(openAiClient);
    }

    private Invocation createInvocation(String methodName, Class<?>[] parameterTypes, Object[] args)
            throws NoSuchMethodException {
        Method method = DemoService.class.getDeclaredMethod(methodName, parameterTypes);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(args);

        AIGenerate annotation = method.getAnnotation(AIGenerate.class);
        return new Invocation(joinPoint, annotation);
    }

    private AiRequest captureSingleRequest(AiClient client) {
        var requestCaptor = org.mockito.ArgumentCaptor.forClass(AiRequest.class);
        verify(client).chat(requestCaptor.capture());
        clearInvocations(client);
        return requestCaptor.getValue();
    }

    private record Invocation(ProceedingJoinPoint joinPoint, AIGenerate annotation) {
    }

    static class DemoService {
        @AIGenerate(prompt = "{{text}}", provider = "missing")
        public String generateWithMissingProvider(String text) {
            return null;
        }

        @AIGenerate(prompt = "translation-template", provider = "openai")
        public String generateByRegisteredTemplate(String text, String targetLang) {
            return null;
        }

        @AIGenerate(prompt = "总结：{{text}}", provider = "openai")
        public String generateByInlineTemplate(String text) {
            return null;
        }

        @AIGenerate(prompt = "{{text}}", provider = "openai")
        public AiResponse generateAiResponse(String text) {
            return null;
        }

        @AIGenerate(prompt = "{{text}}", provider = "openai")
        public String generateString(String text) {
            return null;
        }

        @AIGenerate(prompt = "{{text}}", provider = "openai")
        public Integer generateUnsupportedType(String text) {
            return null;
        }
    }
}
