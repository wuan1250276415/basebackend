package com.basebackend.featuretoggle.annotation;

import java.lang.annotation.*;

/**
 * 启用特性开关模块的注解
 * 用于在主应用类上标注
 *
 * @author BaseBackend
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableFeatureToggle {
}
