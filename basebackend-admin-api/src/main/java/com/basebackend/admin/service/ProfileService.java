package com.basebackend.admin.service;

import com.basebackend.admin.dto.profile.ChangePasswordDTO;
import com.basebackend.admin.dto.profile.ProfileDetailDTO;
import com.basebackend.admin.dto.profile.UpdateProfileDTO;

/**
 * 个人资料服务接口
 *
 * @author Claude Code
 * @since 2025-10-29
 */
public interface ProfileService {

    /**
     * 获取当前用户的详细资料
     *
     * @return 个人资料详情
     */
    ProfileDetailDTO getCurrentUserProfile();

    /**
     * 更新当前用户的个人资料
     *
     * @param dto 更新资料请求
     */
    void updateProfile(UpdateProfileDTO dto);

    /**
     * 修改当前用户密码
     *
     * @param dto 修改密码请求
     */
    void changePassword(ChangePasswordDTO dto);
}
