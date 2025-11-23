package com.basebackend.common.starter;

import com.basebackend.common.security.SecretManagerConfiguration;
import com.basebackend.common.starter.config.JacksonAutoConfiguration;
import com.basebackend.common.starter.config.UserContextAutoConfiguration;
import com.basebackend.common.starter.filter.ContextCleanupFilter;
import com.basebackend.common.starter.properties.CommonProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * BaseBackend Common 模块统一自动配置入口
 * <p>
 * 作为所有通用组件的自动配置聚合点，统一导入各子模块的配置类。
 * 下游项目只需引入 basebackend-common-starter 依赖即可自动启用所有通用功能。
 * </p>
 *
 * <p><b>功能模块：</b></p>
 * <ul>
 *   <li>核心模块：错误码、异常处理、分页查询等</li>
 *   <li>DTO 模块：通用响应结构（Result、PageResult）</li>
 *   <li>工具模块：日期、字符串、ID 生成等工具类</li>
 *   <li>上下文模块：用户/租户上下文管理、自动清理</li>
 *   <li>安全模块：密钥管理、数据脱敏、输入验证</li>
 *   <li>Starter 模块：全局异常处理、Jackson 配置、上下文清理</li>
 * </ul>
 *
 * <h3>配置属性：</h3>
 * <p>
 * 通过 {@link CommonProperties} 统一管理所有配置项，支持外部化配置。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 * @see CommonProperties
 * @see JacksonAutoConfiguration
 * @see ContextCleanupFilter
 */
@AutoConfiguration
@EnableConfigurationProperties(CommonProperties.class)
@Import({
    // 安全模块自动配置
    SecretManagerConfiguration.class,

    // Jackson 序列化配置
    JacksonAutoConfiguration.class,

    // 用户上下文拦截器自动配置
    UserContextAutoConfiguration.class,

    // 上下文清理过滤器（通过 @Component 自动注册）
    ContextCleanupFilter.class

    // 注意：GlobalExceptionHandler 通过 @RestControllerAdvice 自动扫描注册
    // CommonProperties 通过 @EnableConfigurationProperties 自动注册
})
public class CommonAutoConfiguration {

    /**
     * 默认构造函数
     * <p>
     * 此配置类本身不注册任何 Bean，仅作为配置聚合点。
     * 所有实际的 Bean 注册由各子模块的配置类完成。
     * </p>
     */
    public CommonAutoConfiguration() {
        // 配置聚合类，无需逻辑
    }
}
