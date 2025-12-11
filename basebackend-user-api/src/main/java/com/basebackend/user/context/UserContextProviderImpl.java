package com.basebackend.user.context;

import cn.hutool.core.util.StrUtil;
import com.basebackend.common.context.UserContext;
import com.basebackend.common.starter.interceptor.UserContextProvider;
import com.basebackend.jwt.JwtUserDetails;
import com.basebackend.user.entity.SysUser;
import com.basebackend.user.entity.SysUserRole;
import com.basebackend.user.mapper.SysUserMapper;
import com.basebackend.user.mapper.SysUserRoleMapper;
import com.basebackend.user.util.DeptInfoHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * user-api 实现的用户上下文提供器，负责加载完整的用户信息。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserContextProviderImpl implements UserContextProvider {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final DeptInfoHelper deptInfoHelper;
    @Override
    public Optional<UserContext> loadUserContext(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        JwtUserDetails userDetails = (JwtUserDetails) principal;
        SysUser user = userMapper.selectById(userDetails.getUserId());
        if (user == null) {
            return Optional.empty();
        }

        try {
            UserContext.UserContextBuilder builder = UserContext.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .avatar(user.getAvatar())
                    .gender(user.getGender())
                    .deptId(user.getDeptId())
                    .deptName(deptInfoHelper.getDeptName(user.getDeptId()))
                    .userType(user.getUserType())
                    .status(user.getStatus())
                    .ipAddress(getClientIp(request))
                    .requestTime(System.currentTimeMillis());

            // 角色 ID
            List<SysUserRole> userRoles = userRoleMapper.selectByUserId(user.getId());
            List<Long> roleIds = CollectionUtils.isEmpty(userRoles)
                    ? Collections.emptyList()
                    : userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
            builder.roleIds(roleIds);

            // 角色 key
            List<String> roleKeys = userMapper.selectUserRoles(user.getId());
            builder.roles(new HashSet<>(roleKeys));

            // 权限
            List<String> permissions = userMapper.selectUserPermissions(user.getId());
            builder.permissions(new HashSet<>(permissions));

            return Optional.of(builder.build());
        } catch (Exception e) {
            log.error("加载用户上下文失败，principal={}", principal, e);
            return Optional.empty();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            int index = ip.indexOf(',');
            if (index != -1) {
                return ip.substring(0, index);
            }
            return ip;
        }

        ip = request.getHeader("X-Real-IP");
        if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("Proxy-Client-IP");
        if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("WL-Proxy-Client-IP");
        if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("HTTP_CLIENT_IP");
        if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        return request.getRemoteAddr();
    }
}
