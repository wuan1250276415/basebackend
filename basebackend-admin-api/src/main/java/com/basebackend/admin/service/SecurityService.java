package com.basebackend.admin.service;

import com.basebackend.admin.dto.security.User2FADTO;
import com.basebackend.admin.dto.security.UserDeviceDTO;
import com.basebackend.admin.dto.security.UserOperationLogDTO;

import java.util.List;

/**
 * 安全服务接口
 *
 * @author Claude Code
 * @since 2025-10-30
 */
public interface SecurityService {

    /**
     * 获取当前用户的设备列表
     *
     * @return 设备列表
     */
    List<UserDeviceDTO> getCurrentUserDevices();

    /**
     * 移除设备
     *
     * @param deviceId 设备ID
     */
    void removeDevice(Long deviceId);

    /**
     * 信任设备
     *
     * @param deviceId 设备ID
     */
    void trustDevice(Long deviceId);

    /**
     * 获取当前用户的操作日志
     *
     * @param limit 限制数量
     * @return 操作日志列表
     */
    List<UserOperationLogDTO> getCurrentUserOperationLogs(Integer limit);

    /**
     * 获取当前用户的2FA配置
     *
     * @return 2FA配置
     */
    User2FADTO getCurrent2FAConfig();

    /**
     * 启用2FA
     *
     * @param type 2FA类型
     * @param verifyCode 验证码
     */
    void enable2FA(String type, String verifyCode);

    /**
     * 禁用2FA
     *
     * @param verifyCode 验证码
     */
    void disable2FA(String verifyCode);
}
