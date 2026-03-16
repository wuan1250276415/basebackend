package com.basebackend.album.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 修改成员角色请求 DTO
 *
 * @param role 角色: 0=成员 1=管理员
 * @author BearTeam
 */
public record UpdateMemberRoleDTO(
        @NotNull(message = "角色不能为空")
        Integer role
) {
}
