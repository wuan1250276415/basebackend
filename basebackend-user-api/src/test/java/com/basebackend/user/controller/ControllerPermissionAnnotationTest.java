package com.basebackend.user.controller;

import com.basebackend.security.annotation.RequiresPermission;
import com.basebackend.api.model.user.RefreshTokenRequest;
import com.basebackend.user.dto.ResetPasswordRequest;
import com.basebackend.user.dto.UserCreateDTO;
import com.basebackend.user.dto.UserDTO;
import com.basebackend.user.dto.UserQueryDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("用户与角色控制器权限注解测试")
class ControllerPermissionAnnotationTest {

    @Test
    @DisplayName("用户管理关键接口应声明权限")
    void userControllerManagementEndpointsShouldRequirePermission() throws Exception {
        assertMethodPermission(UserController.class, "page",
                new Class<?>[]{int.class, int.class, UserQueryDTO.class},
                "system:user:list");
        assertMethodPermission(UserController.class, "getById",
                new Class<?>[]{Long.class},
                "system:user:view");
        assertMethodPermission(UserController.class, "create",
                new Class<?>[]{UserCreateDTO.class},
                "system:user:create");
        assertMethodPermission(UserController.class, "update",
                new Class<?>[]{Long.class, UserDTO.class},
                "system:user:update");
        assertMethodPermission(UserController.class, "delete",
                new Class<?>[]{Long.class},
                "system:user:delete");
        assertMethodPermission(UserController.class, "deleteBatch",
                new Class<?>[]{List.class},
                "system:user:delete");
        assertMethodPermission(UserController.class, "assignRoles",
                new Class<?>[]{Long.class, List.class},
                "system:user:update");
        assertMethodPermission(UserController.class, "changeStatus",
                new Class<?>[]{Long.class, Integer.class},
                "system:user:update");
        assertMethodPermission(UserController.class, "export",
                new Class<?>[]{UserQueryDTO.class},
                "system:user:list");
    }

    @Test
    @DisplayName("重置密码接口应兼容旧权限并接受更新权限")
    void resetPasswordShouldAcceptLegacyOrUpdatePermission() throws Exception {
        Method method = UserController.class.getDeclaredMethod("resetPassword", Long.class, ResetPasswordRequest.class);
        RequiresPermission annotation = method.getAnnotation(RequiresPermission.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.logical()).isEqualTo(RequiresPermission.Logical.OR);
        assertThat(Arrays.asList(annotation.values()))
                .containsExactlyInAnyOrder("system:user:resetPassword", "system:user:update");
    }

    @Test
    @DisplayName("敏感参数不应继续放在 URL 中")
    void sensitiveParametersShouldUseRequestBody() throws Exception {
        Method refreshMethod = AuthController.class.getDeclaredMethod("refreshToken", RefreshTokenRequest.class);
        Method resetPasswordMethod = UserController.class.getDeclaredMethod("resetPassword",
                Long.class, ResetPasswordRequest.class);

        assertThat(refreshMethod.getParameters()[0].isAnnotationPresent(RequestBody.class)).isTrue();
        assertThat(resetPasswordMethod.getParameters()[1].isAnnotationPresent(RequestBody.class)).isTrue();
    }

    @Test
    @DisplayName("角色管理关键接口应声明权限")
    void roleControllerManagementEndpointsShouldRequirePermission() throws Exception {
        assertMethodPermission(RoleController.class, "page",
                new Class<?>[]{int.class, int.class, String.class, String.class, Integer.class},
                "system:role:list");
        assertMethodPermission(RoleController.class, "getById",
                new Class<?>[]{Long.class},
                "system:role:view");
        assertMethodPermission(RoleController.class, "create",
                new Class<?>[]{com.basebackend.user.dto.RoleDTO.class},
                "system:role:create");
        assertMethodPermission(RoleController.class, "update",
                new Class<?>[]{Long.class, com.basebackend.user.dto.RoleDTO.class},
                "system:role:update");
        assertMethodPermission(RoleController.class, "delete",
                new Class<?>[]{Long.class},
                "system:role:delete");
        assertMethodPermission(RoleController.class, "assignMenus",
                new Class<?>[]{Long.class, List.class},
                "system:role:update");
        assertMethodPermission(RoleController.class, "assignPermissions",
                new Class<?>[]{Long.class, List.class},
                "system:role:update");
        assertMethodPermission(RoleController.class, "assignUsersToRole",
                new Class<?>[]{Long.class, List.class},
                "system:role:update");
        assertMethodPermission(RoleController.class, "removeUserFromRole",
                new Class<?>[]{Long.class, Long.class},
                "system:role:update");
        assertMethodPermission(RoleController.class, "assignResources",
                new Class<?>[]{Long.class, List.class},
                "system:role:update");
        assertMethodPermission(RoleController.class, "configureListOperations",
                new Class<?>[]{Long.class, Map.class},
                "system:role:update");
        assertMethodPermission(RoleController.class, "configureDataPermissions",
                new Class<?>[]{Long.class, Map.class},
                "system:role:update");
    }

    private void assertMethodPermission(Class<?> controllerClass, String methodName, Class<?>[] parameterTypes,
                                        String expectedPermission) throws Exception {
        Method method = controllerClass.getDeclaredMethod(methodName, parameterTypes);
        RequiresPermission annotation = method.getAnnotation(RequiresPermission.class);

        assertThat(annotation)
                .withFailMessage("%s#%s 应声明 @RequiresPermission", controllerClass.getSimpleName(), methodName)
                .isNotNull();
        if (annotation.values().length > 0) {
            assertThat(annotation.values()).contains(expectedPermission);
        } else {
            assertThat(annotation.value()).isEqualTo(expectedPermission);
        }
    }
}
