package com.basebackend.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.user.dto.UserCreateDTO;
import com.basebackend.user.dto.UserDTO;
import com.basebackend.user.dto.UserQueryDTO;
import com.basebackend.user.entity.SysUser;
import com.basebackend.user.mapper.SysUserMapper;
import com.basebackend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    // TODO: 后续添加 AuthServiceClient (Feign) 用于角色管理
    // TODO: 后续添加 DeptServiceClient (Feign) 用于部门信息查询

    @Override
    public Page<UserDTO> page(UserQueryDTO queryDTO, int current, int size) {
        log.info("分页查询用户列表: current={}, size={}", current, size);
        Page<SysUser> page = new Page<>(current, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();

        // 构建查询条件
        if (StrUtil.isNotBlank(queryDTO.getUsername())) {
            wrapper.like(SysUser::getUsername, queryDTO.getUsername());
        }
        if (StrUtil.isNotBlank(queryDTO.getNickname())) {
            wrapper.like(SysUser::getNickname, queryDTO.getNickname());
        }
        if (StrUtil.isNotBlank(queryDTO.getEmail())) {
            wrapper.like(SysUser::getEmail, queryDTO.getEmail());
        }
        if (StrUtil.isNotBlank(queryDTO.getPhone())) {
            wrapper.like(SysUser::getPhone, queryDTO.getPhone());
        }
        if (queryDTO.getDeptId() != null) {
            wrapper.eq(SysUser::getDeptId, queryDTO.getDeptId());
        }
        if (queryDTO.getUserType() != null) {
            wrapper.eq(SysUser::getUserType, queryDTO.getUserType());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, queryDTO.getStatus());
        }
        if (StrUtil.isNotBlank(queryDTO.getBeginTime())) {
            wrapper.ge(SysUser::getCreateTime, queryDTO.getBeginTime());
        }
        if (StrUtil.isNotBlank(queryDTO.getEndTime())) {
            wrapper.le(SysUser::getCreateTime, queryDTO.getEndTime());
        }

        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> userPage = userMapper.selectPage(page, wrapper);

        // 转换为DTO
        List<UserDTO> userDTOs = userPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        Page<UserDTO> result = new Page<>(current, size);
        result.setRecords(userDTOs);
        result.setTotal(userPage.getTotal());
        result.setPages(userPage.getPages());

        return result;
    }

    @Override
    public UserDTO getById(Long id) {
        log.info("根据ID查询用户: {}", id);
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return convertToDTO(user);
    }

    @Override
    public UserDTO getByUsername(String username) {
        log.info("根据用户名查询用户: {}", username);
        SysUser user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return convertToDTO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(UserCreateDTO userCreateDTO) {
        log.info("创建用户: {}", userCreateDTO.getUsername());

        // 检查用户名是否唯一
        if (!checkUsernameUnique(userCreateDTO.getUsername(), null)) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱是否唯一
        if (StrUtil.isNotBlank(userCreateDTO.getEmail()) && !checkEmailUnique(userCreateDTO.getEmail(), null)) {
            throw new RuntimeException("邮箱已存在");
        }

        // 检查手机号是否唯一
        if (StrUtil.isNotBlank(userCreateDTO.getPhone()) && !checkPhoneUnique(userCreateDTO.getPhone(), null)) {
            throw new RuntimeException("手机号已存在");
        }

        // 创建用户
        SysUser user = new SysUser();
        BeanUtil.copyProperties(userCreateDTO, user);
        user.setPassword(passwordEncoder.encode(userCreateDTO.getPassword()));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setCreateBy(1L); // 临时硬编码，后续从上下文获取
        user.setUpdateBy(1L); // 临时硬编码，后续从上下文获取

        userMapper.insert(user);

        // TODO: 分配角色（通过 Feign 调用 auth-service）
        if (userCreateDTO.getRoleIds() != null && !userCreateDTO.getRoleIds().isEmpty()) {
            log.info("用户角色分配：userId={}, roleIds={} (TODO: 实现Feign调用)", user.getId(), userCreateDTO.getRoleIds());
            // assignRoles(user.getId(), userCreateDTO.getRoleIds());
        }

        log.info("用户创建成功: {}", user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(UserDTO userDTO) {
        log.info("更新用户: {}", userDTO.getId());

        SysUser user = userMapper.selectById(userDTO.getId());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 检查用户名是否唯一
        if (!checkUsernameUnique(userDTO.getUsername(), userDTO.getId())) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱是否唯一
        if (StrUtil.isNotBlank(userDTO.getEmail()) && !checkEmailUnique(userDTO.getEmail(), userDTO.getId())) {
            throw new RuntimeException("邮箱已存在");
        }

        // 检查手机号是否唯一
        if (StrUtil.isNotBlank(userDTO.getPhone()) && !checkPhoneUnique(userDTO.getPhone(), userDTO.getId())) {
            throw new RuntimeException("手机号已存在");
        }

        // 更新用户信息
        BeanUtil.copyProperties(userDTO, user);
        user.setUpdateTime(LocalDateTime.now());
        user.setUpdateBy(1L); // 临时硬编码，后续从上下文获取

        userMapper.updateById(user);

        // TODO: 更新角色（通过 Feign 调用 auth-service）
        if (userDTO.getRoleIds() != null) {
            log.info("更新用户角色：userId={}, roleIds={} (TODO: 实现Feign调用)", user.getId(), userDTO.getRoleIds());
            // assignRoles(user.getId(), userDTO.getRoleIds());
        }

        log.info("用户更新成功: {}", user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        log.info("删除用户: {}", id);

        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 逻辑删除
        userMapper.deleteById(id);

        // TODO: 删除用户角色关联（通过 Feign 调用 auth-service）
        log.info("删除用户角色关联：userId={} (TODO: 实现Feign调用)", id);

        log.info("用户删除成功: {}", user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<Long> ids) {
        log.info("批量删除用户: {}", ids);

        for (Long id : ids) {
            delete(id);
        }

        log.info("批量删除用户成功");
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        log.info("重置用户密码: {}", id);

        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        user.setUpdateBy(1L); // 临时硬编码，后续从上下文获取

        userMapper.updateById(user);

        log.info("用户密码重置成功: {}", user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        log.info("分配用户角色: userId={}, roleIds={}", userId, roleIds);

        // TODO: 通过 Feign 调用 auth-service 进行角色分配
        // authServiceClient.assignUserRoles(userId, roleIds);

        log.info("用户角色分配成功 (TODO: 实现Feign调用)");
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        log.info("修改用户状态: id={}, status={}", id, status);

        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        user.setUpdateBy(1L); // 临时硬编码，后续从上下文获取

        userMapper.updateById(user);

        log.info("用户状态修改成功: {}", user.getUsername());
    }

    @Override
    public List<UserDTO> export(UserQueryDTO queryDTO) {
        log.info("导出用户数据");

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();

        // 构建查询条件（同分页查询）
        if (StrUtil.isNotBlank(queryDTO.getUsername())) {
            wrapper.like(SysUser::getUsername, queryDTO.getUsername());
        }
        if (StrUtil.isNotBlank(queryDTO.getNickname())) {
            wrapper.like(SysUser::getNickname, queryDTO.getNickname());
        }
        if (StrUtil.isNotBlank(queryDTO.getEmail())) {
            wrapper.like(SysUser::getEmail, queryDTO.getEmail());
        }
        if (StrUtil.isNotBlank(queryDTO.getPhone())) {
            wrapper.like(SysUser::getPhone, queryDTO.getPhone());
        }
        if (queryDTO.getDeptId() != null) {
            wrapper.eq(SysUser::getDeptId, queryDTO.getDeptId());
        }
        if (queryDTO.getUserType() != null) {
            wrapper.eq(SysUser::getUserType, queryDTO.getUserType());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, queryDTO.getStatus());
        }

        wrapper.orderByDesc(SysUser::getCreateTime);
        List<SysUser> users = userMapper.selectList(wrapper);

        return users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getUserRoles(Long userId) {
        // TODO: 通过 Feign 调用 auth-service 获取用户角色
        // return authServiceClient.getUserRoles(userId);
        log.info("获取用户角色：userId={} (TODO: 实现Feign调用)", userId);
        return new ArrayList<>();
    }

    @Override
    public boolean checkUsernameUnique(String username, Long userId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        if (userId != null) {
            wrapper.ne(SysUser::getId, userId);
        }
        return userMapper.selectCount(wrapper) == 0;
    }

    @Override
    public boolean checkEmailUnique(String email, Long userId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getEmail, email);
        if (userId != null) {
            wrapper.ne(SysUser::getId, userId);
        }
        return userMapper.selectCount(wrapper) == 0;
    }

    @Override
    public boolean checkPhoneUnique(String phone, Long userId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getPhone, phone);
        if (userId != null) {
            wrapper.ne(SysUser::getId, userId);
        }
        return userMapper.selectCount(wrapper) == 0;
    }

    @Override
    public UserDTO getByPhone(String phone) {
        log.info("根据手机号查询用户: {}", phone);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getPhone, phone);
        SysUser user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return convertToDTO(user);
    }

    @Override
    public UserDTO getByEmail(String email) {
        log.info("根据邮箱查询用户: {}", email);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getEmail, email);
        SysUser user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return convertToDTO(user);
    }

    @Override
    public List<UserDTO> getBatchByIds(List<Long> ids) {
        log.info("批量查询用户: {}", ids);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<SysUser> users = userMapper.selectBatchIds(ids);
        return users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDTO> getByDeptId(Long deptId) {
        log.info("根据部门ID查询用户: {}", deptId);
        List<SysUser> users = userMapper.selectUsersByDeptId(deptId);
        return users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 转换为DTO
     */
    private UserDTO convertToDTO(SysUser user) {
        UserDTO dto = new UserDTO();
        BeanUtil.copyProperties(user, dto);

        // TODO: 设置部门名称（通过 Feign 调用 dept-service）
        if (user.getDeptId() != null) {
            // dto.setDeptName(deptServiceClient.getDeptName(user.getDeptId()));
            log.debug("获取部门名称：deptId={} (TODO: 实现Feign调用)", user.getDeptId());
        }

        // TODO: 设置角色信息（通过 Feign 调用 auth-service）
        // List<Long> roleIds = authServiceClient.getUserRoles(user.getId());
        // dto.setRoleIds(roleIds);
        // if (!roleIds.isEmpty()) {
        //     List<String> roleNames = authServiceClient.getRoleNames(roleIds);
        //     dto.setRoleNames(roleNames);
        // }

        return dto;
    }
}
