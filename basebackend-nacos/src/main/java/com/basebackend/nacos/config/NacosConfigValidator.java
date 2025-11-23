package com.basebackend.nacos.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Nacos配置验证器
 * <p>
 * 渐进验证策略：
 * - 关键配置（server-addr）：启动时必验证，失败则拒绝启动
 * - 非关键配置（timeout、retry次数）：记录警告但不阻止启动
 * - 缺失配置：使用默认值继续启动
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class NacosConfigValidator {

    private final NacosConfigProperties properties;
    private final List<String> warnings = new ArrayList<>();

    @PostConstruct
    public void validate() {
        log.info("开始验证Nacos配置...");

        validateConfigCenter();
        validateServiceDiscovery();

        if (!warnings.isEmpty()) {
            log.warn("Nacos配置验证完成，发现 {} 个警告:", warnings.size());
            warnings.forEach(warning -> log.warn("  - {}", warning));
        } else {
            log.info("Nacos配置验证通过");
        }
    }

    /**
     * 验证配置中心配置
     */
    private void validateConfigCenter() {
        NacosConfigProperties.Config config = properties.getConfig();

        // 关键配置验证
        if (!StringUtils.hasText(config.getServerAddr())) {
            throw new IllegalArgumentException("nacos.config.server-addr 不能为空");
        }

        if (!StringUtils.hasText(config.getNamespace())) {
            log.warn("nacos.config.namespace 未配置，使用默认值: public");
        }

        if (!StringUtils.hasText(config.getGroup())) {
            log.warn("nacos.config.group 未配置，使用默认值: DEFAULT_GROUP");
        }

        // 非关键配置验证
        if (config.getServerAddr().equals("127.0.0.1:8848")) {
            warnings.add("使用了默认Nacos地址 127.0.0.1:8848，生产环境请修改为实际地址");
        }

        if (config.getUsername() != null && config.getUsername().equals("nacos") && config.getPassword() != null && config.getPassword().equals("nacos")) {
            log.warn("使用了默认Nacos用户名密码，生产环境请修改");
        }
    }

    /**
     * 验证服务发现配置
     */
    private void validateServiceDiscovery() {
        NacosConfigProperties.Discovery discovery = properties.getDiscovery();

        // 关键配置验证
        if (!StringUtils.hasText(discovery.getServerAddr())) {
            throw new IllegalArgumentException("nacos.discovery.server-addr 不能为空");
        }

        if (!StringUtils.hasText(discovery.getNamespace())) {
            log.warn("nacos.discovery.namespace 未配置，使用默认值: public");
        }

        if (!StringUtils.hasText(discovery.getGroup())) {
            log.warn("nacos.discovery.group 未配置，使用默认值: DEFAULT_GROUP");
        }

        // 权重验证
        if (discovery.getWeight() < 0 || discovery.getWeight() > 1.0) {
            log.warn("nacos.discovery.weight 应该在 0-1 范围内，当前值: {}", discovery.getWeight());
        }

        // 非关键配置验证
        if (discovery.getServerAddr().equals("127.0.0.1:8848")) {
            warnings.add("使用了默认Nacos地址 127.0.0.1:8848，生产环境请修改为实际地址");
        }
    }
}
