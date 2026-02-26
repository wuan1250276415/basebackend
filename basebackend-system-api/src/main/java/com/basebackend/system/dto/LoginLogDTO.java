package com.basebackend.system.dto;

import java.time.LocalDateTime;

/**
 * 登录日志DTO
 */
public record LoginLogDTO(
        /** 日志ID */
        String id,
        /** 用户ID */
        Long userId,
        /** 用户名 */
        String username,
        /** 登录IP */
        String ipAddress,
        /** 登录地点 */
        String loginLocation,
        /** 浏览器类型 */
        String browser,
        /** 操作系统 */
        String os,
        /** 登录状态：0-失败，1-成功 */
        Integer status,
        /** 提示消息 */
        String msg,
        /** 登录时间 */
        LocalDateTime loginTime
) {
}
