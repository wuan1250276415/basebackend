package com.basebackend.api.model.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录请求DTO
 */
public record LoginRequest(
    @NotBlank(message = "用户名不能为空") @Size(max = 64) String username,
    @NotBlank(message = "密码不能为空") String password,
    @Size(max = 10) String captcha,
    @Size(max = 64) String captchaId,
    Boolean rememberMe
) {}
