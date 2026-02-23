package com.basebackend.common.context;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户上下文信息接口
 * <p>
 * 定义租户上下文的标准契约，支持多租户 SaaS 架构。
 * 该接口仅定义数据结构，不包含任何业务逻辑。
 * </p>
 *
 * <h3>设计原则：</h3>
 * <ul>
 *   <li>仅包含租户身份和基本信息的只读访问器</li>
 *   <li>不依赖任何具体实现</li>
 *   <li>支持跨模块、跨服务传递租户上下文</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 获取当前租户上下文
 * TenantContextInfo tenantContext = TenantContextHolder.get();
 * Long tenantId = tenantContext.getTenantId();
 * String tenantCode = tenantContext.getTenantCode();
 * }</pre>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public interface TenantContextInfo extends Serializable {

    /**
     * 获取租户ID
     *
     * @return 租户唯一标识
     */
    Long getTenantId();

    /**
     * 获取租户编码
     *
     * @return 租户编码，可能为 null
     */
    default String getTenantCode() {
        return null;
    }

    /**
     * 获取租户名称
     *
     * @return 租户名称，可能为 null
     */
    default String getTenantName() {
        return null;
    }

    /**
     * 获取租户状态
     * <p>
     * 状态定义：
     * </p>
     * <ul>
     *   <li>0: 禁用</li>
     *   <li>1: 正常</li>
     *   <li>2: 过期</li>
     * </ul>
     *
     * @return 租户状态，默认返回 1（正常）
     */
    default Integer getStatus() {
        return 1;
    }

    /**
     * 判断租户是否正常状态
     *
     * @return 是否正常
     */
    default boolean isEnabled() {
        return Integer.valueOf(1).equals(getStatus());
    }

    /**
     * 判断租户是否被禁用
     *
     * @return 是否禁用
     */
    default boolean isDisabled() {
        return Integer.valueOf(0).equals(getStatus());
    }

    /**
     * 判断租户是否已过期
     *
     * @return 是否过期
     */
    default boolean isExpired() {
        return Integer.valueOf(2).equals(getStatus());
    }

    /**
     * 获取租户套餐/版本
     * <p>
     * 套餐定义示例：
     * </p>
     * <ul>
     *   <li>FREE: 免费版</li>
     *   <li>BASIC: 基础版</li>
     *   <li>PRO: 专业版</li>
     *   <li>ENTERPRISE: 企业版</li>
     * </ul>
     *
     * @return 套餐编码，可能为 null
     */
    default String getPackageCode() {
        return null;
    }

    /**
     * 获取租户到期时间
     *
     * @return 到期时间，可能为 null（表示永久有效）
     */
    default LocalDateTime getExpireTime() {
        return null;
    }

    /**
     * 获取租户联系人邮箱
     *
     * @return 联系人邮箱，可能为 null
     */
    default String getContactEmail() {
        return null;
    }

    /**
     * 获取租户联系人电话
     *
     * @return 联系人电话，可能为 null
     */
    default String getContactPhone() {
        return null;
    }

    /**
     * 获取租户数据隔离模式
     * <p>
     * 隔离模式定义：
     * </p>
     * <ul>
     *   <li>COLUMN: 列级隔离（同一表，通过 tenant_id 字段区分）</li>
     *   <li>SCHEMA: Schema 级隔离（不同 Schema）</li>
     *   <li>DATABASE: 数据库级隔离（不同数据库）</li>
     * </ul>
     *
     * @return 隔离模式，默认返回 "COLUMN"
     */
    default String getIsolationMode() {
        return "COLUMN";
    }
}
