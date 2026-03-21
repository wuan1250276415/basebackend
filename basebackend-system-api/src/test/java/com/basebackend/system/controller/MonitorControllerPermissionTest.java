package com.basebackend.system.controller;

import com.basebackend.system.dto.ForceLogoutRequest;
import com.basebackend.security.annotation.RequiresPermission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MonitorController 权限注解测试")
class MonitorControllerPermissionTest {

    @Test
    @DisplayName("监控接口应恢复显式权限控制")
    void monitorEndpointsShouldRequirePermission() throws Exception {
        assertMethodPermission("getOnlineUsers", new Class<?>[0], "system:monitor:online");
        assertMethodPermission("forceLogout", new Class<?>[]{ForceLogoutRequest.class}, "system:monitor:forceLogout");
        assertMethodPermission("getServerInfo", new Class<?>[0], "system:monitor:server");
        assertMethodPermission("getCacheInfo", new Class<?>[0], "system:monitor:cache");
        assertMethodPermission("clearCache", new Class<?>[]{String.class}, "system:monitor:cacheClean");
        assertMethodPermission("clearAllCache", new Class<?>[0], "system:monitor:cacheClean");
        assertMethodPermission("getSystemStats", new Class<?>[0], "system:monitor:stats");
    }

    @Test
    @DisplayName("强制下线接口应使用请求体接收敏感令牌")
    void forceLogoutShouldUseRequestBody() throws Exception {
        Method method = MonitorController.class.getDeclaredMethod("forceLogout", ForceLogoutRequest.class);
        assertThat(method.getParameters()[0].isAnnotationPresent(RequestBody.class)).isTrue();
    }

    private void assertMethodPermission(String methodName, Class<?>[] parameterTypes, String expectedPermission)
            throws Exception {
        Method method = MonitorController.class.getDeclaredMethod(methodName, parameterTypes);
        RequiresPermission annotation = method.getAnnotation(RequiresPermission.class);

        assertThat(annotation)
                .withFailMessage("MonitorController#%s 应声明 @RequiresPermission", methodName)
                .isNotNull();
        assertThat(annotation.value()).isEqualTo(expectedPermission);
    }
}
