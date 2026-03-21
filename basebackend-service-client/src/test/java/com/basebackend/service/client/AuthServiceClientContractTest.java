package com.basebackend.service.client;

import com.basebackend.api.model.user.RefreshTokenRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthServiceClient 契约测试")
class AuthServiceClientContractTest {

    @Test
    @DisplayName("刷新令牌接口应通过请求体传输敏感值")
    void refreshTokenShouldUseRequestBody() throws Exception {
        Method method = AuthServiceClient.class.getDeclaredMethod("refreshToken", RefreshTokenRequest.class);
        assertThat(method.getParameters()[0].isAnnotationPresent(RequestBody.class)).isTrue();
    }
}
