package com.basebackend.common.export;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExportField {

    String label();

    int order() default 0;

    /**
     * 列宽度（默认 20，仅 Excel 导出时生效）
     */
    int width() default 20;

    /**
     * 日期/数字格式化模式（如 "yyyy-MM-dd"、"#,##0.00"）
     */
    String format() default "";

    /**
     * 自定义转换器类型
     */
    Class<? extends FieldConverter> converter() default FieldConverter.class;
}
