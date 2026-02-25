package com.basebackend.admin.dto;

import java.time.LocalDateTime;

/**
 * 在线用户DTO
 */
public record OnlineUserDTO(
    /** 用户ID */
    Long userId,
    /** 用户名 */
    String username,
    /** 昵称 */
    String nickname,
    /** 部门名称 */
    String deptName,
    /** 登录IP */
    String loginIp,
    /** 登录地点 */
    String loginLocation,
    /** 浏览器 */
    String browser,
    /** 操作系统 */
    String os,
    /** 登录时间 */
    LocalDateTime loginTime,
    /** 最后访问时间 */
    LocalDateTime lastAccessTime,
    /** Token */
    String token
) {}
