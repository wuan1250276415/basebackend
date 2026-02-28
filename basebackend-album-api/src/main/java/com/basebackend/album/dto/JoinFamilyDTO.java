package com.basebackend.album.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 加入家庭请求 DTO
 *
 * @param inviteCode 邀请码
 * @param nickname   家庭内昵称（可选）
 * @author BearTeam
 */
public record JoinFamilyDTO(
        @NotBlank(message = "邀请码不能为空")
        String inviteCode,

        String nickname
) {
}
