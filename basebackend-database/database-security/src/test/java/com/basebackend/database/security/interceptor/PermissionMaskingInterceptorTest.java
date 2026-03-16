package com.basebackend.database.security.interceptor;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.security.annotation.Sensitive;
import com.basebackend.database.security.annotation.SensitiveType;
import com.basebackend.database.security.context.PermissionContext;
import com.basebackend.database.security.service.DataMaskingService;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.Invocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("PermissionMaskingInterceptor 测试")
class PermissionMaskingInterceptorTest {

    @AfterEach
    void tearDown() {
        PermissionContext.clear();
    }

    @Test
    @DisplayName("脱敏异常时应 fail-close 阻断返回")
    void shouldBlockWhenMaskingFails() throws Exception {
        DatabaseEnhancedProperties properties = new DatabaseEnhancedProperties();
        properties.getSecurity().getMasking().setEnabled(true);

        DataMaskingService dataMaskingService = mock(DataMaskingService.class);
        when(dataMaskingService.mask(anyString(), any(SensitiveType.class)))
                .thenThrow(new RuntimeException("mask failed"));

        PermissionMaskingInterceptor interceptor = new PermissionMaskingInterceptor(dataMaskingService, properties);
        Invocation invocation = newInvocation(List.of(new SensitiveEntity("13800001234")));

        assertThatThrownBy(() -> interceptor.intercept(invocation))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("阻断")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    private Invocation newInvocation(List<Object> result) throws Exception {
        ResultSetHandler resultSetHandler = mock(ResultSetHandler.class);
        when(resultSetHandler.handleResultSets(null)).thenReturn(result);
        Method method = ResultSetHandler.class.getMethod("handleResultSets", Statement.class);
        return new Invocation(resultSetHandler, method, new Object[]{null});
    }

    static class SensitiveEntity {
        @Sensitive(type = SensitiveType.PHONE)
        private String phone;

        SensitiveEntity(String phone) {
            this.phone = phone;
        }
    }
}
