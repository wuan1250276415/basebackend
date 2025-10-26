package com.basebackend.featuretoggle.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 特性开关上下文
 * 用于传递用户信息、环境信息等用于特性开关判断
 *
 * @author BaseBackend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureContext {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 环境（dev/test/prod）
     */
    private String environment;

    /**
     * 用户属性（自定义属性）
     */
    @Builder.Default
    private Map<String, String> properties = new HashMap<>();

    /**
     * 添加自定义属性
     */
    public void addProperty(String key, String value) {
        this.properties.put(key, value);
    }

    /**
     * 创建默认上下文（无用户信息）
     */
    public static FeatureContext empty() {
        return FeatureContext.builder().build();
    }

    /**
     * 创建用户上下文
     */
    public static FeatureContext forUser(String userId) {
        return FeatureContext.builder()
                .userId(userId)
                .build();
    }

    /**
     * 创建完整用户上下文
     */
    public static FeatureContext forUser(String userId, String username, String email) {
        return FeatureContext.builder()
                .userId(userId)
                .username(username)
                .email(email)
                .build();
    }
}
