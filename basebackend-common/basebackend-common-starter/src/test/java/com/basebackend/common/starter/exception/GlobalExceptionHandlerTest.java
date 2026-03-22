package com.basebackend.common.starter.exception;

import com.basebackend.common.enums.CommonErrorCode;
import com.basebackend.common.model.Result;
import com.basebackend.common.starter.properties.CommonProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler 测试")
class GlobalExceptionHandlerTest {

    @Test
    @DisplayName("NoResourceFoundException 应映射为 404")
    void shouldReturnNotFoundWhenNoResourceFoundExceptionOccurs() {
        CommonProperties properties = new CommonProperties();
        GlobalExceptionHandler handler = new GlobalExceptionHandler(properties);
        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(),
                "/api/system/application/resource/user/tree/");
        NoResourceFoundException exception = new NoResourceFoundException(
                HttpMethod.GET,
                "static resource",
                "api/system/application/resource/user/tree");

        Result<Void> result = handler.handleNoResourceFoundException(exception, request);

        assertThat(result.getCode()).isEqualTo(CommonErrorCode.NOT_FOUND.getCode());
        assertThat(result.getMessage()).isEqualTo("请求的资源不存在");
    }
}
