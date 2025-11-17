package com.basebackend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @Size(max = 64, message = "用户名长度不能超过64个字符")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 验证码
     */
    @Size(max = 10, message = "验证码长度不能超过10个字符")
    private String captcha;

    /**
     * 验证码标识
     */
    @Size(max = 64, message = "验证码标识长度不能超过64个字符")
    private String captchaId;

    /**
     * 记住我
     */
    private Boolean rememberMe;
}
