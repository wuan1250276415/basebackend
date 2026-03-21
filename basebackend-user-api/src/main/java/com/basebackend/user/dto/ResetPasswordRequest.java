package com.basebackend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 重置密码请求
 */
public record ResetPasswordRequest(

        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 20, message = "新密码长度必须在6-20个字符之间")
        String newPassword
) {
}
