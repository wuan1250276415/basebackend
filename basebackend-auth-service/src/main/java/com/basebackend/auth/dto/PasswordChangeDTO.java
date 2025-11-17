package com.basebackend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 密码修改DTO
 */
@Data
public class PasswordChangeDTO {

    /**
     * 旧密码
     */
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    private String newPassword;

    /**
     * 确认密码
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
