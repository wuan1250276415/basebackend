package com.basebackend.auth.service;

import com.basebackend.auth.dto.LoginRequest;
import com.basebackend.auth.dto.LoginResponse;
import com.basebackend.auth.dto.PasswordChangeDTO;

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
    LoginResponse.UserInfo getCurrentUserInfo();

    /**
     * 修改密码
     */
    void changePassword(PasswordChangeDTO passwordChangeDTO);

    /**
     * 重置密码
     */
    void resetPassword(Long userId, String newPassword);
}
