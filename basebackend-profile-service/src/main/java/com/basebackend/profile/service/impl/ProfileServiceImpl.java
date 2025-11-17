package com.basebackend.profile.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.common.model.Result;
import com.basebackend.feign.client.DeptFeignClient;
import com.basebackend.feign.client.UserFeignClient;
import com.basebackend.feign.dto.dept.DeptBasicDTO;
import com.basebackend.feign.dto.user.UserBasicDTO;
import com.basebackend.observability.metrics.CustomMetrics;
import com.basebackend.profile.dto.profile.ChangePasswordDTO;
import com.basebackend.profile.dto.profile.ProfileDetailDTO;
import com.basebackend.profile.dto.profile.UpdateProfileDTO;
import com.basebackend.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * 个人资料服务实现类
 *
 * @author BaseBackend Team
 * @since 2025-11-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserFeignClient userFeignClient;
    private final DeptFeignClient deptFeignClient;
    private final CustomMetrics customMetrics;

    @Override
    public ProfileDetailDTO getCurrentUserProfile() {
        log.info("获取当前用户个人资料");
        customMetrics.recordBusinessOperation("profile", "get");

        // 获取当前用户信息
        UserBasicDTO user = getCurrentUser();

        // 转换为 ProfileDetailDTO
        ProfileDetailDTO dto = BeanUtil.copyProperties(user, ProfileDetailDTO.class);
        dto.setUserId(user.getId());

        // 获取部门名称
        if (user.getDeptId() != null) {
            Result<DeptBasicDTO> deptResult = deptFeignClient.getById(user.getDeptId());
            if (isSuccess(deptResult) && deptResult.getData() != null) {
                dto.setDeptName(deptResult.getData().getDeptName());
            }
        }

        return dto;
    }

    @Override
    public void updateProfile(UpdateProfileDTO dto) {
        log.info("更新用户个人资料: {}", dto);
        customMetrics.recordBusinessOperation("profile", "update");

        // 获取当前用户信息
        UserBasicDTO currentUser = getCurrentUser();
        Long currentUserId = currentUser.getId();

        // 验证邮箱唯一性
        if (dto.getEmail() != null && !dto.getEmail().equals(currentUser.getEmail())) {
            Result<Boolean> emailCheckResult = userFeignClient.checkEmailUnique(dto.getEmail(), currentUserId);
            if (!isSuccess(emailCheckResult) || !Boolean.TRUE.equals(emailCheckResult.getData())) {
                throw new BusinessException("邮箱已被使用");
            }
        }

        // 验证手机号唯一性
        if (dto.getPhone() != null && !dto.getPhone().equals(currentUser.getPhone())) {
            Result<Boolean> phoneCheckResult = userFeignClient.checkPhoneUnique(dto.getPhone(), currentUserId);
            if (!isSuccess(phoneCheckResult) || !Boolean.TRUE.equals(phoneCheckResult.getData())) {
                throw new BusinessException("手机号已被使用");
            }
        }

        // 构建更新对象
        UserBasicDTO updateUser = new UserBasicDTO();
        updateUser.setId(currentUserId);
        BeanUtil.copyProperties(dto, updateUser);

        // 调用 Feign 接口更新用户信息
        Result<Void> updateResult = userFeignClient.updateUserProfile(currentUserId, updateUser);
        if (!isSuccess(updateResult)) {
            log.error("更新个人资料失败: userId={}, result={}", currentUserId, updateResult);
            throw new BusinessException("更新个人资料失败");
        }

        log.info("用户个人资料更新成功: userId={}", currentUserId);
    }

    @Override
    public void changePassword(ChangePasswordDTO dto) {
        log.info("修改用户密码");
        customMetrics.recordBusinessOperation("profile", "change_password");

        // 验证两次输入的新密码是否一致
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次输入的新密码不一致");
        }

        // 验证新密码不能与旧密码相同
        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            throw new BusinessException("新密码不能与旧密码相同");
        }

        // 获取当前用户信息
        UserBasicDTO currentUser = getCurrentUser();
        Long currentUserId = currentUser.getId();

        // 调用 Feign 接口修改密码
        Result<Void> changeResult = userFeignClient.changePassword(
                currentUserId,
                dto.getOldPassword(),
                dto.getNewPassword()
        );

        if (!isSuccess(changeResult)) {
            log.error("修改密码失败: userId={}, result={}", currentUserId, changeResult);
            // 从 Result 中获取错误信息
            String errorMsg = changeResult != null && changeResult.getMessage() != null
                    ? changeResult.getMessage()
                    : "修改密码失败";
            throw new BusinessException(errorMsg);
        }

        log.info("用户密码修改成功: userId={}", currentUserId);
    }

    /**
     * 获取当前登录用户信息
     * 通过 SecurityContext 获取用户名，然后通过 Feign 调用获取用户详细信息
     *
     * @return 用户信息
     */
    private UserBasicDTO getCurrentUser() {
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

        return result.getData();
    }

    private boolean isSuccess(Result<?> result) {
        return result != null && result.getCode() != null && result.getCode() == 200;
    }
}
