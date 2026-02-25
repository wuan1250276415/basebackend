package com.basebackend.admin.dto;

import com.basebackend.common.validation.SafeString;
import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求DTO
 */
public record LoginRequest(
    @NotBlank(message = "用户名不能为空") @SafeString(maxLength = 64) String username,
    @NotBlank(message = "密码不能为空") String password,
    @SafeString(maxLength = 10) String captcha,
    @SafeString(maxLength = 64) String captchaId,
    Boolean rememberMe
) {}
