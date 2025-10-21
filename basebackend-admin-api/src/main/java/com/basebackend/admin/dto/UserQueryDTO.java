package com.basebackend.admin.dto;

import com.basebackend.common.validation.SafeString;
import lombok.Data;

/**
 * 用户查询DTO
 */
@Data
public class UserQueryDTO {

    /**
     * 用户名
     */
    @SafeString(maxLength = 20)
    private String username;

    /**
     * 昵称
     */
    @SafeString(maxLength = 30)
    private String nickname;

    /**
     * 邮箱
     */
    @SafeString(maxLength = 50)
    private String email;

    /**
     * 手机号
     */
    @SafeString(maxLength = 20)
    private String phone;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 用户类型
     */
    private Integer userType;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 开始时间
     */
    @SafeString(maxLength = 32)
    private String beginTime;

    /**
     * 结束时间
     */
    @SafeString(maxLength = 32)
    private String endTime;
}
