package com.basebackend.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.admin.dto.security.User2FADTO;
import com.basebackend.admin.dto.security.UserDeviceDTO;
import com.basebackend.admin.dto.security.UserOperationLogDTO;
import com.basebackend.admin.entity.User2FA;
import com.basebackend.admin.entity.UserDevice;
import com.basebackend.admin.entity.UserOperationLog;
import com.basebackend.admin.entity.SysUser;
import com.basebackend.admin.mapper.User2FAMapper;
import com.basebackend.admin.mapper.UserDeviceMapper;
import com.basebackend.admin.mapper.UserOperationLogMapper;
import com.basebackend.admin.mapper.SysUserMapper;
import com.basebackend.admin.service.SecurityService;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.observability.metrics.CustomMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 安全服务实现类
 *
 * @author Claude Code
 * @since 2025-10-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityServiceImpl implements SecurityService {

    private final UserDeviceMapper deviceMapper;
    private final UserOperationLogMapper operationLogMapper;
    private final User2FAMapper user2FAMapper;
    private final SysUserMapper userMapper;
    private final CustomMetrics customMetrics;

    @Override
    public List<UserDeviceDTO> getCurrentUserDevices() {
        log.info("获取当前用户设备列表");
        customMetrics.recordBusinessOperation("security", "get_devices");

        Long currentUserId = getCurrentUserId();
        LambdaQueryWrapper<UserDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDevice::getUserId, currentUserId)
               .orderByDesc(UserDevice::getLastActiveTime);

        List<UserDevice> devices = deviceMapper.selectList(wrapper);
        return devices.stream()
                .map(device -> BeanUtil.copyProperties(device, UserDeviceDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeDevice(Long deviceId) {
        log.info("移除设备: deviceId={}", deviceId);
        customMetrics.recordBusinessOperation("security", "remove_device");

        Long currentUserId = getCurrentUserId();

        // 验证设备归属
        UserDevice device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        if (!device.getUserId().equals(currentUserId)) {
            throw new BusinessException("无权限操作此设备");
        }

        int result = deviceMapper.deleteById(deviceId);
        if (result <= 0) {
            throw new BusinessException("移除设备失败");
        }

        log.info("设备移除成功: deviceId={}", deviceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void trustDevice(Long deviceId) {
        log.info("信任设备: deviceId={}", deviceId);
        customMetrics.recordBusinessOperation("security", "trust_device");

        Long currentUserId = getCurrentUserId();

        // 验证设备归属
        UserDevice device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        if (!device.getUserId().equals(currentUserId)) {
            throw new BusinessException("无权限操作此设备");
        }

        device.setIsTrusted(1);
        device.setUpdateTime(LocalDateTime.now());

        int result = deviceMapper.updateById(device);
        if (result <= 0) {
            throw new BusinessException("信任设备失败");
        }

        log.info("设备已设为信任: deviceId={}", deviceId);
    }

    @Override
    public List<UserOperationLogDTO> getCurrentUserOperationLogs(Integer limit) {
        log.info("获取当前用户操作日志, limit={}", limit);
        customMetrics.recordBusinessOperation("security", "get_operation_logs");

        Long currentUserId = getCurrentUserId();

        LambdaQueryWrapper<UserOperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserOperationLog::getUserId, currentUserId)
               .orderByDesc(UserOperationLog::getCreateTime)
               .last(limit != null && limit > 0 ? "LIMIT " + limit : "LIMIT 50");

        List<UserOperationLog> logs = operationLogMapper.selectList(wrapper);
        return logs.stream()
                .map(log -> BeanUtil.copyProperties(log, UserOperationLogDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public User2FADTO getCurrent2FAConfig() {
        log.info("获取当前用户2FA配置");
        customMetrics.recordBusinessOperation("security", "get_2fa_config");

        Long currentUserId = getCurrentUserId();

        LambdaQueryWrapper<User2FA> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User2FA::getUserId, currentUserId);
        User2FA user2FA = user2FAMapper.selectOne(wrapper);

        if (user2FA == null) {
            // 返回默认配置
            User2FADTO dto = new User2FADTO();
            dto.setEnabled(0);
            return dto;
        }

        User2FADTO dto = BeanUtil.copyProperties(user2FA, User2FADTO.class);

        // 脱敏处理
        if (user2FA.getVerifyPhone() != null) {
            dto.setVerifyPhone(maskPhone(user2FA.getVerifyPhone()));
        }
        if (user2FA.getVerifyEmail() != null) {
            dto.setVerifyEmail(maskEmail(user2FA.getVerifyEmail()));
        }

        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enable2FA(String type, String verifyCode) {
        log.info("启用2FA: type={}", type);
        customMetrics.recordBusinessOperation("security", "enable_2fa");

        Long currentUserId = getCurrentUserId();

        // TODO: 验证 verifyCode

        // 检查是否已存在配置
        LambdaQueryWrapper<User2FA> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User2FA::getUserId, currentUserId);
        User2FA existing = user2FAMapper.selectOne(wrapper);

        if (existing != null) {
            // 更新现有配置
            existing.setType(type);
            existing.setEnabled(1);
            existing.setUpdateTime(LocalDateTime.now());
            user2FAMapper.updateById(existing);
        } else {
            // 创建新配置
            User2FA user2FA = new User2FA();
            user2FA.setUserId(currentUserId);
            user2FA.setType(type);
            user2FA.setEnabled(1);
            user2FA.setCreateTime(LocalDateTime.now());
            user2FA.setUpdateTime(LocalDateTime.now());
            user2FAMapper.insert(user2FA);
        }

        log.info("2FA启用成功: userId={}, type={}", currentUserId, type);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable2FA(String verifyCode) {
        log.info("禁用2FA");
        customMetrics.recordBusinessOperation("security", "disable_2fa");

        Long currentUserId = getCurrentUserId();

        // TODO: 验证 verifyCode

        LambdaQueryWrapper<User2FA> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User2FA::getUserId, currentUserId);
        User2FA user2FA = user2FAMapper.selectOne(wrapper);

        if (user2FA == null) {
            throw new BusinessException("2FA配置不存在");
        }

        user2FA.setEnabled(0);
        user2FA.setUpdateTime(LocalDateTime.now());
        user2FAMapper.updateById(user2FA);

        log.info("2FA禁用成功: userId={}", currentUserId);
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("未登录或登录已过期");
        }

        String username = authentication.getName();
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        SysUser user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        return user.getId();
    }

    /**
     * 手机号脱敏
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 邮箱脱敏
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        if (parts[0].length() <= 2) {
            return "*@" + parts[1];
        }
        return parts[0].substring(0, 2) + "***@" + parts[1];
    }
}
