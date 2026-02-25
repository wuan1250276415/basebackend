package com.basebackend.feign.dto.log;

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
public record UserOperationLogDTO(

        /**
         * 日志ID
         */
        Long id,

        /**
         * 用户ID
         */
        Long userId,

        /**
         * 操作类型（login, logout, update_profile, change_password等）
         */
        String operationType,

        /**
         * 操作描述
         */
        String operationDesc,

        /**
         * IP地址
         */
        String ipAddress,

        /**
         * 位置
         */
        String location,

        /**
         * 浏览器
         */
        String browser,

        /**
         * 操作系统
         */
        String os,

        /**
         * 请求参数
         */
        String requestParams,

        /**
         * 操作状态（0-失败，1-成功）
         */
        Integer status,

        /**
         * 错误信息
         */
        String errorMsg,

        /**
         * 创建时间
         */
        LocalDateTime createTime

) implements Serializable {
}
