package com.basebackend.generator.core.engine;

import com.basebackend.generator.entity.EngineType;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;

/**
 * Velocity模板引擎实现
 */
@Slf4j
@Component
public class VelocityTemplateEngine implements TemplateEngine {

    public VelocityTemplateEngine() {
        Properties props = new Properties();
        props.setProperty("resource.loader", "string");
        props.setProperty("string.resource.loader.class", "org.apache.velocity.runtime.resource.loader.StringResourceLoader");
        props.setProperty("input.encoding", "UTF-8");
        props.setProperty("output.encoding", "UTF-8");
        Velocity.init(props);
    }

    @Override
    public String render(String template, Map<String, Object> dataModel) {
        try {
            VelocityContext context = new VelocityContext(dataModel);
            StringWriter writer = new StringWriter();
            Velocity.evaluate(context, writer, "template", template);
            return writer.toString();
        } catch (Exception e) {
            log.error("Velocity模板渲染失败", e);
            throw new RuntimeException("模板渲染失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void validate(String template) throws Exception {
        try {
            VelocityContext context = new VelocityContext();
            StringWriter writer = new StringWriter();
            Velocity.evaluate(context, writer, "validation", template);
        } catch (Exception e) {
            throw new Exception("Velocity模板语法错误: " + e.getMessage(), e);
        }
    }

    @Override
    public EngineType getType() {
        return EngineType.VELOCITY;
    }
}
