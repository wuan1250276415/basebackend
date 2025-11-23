package com.basebackend.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.common.model.Result;
import com.basebackend.feign.client.DeptFeignClient;
import com.basebackend.feign.dto.dept.DeptBasicDTO;
import com.basebackend.user.dto.profile.ChangePasswordDTO;
import com.basebackend.user.dto.profile.ProfileDetailDTO;
import com.basebackend.user.dto.profile.UpdateProfileDTO;
import com.basebackend.user.entity.SysUser;
import com.basebackend.user.mapper.SysUserMapper;
import com.basebackend.user.service.ProfileService;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.observability.metrics.CustomMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 个人资料服务实现类
 *
 * @author Claude Code
 * @since 2025-10-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final SysUserMapper userMapper;
    private final ObjectProvider<DeptFeignClient> deptFeignClientProvider;
    private final PasswordEncoder passwordEncoder;
    private final CustomMetrics customMetrics;

    @Override
    public ProfileDetailDTO getCurrentUserProfile() {
        log.info("获取当前用户个人资料");
        customMetrics.recordBusinessOperation("profile", "get");

        Long currentUserId = getCurrentUserId();
        SysUser user = userMapper.selectById(currentUserId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        ProfileDetailDTO dto = BeanUtil.copyProperties(user, ProfileDetailDTO.class);
        dto.setUserId(user.getId());

        // 获取部门名称
        // 安全调用部门服务，失败不影响登录
        if (user.getDeptId() != null) {
            var deptFeignClient = deptFeignClientProvider.getIfAvailable();
            if (deptFeignClient != null) {
                try {
                    Result<DeptBasicDTO> deptResult = deptFeignClient.getById(user.getDeptId());
                    if (deptResult != null && deptResult.getCode() == 200 && deptResult.getData() != null) {
                        dto.setDeptName(deptResult.getData().getDeptName());
                    } else {
                        log.warn("获取部门信息失败或返回空: deptId={}, message={}",
                                user.getDeptId(), deptResult != null ? deptResult.getMessage() : "null");
                        dto.setDeptName(""); // 设置默认值
                    }
                } catch (Exception e) {
                    log.error("调用部门服务异常: deptId={}, error={}", user.getDeptId(), e.getMessage(), e);
                    dto.setDeptName(""); // 设置默认值，不影响登录流程
                }
            }
        }

        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(UpdateProfileDTO dto) {
        log.info("更新用户个人资料: {}", dto);
        customMetrics.recordBusinessOperation("profile", "update");

        Long currentUserId = getCurrentUserId();
        SysUser user = userMapper.selectById(currentUserId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 验证邮箱唯一性
        if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail())) {
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUser::getEmail, dto.getEmail())
                   .ne(SysUser::getId, currentUserId);
            if (userMapper.selectCount(wrapper) > 0) {
                throw new BusinessException("邮箱已被使用");
            }
        }

        // 验证手机号唯一性
        if (dto.getPhone() != null && !dto.getPhone().equals(user.getPhone())) {
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUser::getPhone, dto.getPhone())
                   .ne(SysUser::getId, currentUserId);
            if (userMapper.selectCount(wrapper) > 0) {
                throw new BusinessException("手机号已被使用");
            }
        }

        // 更新用户信息
        SysUser updateUser = new SysUser();
        updateUser.setId(currentUserId);
        BeanUtil.copyProperties(dto, updateUser);

        int result = userMapper.updateById(updateUser);
        if (result <= 0) {
            throw new BusinessException("更新个人资料失败");
        }

        log.info("用户个人资料更新成功: userId={}", currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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

        Long currentUserId = getCurrentUserId();
        SysUser user = userMapper.selectById(currentUserId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 验证旧密码是否正确
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("当前密码不正确");
        }

        // 更新密码
        SysUser updateUser = new SysUser();
        updateUser.setId(currentUserId);
        updateUser.setPassword(passwordEncoder.encode(dto.getNewPassword()));

        int result = userMapper.updateById(updateUser);
        if (result <= 0) {
            throw new BusinessException("修改密码失败");
        }

        log.info("用户密码修改成功: userId={}", currentUserId);
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID
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
}
