package com.basebackend.common.masking;

import com.basebackend.common.masking.jackson.MaskingJsonSerializer;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = MaskingJsonSerializer.class)
public @interface Mask {
    MaskType value();
    String customPattern() default "";
    char maskChar() default '*';
}
