package com.basebackend.search.annotation;

import com.basebackend.search.model.IndexDefinition.FieldType;

import java.lang.annotation.*;

/**
 * 搜索字段注解
 * <p>
 * 标注在实体字段上，声明字段的索引类型和分词器。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SearchField {

    /** 字段类型 */
    FieldType type() default FieldType.TEXT;

    /** 分词器（仅 TEXT 类型有效） */
    String analyzer() default "";

    /** 搜索分词器 */
    String searchAnalyzer() default "";

    /** 是否索引 */
    boolean index() default true;

    /** 是否存储原始值 */
    boolean store() default false;
}
