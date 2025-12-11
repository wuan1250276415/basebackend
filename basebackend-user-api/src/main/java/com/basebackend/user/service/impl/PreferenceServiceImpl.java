package com.basebackend.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.user.dto.preference.UpdatePreferenceDTO;
import com.basebackend.user.dto.preference.UserPreferenceDTO;
import com.basebackend.user.entity.UserPreference;
import com.basebackend.user.mapper.SysUserMapper;
import com.basebackend.user.mapper.UserPreferenceMapper;
import com.basebackend.user.service.PreferenceService;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.observability.metrics.CustomMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户偏好设置服务实现类
 *
 * @author Claude Code
 * @since 2025-10-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreferenceServiceImpl implements PreferenceService {

    private final UserPreferenceMapper preferenceMapper;
    private final CustomMetrics customMetrics;

    @Override
    public UserPreferenceDTO getCurrentUserPreference() {
        log.info("获取当前用户偏好设置");
        customMetrics.recordBusinessOperation("preference", "get");

        Long currentUserId = UserContextHolder.getUserId();

        LambdaQueryWrapper<UserPreference> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPreference::getUserId, currentUserId);
        UserPreference preference = preferenceMapper.selectOne(wrapper);

        if (preference == null) {
            // 返回默认偏好设置
            UserPreferenceDTO dto = new UserPreferenceDTO();
            dto.setTheme("light");
            dto.setLanguage("zh-CN");
            dto.setEmailNotification(1);
            dto.setSmsNotification(0);
            dto.setSystemNotification(1);
            return dto;
        }

        return BeanUtil.copyProperties(preference, UserPreferenceDTO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePreference(UpdatePreferenceDTO dto) {
        log.info("更新用户偏好设置: {}", dto);
        customMetrics.recordBusinessOperation("preference", "update");

        Long currentUserId = UserContextHolder.getUserId();

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

}
