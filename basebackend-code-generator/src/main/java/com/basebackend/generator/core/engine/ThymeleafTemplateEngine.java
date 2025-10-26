package com.basebackend.generator.core.engine;

import com.basebackend.generator.entity.EngineType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.Locale;
import java.util.Map;

/**
 * Thymeleaf模板引擎实现
 */
@Slf4j
@Component
public class ThymeleafTemplateEngine implements com.basebackend.generator.core.engine.TemplateEngine {

    private final TemplateEngine templateEngine;

    public ThymeleafTemplateEngine() {
        StringTemplateResolver templateResolver = new StringTemplateResolver();
        templateResolver.setTemplateMode(TemplateMode.TEXT);
        templateResolver.setCacheable(false);

        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
    }

    @Override
    public String render(String template, Map<String, Object> dataModel) {
        try {
            Context context = new Context(Locale.getDefault(), dataModel);
            return templateEngine.process(template, context);
        } catch (Exception e) {
            log.error("Thymeleaf模板渲染失败", e);
            throw new RuntimeException("模板渲染失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void validate(String template) throws Exception {
        try {
            Context context = new Context();
            templateEngine.process(template, context);
        } catch (Exception e) {
            throw new Exception("Thymeleaf模板语法错误: " + e.getMessage(), e);
        }
    }

    @Override
    public EngineType getType() {
        return EngineType.THYMELEAF;
    }
}
