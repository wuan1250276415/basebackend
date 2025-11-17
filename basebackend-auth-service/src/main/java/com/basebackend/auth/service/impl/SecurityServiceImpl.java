package com.basebackend.auth.service.impl;

import com.basebackend.auth.service.SecurityService;
import com.basebackend.feign.client.SecurityFeignClient;
import com.basebackend.feign.dto.security.User2FADTO;
import com.basebackend.feign.dto.security.UserDeviceDTO;
import com.basebackend.feign.dto.security.UserOperationLogDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 安全服务实现类
 * 通过 Feign 调用 admin-api 的安全服务
 *
 * @author BaseBackend Team
 * @since 2025-11-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityServiceImpl implements SecurityService {

    private final SecurityFeignClient securityFeignClient;

    @Override
    public List<UserDeviceDTO> getCurrentUserDevices() {
        log.info("获取当前用户设备列表");
        var result = securityFeignClient.getCurrentUserDevices();
        return result.getData() != null ? result.getData() : List.of();
    }

    @Override
    public void removeDevice(Long deviceId) {
        log.info("移除设备: deviceId={}", deviceId);
        var result = securityFeignClient.removeDevice(deviceId);
        if (result.getCode() != 200) {
            throw new RuntimeException(result.getMessage());
        }
    }

    @Override
    public void trustDevice(Long deviceId) {
        log.info("信任设备: deviceId={}", deviceId);
        var result = securityFeignClient.trustDevice(deviceId);
        if (result.getCode() != 200) {
            throw new RuntimeException(result.getMessage());
        }
    }

    @Override
    public List<UserOperationLogDTO> getCurrentUserOperationLogs(Integer limit) {
        log.info("获取当前用户操作日志, limit={}", limit);
        var result = securityFeignClient.getOperationLogs(limit);
        return result.getData() != null ? result.getData() : List.of();
    }

    @Override
    public User2FADTO getCurrent2FAConfig() {
        log.info("获取当前用户2FA配置");
        var result = securityFeignClient.get2FAConfig();
        return result.getData();
    }

    @Override
    public void enable2FA(String type, String verifyCode) {
        log.info("启用2FA: type={}", type);
        var result = securityFeignClient.enable2FA(type, verifyCode);
        if (result.getCode() != 200) {
            throw new RuntimeException(result.getMessage());
        }
    }

    @Override
    public void disable2FA(String verifyCode) {
        log.info("禁用2FA");
        var result = securityFeignClient.disable2FA(verifyCode);
        if (result.getCode() != 200) {
            throw new RuntimeException(result.getMessage());
        }
    }
}
