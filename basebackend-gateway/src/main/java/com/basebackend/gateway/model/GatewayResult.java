package com.basebackend.gateway.model;

import com.alibaba.fastjson2.JSON;
import lombok.Data;

/**
 * 网关统一响应结果
 */
@Data
public class GatewayResult<T> {
    
    /**
     * 状态码
     */
    private Integer code;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 数据
     */
    private T data;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    public GatewayResult() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public GatewayResult(Integer code, String message) {
        this();
        this.code = code;
        this.message = message;
    }
    
    public GatewayResult(Integer code, String message, T data) {
        this(code, message);
        this.data = data;
    }
    
    /**
     * 成功响应
     */
    public static <T> GatewayResult<T> success() {
        return new GatewayResult<>(200, "操作成功");
    }
    
    /**
     * 成功响应带数据
     */
    public static <T> GatewayResult<T> success(T data) {
        return new GatewayResult<>(200, "操作成功", data);
    }
    
    /**
     * 成功响应带消息和数据
     */
    public static <T> GatewayResult<T> success(String message, T data) {
        return new GatewayResult<>(200, message, data);
    }
    
    /**
     * 错误响应
     */
    public static <T> GatewayResult<T> error(Integer code, String message) {
        return new GatewayResult<>(code, message);
    }
    
    /**
     * 错误响应
     */
    public static <T> GatewayResult<T> error(String message) {
        return new GatewayResult<>(500, message);
    }
    
    /**
     * 转换为JSON字符串
     */
    public String toJsonString() {
        return JSON.toJSONString(this);
    }
}
