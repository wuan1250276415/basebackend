package com.basebackend.api.model.user;

import jakarta.validation.constraints.NotBlank;

/**
 * 刷新令牌请求
 */
public record RefreshTokenRequest(

        @NotBlank(message = "刷新令牌不能为空")
        String refreshToken
) {
}
