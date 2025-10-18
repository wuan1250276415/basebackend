package com.basebackend.demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.common.model.Result;
import com.basebackend.demo.entity.User;
import com.basebackend.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户演示控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserDemoController {

    private final UserService userService;

    /**
     * 获取所有用户
     */
    @GetMapping
    public Result<List<User>> list() {
        log.info("获取所有用户");
        List<User> users = userService.list();
        // 隐藏密码
        users.forEach(user -> user.setPassword("******"));
        return Result.success(users);
    }

    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        log.info("根据ID获取用户: {}", id);
        User user = userService.getById(id);
        if (user != null) {
            user.setPassword("******");
            return Result.success(user);
        }
        return Result.error(404, "用户不存在");
    }

    /**
     * 根据用户名获取用户
     */
    @GetMapping("/username/{username}")
    public Result<User> getByUsername(@PathVariable String username) {
        log.info("根据用户名获取用户: {}", username);
        User user = userService.getByUsername(username);
        if (user != null) {
            user.setPassword("******");
            return Result.success(user);
        }
        return Result.error(404, "用户不存在");
    }

    /**
     * 分页查询用户
     */
    @GetMapping("/page")
    public Result<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        log.info("分页查询用户 - current: {}, size: {}", current, size);

        Page<User> page = userService.page(current, size);
        page.getRecords().forEach(user -> user.setPassword("******"));

        Map<String, Object> data = new HashMap<>();
        data.put("records", page.getRecords());
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("size", page.getSize());
        data.put("pages", page.getPages());

        return Result.success(data);
    }

    /**
     * 创建用户
     */
    @PostMapping
    public Result<String> create(@RequestBody User user) {
        log.info("创建用户: {}", user.getUsername());

        // 检查用户名是否已存在
        User existUser = userService.getByUsername(user.getUsername());
        if (existUser != null) {
            return Result.error(400, "用户名已存在");
        }

        boolean success = userService.create(user);
        if (success) {
            return Result.success("用户创建成功");
        }
        return Result.error(500, "用户创建失败");
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Long id, @RequestBody User user) {
        log.info("更新用户: {}", id);

        User existUser = userService.getById(id);
        if (existUser == null) {
            return Result.error(404, "用户不存在");
        }

        user.setId(id);
        boolean success = userService.update(user);
        if (success) {
            return Result.success("用户更新成功");
        }
        return Result.error(500, "用户更新失败");
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除用户: {}", id);

        User existUser = userService.getById(id);
        if (existUser == null) {
            return Result.error(404, "用户不存在");
        }

        boolean success = userService.delete(id);
        if (success) {
            return Result.success("用户删除成功");
        }
        return Result.error(500, "用户删除失败");
    }

    /**
     * 搜索用户
     */
    @GetMapping("/search")
    public Result<List<User>> search(@RequestParam String keyword) {
        log.info("搜索用户: {}", keyword);
        List<User> users = userService.search(keyword);
        users.forEach(user -> user.setPassword("******"));
        return Result.success(users);
    }

    /**
     * 获取用户统计信息
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        log.info("获取用户统计信息");

        List<User> allUsers = userService.list();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", allUsers.size());
        stats.put("activeCount", allUsers.stream().filter(u -> u.getStatus() == 1).count());
        stats.put("inactiveCount", allUsers.stream().filter(u -> u.getStatus() == 0).count());

        return Result.success(stats);
    }
}
