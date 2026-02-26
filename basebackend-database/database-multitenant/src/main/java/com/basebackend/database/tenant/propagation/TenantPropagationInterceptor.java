package com.basebackend.database.tenant.propagation;

import com.basebackend.database.tenant.context.TenantContext;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * 租户上下文传播拦截器（RestClient / RestTemplate）
 * <p>
 * 在微服务间 HTTP 调用时，自动将当前租户 ID 通过 Header 传递到下游服务。
 */
public class TenantPropagationInterceptor implements ClientHttpRequestInterceptor {

    private final String headerName;

    public TenantPropagationInterceptor() {
        this("X-Tenant-Id");
    }

    public TenantPropagationInterceptor(String headerName) {
        this.headerName = headerName;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                         ClientHttpRequestExecution execution) throws IOException {
        String tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.isBlank()) {
            request.getHeaders().set(headerName, tenantId);
        }
        return execution.execute(request, body);
    }
}
