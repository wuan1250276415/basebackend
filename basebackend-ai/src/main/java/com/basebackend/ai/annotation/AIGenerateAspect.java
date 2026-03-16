package com.basebackend.ai.annotation;

import com.basebackend.ai.client.AiClient;
import com.basebackend.ai.client.AiRequest;
import com.basebackend.ai.client.AiResponse;
import com.basebackend.ai.prompt.PromptTemplate;
import com.basebackend.ai.prompt.PromptTemplateRegistry;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link AIGenerate} 注解切面
 * <p>
 * 拦截标注了 @AIGenerate 的方法，将方法参数注入 Prompt 模板，
 * 调用 AI 客户端，用生成结果作为方法返回值。
 */
@Slf4j
@Aspect
public class AIGenerateAspect {

    private final Map<String, AiClient> clientMap;
    private final AiClient defaultClient;
    private final PromptTemplateRegistry templateRegistry;
    private final boolean strictProviderResolution;

    public AIGenerateAspect(Map<String, AiClient> clientMap, AiClient defaultClient,
                            PromptTemplateRegistry templateRegistry) {
        this(clientMap, defaultClient, templateRegistry, false);
    }

    public AIGenerateAspect(Map<String, AiClient> clientMap, AiClient defaultClient,
                            PromptTemplateRegistry templateRegistry, boolean strictProviderResolution) {
        this.clientMap = clientMap;
        this.defaultClient = defaultClient;
        this.templateRegistry = templateRegistry;
        this.strictProviderResolution = strictProviderResolution;
    }

    @Around("@annotation(aiGenerate)")
    public Object around(ProceedingJoinPoint joinPoint, AIGenerate aiGenerate) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();

        // 1. 解析方法参数为变量映射
        Map<String, Object> variables = extractVariables(method, joinPoint.getArgs());

        // 2. 渲染 Prompt
        String promptText = resolvePrompt(aiGenerate.prompt(), variables);

        // 3. 选择客户端
        AiClient client = resolveClient(aiGenerate.provider());

        // 4. 校验返回类型
        validateReturnType(method);

        // 5. 构建请求
        AiRequest.Builder requestBuilder = AiRequest.builder()
                .addMessage(com.basebackend.ai.client.AiMessage.user(promptText))
                .temperature(aiGenerate.temperature());

        if (!aiGenerate.model().isEmpty()) {
            requestBuilder.model(aiGenerate.model());
        }
        if (aiGenerate.maxTokens() > 0) {
            requestBuilder.maxTokens(aiGenerate.maxTokens());
        }

        // 6. 调用 AI
        AiResponse response = client.chat(requestBuilder.build());

        log.debug("@AIGenerate 调用完成: method={}, provider={}, tokens={}, latency={}ms",
                method.getName(), client.getProvider(), response.usage().totalTokens(), response.latencyMs());

        // 7. 返回结果（根据方法返回类型适配）
        return adaptReturnType(response, method.getReturnType());
    }

    private Map<String, Object> extractVariables(Method method, Object[] args) {
        Map<String, Object> variables = new HashMap<>();
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            variables.put(parameters[i].getName(), args[i]);
        }
        return variables;
    }

    private String resolvePrompt(String prompt, Map<String, Object> variables) {
        // 如果是已注册的模板名称，从注册中心获取
        if (templateRegistry.contains(prompt)) {
            return templateRegistry.render(prompt, variables);
        }
        // 否则当作内联模板渲染
        return new PromptTemplate("inline", prompt).render(variables);
    }

    private AiClient resolveClient(String provider) {
        if (provider == null || provider.isEmpty()) {
            return defaultClient;
        }
        AiClient client = clientMap.get(provider);
        if (client == null) {
            if (strictProviderResolution) {
                throw new IllegalStateException("严格模式下指定的 AI Provider '%s' 不存在".formatted(provider));
            }
            log.warn("指定的 AI Provider '{}' 不存在，使用默认 Provider", provider);
            return defaultClient;
        }
        return client;
    }

    private void validateReturnType(Method method) {
        Class<?> returnType = method.getReturnType();
        if (returnType == String.class || returnType == AiResponse.class) {
            return;
        }
        throw new IllegalStateException(
                "@AIGenerate 标注方法仅支持返回 String 或 AiResponse，当前方法 %s#%s 返回 %s"
                        .formatted(method.getDeclaringClass().getSimpleName(), method.getName(), returnType.getName())
        );
    }

    private Object adaptReturnType(AiResponse response, Class<?> returnType) {
        if (returnType == AiResponse.class) {
            return response;
        }
        // 已在 validateReturnType 中校验，仅剩 String 类型
        return response.content();
    }
}
