package com.basebackend.user.service;

import com.basebackend.common.context.UserContext;
import com.basebackend.feign.dto.user.LoginRequest;
import com.basebackend.feign.dto.user.LoginResponse;
import com.basebackend.user.dto.PasswordChangeDTO;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest loginRequest);

    /**
     * 用户登出
     */
    void logout();

    /**
     * 刷新Token
     */
    LoginResponse refreshToken(String refreshToken);

    /**
     * 获取当前用户信息
     */
    UserContext getCurrentUserInfo();

    /**
     * 修改密码
     */
    void changePassword(PasswordChangeDTO passwordChangeDTO);

    /**
     * 重置密码
     */
    void resetPassword(Long userId, String newPassword);

    /**
     * 微信单点登录
     * 根据手机号查询用户，如果存在则返回token，如果不存在则创建用户后返回token
     *
     * @param phone 手机号
     * @return 登录响应信息（包含token和用户信息）
     */
    LoginResponse wechatLogin(String phone);
}
