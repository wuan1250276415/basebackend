package com.basebackend.common.idempotent.enums;

/**
 * 幂等策略枚举
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public enum IdempotentStrategy {

    /**
     * 基于前端传来的幂等 Token（Header: X-Idempotent-Token）
     */
    TOKEN,

    /**
     * 基于请求参数 MD5 + 用户ID
     */
    PARAM,

    /**
     * 基于 SpEL 表达式
     */
    SPEL
}
