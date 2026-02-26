package com.basebackend.user.dto;

import com.basebackend.common.validation.SafeString;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 用户DTO
 */
public record UserDTO(
    Long id,
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度必须在2-20个字符之间")
    @SafeString(maxLength = 20)
    String username,
    @NotBlank(message = "昵称不能为空")
    @Size(max = 30, message = "昵称长度不能超过30个字符")
    @SafeString(maxLength = 30)
    String nickname,
    @Email(message = "邮箱格式不正确")
    @Size(max = 50, message = "邮箱长度不能超过50个字符")
    @SafeString(maxLength = 50)
    String email,
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @SafeString(maxLength = 20)
    String phone,
    @SafeString(maxLength = 255)
    String avatar,
    Integer gender,
    LocalDate birthday,
    Long deptId,
    @SafeString(maxLength = 64)
    String deptName,
    Integer userType,
    Integer status,
    List<Long> roleIds,
    List<String> roleNames,
    @SafeString(maxLength = 255)
    String remark
) {}
