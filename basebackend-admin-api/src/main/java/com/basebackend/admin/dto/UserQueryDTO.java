package com.basebackend.admin.dto;

import com.basebackend.common.validation.SafeString;

/**
 * 用户查询DTO
 */
public record UserQueryDTO(
    /** 用户名 */
    @SafeString(maxLength = 20)
    String username,
    /** 昵称 */
    @SafeString(maxLength = 30)
    String nickname,
    /** 邮箱 */
    @SafeString(maxLength = 50)
    String email,
    /** 手机号 */
    @SafeString(maxLength = 20)
    String phone,
    /** 部门ID */
    Long deptId,
    /** 用户类型 */
    Integer userType,
    /** 状态 */
    Integer status,
    /** 开始时间 */
    @SafeString(maxLength = 32)
    String beginTime,
    /** 结束时间 */
    @SafeString(maxLength = 32)
    String endTime
) {}
