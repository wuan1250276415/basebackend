/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.annotation.PostConstruct
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.util.StringUtils
 */
package com.basebackend.nacos.config;

import com.basebackend.nacos.config.NacosConfigProperties;
import com.basebackend.nacos.security.CredentialEncryptionService;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

public class NacosConfigValidator {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(NacosConfigValidator.class);
    private final NacosConfigProperties properties;
    private final Environment environment;
    private final List<String> warnings = new ArrayList<String>();

    @PostConstruct
    public void validate() {
        log.info("\u5f00\u59cb\u9a8c\u8bc1Nacos\u914d\u7f6e...");
        this.validateConfigCenter();
        this.validateServiceDiscovery();
        if (!this.warnings.isEmpty()) {
            log.warn("Nacos\u914d\u7f6e\u9a8c\u8bc1\u5b8c\u6210\uff0c\u53d1\u73b0 {} \u4e2a\u8b66\u544a:", (Object)this.warnings.size());
            this.warnings.forEach(warning -> log.warn("  - {}", warning));
        } else {
            log.info("Nacos\u914d\u7f6e\u9a8c\u8bc1\u901a\u8fc7");
        }
    }

    private void validateConfigCenter() {
        NacosConfigProperties.Config config = this.properties.getConfig();
        if (!config.isEnabled()) {
            log.info("nacos.config.enabled=false，跳过配置中心校验");
            return;
        }
        if (!StringUtils.hasText((String)config.getServerAddr())) {
            throw new IllegalArgumentException("nacos.config.server-addr \u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (!StringUtils.hasText((String)config.getNamespace())) {
            log.warn("nacos.config.namespace \u672a\u914d\u7f6e\uff0c\u4f7f\u7528\u9ed8\u8ba4\u503c: public");
        }
        if (!StringUtils.hasText((String)config.getGroup())) {
            log.warn("nacos.config.group \u672a\u914d\u7f6e\uff0c\u4f7f\u7528\u9ed8\u8ba4\u503c: DEFAULT_GROUP");
        }
        if (config.getServerAddr().equals("127.0.0.1:8848")) {
            this.warnings.add("\u4f7f\u7528\u4e86\u9ed8\u8ba4Nacos\u5730\u5740 127.0.0.1:8848\uff0c\u751f\u4ea7\u73af\u5883\u8bf7\u4fee\u6539\u4e3a\u5b9e\u9645\u5730\u5740");
        }
        this.validateCredentials("nacos.config", config.getUsername(), config.getPassword());
    }

    private void validateServiceDiscovery() {
        NacosConfigProperties.Discovery discovery = this.properties.getDiscovery();
        if (!discovery.isEnabled()) {
            log.info("nacos.discovery.enabled=false，跳过服务发现校验");
            return;
        }
        if (!StringUtils.hasText((String)discovery.getServerAddr())) {
            throw new IllegalArgumentException("nacos.discovery.server-addr \u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (!StringUtils.hasText((String)discovery.getNamespace())) {
            log.warn("nacos.discovery.namespace \u672a\u914d\u7f6e\uff0c\u4f7f\u7528\u9ed8\u8ba4\u503c: public");
        }
        if (!StringUtils.hasText((String)discovery.getGroup())) {
            log.warn("nacos.discovery.group \u672a\u914d\u7f6e\uff0c\u4f7f\u7528\u9ed8\u8ba4\u503c: DEFAULT_GROUP");
        }
        if (discovery.getWeight() < 0.0 || discovery.getWeight() > 1.0) {
            log.warn("nacos.discovery.weight \u5e94\u8be5\u5728 0-1 \u8303\u56f4\u5185\uff0c\u5f53\u524d\u503c: {}", (Object)discovery.getWeight());
        }
        if (discovery.getServerAddr().equals("127.0.0.1:8848")) {
            this.warnings.add("\u4f7f\u7528\u4e86\u9ed8\u8ba4Nacos\u5730\u5740 127.0.0.1:8848\uff0c\u751f\u4ea7\u73af\u5883\u8bf7\u4fee\u6539\u4e3a\u5b9e\u9645\u5730\u5740");
        }
        this.validateCredentials("nacos.discovery", discovery.getUsername(), discovery.getPassword());
    }

    private void validateCredentials(String prefix, String username, String password) {
        boolean hasUsername = StringUtils.hasText((String)username);
        boolean hasPassword = StringUtils.hasText((String)password);
        if (hasUsername != hasPassword) {
            this.warnings.add(prefix + " \u7528\u6237\u540d\u4e0e\u5bc6\u7801\u5efa\u8bae\u540c\u65f6\u914d\u7f6e\uff0c\u907f\u514d\u8ba4\u8bc1\u5931\u8d25");
        }
        if (!hasUsername && !hasPassword) {
            this.warnings.add(prefix + " \u672a\u914d\u7f6e\u7528\u6237\u540d/\u5bc6\u7801\uff0c\u5982Nacos\u5f00\u542f\u9274\u6743\u5c06\u65e0\u6cd5\u8fde\u63a5");
            return;
        }
        if ("nacos".equals(username) && "nacos".equals(password)) {
            this.warnings.add(prefix + " \u4f7f\u7528\u4e86\u9ed8\u8ba4Nacos\u5f31\u53e3\u4ee4\uff0c\u751f\u4ea7\u73af\u5883\u8bf7\u7acb\u5373\u66f4\u6362");
        }
        boolean startsWithEnc = password != null && password.startsWith(CredentialEncryptionService.ENCRYPTED_PREFIX);
        boolean endsWithEnc = password != null && password.endsWith(CredentialEncryptionService.ENCRYPTED_SUFFIX);
        if (startsWithEnc ^ endsWithEnc) {
            throw new IllegalArgumentException(prefix + ".password \u52a0\u5bc6\u683c\u5f0f\u975e\u6cd5\uff0c\u5e94\u4e3a ENC(...)");
        }
        if (startsWithEnc && CredentialEncryptionService.isDefaultKeySeedInUse(this.environment) && !CredentialEncryptionService.failOnDefaultKey(this.environment)) {
            this.warnings.add(prefix + " \u542f\u7528\u4e86ENC\u52a0\u5bc6\uff0c\u4f46\u5f53\u524d\u4ecd\u4f7f\u7528\u9ed8\u8ba4\u5bc6\u94a5\uff0c\u751f\u4ea7\u73af\u5883\u8bf7\u8bbe\u7f6e NACOS_ENCRYPTION_KEY");
        }
    }

    @Generated
    public NacosConfigValidator(NacosConfigProperties properties) {
        this(properties, null);
    }

    public NacosConfigValidator(NacosConfigProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }
}
