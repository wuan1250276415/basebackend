package com.basebackend.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.demo.entity.User;
import com.basebackend.demo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    /**
     * 根据ID查询用户
     */
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    /**
     * 根据用户名查询用户
     */
    public User getByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    /**
     * 查询所有用户
     */
    public List<User> list() {
        return userMapper.selectList(null);
    }

    /**
     * 分页查询用户
     */
    public Page<User> page(int current, int size) {
        Page<User> page = new Page<>(current, size);
        return userMapper.selectPage(page, null);
    }

    /**
     * 创建用户
     */
    public boolean create(User user) {
        return userMapper.insert(user) > 0;
    }

    /**
     * 更新用户
     */
    public boolean update(User user) {
        return userMapper.updateById(user) > 0;
    }

    /**
     * 删除用户（逻辑删除）
     */
    public boolean delete(Long id) {
        return userMapper.deleteById(id) > 0;
    }

    /**
     * 搜索用户
     */
    public List<User> search(String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(User::getUsername, keyword)
                .or()
                .like(User::getNickname, keyword)
                .or()
                .like(User::getEmail, keyword);
        return userMapper.selectList(wrapper);
    }
}
