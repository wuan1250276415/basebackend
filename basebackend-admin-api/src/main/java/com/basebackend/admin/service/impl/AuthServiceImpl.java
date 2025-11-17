package com.basebackend.admin.service.impl;

import cn.hutool.core.util.StrUtil;
import com.basebackend.admin.dto.LoginLogDTO;
import com.basebackend.admin.dto.LoginRequest;
import com.basebackend.admin.dto.LoginResponse;
import com.basebackend.admin.dto.PasswordChangeDTO;
import com.basebackend.admin.entity.SysUser;
import com.basebackend.admin.mapper.SysDeptMapper;
import com.basebackend.admin.mapper.SysUserMapper;
import com.basebackend.admin.service.AuthService;
import com.basebackend.admin.service.LogService;
import com.basebackend.cache.service.RedisService;
import com.basebackend.web.util.IpUtil;
import com.basebackend.web.util.UserAgentUtil;
import com.basebackend.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisService redisService;
    private final LogService logService;

    private static final String LOGIN_TOKEN_KEY = "login_tokens:";
    private static final String USER_PERMISSIONS_KEY = "user_permissions:";
    private static final String USER_ROLES_KEY = "user_roles:";
    private static final String ONLINE_USER_KEY = "online_users:";

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        log.info("用户登录: {}", loginRequest.getUsername());

        // 获取请求信息
        HttpServletRequest request = getHttpServletRequest();
        String ipAddress = IpUtil.getIpAddress(request);
        String location = IpUtil.getLocationByIp(ipAddress);
        String browser = UserAgentUtil.getBrowser(request);
        String os = UserAgentUtil.getOperatingSystem(request);

        LoginLogDTO loginLog = new LoginLogDTO();
        loginLog.setUsername(loginRequest.getUsername());
        loginLog.setIpAddress(ipAddress);
        loginLog.setLoginLocation(location);
        loginLog.setBrowser(browser);
        loginLog.setOs(os);
        loginLog.setLoginTime(LocalDateTime.now());

        try {
            // 查询用户
            SysUser user = userMapper.selectByUsername(loginRequest.getUsername());
            if (user == null) {
                loginLog.setStatus(0);
                loginLog.setMsg("用户不存在");
                recordLoginLog(loginLog);
                throw new RuntimeException("用户不存在");
            }

            // 验证密码
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                loginLog.setUserId(user.getId());
                loginLog.setStatus(0);
                loginLog.setMsg("密码错误");
                recordLoginLog(loginLog);
                throw new RuntimeException("密码错误");
            }

            // 检查用户状态
            if (user.getStatus() == 0) {
                loginLog.setUserId(user.getId());
                loginLog.setStatus(0);
                loginLog.setMsg("用户已被禁用");
                recordLoginLog(loginLog);
                throw new RuntimeException("用户已被禁用");
            }

            // 设置成功日志
            loginLog.setUserId(user.getId());
            loginLog.setStatus(1);
            loginLog.setMsg("登录成功");

            // 更新登录信息
            user.setLoginIp(ipAddress);
            user.setLoginTime(LocalDateTime.now());
            userMapper.updateById(user);

            // 生成Token
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getId());
            claims.put("username", user.getUsername());
            claims.put("userType", user.getUserType());
            claims.put("deptId", user.getDeptId());

            String token = jwtUtil.generateToken(user.getUsername(), claims);

            // 缓存用户信息
            String userKey = LOGIN_TOKEN_KEY + user.getUsername();
            redisService.set(userKey, token, 24 * 60 * 60); // 24小时

            // 保存在线用户信息
            saveOnlineUser(user, token, ipAddress, location, browser, os);

            // 获取用户权限和角色
            List<String> permissions = userMapper.selectUserPermissions(user.getId());
            List<String> roles = userMapper.selectUserRoles(user.getId());

            // 缓存权限和角色
            redisService.set(USER_PERMISSIONS_KEY + user.getId(), permissions, 24 * 60 * 60);
            redisService.set(USER_ROLES_KEY + user.getId(), roles, 24 * 60 * 60);

            // 构建用户信息
            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
            userInfo.setUserId(user.getId());
            userInfo.setUsername(user.getUsername());
            userInfo.setNickname(user.getNickname());
            userInfo.setEmail(user.getEmail());
            userInfo.setPhone(user.getPhone());
            userInfo.setAvatar(user.getAvatar());
            userInfo.setGender(user.getGender());
            userInfo.setDeptId(user.getDeptId());
            userInfo.setUserType(user.getUserType());
            userInfo.setStatus(user.getStatus());

            // 设置部门名称
            if (user.getDeptId() != null) {
                var dept = deptMapper.selectById(user.getDeptId());
                if (dept != null) {
                    userInfo.setDeptName(dept.getDeptName());
                }
            }

            // 构建响应
            LoginResponse response = new LoginResponse();
            response.setAccessToken(token);
            response.setExpiresIn(24 * 60 * 60L); // 24小时
            response.setUserInfo(userInfo);
            response.setPermissions(permissions);
            response.setRoles(roles);

            // 记录登录日志
            recordLoginLog(loginLog);

            log.info("用户登录成功: {}", user.getUsername());
            return response;
        } catch (Exception e) {
            loginLog.setStatus(0);
            loginLog.setMsg("登录失败: " + e.getMessage());
            recordLoginLog(loginLog);
            throw e;
        }
    }

    @Override
    public void logout() {
        // 从请求上下文获取当前用户ID
        // 这里简化处理，实际应该从SecurityContext或ThreadLocal获取
        log.info("用户登出");
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        if (StrUtil.isBlank(refreshToken)) {
            throw new RuntimeException("刷新Token不能为空");
        }

        // 验证Token
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new RuntimeException("刷新Token无效");
        }

        // 获取用户信息
        String username = jwtUtil.getSubjectFromToken(refreshToken);
        SysUser user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 生成新Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("userType", user.getUserType());
        claims.put("deptId", user.getDeptId());

        String newToken = jwtUtil.generateToken(user.getUsername(), claims);

        // 更新缓存
        String userKey = LOGIN_TOKEN_KEY + user.getId();
        redisService.set(userKey, newToken, 24 * 60 * 60);

        // 获取用户权限和角色
        List<String> permissions = userMapper.selectUserPermissions(user.getId());
        List<String> roles = userMapper.selectUserRoles(user.getId());

        // 构建用户信息
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setUserId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setNickname(user.getNickname());
        userInfo.setEmail(user.getEmail());
        userInfo.setPhone(user.getPhone());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setGender(user.getGender());
        userInfo.setDeptId(user.getDeptId());
        userInfo.setUserType(user.getUserType());
        userInfo.setStatus(user.getStatus());

        // 设置部门名称
        if (user.getDeptId() != null) {
            var dept = deptMapper.selectById(user.getDeptId());
            if (dept != null) {
                userInfo.setDeptName(dept.getDeptName());
            }
        }

        // 构建响应
        LoginResponse response = new LoginResponse();
        response.setAccessToken(newToken);
        response.setExpiresIn(24 * 60 * 60L);
        response.setUserInfo(userInfo);
        response.setPermissions(permissions);
        response.setRoles(roles);

        return response;
    }

    @Override
    public LoginResponse.UserInfo getCurrentUserInfo() {
        // 从请求上下文获取当前用户ID
        // 这里简化处理，实际应该从SecurityContext或ThreadLocal获取
        throw new UnsupportedOperationException("需要实现获取当前用户信息");
    }

    @Override
    public void changePassword(PasswordChangeDTO passwordChangeDTO) {
        // 验证新密码和确认密码是否一致
        if (!passwordChangeDTO.getNewPassword().equals(passwordChangeDTO.getConfirmPassword())) {
            throw new RuntimeException("新密码和确认密码不一致");
        }

        // 从请求上下文获取当前用户ID
        // 这里简化处理，实际应该从SecurityContext或ThreadLocal获取
        Long userId = 1L; // 临时硬编码

        // 查询用户
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(passwordChangeDTO.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(passwordChangeDTO.getNewPassword()));
        userMapper.updateById(user);

        log.info("用户修改密码成功: {}", user.getUsername());
    }

    @Override
    public void resetPassword(Long userId, String newPassword) {
        // 查询用户
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);

        log.info("重置用户密码成功: {}", user.getUsername());
    }

    /**
     * 获取HttpServletRequest
     */
    private HttpServletRequest getHttpServletRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 记录登录日志（异步）
     */
    private void recordLoginLog(LoginLogDTO loginLog) {
        try {
            logService.recordLoginLog(loginLog);
        } catch (Exception e) {
            log.error("记录登录日志失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 保存在线用户信息到Redis
     */
    private void saveOnlineUser(SysUser user, String token, String ipAddress, String location, String browser, String os) {
        try {
            Map<String, Object> onlineUser = new HashMap<>();
            onlineUser.put("userId", user.getId());
            onlineUser.put("username", user.getUsername());
            onlineUser.put("nickname", user.getNickname());
            onlineUser.put("deptId", user.getDeptId());
            if (user.getDeptId() != null) {
                var dept = deptMapper.selectById(user.getDeptId());
                if (dept != null) {
                    onlineUser.put("deptName", dept.getDeptName());
                }
            }
            onlineUser.put("loginIp", ipAddress);
            onlineUser.put("loginLocation", location);
            onlineUser.put("browser", browser);
            onlineUser.put("os", os);
            onlineUser.put("loginTime", LocalDateTime.now().toString());
            onlineUser.put("lastAccessTime", LocalDateTime.now().toString());
            onlineUser.put("token", token);

            // 存储到Redis，key为 online_users:userId
            String onlineUserKey = ONLINE_USER_KEY + user.getId();
            redisService.set(onlineUserKey, onlineUser, 24 * 60 * 60); // 24小时
        } catch (Exception e) {
            log.error("保存在线用户信息失败: {}", e.getMessage(), e);
        }
    }
}
