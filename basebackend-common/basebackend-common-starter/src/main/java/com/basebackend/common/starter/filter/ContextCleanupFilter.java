package com.basebackend.common.starter.filter;

import com.basebackend.common.context.TenantContextHolder;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.starter.properties.CommonProperties;
import jakarta.servlet.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 上下文清理过滤器
 * <p>
 * 在请求处理完成后自动清除 {@link UserContextHolder} 和 {@link TenantContextHolder} 中的上下文,
 * 避免内存泄漏和线程污染。
 * </p>
 *
 * <h3>工作原理：</h3>
 * <ol>
 *   <li>在请求结束后,无论是否发生异常,都会执行清理操作</li>
 *   <li>使用 try-finally 确保清理代码一定会执行</li>
 *   <li>可通过配置关闭自动清理,适用于特殊场景</li>
 * </ol>
 *
 * <h3>执行顺序：</h3>
 * <p>
 * 过滤器执行顺序为 {@code Integer.MIN_VALUE + 100},确保在大多数过滤器之前执行,
 * 这样可以在整个请求链路中保持上下文可用,并在最后统一清理。
 * </p>
 *
 * <h3>配置示例：</h3>
 * <pre>
 * basebackend:
 *   common:
 *     context:
 *       auto-cleanup: true
 *       filter-order: -2147483548  # Integer.MIN_VALUE + 100
 * </pre>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 * @see UserContextHolder
 * @see TenantContextHolder
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnWebApplication
@ConditionalOnClass({UserContextHolder.class, TenantContextHolder.class})
@ConditionalOnProperty(prefix = "basebackend.common.context", name = "auto-cleanup", havingValue = "true", matchIfMissing = true)
public class ContextCleanupFilter implements Filter, Ordered {

    private final CommonProperties commonProperties;

    /**
     * 过滤器执行顺序
     * <p>
     * 默认为 {@code Integer.MIN_VALUE + 100},确保在其他过滤器之前执行。
     * </p>
     *
     * @return 执行顺序值
     */
    @Override
    public int getOrder() {
        return commonProperties.getContext().getFilterOrder();
    }

    /**
     * 过滤器初始化
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("Context cleanup filter initialized with order={}", getOrder());
        Filter.super.init(filterConfig);
    }

    /**
     * 执行过滤逻辑
     * <p>
     * 在请求处理完成后,无论是否发生异常,都会清除上下文。
     * </p>
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param chain    过滤器链
     * @throws IOException      IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            // 继续执行过滤器链
            chain.doFilter(request, response);
        } finally {
            // 清理上下文 (无论是否发生异常都会执行)
            cleanupContext();
        }
    }

    /**
     * 清理上下文
     * <p>
     * 清除 UserContextHolder 和 TenantContextHolder 中的数据。
     * </p>
     */
    private void cleanupContext() {
        try {
            // 清除用户上下文
            if (UserContextHolder.isPresent()) {
                if (log.isDebugEnabled()) {
                    log.debug("Cleaning user context: userId={}", UserContextHolder.getUserId());
                }
                UserContextHolder.clear();
            }

            // 清除租户上下文
            if (TenantContextHolder.isPresent()) {
                if (log.isDebugEnabled()) {
                    log.debug("Cleaning tenant context: tenantId={}", TenantContextHolder.getTenantId());
                }
                TenantContextHolder.clear();
            }
        } catch (Exception e) {
            // 清理过程中的异常不应影响业务流程
            log.error("Failed to cleanup context", e);
        }
    }

    /**
     * 过滤器销毁
     */
    @Override
    public void destroy() {
        log.info("Context cleanup filter destroyed");
        Filter.super.destroy();
    }
}
