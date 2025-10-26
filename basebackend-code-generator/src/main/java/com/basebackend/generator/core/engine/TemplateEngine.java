package com.basebackend.generator.core.engine;

import com.basebackend.generator.entity.EngineType;

import java.util.Map;

/**
 * 模板引擎接口
 */
public interface TemplateEngine {

    /**
     * 渲染模板
     *
     * @param template  模板内容
     * @param dataModel 数据模型
     * @return 渲染后的内容
     */
    String render(String template, Map<String, Object> dataModel);

    /**
     * 验证模板语法
     *
     * @param template 模板内容
     * @throws Exception 语法错误时抛出异常
     */
    void validate(String template) throws Exception;

    /**
     * 获取引擎类型
     *
     * @return 引擎类型
     */
    EngineType getType();
}
