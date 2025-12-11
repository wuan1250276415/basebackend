package com.basebackend.user.service.impl;

import com.basebackend.common.exception.BusinessException;
import com.basebackend.observability.metrics.CustomMetrics;
import com.basebackend.user.dto.profile.ChangePasswordDTO;
import com.basebackend.user.entity.SysUser;
import com.basebackend.user.mapper.SysUserMapper;
import com.basebackend.user.util.DeptInfoHelper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

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

    @Test
    @DisplayName("修改密码 - 两次密码不一致")
    void testChangePassword_PasswordMismatch() {
        doNothing().when(customMetrics).recordBusinessOperation(anyString(), anyString());
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("oldPassword");
        dto.setNewPassword("newPassword123");
        dto.setConfirmPassword("differentPassword");
        assertThrows(BusinessException.class, () -> profileService.changePassword(dto));
    }

    @Test
    @DisplayName("修改密码 - 新旧密码相同")
    void testChangePassword_SamePassword() {
        doNothing().when(customMetrics).recordBusinessOperation(anyString(), anyString());
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("samePassword");
        dto.setNewPassword("samePassword");
        dto.setConfirmPassword("samePassword");
        assertThrows(BusinessException.class, () -> profileService.changePassword(dto));
    }

    @Test
    @DisplayName("获取个人资料 - 未登录")
    void testGetCurrentUserProfile_NotAuthenticated() {
        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(null);
            doNothing().when(customMetrics).recordBusinessOperation(anyString(), anyString());
            assertThrows(BusinessException.class, () -> profileService.getCurrentUserProfile());
        }
    }
}
