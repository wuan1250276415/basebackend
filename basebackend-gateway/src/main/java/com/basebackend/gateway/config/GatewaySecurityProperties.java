package com.basebackend.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 网关安全配置属性
 * <p>
 * 从配置文件中读取认证白名单、超时设置等安全相关配置，
 * 避免硬编码，提高配置灵活性。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "gateway.security")
public class GatewaySecurityProperties {

    /**
     * 认证白名单路径
     * 支持 Ant 风格路径匹配
     */
    private List<String> whitelist = new ArrayList<>();

    /**
     * Actuator 端点白名单
     * 默认只允许 health 端点
     */
    private List<String> actuatorWhitelist = new ArrayList<>();

    /**
     * Redis 操作超时时间
     */
    private Duration redisTimeout = Duration.ofSeconds(2);

    /**
     * 是否启用严格模式
     * 严格模式下，未配置的路径都需要认证
     */
    private boolean strictMode = true;

    /**
     * 是否记录认证详细日志
     */
    private boolean debugLogging = false;

    /**
     * 获取合并后的完整白名单
     * 包括用户配置的白名单和 actuator 白名单
     */
    public List<String> getFullWhitelist() {
        List<String> fullList = new ArrayList<>(whitelist);
        fullList.addAll(actuatorWhitelist);
        return fullList;
    }
}
