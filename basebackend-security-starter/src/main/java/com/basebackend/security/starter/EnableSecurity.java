package com.basebackend.security.starter;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用 BaseBackend 安全功能
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(SecurityAutoConfiguration.class)
public @interface EnableSecurity {
}
