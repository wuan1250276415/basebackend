package com.basebackend.auth.dto;

import com.basebackend.common.validation.SafeString;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求DTO
 */
@Data
public class LoginRequest {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @SafeString(maxLength = 64)
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 验证码
     */
    @SafeString(maxLength = 10)
    private String captcha;

    /**
     * 验证码标识
     */
    @SafeString(maxLength = 64)
    private String captchaId;

    /**
     * 记住我
     */
    private Boolean rememberMe;
}
