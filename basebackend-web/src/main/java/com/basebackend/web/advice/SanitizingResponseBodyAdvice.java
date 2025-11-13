package com.basebackend.web.advice;

import com.basebackend.common.model.Result;
import com.basebackend.common.util.SanitizationUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 响应体统一清洗，防止向客户端输出未经过滤的危险字符串
 */
@RestControllerAdvice
public class SanitizingResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof Result<?> result) {
            String message = result.getMessage();
            if (message != null) {
                result.setMessage(SanitizationUtils.sanitize(message));
            }
            Object data = result.getData();
            if (data instanceof String stringData) {
                @SuppressWarnings("unchecked")
                Result<Object> mutable = (Result<Object>) result;
                mutable.setData(SanitizationUtils.sanitize(stringData));
                return mutable;
            }
            return result;
        }
        if (body instanceof String str) {
            return SanitizationUtils.sanitize(str);
        }
        return body;
    }
}
