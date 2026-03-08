package com.basebackend.database.security.interceptor;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.security.annotation.Sensitive;
import com.basebackend.database.security.service.EncryptionService;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.Invocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("DecryptionInterceptor 测试")
class DecryptionInterceptorTest {

    @Test
    @DisplayName("解密异常时应 fail-close 阻断返回")
    void shouldBlockWhenDecryptionFails() throws Exception {
        DatabaseEnhancedProperties properties = new DatabaseEnhancedProperties();
        properties.getSecurity().getEncryption().setEnabled(true);

        EncryptionService encryptionService = mock(EncryptionService.class);
        when(encryptionService.decrypt(anyString())).thenThrow(new RuntimeException("decrypt failed"));

        DecryptionInterceptor interceptor = new DecryptionInterceptor(encryptionService, properties);
        Invocation invocation = newInvocation(List.of(new SensitiveEntity("encrypted_value")));

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
        @Sensitive
        private String phone;

        SensitiveEntity(String phone) {
            this.phone = phone;
        }
    }
}
