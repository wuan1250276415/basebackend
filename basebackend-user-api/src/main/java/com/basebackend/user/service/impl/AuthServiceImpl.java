package com.basebackend.user.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.api.model.user.LoginRequest;
import com.basebackend.api.model.user.LoginResponse;
import com.basebackend.common.context.UserContext;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.enums.CommonErrorCode;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.common.util.IpUtil;
import com.basebackend.common.util.UserAgentUtil;
import com.basebackend.jwt.JwtException;
import com.basebackend.jwt.JwtProperties;
import com.basebackend.jwt.JwtUtil;
import com.basebackend.jwt.JwtValidationResult;
import com.basebackend.user.dto.LoginLogDTO;
import com.basebackend.user.dto.PasswordChangeDTO;
import com.basebackend.user.entity.SysUser;
import com.basebackend.user.mapper.SysUserMapper;
import com.basebackend.user.service.AuthService;
import com.basebackend.user.service.LogService;
import com.basebackend.user.service.UserSessionService;
import com.basebackend.user.util.DeptInfoHelper;
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
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper userMapper;
    private final DeptInfoHelper deptInfoHelper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final UserSessionService userSessionService;
    private final LogService logService;

    private static final String INVALID_LOGIN_MESSAGE = "用户名或密码错误";

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        log.info("用户登录: {}", loginRequest.username());

        RequestMetadata requestMetadata = resolveRequestMetadata();

        LoginLogDTO loginLog = new LoginLogDTO();
        loginLog.setUsername(loginRequest.username());
        loginLog.setIpAddress(requestMetadata.ipAddress());
        loginLog.setLoginLocation(requestMetadata.location());
        loginLog.setBrowser(requestMetadata.browser());
        loginLog.setOs(requestMetadata.os());
        loginLog.setLoginTime(LocalDateTime.now());

        try {
            SysUser user = userMapper.selectByUsername(loginRequest.username());
            if (user == null) {
                loginLog.setStatus(0);
                loginLog.setMsg("用户不存在");
                throw new BusinessException(CommonErrorCode.UNAUTHORIZED, INVALID_LOGIN_MESSAGE);
            }
            if (!passwordEncoder.matches(loginRequest.password(), user.getPassword())) {
                loginLog.setUserId(user.getId());
                loginLog.setStatus(0);
                loginLog.setMsg("密码错误");
                throw new BusinessException(CommonErrorCode.UNAUTHORIZED, INVALID_LOGIN_MESSAGE);
            }
            if (user.getStatus() == 0) {
                loginLog.setUserId(user.getId());
                loginLog.setStatus(0);
                loginLog.setMsg("用户已被禁用");
                throw BusinessException.forbidden("用户已被禁用");
            }

            loginLog.setUserId(user.getId());
            loginLog.setStatus(1);
            loginLog.setMsg("登录成功");

            user.setLoginIp(requestMetadata.ipAddress());
            user.setLoginTime(LocalDateTime.now());
            userMapper.updateById(user);

            List<String> permissions = userMapper.selectUserPermissions(user.getId());
            List<String> roles = userMapper.selectUserRoles(user.getId());
            SessionTokens sessionTokens = createSessionTokens(user);
            persistSession(user, sessionTokens, permissions, roles, requestMetadata);

            LoginResponse response = buildLoginResponse(user, sessionTokens, permissions, roles);
            recordLoginLog(loginLog);
            log.info("用户登录成功: {}", user.getUsername());
            return response;
        } catch (Exception e) {
            loginLog.setStatus(0);
            if (StrUtil.isBlank(loginLog.getMsg())) {
                loginLog.setMsg("登录失败: " + e.getMessage());
            }
            recordLoginLog(loginLog);
            throw e;
        }
    }

    @Override
    public void logout() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "用户未登录，无法登出");
        }

        userSessionService.invalidateSession(userId);
        log.info("用户登出成功: userId={}", userId);
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        if (StrUtil.isBlank(refreshToken)) {
            throw new BusinessException(CommonErrorCode.REFRESH_TOKEN_MISSING, "刷新Token不能为空");
        }

        JwtValidationResult validationResult = jwtUtil.validateTokenSafe(refreshToken, JwtUtil.TOKEN_TYPE_REFRESH);
        if (!validationResult.isValid()) {
            throw convertRefreshTokenException(validationResult);
        }

        String username = jwtUtil.getSubjectFromToken(refreshToken);
        SysUser user = userMapper.selectByUsername(username);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        if (user.getStatus() == 0) {
            throw BusinessException.forbidden("用户已被禁用");
        }

        String storedRefreshToken = userSessionService.getRefreshToken(user.getId());
        if (!StrUtil.equals(refreshToken, storedRefreshToken)) {
            throw new BusinessException(CommonErrorCode.REFRESH_TOKEN_INVALID, "刷新Token无效");
        }

        List<String> permissions = userMapper.selectUserPermissions(user.getId());
        List<String> roles = userMapper.selectUserRoles(user.getId());
        SessionTokens sessionTokens = createSessionTokens(user);
        persistSession(user, sessionTokens, permissions, roles, resolveRequestMetadata());
        return buildLoginResponse(user, sessionTokens, permissions, roles);
    }

    @Override
    public UserContext getCurrentUserInfo() {
        UserContext userContext = UserContextHolder.get();
        if (userContext == null || userContext.getUserId() == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "用户未登录");
        }
        return userContext;
    }

    @Override
    public void changePassword(PasswordChangeDTO passwordChangeDTO) {
        if (!passwordChangeDTO.newPassword().equals(passwordChangeDTO.confirmPassword())) {
            throw BusinessException.paramError("新密码和确认密码不一致");
        }

        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "用户未登录，无法修改密码");
        }

        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        if (!passwordEncoder.matches(passwordChangeDTO.oldPassword(), user.getPassword())) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "旧密码错误");
        }

        user.setPassword(passwordEncoder.encode(passwordChangeDTO.newPassword()));
        userMapper.updateById(user);
        userSessionService.invalidateSession(userId);

        log.info("用户修改密码成功: {}", user.getUsername());
    }

    @Override
    public void resetPassword(Long userId, String newPassword) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        userSessionService.invalidateSession(userId);

        log.info("重置用户密码成功: {}", user.getUsername());
    }

    @Override
    public LoginResponse wechatLogin(String phone) {
        log.info("微信单点登录: phone={}", phone);

        RequestMetadata requestMetadata = resolveRequestMetadata();

        LoginLogDTO loginLog = new LoginLogDTO();
        loginLog.setUsername(phone);
        loginLog.setIpAddress(requestMetadata.ipAddress());
        loginLog.setLoginLocation(requestMetadata.location());
        loginLog.setBrowser(requestMetadata.browser());
        loginLog.setOs(requestMetadata.os());
        loginLog.setLoginTime(LocalDateTime.now());

        try {
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUser::getPhone, phone);
            SysUser user = userMapper.selectOne(wrapper);

            if (user == null) {
                log.info("用户不存在，创建新用户: phone={}", phone);
                user = new SysUser();
                user.setUsername(phone);
                user.setPhone(phone);
                user.setNickname("微信用户_" + phone.substring(phone.length() - 4));
                user.setPassword(passwordEncoder.encode("123456"));
                user.setUserType(2);
                user.setStatus(1);
                user.setLoginIp(requestMetadata.ipAddress());
                user.setLoginTime(LocalDateTime.now());
                user.setCreateTime(LocalDateTime.now());

                userMapper.insert(user);
                log.info("新用户创建成功: userId={}, phone={}", user.getId(), phone);
            } else {
                if (user.getStatus() == 0) {
                    loginLog.setUserId(user.getId());
                    loginLog.setStatus(0);
                    loginLog.setMsg("用户已被禁用");
                    throw BusinessException.forbidden("用户已被禁用");
                }

                user.setLoginIp(requestMetadata.ipAddress());
                user.setLoginTime(LocalDateTime.now());
                userMapper.updateById(user);
            }

            loginLog.setUserId(user.getId());
            loginLog.setStatus(1);
            loginLog.setMsg("微信登录成功");

            List<String> permissions = userMapper.selectUserPermissions(user.getId());
            List<String> roles = userMapper.selectUserRoles(user.getId());
            SessionTokens sessionTokens = createSessionTokens(user);
            persistSession(user, sessionTokens, permissions, roles, requestMetadata);

            LoginResponse response = buildLoginResponse(user, sessionTokens, permissions, roles);
            recordLoginLog(loginLog);

            log.info("微信单点登录成功: phone={}, userId={}", phone, user.getId());
            return response;
        } catch (Exception e) {
            loginLog.setStatus(0);
            if (StrUtil.isBlank(loginLog.getMsg())) {
                loginLog.setMsg("微信登录失败: " + e.getMessage());
            }
            recordLoginLog(loginLog);
            throw e;
        }
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
    private void saveOnlineUser(SysUser user, String token, RequestMetadata requestMetadata) {
        try {
            Map<String, Object> onlineUser = new HashMap<>();
            onlineUser.put("userId", user.getId());
            onlineUser.put("username", user.getUsername());
            onlineUser.put("nickname", user.getNickname());
            onlineUser.put("deptId", user.getDeptId());
            onlineUser.put("deptName", deptInfoHelper.getDeptName(user.getDeptId()));
            onlineUser.put("loginIp", requestMetadata.ipAddress());
            onlineUser.put("loginLocation", requestMetadata.location());
            onlineUser.put("browser", requestMetadata.browser());
            onlineUser.put("os", requestMetadata.os());
            onlineUser.put("loginTime", LocalDateTime.now().toString());
            onlineUser.put("lastAccessTime", LocalDateTime.now().toString());
            onlineUser.put("token", token);

            userSessionService.storeOnlineUser(user.getId(), onlineUser, getAccessTokenTtlSeconds());
        } catch (Exception e) {
            log.error("保存在线用户信息失败: {}", e.getMessage(), e);
        }
    }

    private SessionTokens createSessionTokens(SysUser user) {
        String accessToken = jwtUtil.generateToken(user.getUsername(), buildTokenClaims(user));
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());
        return new SessionTokens(accessToken, refreshToken);
    }

    private Map<String, Object> buildTokenClaims(SysUser user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("userType", user.getUserType());
        claims.put("deptId", user.getDeptId());
        return claims;
    }

    private void persistSession(SysUser user, SessionTokens sessionTokens, List<String> permissions,
                                List<String> roles, RequestMetadata requestMetadata) {
        userSessionService.replaceSession(user.getId(), sessionTokens.accessToken(), sessionTokens.refreshToken(),
                getAccessTokenTtlSeconds(), getRefreshTokenTtlSeconds());
        userSessionService.storeAuthorities(user.getId(), permissions, roles, getAccessTokenTtlSeconds());
        saveOnlineUser(user, sessionTokens.accessToken(), requestMetadata);
    }

    private LoginResponse buildLoginResponse(SysUser user, SessionTokens sessionTokens, List<String> permissions,
                                             List<String> roles) {
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(), user.getUsername(), user.getNickname(),
                user.getEmail(), user.getPhone(), user.getAvatar(),
                user.getGender(), user.getDeptId(),
                deptInfoHelper.getDeptName(user.getDeptId()),
                user.getUserType(), user.getStatus()
        );

        return new LoginResponse(
                sessionTokens.accessToken(),
                sessionTokens.refreshToken(),
                getAccessTokenTtlSeconds(),
                userInfo,
                permissions,
                roles
        );
    }

    private RequestMetadata resolveRequestMetadata() {
        HttpServletRequest request = getHttpServletRequest();
        String ipAddress = IpUtil.getIpAddress(request);
        return new RequestMetadata(
                ipAddress,
                IpUtil.getLocationByIp(ipAddress),
                UserAgentUtil.getBrowser(request),
                UserAgentUtil.getOperatingSystem(request)
        );
    }

    private BusinessException convertRefreshTokenException(JwtValidationResult validationResult) {
        if (validationResult.getErrorType() == JwtException.ErrorType.EXPIRED) {
            return new BusinessException(CommonErrorCode.REFRESH_TOKEN_EXPIRED, "刷新Token已过期");
        }
        return new BusinessException(CommonErrorCode.REFRESH_TOKEN_INVALID, "刷新Token无效");
    }

    private long getAccessTokenTtlSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(jwtProperties.getExpiration());
    }

    private long getRefreshTokenTtlSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(jwtProperties.getRefreshTokenExpiration());
    }

    private record SessionTokens(String accessToken, String refreshToken) {
    }

    private record RequestMetadata(String ipAddress, String location, String browser, String os) {
    }
}
