package com.basebackend.profile.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.common.model.Result;
import com.basebackend.feign.client.UserFeignClient;
import com.basebackend.feign.dto.user.UserBasicDTO;
import com.basebackend.observability.metrics.CustomMetrics;
import com.basebackend.profile.dto.preference.UpdatePreferenceDTO;
import com.basebackend.profile.dto.preference.UserPreferenceDTO;
import com.basebackend.profile.entity.UserPreference;
import com.basebackend.profile.mapper.UserPreferenceMapper;
import com.basebackend.profile.service.PreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户偏好设置服务实现类
 *
 * @author BaseBackend Team
 * @since 2025-11-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreferenceServiceImpl implements PreferenceService {

    private final UserPreferenceMapper preferenceMapper;
    private final UserFeignClient userFeignClient;
    private final CustomMetrics customMetrics;

    @Override
    public UserPreferenceDTO getCurrentUserPreference() {
        log.info("获取当前用户偏好设置");
        customMetrics.recordBusinessOperation("preference", "get");

        Long currentUserId = getCurrentUserId();

        LambdaQueryWrapper<UserPreference> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPreference::getUserId, currentUserId);
        UserPreference preference = preferenceMapper.selectOne(wrapper);

        if (preference == null) {
            // 返回默认偏好设置
            UserPreferenceDTO dto = new UserPreferenceDTO();
            dto.setTheme("light");
            dto.setLanguage("zh-CN");
            dto.setTimezone("Asia/Shanghai");
            dto.setDateFormat("YYYY-MM-DD");
            dto.setTimeFormat("HH:mm:ss");
            dto.setLayout("side");
            dto.setMenuCollapse(0);
            dto.setEmailNotification(1);
            dto.setSmsNotification(0);
            dto.setSystemNotification(1);
            dto.setPageSize(10);
            dto.setAutoSave(1);
            return dto;
        }

        return BeanUtil.copyProperties(preference, UserPreferenceDTO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePreference(UpdatePreferenceDTO dto) {
        log.info("更新用户偏好设置: {}", dto);
        customMetrics.recordBusinessOperation("preference", "update");

        Long currentUserId = getCurrentUserId();

        LambdaQueryWrapper<UserPreference> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPreference::getUserId, currentUserId);
        UserPreference existing = preferenceMapper.selectOne(wrapper);

        if (existing != null) {
            // 更新现有偏好设置
            UserPreference updatePreference = new UserPreference();
            updatePreference.setId(existing.getId());
            BeanUtil.copyProperties(dto, updatePreference);
            updatePreference.setUpdateTime(LocalDateTime.now());

            int result = preferenceMapper.updateById(updatePreference);
            if (result <= 0) {
                throw new BusinessException("更新偏好设置失败");
            }
        } else {
            // 创建新的偏好设置
            UserPreference newPreference = new UserPreference();
            newPreference.setUserId(currentUserId);
            BeanUtil.copyProperties(dto, newPreference);
            newPreference.setCreateTime(LocalDateTime.now());
            newPreference.setUpdateTime(LocalDateTime.now());

            int result = preferenceMapper.insert(newPreference);
            if (result <= 0) {
                throw new BusinessException("创建偏好设置失败");
            }
        }

        log.info("用户偏好设置更新成功: userId={}", currentUserId);
    }

    /**
     * 获取当前登录用户ID
     * 通过 SecurityContext 获取用户名，然后通过 Feign 调用获取用户信息
     *
     * @return 用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("未登录或登录已过期");
        }

        String username = authentication.getName();

        // 通过 Feign 调用获取用户信息
        Result<UserBasicDTO> result = userFeignClient.getByUsername(username);
        if (!isSuccess(result) || result.getData() == null) {
            log.error("通过用户名获取用户信息失败: username={}", username);
            throw new BusinessException("用户不存在");
        }

        return result.getData().getId();
    }

    private boolean isSuccess(Result<?> result) {
        return result != null && result.getCode() != null && result.getCode() == 200;
    }
}
