package com.basebackend.auth.service.impl;

import com.basebackend.auth.dto.LoginRequest;
import com.basebackend.auth.dto.LoginResponse;
import com.basebackend.auth.dto.PasswordChangeDTO;
import com.basebackend.auth.service.AuthService;
import com.basebackend.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PasswordEncoder passwordEncoder;
    
    private static final String TOKEN_PREFIX = "auth:token:";
    private static final String USER_PREFIX = "auth:user:";
    private static final long TOKEN_EXPIRE_TIME = 3600; // 1小时

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        log.info("用户登录: {}", loginRequest.getUsername());
        
        try {
            // 1. 验证验证码（如果需要）
            // TODO: 实现验证码验证
            
            // 2. 验证用户名密码（这里使用模拟数据，实际应该调用user-api）
            // TODO: 通过Feign调用user-api验证用户
            // UserDTO user = userServiceClient.getByUsername(loginRequest.getUsername());
            // if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            //     throw new RuntimeException("用户名或密码错误");
            // }
            
            // 模拟用户数据
            Long userId = 1L;
            String username = loginRequest.getUsername();
            
            // 3. 生成JWT Token
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", userId);
            claims.put("username", username);
            String accessToken = jwtUtil.generateToken(username, claims);
            
            // 4. 缓存登录信息
            String tokenKey = TOKEN_PREFIX + accessToken;
            String userKey = USER_PREFIX + userId;
            redisTemplate.opsForValue().set(tokenKey, userId, TOKEN_EXPIRE_TIME, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(userKey, username, TOKEN_EXPIRE_TIME, TimeUnit.SECONDS);
            
            // 5. 构建响应
            LoginResponse response = new LoginResponse();
            response.setAccessToken(accessToken);
            response.setExpiresIn(TOKEN_EXPIRE_TIME);
            
            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
            userInfo.setUserId(userId);
            userInfo.setUsername(username);
            response.setUserInfo(userInfo);
            
            log.info("用户登录成功: {}", username);
            return response;
        } catch (Exception e) {
            log.error("用户登录失败: {}", loginRequest.getUsername(), e);
            throw new RuntimeException("登录失败: " + e.getMessage());
        }
    }

    @Override
    public void logout() {
        log.info("用户登出");
        try {
            // TODO: 从SecurityContext获取当前用户信息
            // 1. 获取当前Token
            // 2. 清除Token缓存
            // 3. 清除用户缓存
            log.info("用户登出成功");
        } catch (Exception e) {
            log.error("用户登出失败", e);
            throw new RuntimeException("登出失败: " + e.getMessage());
        }
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        log.info("刷新Token");
        try {
            // 1. 验证refreshToken
            if (!verifyToken(refreshToken)) {
                throw new RuntimeException("Token无效");
            }
            
            // 2. 从Token中获取用户信息
            Claims claims = jwtUtil.getClaimsFromToken(refreshToken);
            if (claims == null) {
                throw new RuntimeException("Token解析失败");
            }
            Long userId = claims.get("userId", Long.class);
            String username = claims.get("username", String.class);
            
            if (username == null) {
                throw new RuntimeException("用户信息不存在");
            }
            
            // 3. 生成新的accessToken
            Map<String, Object> newClaims = new HashMap<>();
            newClaims.put("userId", userId);
            newClaims.put("username", username);
            String newAccessToken = jwtUtil.generateToken(username, newClaims);
            
            // 4. 更新缓存
            String tokenKey = TOKEN_PREFIX + newAccessToken;
            redisTemplate.opsForValue().set(tokenKey, userId, TOKEN_EXPIRE_TIME, TimeUnit.SECONDS);
            
            // 5. 构建响应
            LoginResponse response = new LoginResponse();
            response.setAccessToken(newAccessToken);
            response.setExpiresIn(TOKEN_EXPIRE_TIME);
            
            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
            userInfo.setUserId(userId);
            userInfo.setUsername(username);
            response.setUserInfo(userInfo);
            
            log.info("Token刷新成功: {}", username);
            return response;
        } catch (Exception e) {
            log.error("Token刷新失败", e);
            throw new RuntimeException("Token刷新失败: " + e.getMessage());
        }
    }

    @Override
    public LoginResponse.UserInfo getCurrentUserInfo() {
        log.info("获取当前用户信息");
        try {
            // TODO: 从SecurityContext获取用户ID
            // 实际应该从Spring Security上下文获取
            Long userId = 1L; // 模拟数据
            String username = (String) redisTemplate.opsForValue().get(USER_PREFIX + userId);
            
            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
            userInfo.setUserId(userId);
            userInfo.setUsername(username != null ? username : "admin");
            
            return userInfo;
        } catch (Exception e) {
            log.error("获取当前用户信息失败", e);
            throw new RuntimeException("获取用户信息失败: " + e.getMessage());
        }
    }

    @Override
    public void changePassword(PasswordChangeDTO passwordChangeDTO) {
        log.info("修改密码");
        try {
            // TODO: 实现密码修改逻辑
            // 1. 获取当前用户
            // 2. 验证旧密码
            // 3. 验证新密码与确认密码一致
            if (!passwordChangeDTO.getNewPassword().equals(passwordChangeDTO.getConfirmPassword())) {
                throw new RuntimeException("两次输入的密码不一致");
            }
            // 4. 调用user-api更新密码
            // 5. 清除用户缓存，强制重新登录
            log.info("密码修改成功");
        } catch (Exception e) {
            log.error("密码修改失败", e);
            throw new RuntimeException("密码修改失败: " + e.getMessage());
        }
    }

    @Override
    public boolean verifyToken(String token) {
        log.info("验证Token");
        try {
            // 1. 验证Token格式和签名
            if (!jwtUtil.validateToken(token)) {
                return false;
            }
            
            // 2. 验证Token是否在缓存中
            String tokenKey = TOKEN_PREFIX + token;
            Object userId = redisTemplate.opsForValue().get(tokenKey);
            
            return userId != null;
        } catch (Exception e) {
            log.error("Token验证失败", e);
            return false;
        }
    }
}
