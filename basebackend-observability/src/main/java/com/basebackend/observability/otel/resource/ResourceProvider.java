package com.basebackend.observability.otel.resource;

import com.basebackend.observability.otel.config.OtelProperties;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * OpenTelemetry 资源提供者
 * <p>
 * 负责构建 {@link Resource} 实例，包含服务元数据和主机信息。
 * Resource 会被附加到所有导出的 Telemetry 数据中，用于标识数据来源。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Component
public class ResourceProvider {

    private static final Logger log = LoggerFactory.getLogger(ResourceProvider.class);

    private final OtelProperties properties;

    public ResourceProvider(@org.springframework.beans.factory.annotation.Autowired(required = false) OtelProperties properties) {
        this.properties = properties;
    }

    /**
     * 创建包含服务元数据和主机信息的 Resource
     * <p>
     * Resource 会与 SDK 默认的资源属性合并，包括：
     * <ul>
     *     <li>telemetry.sdk.name: "opentelemetry"</li>
     *     <li>telemetry.sdk.language: "java"</li>
     *     <li>telemetry.sdk.version: OpenTelemetry SDK 版本</li>
     * </ul>
     * </p>
     *
     * @return 构建的 Resource 实例
     */
    public Resource resource() {
        if (properties == null) {
            log.warn("OtelProperties not available, using default resource");
            return Resource.getDefault();
        }
        
        AttributesBuilder builder = Attributes.builder();

        // 服务名称（必填）
        if (StringUtils.hasText(properties.getService().getName())) {
            builder.put(ResourceAttributes.SERVICE_NAME, properties.getService().getName());
        } else {
            log.warn("未配置 service.name，将使用默认值 'unknown_service:java'");
            builder.put(ResourceAttributes.SERVICE_NAME, "unknown_service:java");
        }

        // 服务版本
        if (StringUtils.hasText(properties.getService().getVersion())) {
            builder.put(ResourceAttributes.SERVICE_VERSION, properties.getService().getVersion());
        }

        // 部署环境
        if (StringUtils.hasText(properties.getService().getEnvironment())) {
            builder.put(ResourceAttributes.DEPLOYMENT_ENVIRONMENT, properties.getService().getEnvironment());
        }

        // 主机名
        String hostName = resolveHostName();
        if (StringUtils.hasText(hostName)) {
            builder.put(ResourceAttributes.HOST_NAME, hostName);
        }

        Resource customResource = Resource.create(builder.build());
        Resource defaultResource = Resource.getDefault();

        // 合并默认资源和自定义资源
        Resource mergedResource = defaultResource.merge(customResource);

        log.info("OpenTelemetry Resource 已创建: {}", mergedResource.getAttributes());
        return mergedResource;
    }

    /**
     * 解析当前主机名
     *
     * @return 主机名，解析失败时返回 null
     */
    private String resolveHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            log.warn("无法解析主机名，Resource 将不包含 host.name 属性", ex);
            return null;
        }
    }
}
