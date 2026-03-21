package com.basebackend.user.service.impl;

import com.basebackend.api.model.user.LoginRequest;
import com.basebackend.api.model.user.LoginResponse;
import com.basebackend.common.context.UserContext;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.jwt.JwtProperties;
import com.basebackend.jwt.JwtUtil;
import com.basebackend.jwt.JwtValidationResult;
import com.basebackend.user.dto.LoginLogDTO;
import com.basebackend.user.dto.PasswordChangeDTO;
import com.basebackend.user.entity.SysUser;
import com.basebackend.user.mapper.SysUserMapper;
import com.basebackend.user.service.LogService;
import com.basebackend.user.service.UserSessionService;
import com.basebackend.user.util.DeptInfoHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    private JwtProperties jwtProperties;
    @Mock
    private UserSessionService userSessionService;
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

        loginRequest = new LoginRequest("testuser", "password123", "captcha", "captchaId", true);

        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setRemoteAddr("127.0.0.1");
        mockRequest.addHeader("User-Agent", "Mozilla/5.0");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));

        when(jwtProperties.getExpiration()).thenReturn(86400000L);
        when(jwtProperties.getRefreshTokenExpiration()).thenReturn(604800000L);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("登录成功")
    void testLogin_Success() {
        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(eq("testuser"), anyMap())).thenReturn("jwt-token");
        when(jwtUtil.generateRefreshToken("testuser")).thenReturn("refresh-token");
        when(userMapper.selectUserPermissions(1L)).thenReturn(Arrays.asList("user:read"));
        when(userMapper.selectUserRoles(1L)).thenReturn(Arrays.asList("admin"));
        when(userMapper.updateById(any(SysUser.class))).thenReturn(1);
        when(deptInfoHelper.getDeptName(1L)).thenReturn("测试部门");
        doNothing().when(logService).recordLoginLog(any());

        LoginResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("测试部门", response.userInfo().deptName());
        verify(userSessionService).replaceSession(1L, "jwt-token", "refresh-token", 86400L, 604800L);
        verify(userSessionService).storeAuthorities(eq(1L), anyList(), anyList(), eq(86400L));
        verify(userSessionService).storeOnlineUser(eq(1L), anyMap(), eq(86400L));
        verify(logService, times(1)).recordLoginLog(any(LoginLogDTO.class));
    }

    @Test
    @DisplayName("登录失败 - 用户不存在")
    void testLogin_UserNotFound() {
        when(userMapper.selectByUsername("nonexistent")).thenReturn(null);
        loginRequest = new LoginRequest("nonexistent", "password123", "captcha", "captchaId", true);
        doNothing().when(logService).recordLoginLog(any());

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(loginRequest));
        assertEquals("用户名或密码错误", ex.getMessage());
        assertEquals(401, ex.getCode());

        ArgumentCaptor<LoginLogDTO> loginLogCaptor = ArgumentCaptor.forClass(LoginLogDTO.class);
        verify(logService, times(1)).recordLoginLog(loginLogCaptor.capture());
        assertEquals("用户不存在", loginLogCaptor.getValue().getMsg());
        assertEquals(0, loginLogCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("登录失败 - 密码错误")
    void testLogin_WrongPassword() {
        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("wrong", "encodedPassword")).thenReturn(false);
        loginRequest = new LoginRequest("testuser", "wrong", "captcha", "captchaId", true);
        doNothing().when(logService).recordLoginLog(any());

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(loginRequest));
        assertEquals("用户名或密码错误", ex.getMessage());
        assertEquals(401, ex.getCode());

        ArgumentCaptor<LoginLogDTO> loginLogCaptor = ArgumentCaptor.forClass(LoginLogDTO.class);
        verify(logService, times(1)).recordLoginLog(loginLogCaptor.capture());
        assertEquals("密码错误", loginLogCaptor.getValue().getMsg());
        assertEquals(0, loginLogCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("登录失败 - 用户已禁用")
    void testLogin_UserDisabled() {
        testUser.setStatus(0);
        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        doNothing().when(logService).recordLoginLog(any());

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(loginRequest));
        assertEquals("用户已被禁用", ex.getMessage());
        assertEquals(403, ex.getCode());

        ArgumentCaptor<LoginLogDTO> loginLogCaptor = ArgumentCaptor.forClass(LoginLogDTO.class);
        verify(logService, times(1)).recordLoginLog(loginLogCaptor.capture());
        assertEquals("用户已被禁用", loginLogCaptor.getValue().getMsg());
        assertEquals(0, loginLogCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("刷新Token成功")
    void testRefreshToken_Success() {
        when(jwtUtil.validateTokenSafe("valid-refresh", JwtUtil.TOKEN_TYPE_REFRESH))
                .thenReturn(JwtValidationResult.success(null));
        when(jwtUtil.getSubjectFromToken("valid-refresh")).thenReturn("testuser");
        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);
        when(userSessionService.getRefreshToken(1L)).thenReturn("valid-refresh");
        when(jwtUtil.generateToken(eq("testuser"), anyMap())).thenReturn("new-token");
        when(jwtUtil.generateRefreshToken("testuser")).thenReturn("new-refresh-token");
        when(userMapper.selectUserPermissions(1L)).thenReturn(Arrays.asList("user:read"));
        when(userMapper.selectUserRoles(1L)).thenReturn(Arrays.asList("admin"));
        when(deptInfoHelper.getDeptName(1L)).thenReturn("测试部门");

        LoginResponse response = authService.refreshToken("valid-refresh");

        assertNotNull(response);
        assertEquals("new-token", response.accessToken());
        assertEquals("new-refresh-token", response.refreshToken());
        verify(userSessionService).replaceSession(1L, "new-token", "new-refresh-token", 86400L, 604800L);
    }

    @Test
    @DisplayName("刷新Token失败 - Token为空")
    void testRefreshToken_EmptyToken() {
        BusinessException ex = assertThrows(BusinessException.class, () -> authService.refreshToken(""));
        assertEquals("刷新Token不能为空", ex.getMessage());
    }

    @Test
    @DisplayName("刷新Token失败 - Token无效")
    void testRefreshToken_InvalidToken() {
        when(jwtUtil.validateTokenSafe("invalid", JwtUtil.TOKEN_TYPE_REFRESH))
                .thenReturn(JwtValidationResult.failure(null, "invalid"));

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.refreshToken("invalid"));
        assertEquals("刷新Token无效", ex.getMessage());
    }

    @Test
    @DisplayName("刷新Token失败 - Access Token 不允许用于刷新")
    void testRefreshToken_AccessTokenRejected() {
        when(jwtUtil.validateTokenSafe("access-token", JwtUtil.TOKEN_TYPE_REFRESH))
                .thenReturn(JwtValidationResult.failure(null, "token type mismatch"));

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.refreshToken("access-token"));
        assertEquals("刷新Token无效", ex.getMessage());
    }

    @Test
    @DisplayName("修改密码 - 密码不一致")
    void testChangePassword_Mismatch() {
        PasswordChangeDTO dto = new PasswordChangeDTO("old", "new1", "new2");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.changePassword(dto));
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
        verify(userSessionService).invalidateSession(1L);
    }

    @Test
    @DisplayName("重置密码失败 - 用户不存在")
    void testResetPassword_UserNotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.resetPassword(999L, "pwd"));
        assertEquals("用户不存在", ex.getMessage());
    }

    @Test
    @DisplayName("登出成功")
    void testLogout() {
        UserContextHolder.set(UserContext.builder().userId(1L).username("testuser").build());

        assertDoesNotThrow(() -> authService.logout());
        verify(userSessionService).invalidateSession(1L);
    }

    @Test
    @DisplayName("获取当前用户信息失败 - 用户未登录")
    void testGetCurrentUserInfo_Unauthorized() {
        BusinessException ex = assertThrows(BusinessException.class, () -> authService.getCurrentUserInfo());
        assertEquals("用户未登录", ex.getMessage());
        assertEquals(401, ex.getCode());
    }

    @Test
    @DisplayName("微信单点登录已禁用")
    void testWechatLogin_Disabled() {
        BusinessException ex = assertThrows(BusinessException.class, () -> authService.wechatLogin("13800138000"));
        assertEquals("微信单点登录已禁用，待接入可信第三方认证后开放", ex.getMessage());
        assertEquals(403, ex.getCode());
    }
}
