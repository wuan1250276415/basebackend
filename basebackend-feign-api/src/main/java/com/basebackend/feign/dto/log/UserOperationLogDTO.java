package com.basebackend.feign.dto.log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户操作日志DTO
 * <p>
 * 用于Feign跨服务调用时的数据传输
 *
 * @author BaseBackend
 * @since 2025-12-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOperationLogDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 操作类型（login, logout, update_profile, change_password等）
     */
    private String operationType;

    /**
     * 操作描述
     */
    private String operationDesc;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 位置
     */
    private String location;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 请求参数
     */
    private String requestParams;

    /**
     * 操作状态（0-失败，1-成功）
     */
    private Integer status;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
