package com.basebackend.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网关统一响应结果
 *
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GatewayResult<T> {

    private int code;
    private String message;
    private T data;

    public GatewayResult(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public String toJsonString() {
        return "{\"code\":" + code + ",\"message\":\"" + (message != null ? message.replace("\"", "\\\"") : "") + "\"}";
    }

    public static <T> GatewayResult<T> success(T data) {
        return new GatewayResult<>(200, "success", data);
    }

    public static <T> GatewayResult<T> fail(int code, String message) {
        return new GatewayResult<>(code, message);
    }
}
