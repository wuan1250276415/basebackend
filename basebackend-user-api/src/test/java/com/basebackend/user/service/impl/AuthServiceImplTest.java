package com.basebackend.user.service.impl;

import com.basebackend.cache.service.RedisService;
import com.basebackend.feign.dto.user.LoginRequest;
import com.basebackend.feign.dto.user.LoginResponse;
import com.basebackend.jwt.JwtUtil;
import com.basebackend.user.dto.PasswordChangeDTO;
import com.basebackend.user.entity.SysUser;
import com.basebackend.user.mapper.SysUserMapper;
import com.basebackend.user.service.LogService;
import com.basebackend.user.util.DeptInfoHelper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("认证服务测试")
class AuthServiceImplTest {

    @Mock
    private SysUserMapper userMapper;
    @Mock
    private DeptInfoHelper deptInfoHelper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private RedisService redisService;
    @Mock
    private LogService logService;

    @InjectMocks
    private AuthServiceImpl authService;

    private SysUser testUser;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = new SysUser();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword");
        testUser.setNickname("测试用户");
        testUser.setDeptId(1L);
        testUser.setUserType(1);
        testUser.setStatus(1);
        testUser.setCreateTime(LocalDateTime.now());

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setRemoteAddr("127.0.0.1");
        mockRequest.addHeader("User-Agent", "Mozilla/5.0");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("登录成功")
    void testLogin_Success() {
        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(eq("testuser"), anyMap())).thenReturn("jwt-token");
        when(userMapper.selectUserPermissions(1L)).thenReturn(Arrays.asList("user:read"));
        when(userMapper.selectUserRoles(1L)).thenReturn(Arrays.asList("admin"));
        when(userMapper.updateById(any(SysUser.class))).thenReturn(1);
        doNothing().when(logService).recordLoginLog(any());

        LoginResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.getAccessToken());
        verify(redisService).set(anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("登录失败 - 用户不存在")
    void testLogin_UserNotFound() {
        when(userMapper.selectByUsername("nonexistent")).thenReturn(null);
        loginRequest.setUsername("nonexistent");
        doNothing().when(logService).recordLoginLog(any());

        RuntimeException ex = assertThrows(RuntimeException.class, 
            () -> authService.login(loginRequest));
        assertEquals("用户不存在", ex.getMessage());
    }

    @Test
    @DisplayName("登录失败 - 密码错误")
    void testLogin_WrongPassword() {
        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("wrong", "encodedPassword")).thenReturn(false);
        loginRequest.setPassword("wrong");
        doNothing().when(logService).recordLoginLog(any());

        RuntimeException ex = assertThrows(RuntimeException.class, 
            () -> authService.login(loginRequest));
        assertEquals("密码错误", ex.getMessage());
    }

    @Test
    @DisplayName("登录失败 - 用户已禁用")
    void testLogin_UserDisabled() {
        testUser.setStatus(0);
        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        doNothing().when(logService).recordLoginLog(any());

        RuntimeException ex = assertThrows(RuntimeException.class, 
            () -> authService.login(loginRequest));
        assertEquals("用户已被禁用", ex.getMessage());
    }

    @Test
    @DisplayName("刷新Token成功")
    void testRefreshToken_Success() {
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.getSubjectFromToken("valid-token")).thenReturn("testuser");
        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);
        when(jwtUtil.generateToken(eq("testuser"), anyMap())).thenReturn("new-token");
        when(userMapper.selectUserPermissions(1L)).thenReturn(Arrays.asList("user:read"));
        when(userMapper.selectUserRoles(1L)).thenReturn(Arrays.asList("admin"));

        LoginResponse response = authService.refreshToken("valid-token");

        assertNotNull(response);
        assertEquals("new-token", response.getAccessToken());
    }

    @Test
    @DisplayName("刷新Token失败 - Token为空")
    void testRefreshToken_EmptyToken() {
        RuntimeException ex = assertThrows(RuntimeException.class, 
            () -> authService.refreshToken(""));
        assertEquals("刷新Token不能为空", ex.getMessage());
    }

    @Test
    @DisplayName("刷新Token失败 - Token无效")
    void testRefreshToken_InvalidToken() {
        when(jwtUtil.validateToken("invalid")).thenReturn(false);
        RuntimeException ex = assertThrows(RuntimeException.class, 
            () -> authService.refreshToken("invalid"));
        assertEquals("刷新Token无效", ex.getMessage());
    }

    @Test
    @DisplayName("修改密码 - 密码不一致")
    void testChangePassword_Mismatch() {
        PasswordChangeDTO dto = new PasswordChangeDTO();
        dto.setOldPassword("old");
        dto.setNewPassword("new1");
        dto.setConfirmPassword("new2");

        RuntimeException ex = assertThrows(RuntimeException.class, 
            () -> authService.changePassword(dto));
        assertEquals("新密码和确认密码不一致", ex.getMessage());
    }

    @Test
    @DisplayName("重置密码成功")
    void testResetPassword_Success() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(passwordEncoder.encode("newPwd")).thenReturn("encoded");
        when(userMapper.updateById(any(SysUser.class))).thenReturn(1);

        assertDoesNotThrow(() -> authService.resetPassword(1L, "newPwd"));
        verify(passwordEncoder).encode("newPwd");
    }

    @Test
    @DisplayName("重置密码失败 - 用户不存在")
    void testResetPassword_UserNotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);
        RuntimeException ex = assertThrows(RuntimeException.class, 
            () -> authService.resetPassword(999L, "pwd"));
        assertEquals("用户不存在", ex.getMessage());
    }

    @Test
    @DisplayName("登出成功")
    void testLogout() {
        assertDoesNotThrow(() -> authService.logout());
    }
}
