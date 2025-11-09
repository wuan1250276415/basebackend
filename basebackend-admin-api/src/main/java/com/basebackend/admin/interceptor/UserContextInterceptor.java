package com.basebackend.admin.interceptor;

import cn.hutool.core.util.StrUtil;
import com.basebackend.admin.context.UserContext;
import com.basebackend.admin.context.UserContextHolder;
import com.basebackend.admin.entity.SysDept;
import com.basebackend.admin.entity.SysUser;
import com.basebackend.admin.mapper.SysDeptMapper;
import com.basebackend.admin.mapper.SysUserMapper;
import com.basebackend.admin.mapper.SysUserRoleMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户上下文拦截器
 * 在每个请求开始时加载完整的用户信息并设置到 UserContextHolder
 * 在请求结束时清空用户上下文
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserContextInterceptor implements HandlerInterceptor {

    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;
    private final SysUserRoleMapper userRoleMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 获取当前认证信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {

            try {
                String username = authentication.getName();

                // 加载完整的用户信息
                SysUser user = userMapper.selectByUsername(username);
                if (user != null) {
                    UserContext userContext = buildUserContext(user, request);
                    UserContextHolder.setContext(userContext);

                    log.debug("用户上下文已设置: userId={}, username={}, deptId={}",
                            user.getId(), user.getUsername(), user.getDeptId());
                }
            } catch (Exception e) {
                log.error("设置用户上下文失败", e);
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, Exception ex) {
        // 清空用户上下文，防止内存泄漏
        UserContextHolder.clear();
    }

    /**
     * 构建用户上下文
     */
    private UserContext buildUserContext(SysUser user, HttpServletRequest request) {
        UserContext.UserContextBuilder builder = UserContext.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .gender(user.getGender())
                .deptId(user.getDeptId())
                .userType(user.getUserType())
                .status(user.getStatus())
                .ipAddress(getClientIp(request))
                .requestTime(System.currentTimeMillis());

        // 加载部门名称
        if (user.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(user.getDeptId());
            if (dept != null) {
                builder.deptName(dept.getDeptName());
            }
        }

        // 加载角色ID列表
        List<Long> roleIds = userRoleMapper.selectList(null).stream()
                .filter(ur -> ur.getUserId().equals(user.getId()))
                .map(ur -> ur.getRoleId())
                .collect(Collectors.toList());
        builder.roleIds(roleIds);

        // 加载角色Key列表
        List<String> roleKeys = userMapper.selectUserRoles(user.getId());
        builder.roles(new HashSet<>(roleKeys));

        // 加载权限列表
        List<String> permissions = userMapper.selectUserPermissions(user.getId());
        builder.permissions(new HashSet<>(permissions));

        return builder.build();
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            // 多次反向代理后会有多个IP值，第一个为真实IP
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
