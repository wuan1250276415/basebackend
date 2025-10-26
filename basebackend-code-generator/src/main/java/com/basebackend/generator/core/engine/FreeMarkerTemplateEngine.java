package com.basebackend.generator.core.engine;

import com.basebackend.generator.entity.EngineType;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

/**
 * FreeMarker模板引擎实现
 */
@Slf4j
@Component
public class FreeMarkerTemplateEngine implements TemplateEngine {

    private final Configuration configuration;

    public FreeMarkerTemplateEngine() {
        this.configuration = new Configuration(Configuration.VERSION_2_3_32);
        configuration.setDefaultEncoding("UTF-8");
        configuration.setNumberFormat("0.######");
    }

    @Override
    public String render(String template, Map<String, Object> dataModel) {
        try {
            Template tpl = new Template("template", new StringReader(template), configuration);
            StringWriter writer = new StringWriter();
            tpl.process(dataModel, writer);
            return writer.toString();
        } catch (IOException | TemplateException e) {
            log.error("FreeMarker模板渲染失败", e);
            throw new RuntimeException("模板渲染失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void validate(String template) throws Exception {
        new Template("template", new StringReader(template), configuration);
    }

    @Override
    public EngineType getType() {
        return EngineType.FREEMARKER;
    }
}
