/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.context.annotation.Import
 */
package com.basebackend.nacos.annotation;

import com.basebackend.nacos.annotation.NacosImportSelector;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

@Target(value={ElementType.TYPE})
@Retention(value=RetentionPolicy.RUNTIME)
@Documented
@Import(value={NacosImportSelector.class})
public @interface EnableNacosSupport {
    public boolean config() default true;

    public boolean discovery() default true;
}

