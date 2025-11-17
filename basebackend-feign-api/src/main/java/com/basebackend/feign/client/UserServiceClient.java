package com.basebackend.feign.client;

import com.basebackend.common.model.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 用户服务Feign客户端
 */
@FeignClient(name = "basebackend-user-api", path = "/api/user")
public interface UserServiceClient {

    /**
     * 根据用户名获取用户信息
     */
    @GetMapping("/users/by-username")
    Result<Object> getUserByUsername(@RequestParam("username") String username);

    /**
     * 根据用户ID获取用户信息
     */
    @GetMapping("/users/{id}")
    Result<Object> getUserById(@PathVariable("id") Long id);

    /**
     * 根据用户ID获取用户角色
     */
    @GetMapping("/users/{id}/roles")
    Result<Object> getUserRoles(@PathVariable("id") Long id);

    /**
     * 根据用户ID获取用户权限
     */
    @GetMapping("/users/{id}/permissions")
    Result<Object> getUserPermissions(@PathVariable("id") Long id);
}
