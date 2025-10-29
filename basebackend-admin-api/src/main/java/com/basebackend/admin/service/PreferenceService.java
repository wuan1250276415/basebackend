package com.basebackend.admin.service;

import com.basebackend.admin.dto.preference.UpdatePreferenceDTO;
import com.basebackend.admin.dto.preference.UserPreferenceDTO;

/**
 * 用户偏好设置服务接口
 *
 * @author Claude Code
 * @since 2025-10-30
 */
public interface PreferenceService {

    /**
     * 获取当前用户的偏好设置
     *
     * @return 偏好设置
     */
    UserPreferenceDTO getCurrentUserPreference();

    /**
     * 更新当前用户的偏好设置
     *
     * @param dto 更新偏好设置请求
     */
    void updatePreference(UpdatePreferenceDTO dto);
}
