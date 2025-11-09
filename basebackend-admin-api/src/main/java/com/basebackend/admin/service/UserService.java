package com.basebackend.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.admin.dto.UserCreateDTO;
import com.basebackend.admin.dto.UserDTO;
import com.basebackend.admin.dto.UserQueryDTO;
import com.basebackend.admin.entity.SysUser;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 分页查询用户列表
     */
    Page<UserDTO> page(UserQueryDTO queryDTO, int current, int size);

    /**
     * 根据ID查询用户
     */
    UserDTO getById(Long id);

    /**
     * 根据用户名查询用户
     */
    UserDTO getByUsername(String username);

    /**
     * 创建用户
     */
    void create(UserCreateDTO userCreateDTO);

    /**
     * 更新用户
     */
    void update(UserDTO userDTO);

    /**
     * 删除用户
     */
    void delete(Long id);

    /**
     * 批量删除用户
     */
    void deleteBatch(List<Long> ids);

    /**
     * 重置密码
     */
    void resetPassword(Long id, String newPassword);

    /**
     * 分配角色
     */
    void assignRoles(Long userId, List<Long> roleIds);

    /**
     * 启用/禁用用户
     */
    void changeStatus(Long id, Integer status);

    /**
     * 导出用户
     */
    List<UserDTO> export(UserQueryDTO queryDTO);

    /**
     * 获取用户角色列表
     */
    List<Long> getUserRoles(Long userId);

    /**
     * 检查用户名是否唯一
     */
    boolean checkUsernameUnique(String username, Long userId);

    /**
     * 检查邮箱是否唯一
     */
    boolean checkEmailUnique(String email, Long userId);

    /**
     * 检查手机号是否唯一
     */
    boolean checkPhoneUnique(String phone, Long userId);

    /**
     * 根据手机号查询用户
     */
    UserDTO getByPhone(String phone);

    /**
     * 根据邮箱查询用户
     */
    UserDTO getByEmail(String email);

    /**
     * 批量查询用户
     */
    List<UserDTO> getBatchByIds(List<Long> ids);

    /**
     * 根据部门ID查询用户列表
     */
    List<UserDTO> getByDeptId(Long deptId);
}
