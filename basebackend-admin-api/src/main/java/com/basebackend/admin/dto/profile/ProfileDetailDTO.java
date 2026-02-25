package com.basebackend.admin.dto.profile;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 个人资料详情 DTO
 *
 * @author Claude Code
 * @since 2025-10-29
 */
@Schema(description = "个人资料详情")
public record ProfileDetailDTO(
    @Schema(description = "用户ID") Long userId,
    @Schema(description = "用户名") String username,
    @Schema(description = "昵称") String nickname,
    @Schema(description = "邮箱") String email,
    @Schema(description = "手机号") String phone,
    @Schema(description = "头像URL") String avatar,
    @Schema(description = "性别: 0-未知, 1-男, 2-女") Integer gender,
    @Schema(description = "生日") LocalDate birthday,
    @Schema(description = "部门ID") Long deptId,
    @Schema(description = "部门名称") String deptName,
    @Schema(description = "用户类型: 1-系统用户, 2-普通用户") Integer userType,
    @Schema(description = "状态: 0-禁用, 1-启用") Integer status,
    @Schema(description = "最后登录IP") String loginIp,
    @Schema(description = "最后登录时间") LocalDateTime loginTime,
    @Schema(description = "创建时间") LocalDateTime createTime
) {}
