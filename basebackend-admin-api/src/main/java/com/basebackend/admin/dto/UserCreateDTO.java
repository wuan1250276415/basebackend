package com.basebackend.admin.dto;

import com.basebackend.common.validation.SafeString;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * 用户创建DTO
 */
public record UserCreateDTO(
    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度必须在2-20个字符之间")
    @SafeString(maxLength = 20)
    String username,
    /** 密码 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    String password,
    /** 昵称 */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 30, message = "昵称长度不能超过30个字符")
    @SafeString(maxLength = 30)
    String nickname,
    /** 邮箱 */
    @Email(message = "邮箱格式不正确")
    @Size(max = 50, message = "邮箱长度不能超过50个字符")
    @SafeString(maxLength = 50)
    String email,
    /** 手机号 */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @SafeString(maxLength = 20)
    String phone,
    /** 头像 */
    @SafeString(maxLength = 255)
    String avatar,
    /** 性别：0-未知，1-男，2-女 */
    Integer gender,
    /** 生日 */
    LocalDate birthday,
    /** 部门ID */
    Long deptId,
    /** 用户类型：1-系统用户，2-普通用户 */
    Integer userType,
    /** 状态：0-禁用，1-启用 */
    Integer status,
    /** 角色ID列表 */
    List<Long> roleIds,
    /** 备注 */
    @SafeString(maxLength = 255)
    String remark
) {}
