package com.basebackend.feign.dto.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户基础信息 DTO（用于 Feign 调用）
 *
 * @author Claude Code
 * @since 2025-11-08
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "用户基础信息")
public record UserBasicDTO(

        @Schema(description = "用户ID")
        Long id,

        @Schema(description = "用户名")
        String username,

        @Schema(description = "昵称")
        String nickname,

        @Schema(description = "真实姓名")
        String realName,

        @Schema(description = "邮箱")
        String email,

        @Schema(description = "手机号")
        String phone,

        @Schema(description = "性别：0-未知，1-男，2-女")
        Integer gender,

        @Schema(description = "头像URL")
        String avatar,

        @Schema(description = "部门ID")
        Long deptId,

        @Schema(description = "部门名称")
        String deptName,

        @Schema(description = "职位")
        String position,

        @Schema(description = "状态：0-禁用，1-启用")
        Integer status,

        @Schema(description = "角色ID列表")
        List<Long> roleIds,

        @Schema(description = "角色名称列表")
        List<String> roleNames,

        @Schema(description = "创建时间")
        LocalDateTime createTime,

        @Schema(description = "更新时间")
        LocalDateTime updateTime

) implements Serializable {
}
