package com.basebackend.system.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 强制下线请求
 */
public record ForceLogoutRequest(

        @NotBlank(message = "用户令牌不能为空")
        String token
) {
}
