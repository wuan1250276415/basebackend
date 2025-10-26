package com.basebackend.generator.core.engine;

import com.basebackend.generator.entity.EngineType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 模板引擎工厂
 */
@Component
@RequiredArgsConstructor
public class TemplateEngineFactory {

    private final List<TemplateEngine> engines;
    private Map<EngineType, TemplateEngine> engineMap;

    /**
     * 获取模板引擎
     *
     * @param type 引擎类型
     * @return 模板引擎实例
     */
    public TemplateEngine getEngine(EngineType type) {
        if (engineMap == null) {
            engineMap = engines.stream()
                    .collect(Collectors.toMap(TemplateEngine::getType, Function.identity()));
        }
        
        TemplateEngine engine = engineMap.get(type);
        if (engine == null) {
            throw new IllegalArgumentException("不支持的模板引擎类型: " + type);
        }
        return engine;
    }

    /**
     * 根据字符串获取模板引擎
     *
     * @param typeStr 引擎类型字符串
     * @return 模板引擎实例
     */
    public TemplateEngine getEngine(String typeStr) {
        return getEngine(EngineType.valueOf(typeStr));
    }
}
