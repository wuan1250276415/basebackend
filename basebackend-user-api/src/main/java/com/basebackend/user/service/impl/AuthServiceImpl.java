package com.basebackend.user.service.impl;

import cn.hutool.core.util.StrUtil;
import com.basebackend.common.context.UserContext;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.model.Result;
import com.basebackend.feign.dto.dept.DeptBasicDTO;
import com.basebackend.user.dto.LoginLogDTO;
import com.basebackend.user.dto.LoginRequest;
import com.basebackend.user.dto.LoginResponse;
import com.basebackend.user.dto.PasswordChangeDTO;
import com.basebackend.user.entity.SysUser;
import com.basebackend.user.mapper.SysUserMapper;
import com.basebackend.user.service.AuthService;
import com.basebackend.user.service.LogService;
import com.basebackend.cache.service.RedisService;
import com.basebackend.jwt.JwtUtil;
import com.basebackend.web.util.IpUtil;
import com.basebackend.web.util.UserAgentUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
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
    private final ObjectProvider<com.basebackend.feign.client.DeptFeignClient> deptFeignClientProvider;
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

            // 缓存用户信息 - 统一使用userId作为缓存键，保持与refreshToken一致性
            String userKey = LOGIN_TOKEN_KEY + user.getId();
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
            
            // 安全调用部门服务，失败不影响登录
            if (user.getDeptId() != null) {
                var deptFeignClient = deptFeignClientProvider.getIfAvailable();
                if (deptFeignClient != null) {
                    try {
                        Result<DeptBasicDTO> deptResult = deptFeignClient.getById(user.getDeptId());
                        if (deptResult != null && deptResult.getCode() == 200 && deptResult.getData() != null) {
                            userInfo.setDeptName(deptResult.getData().getDeptName());
                        } else {
                            log.warn("获取部门信息失败或返回空: deptId={}, message={}", 
                                    user.getDeptId(), deptResult != null ? deptResult.getMessage() : "null");
                            userInfo.setDeptName(""); // 设置默认值
                        }
                    } catch (Exception e) {
                        log.error("调用部门服务异常: deptId={}, error={}", user.getDeptId(), e.getMessage(), e);
                        userInfo.setDeptName(""); // 设置默认值，不影响登录流程
                    }
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

        // 更新缓存 - 与登录保持一致，使用userId作为缓存键
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
        
        // 安全调用部门服务，失败不影响刷新token
        if (user.getDeptId() != null) {
            var deptFeignClient = deptFeignClientProvider.getIfAvailable();
            if (deptFeignClient != null) {
                try {
                    Result<DeptBasicDTO> deptResult = deptFeignClient.getById(user.getDeptId());
                    if (deptResult != null && deptResult.getCode() == 200 && deptResult.getData() != null) {
                        userInfo.setDeptName(deptResult.getData().getDeptName());
                    } else {
                        log.warn("刷新token时获取部门信息失败: deptId={}, message={}", 
                                user.getDeptId(), deptResult != null ? deptResult.getMessage() : "null");
                        userInfo.setDeptName("");
                    }
                } catch (Exception e) {
                    log.error("刷新token时调用部门服务异常: deptId={}, error={}", user.getDeptId(), e.getMessage(), e);
                    userInfo.setDeptName("");
                }
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
    public UserContext getCurrentUserInfo() {
        // 从请求上下文获取当前用户ID
        UserContext userContext = UserContextHolder.get();
        return userContext;
    }

    @Override
    public void changePassword(PasswordChangeDTO passwordChangeDTO) {
        // 验证新密码和确认密码是否一致
        if (!passwordChangeDTO.getNewPassword().equals(passwordChangeDTO.getConfirmPassword())) {
            throw new RuntimeException("新密码和确认密码不一致");
        }

        // 从用户上下文获取当前用户ID - 修复硬编码用户ID安全问题
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new IllegalStateException("用户未登录，无法修改密码");
        }

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

            // 安全调用部门服务获取部门名称
            if (user.getDeptId() != null) {
                var deptFeignClient = deptFeignClientProvider.getIfAvailable();
                if (deptFeignClient != null) {
                    try {
                        Result<DeptBasicDTO> deptResult = deptFeignClient.getById(user.getDeptId());
                        if (deptResult != null && deptResult.getCode() == 200 && deptResult.getData() != null) {
                            onlineUser.put("deptName", deptResult.getData().getDeptName());
                        } else {
                            onlineUser.put("deptName", "");
                        }
                    } catch (Exception e) {
                        log.error("保存在线用户时获取部门名称失败: deptId={}, error={}", 
                                user.getDeptId(), e.getMessage());
                        onlineUser.put("deptName", "");
                    }
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
