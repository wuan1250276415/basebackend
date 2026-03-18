package com.basebackend.user.service.impl;

import com.basebackend.common.context.UserContext;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.observability.metrics.CustomMetrics;
import com.basebackend.user.dto.profile.ChangePasswordDTO;
import com.basebackend.user.entity.SysUser;
import com.basebackend.user.mapper.SysUserMapper;
import com.basebackend.user.service.UserSessionService;
import com.basebackend.user.util.DeptInfoHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("个人资料服务测试")
class ProfileServiceImplTest {

    @Mock
    private SysUserMapper userMapper;
    @Mock
    private DeptInfoHelper deptInfoHelper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CustomMetrics customMetrics;
    @Mock
    private UserSessionService userSessionService;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private SysUser testUser;

    @BeforeEach
    void setUp() {
        testUser = new SysUser();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword");
        testUser.setNickname("测试用户");
        testUser.setDeptId(1L);
        testUser.setStatus(1);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("修改密码 - 两次密码不一致")
    void testChangePassword_PasswordMismatch() {
        doNothing().when(customMetrics).recordBusinessOperation(anyString(), anyString());
        ChangePasswordDTO dto = new ChangePasswordDTO("oldPassword", "newPassword123", "differentPassword");
        assertThrows(BusinessException.class, () -> profileService.changePassword(dto));
    }

    @Test
    @DisplayName("修改密码 - 新旧密码相同")
    void testChangePassword_SamePassword() {
        doNothing().when(customMetrics).recordBusinessOperation(anyString(), anyString());
        ChangePasswordDTO dto = new ChangePasswordDTO("samePassword", "samePassword", "samePassword");
        assertThrows(BusinessException.class, () -> profileService.changePassword(dto));
    }

    @Test
    @DisplayName("获取个人资料 - 未登录")
    void testGetCurrentUserProfile_NotAuthenticated() {
        doNothing().when(customMetrics).recordBusinessOperation(anyString(), anyString());
        assertThrows(BusinessException.class, () -> profileService.getCurrentUserProfile());
    }

    @Test
    @DisplayName("修改密码成功后应使当前会话失效")
    void testChangePassword_ShouldInvalidateSession() {
        doNothing().when(customMetrics).recordBusinessOperation(anyString(), anyString());
        UserContextHolder.set(UserContext.builder().userId(1L).username("testuser").build());
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
        when(userMapper.updateById(any(SysUser.class))).thenReturn(1);

        ChangePasswordDTO dto = new ChangePasswordDTO("oldPassword", "newPassword123", "newPassword123");

        assertDoesNotThrow(() -> profileService.changePassword(dto));
        verify(userSessionService).invalidateSession(1L);
    }
}
