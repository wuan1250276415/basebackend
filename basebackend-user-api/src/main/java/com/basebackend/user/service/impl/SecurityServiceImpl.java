package com.basebackend.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.codec.Base32;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.common.model.Result;
import com.basebackend.service.client.OperationLogServiceClient;
import com.basebackend.observability.metrics.CustomMetrics;
import com.basebackend.user.dto.security.User2FADTO;
import com.basebackend.user.dto.security.UserDeviceDTO;
import com.basebackend.user.dto.security.UserOperationLogDTO;
import com.basebackend.user.entity.SysUser;
import com.basebackend.user.entity.User2FA;
import com.basebackend.user.entity.UserDevice;
import com.basebackend.user.mapper.SysUserMapper;
import com.basebackend.user.mapper.User2FAMapper;
import com.basebackend.user.mapper.UserDeviceMapper;
import com.basebackend.user.service.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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

    private static final Set<String> SUPPORTED_2FA_TYPES = Set.of("totp", "sms", "email");
    private static final int TOTP_DIGITS = 6;
    private static final int TOTP_TIME_STEP_SECONDS = 30;
    private static final int TOTP_ALLOWED_DRIFT_STEPS = 1;

    private final UserDeviceMapper deviceMapper;
    private final OperationLogServiceClient operationLogServiceClient;
    private final User2FAMapper user2FAMapper;
    private final SysUserMapper userMapper;
    private final CustomMetrics customMetrics;

    @Override
    public List<UserDeviceDTO> getCurrentUserDevices() {
        log.info("获取当前用户设备列表");
        customMetrics.recordBusinessOperation("security", "get_devices");

        Long currentUserId = UserContextHolder.getUserId();
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

        Long currentUserId = UserContextHolder.getUserId();

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

        Long currentUserId = UserContextHolder.getUserId();

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

        Long currentUserId = UserContextHolder.getUserId();

        try {
            // 通过Feign调用system-api获取操作日志
            Result<List<com.basebackend.api.model.log.UserOperationLogDTO>> result = operationLogServiceClient
                    .getUserOperationLogs(currentUserId, limit);

            if (result == null || !result.isSuccess() || result.getData() == null) {
                log.warn("获取操作日志失败或无数据");
                return Collections.emptyList();
            }

            // 转换DTO类型
            return result.getData().stream()
                    .map(feignDto -> BeanUtil.copyProperties(feignDto, UserOperationLogDTO.class))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("通过Feign获取操作日志失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public User2FADTO getCurrent2FAConfig() {
        log.info("获取当前用户2FA配置");
        customMetrics.recordBusinessOperation("security", "get_2fa_config");

        Long currentUserId = UserContextHolder.getUserId();

        LambdaQueryWrapper<User2FA> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User2FA::getUserId, currentUserId);
        User2FA user2FA = user2FAMapper.selectOne(wrapper);

        if (user2FA == null) {
            // 返回默认配置
            return new User2FADTO(null, null, 0, null, null, null, null);
        }

        // 构建DTO，同时进行脱敏处理
        return new User2FADTO(
                user2FA.getId(),
                user2FA.getType(),
                user2FA.getEnabled(),
                user2FA.getVerifyPhone() != null ? maskPhone(user2FA.getVerifyPhone()) : null,
                user2FA.getVerifyEmail() != null ? maskEmail(user2FA.getVerifyEmail()) : null,
                user2FA.getLastVerifyTime(),
                user2FA.getCreateTime()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enable2FA(String type, String verifyCode) {
        log.info("启用2FA: type={}", type);
        customMetrics.recordBusinessOperation("security", "enable_2fa");

        Long currentUserId = UserContextHolder.getUserId();
        String normalizedType = normalize2FAType(type);

        // 检查是否已存在配置
        LambdaQueryWrapper<User2FA> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User2FA::getUserId, currentUserId);
        User2FA existing = user2FAMapper.selectOne(wrapper);
        verify2FACode(existing, normalizedType, verifyCode, "启用");

        if (existing != null) {
            // 更新现有配置
            existing.setType(normalizedType);
            existing.setEnabled(1);
            existing.setLastVerifyTime(LocalDateTime.now());
            existing.setUpdateTime(LocalDateTime.now());
            user2FAMapper.updateById(existing);
        } else {
            throw BusinessException.forbidden("当前用户未完成2FA初始化，无法启用");
        }

        log.info("2FA启用成功: userId={}, type={}", currentUserId, normalizedType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable2FA(String verifyCode) {
        log.info("禁用2FA");
        customMetrics.recordBusinessOperation("security", "disable_2fa");

        Long currentUserId = UserContextHolder.getUserId();

        LambdaQueryWrapper<User2FA> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User2FA::getUserId, currentUserId);
        User2FA user2FA = user2FAMapper.selectOne(wrapper);

        if (user2FA == null) {
            throw new BusinessException("2FA配置不存在");
        }

        verify2FACode(user2FA, user2FA.getType(), verifyCode, "禁用");
        user2FA.setEnabled(0);
        user2FA.setLastVerifyTime(LocalDateTime.now());
        user2FA.setUpdateTime(LocalDateTime.now());
        user2FAMapper.updateById(user2FA);

        log.info("2FA禁用成功: userId={}", currentUserId);
    }

    private String normalize2FAType(String type) {
        if (!StringUtils.hasText(type)) {
            throw BusinessException.paramError("2FA类型不能为空");
        }

        String normalizedType = type.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_2FA_TYPES.contains(normalizedType)) {
            throw BusinessException.paramError("不支持的2FA类型: " + type);
        }
        return normalizedType;
    }

    private void verify2FACode(User2FA user2FA, String type, String verifyCode, String operation) {
        String normalizedType = normalize2FAType(type);
        if (!StringUtils.hasText(verifyCode) || !verifyCode.trim().matches("\\d{6}")) {
            throw BusinessException.paramError("验证码格式错误");
        }

        if ("totp".equals(normalizedType)) {
            verifyTotpCode(user2FA, verifyCode.trim(), operation);
            return;
        }

        throw BusinessException.forbidden("当前2FA类型尚未接入可信验证码校验，已禁止" + operation + "操作");
    }

    private void verifyTotpCode(User2FA user2FA, String verifyCode, String operation) {
        if (user2FA == null || !StringUtils.hasText(user2FA.getSecretKey())) {
            throw BusinessException.forbidden("当前用户未完成TOTP密钥初始化，无法" + operation + "2FA");
        }
        if (!isValidTotpCode(user2FA.getSecretKey(), verifyCode)) {
            throw BusinessException.forbidden("验证码错误或已过期");
        }
    }

    private boolean isValidTotpCode(String secretKey, String verifyCode) {
        byte[] decodedSecret = decodeTotpSecret(secretKey);
        long currentCounter = Instant.now().getEpochSecond() / TOTP_TIME_STEP_SECONDS;

        for (long offset = -TOTP_ALLOWED_DRIFT_STEPS; offset <= TOTP_ALLOWED_DRIFT_STEPS; offset++) {
            if (generateTotp(decodedSecret, currentCounter + offset).equals(verifyCode)) {
                return true;
            }
        }
        return false;
    }

    private byte[] decodeTotpSecret(String secretKey) {
        try {
            return Base32.decode(secretKey.replace(" ", "").replace("-", "").toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            log.warn("TOTP密钥解析失败: {}", e.getMessage());
            throw BusinessException.forbidden("当前用户的TOTP密钥无效，无法校验验证码");
        }
    }

    private String generateTotp(byte[] secretKey, long counter) {
        try {
            byte[] counterBytes = ByteBuffer.allocate(Long.BYTES).putLong(counter).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secretKey, "HmacSHA1"));
            byte[] hash = mac.doFinal(counterBytes);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binary % (int) Math.pow(10, TOTP_DIGITS);
            return String.format("%0" + TOTP_DIGITS + "d", otp);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("TOTP验证码生成失败", e);
            throw BusinessException.forbidden("验证码校验失败");
        }
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
