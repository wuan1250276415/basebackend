package com.basebackend.observability.otel.resource;

import io.opentelemetry.api.common.AttributeKey;

/**
 * OpenTelemetry 资源属性常量
 * <p>
 * 封装了 OpenTelemetry 语义约定中定义的标准资源属性，
 * 用于标识服务实例的身份和运行环境。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/resource/">Resource Semantic Conventions</a>
 */
public final class ResourceAttributes {

    private ResourceAttributes() {
        throw new UnsupportedOperationException("工具类不支持实例化");
    }

    /**
     * 服务名称 (service.name)
     * <p>必填属性，标识逻辑服务名称</p>
     */
    public static final AttributeKey<String> SERVICE_NAME =
            ResourceAttributes.SERVICE_NAME;

    /**
     * 服务版本 (service.version)
     * <p>服务的版本号，如 1.0.0</p>
     */
    public static final AttributeKey<String> SERVICE_VERSION =
            ResourceAttributes.SERVICE_VERSION;

    /**
     * 部署环境 (deployment.environment)
     * <p>服务运行的环境，如 dev, test, staging, prod</p>
     */
    public static final AttributeKey<String> DEPLOYMENT_ENVIRONMENT =
            ResourceAttributes.DEPLOYMENT_ENVIRONMENT;

    /**
     * 主机名 (host.name)
     * <p>服务运行的物理或虚拟主机名称</p>
     */
    public static final AttributeKey<String> HOST_NAME =
            ResourceAttributes.HOST_NAME;
}
