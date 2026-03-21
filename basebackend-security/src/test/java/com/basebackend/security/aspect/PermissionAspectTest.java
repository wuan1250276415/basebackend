package com.basebackend.security.aspect;

import com.basebackend.security.annotation.RequiresPermission;
import com.basebackend.security.service.PermissionService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionAspect 通配符权限测试")
class PermissionAspectTest {

    @Mock
    private PermissionService permissionService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Test
    @DisplayName("应允许前缀通配符权限命中精确权限")
    void shouldAllowPrefixWildcardPermission() throws Throwable {
        PermissionAspect permissionAspect = new PermissionAspect(permissionService);
        RequiresPermission annotation = TestTarget.class
                .getDeclaredMethod("updateRole")
                .getAnnotation(RequiresPermission.class);

        when(permissionService.getCurrentUserPermissions()).thenReturn(List.of("system:role:*"));
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = permissionAspect.checkPermission(joinPoint, annotation);

        assertThat(result).isEqualTo("ok");
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("应允许分段通配符权限命中精确权限")
    void shouldAllowSegmentWildcardPermission() throws Throwable {
        PermissionAspect permissionAspect = new PermissionAspect(permissionService);
        RequiresPermission annotation = TestTarget.class
                .getDeclaredMethod("viewUser")
                .getAnnotation(RequiresPermission.class);

        when(permissionService.getCurrentUserPermissions()).thenReturn(List.of("system:*:view"));
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = permissionAspect.checkPermission(joinPoint, annotation);

        assertThat(result).isEqualTo("ok");
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("权限不匹配时应拒绝访问")
    void shouldDenyWhenPermissionMissing() throws Throwable {
        PermissionAspect permissionAspect = new PermissionAspect(permissionService);
        RequiresPermission annotation = TestTarget.class
                .getDeclaredMethod("deleteUser")
                .getAnnotation(RequiresPermission.class);

        when(permissionService.getCurrentUserPermissions()).thenReturn(List.of("system:user:view"));

        assertThrows(AccessDeniedException.class, () -> permissionAspect.checkPermission(joinPoint, annotation));
    }

    private static class TestTarget {

        @RequiresPermission("system:role:update")
        void updateRole() {
        }

        @RequiresPermission("system:user:view")
        void viewUser() {
        }

        @RequiresPermission("system:user:delete")
        void deleteUser() {
        }
    }
}
