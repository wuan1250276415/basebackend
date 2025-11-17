package com.basebackend.oauth2.provider;

import com.basebackend.feign.client.UserFeignClient;
import com.basebackend.oauth2.user.OAuth2UserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * OAuth2.0用户详情服务
 * 负责从用户服务获取用户信息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2UserDetailsService implements UserDetailsService {

    private final UserFeignClient userFeignClient;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("OAuth2.0加载用户详情: {}", username);

        try {
            // 通过Feign调用用户服务
            var userResult = userFeignClient.getByUsername(username);
            if (userResult == null || userResult.getData() == null) {
                log.warn("用户不存在: {}", username);
                throw new UsernameNotFoundException("用户不存在: " + username);
            }

            var user = userResult.getData();

            // 获取用户角色和权限
            List<String> roles = userFeignClient.getUserRoles(user.getId());
            List<String> permissions = userFeignClient.getUserPermissions(user.getId());

            // 构建OAuth2.0用户详情
            OAuth2UserDetails userDetails = new OAuth2UserDetails.OAuth2UserBuilder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .password(user.getPassword())
                    .nickname(user.getNickname())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .avatar(user.getAvatar())
                    .gender(user.getGender())
                    .deptId(user.getDeptId())
                    .deptName(user.getDeptName())
                    .userType(user.getUserType())
                    .status(user.getStatus())
                    .roles(roles)
                    .permissions(permissions)
                    .build();

            log.debug("OAuth2.0用户详情加载成功: {}", username);
            return userDetails;
        } catch (Exception e) {
            log.error("加载用户详情失败: {}", username, e);
            throw new UsernameNotFoundException("用户不存在: " + username, e);
        }
    }

    /**
     * 根据用户ID加载用户详情
     */
    public UserDetails loadUserByUserId(Long userId) throws UsernameNotFoundException {
        log.debug("OAuth2.0根据用户ID加载用户详情: {}", userId);

        try {
            // 通过Feign调用用户服务
            var userResult = userFeignClient.getById(userId);
            if (userResult == null || userResult.getData() == null) {
                log.warn("用户不存在: {}", userId);
                throw new UsernameNotFoundException("用户不存在: " + userId);
            }

            var user = userResult.getData();

            // 获取用户角色和权限
            List<String> roles = userFeignClient.getUserRoles(userId);
            List<String> permissions = userFeignClient.getUserPermissions(userId);

            // 构建OAuth2.0用户详情
            OAuth2UserDetails userDetails = new OAuth2UserDetails.OAuth2UserBuilder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .password(user.getPassword())
                    .nickname(user.getNickname())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .avatar(user.getAvatar())
                    .gender(user.getGender())
                    .deptId(user.getDeptId())
                    .deptName(user.getDeptName())
                    .userType(user.getUserType())
                    .status(user.getStatus())
                    .roles(roles)
                    .permissions(permissions)
                    .build();

            log.debug("OAuth2.0用户详情加载成功: {}", userId);
            return userDetails;
        } catch (Exception e) {
            log.error("加载用户详情失败: {}", userId, e);
            throw new UsernameNotFoundException("用户不存在: " + userId, e);
        }
    }

    /**
     * 密码认证提供者
     * 用于密码模式认证
     */
    @Service
    @RequiredArgsConstructor
    public static class OAuth2UserDetailsAuthenticationProvider {

        private final OAuth2UserDetailsService userDetailsService;
        private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

        /**
         * 认证用户
         */
        public OAuth2UserDetails authenticate(String username, String password) {
            log.debug("OAuth2.0密码模式认证: {}", username);

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 验证密码
            if (!passwordEncoder.matches(password, userDetails.getPassword())) {
                log.warn("密码错误: {}", username);
                throw new org.springframework.security.core.AuthenticationException("密码错误") {
                };
            }

            // 检查用户状态
            if (!userDetails.isEnabled()) {
                log.warn("用户已被禁用: {}", username);
                throw new org.springframework.security.core.AuthenticationException("用户已被禁用") {
                };
            }

            if (!userDetails.isAccountNonLocked()) {
                log.warn("用户账户被锁定: {}", username);
                throw new org.springframework.security.core.AuthenticationException("用户账户被锁定") {
                };
            }

            return (OAuth2UserDetails) userDetails;
        }
    }
}
