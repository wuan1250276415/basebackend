package com.basebackend.system.controller.internal;

import com.basebackend.api.model.log.UserOperationLogDTO;
import com.basebackend.common.context.UserContext;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.service.client.InternalRequestAuth;
import com.basebackend.system.security.InternalRequestAuthValidator;
import com.basebackend.system.service.OperationLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperationLogInternalController 测试")
class OperationLogInternalControllerTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Mock
    private OperationLogService operationLogService;

    private OperationLogInternalController controller;

    @BeforeEach
    void setUp() {
        InternalRequestAuthValidator validator =
                new InternalRequestAuthValidator(SECRET, 300_000L, "basebackend-user-api");
        controller = new OperationLogInternalController(operationLogService, validator);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("未携带内部签名时应拒绝读取")
    void shouldRejectUnsignedInternalRead() {
        UserContextHolder.set(UserContext.builder().userId(1L).build());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/internal/operation-log/user/1");

        assertThatThrownBy(() -> controller.getUserOperationLogs(1L, 20, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非法内部调用");
        verifyNoInteractions(operationLogService);
    }

    @Test
    @DisplayName("普通用户不应读取其他用户日志")
    void shouldRejectCrossUserReadForNonAdmin() {
        UserContextHolder.set(UserContext.builder().userId(1L).build());

        MockHttpServletRequest request = signedRequest("GET", "/api/internal/operation-log/user/2");

        assertThatThrownBy(() -> controller.getUserOperationLogs(2L, 20, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权限读取其他用户");
        verifyNoInteractions(operationLogService);
    }

    @Test
    @DisplayName("签名合法且用户匹配时应返回日志")
    void shouldAllowSignedReadForCurrentUser() {
        UserContextHolder.set(UserContext.builder().userId(1L).build());
        MockHttpServletRequest request = signedRequest("GET", "/api/internal/operation-log/user/1");
        List<UserOperationLogDTO> logs = List.of(new UserOperationLogDTO(
                10L, 1L, "login", "登录", "127.0.0.1", null, null, null, null, 1, null, LocalDateTime.now()
        ));
        when(operationLogService.getUserOperationLogs(1L, 20)).thenReturn(logs);

        var result = controller.getUserOperationLogs(1L, 20, request);

        assertThat(result.getData()).hasSize(1);
        verify(operationLogService).getUserOperationLogs(1L, 20);
    }

    @Test
    @DisplayName("带签名的写入请求在用户匹配时应放行")
    void shouldAllowSignedSaveForCurrentUser() {
        UserContextHolder.set(UserContext.builder().userId(1L).build());
        MockHttpServletRequest request = signedRequest("POST", "/api/internal/operation-log/save");
        UserOperationLogDTO dto = new UserOperationLogDTO(
                null, 1L, "update_profile", "更新资料", "127.0.0.1",
                null, null, null, null, 1, null, null
        );

        controller.saveOperationLog(dto, request);

        verify(operationLogService).saveOperationLog(dto);
    }

    private MockHttpServletRequest signedRequest(String method, String path) {
        long timestamp = System.currentTimeMillis();
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.addHeader(InternalRequestAuth.HEADER_INTERNAL_CALL, "true");
        request.addHeader(InternalRequestAuth.HEADER_SERVICE_NAME, "basebackend-user-api");
        request.addHeader(InternalRequestAuth.HEADER_TIMESTAMP, String.valueOf(timestamp));
        request.addHeader(
                InternalRequestAuth.HEADER_SIGNATURE,
                InternalRequestAuth.sign(SECRET, "basebackend-user-api", timestamp, method, path)
        );
        return request;
    }
}
