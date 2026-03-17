package com.basebackend.security.config;

import com.basebackend.security.service.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

/**
 * 当应用上下文中不存在 {@link PermissionService} Bean 时输出告警。
 * <p>
 * 若无 PermissionService，{@code @RequiresPermission} / {@code @RequiresRole} /
 * {@code @DataScope} 注解将静默失效（因对应 Aspect 受 {@code @ConditionalOnBean} 保护不会注册）。
 */
@Slf4j
@Configuration
@ConditionalOnMissingBean(PermissionService.class)
public class PermissionServiceCheckConfiguration {

    @PostConstruct
    public void warnMissingPermissionService() {
        log.error("===============================================================");
        log.error(" 未检测到 PermissionService Bean！");
        log.error(" @RequiresPermission / @RequiresRole / @DataScope 注解将不会生效。");
        log.error(" 请在当前微服务中实现 PermissionService 接口并注册为 Spring Bean。");
        log.error("===============================================================");
    }
}
