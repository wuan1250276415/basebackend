package com.basebackend.scheduler.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 调度模块基础请求类
 * <p>
 * 提供通用的请求元数据字段，所有调度相关的请求DTO应继承此类。
 * 主要用于传递跨服务的可观测性信息。
 * </p>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-24
 */
@Data
public abstract class BaseRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 链路追踪ID
     * <p>
     * 由上游服务生成并传递，用于跨服务的链路追踪和日志关联。
     * 如果未提供，网关或第一个处理节点应自动生成。
     * </p>
     */
    private String traceId;

    /**
     * 请求发起者ID（可选）
     * <p>
     * 用于记录操作审计，标识执行操作的用户或系统账号。
     * </p>
     */
    private String operatorId;
}
