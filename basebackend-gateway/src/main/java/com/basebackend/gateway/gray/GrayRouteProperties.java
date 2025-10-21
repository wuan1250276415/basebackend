package com.basebackend.gateway.gray;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 灰度路由配置
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "gateway.gray")
public class GrayRouteProperties {

    /**
     * 是否启用灰度路由
     */
    private Boolean enabled = false;

    /**
     * 灰度规则列表
     */
    private List<GrayRule> rules = new ArrayList<>();

    @Data
    public static class GrayRule {
        /**
         * 服务名
         */
        private String serviceName;

        /**
         * 灰度版本
         */
        private String grayVersion;

        /**
         * 稳定版本
         */
        private String stableVersion;

        /**
         * 灰度策略类型（header/user/ip/weight）
         */
        private String strategy;

        /**
         * Header名称（strategy=header时使用）
         */
        private String headerName;

        /**
         * Header值（strategy=header时使用）
         */
        private String headerValue;

        /**
         * 用户ID列表（strategy=user时使用）
         */
        private List<String> userIds;

        /**
         * IP列表（strategy=ip时使用）
         */
        private List<String> ipList;

        /**
         * 灰度权重百分比0-100（strategy=weight时使用）
         */
        private Integer weight;
    }
}
