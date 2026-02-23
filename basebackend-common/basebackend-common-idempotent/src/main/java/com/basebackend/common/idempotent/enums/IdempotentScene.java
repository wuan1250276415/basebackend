package com.basebackend.common.idempotent.enums;

/**
 * 幂等场景枚举
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public enum IdempotentScene {

    /**
     * 接口级别（HTTP API）
     */
    API,

    /**
     * 消息消费级别（MQ Consumer）
     */
    MQ
}
