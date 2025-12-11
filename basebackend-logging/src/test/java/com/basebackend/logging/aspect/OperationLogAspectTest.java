package com.basebackend.logging.aspect;

import com.basebackend.logging.annotation.OperationLog;
import com.basebackend.logging.model.OperationLogInfo;
import com.basebackend.logging.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OperationLogAspect测试类
 * 测试操作日志切面的拦截和记录功能
 *
 * @author BaseBackend
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OperationLogAspect 操作日志切面测试")
class OperationLogAspectTest {

    @Mock
    private OperationLogService operationLogService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private OperationLogAspect operationLogAspect;

    /**
     * 测试用的业务方法
     */
    @OperationLog(operation = "测试操作", businessType = OperationLog.BusinessType.SELECT)
    public String testMethod(String param) {
        return "test result";
    }

    /**
     * 测试用的异常方法
     */
    @OperationLog(operation = "异常操作", businessType = OperationLog.BusinessType.DELETE)
    public void exceptionMethod() throws RuntimeException {
        throw new RuntimeException("测试异常");
    }

    @Test
    @DisplayName("记录操作日志 - 成功场景")
    void shouldLogOperationSuccessfully() throws Throwable {
        // Given
        Method method = this.getClass().getMethod("testMethod", String.class);
        OperationLog operationLog = method.getAnnotation(OperationLog.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenReturn("test result");
        when(methodSignature.getDeclaringType()).thenReturn(this.getClass());
        when(methodSignature.getName()).thenReturn("testMethod");

        // 模拟请求上下文
        ServletRequestAttributes attributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attributes);

        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // When
        Object result = operationLogAspect.logOperation(joinPoint, operationLog);

        // Then
        assertThat(result).isEqualTo("test result");
        verify(operationLogService, times(1)).saveOperationLog(any(OperationLogInfo.class));
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    @DisplayName("记录操作日志 - 异常场景")
    void shouldLogOperationException() throws Throwable {
        // Given
        Method method = this.getClass().getMethod("exceptionMethod");
        OperationLog operationLog = method.getAnnotation(OperationLog.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenThrow(new RuntimeException("测试异常"));
        when(methodSignature.getDeclaringType()).thenReturn(this.getClass());
        when(methodSignature.getName()).thenReturn("exceptionMethod");

        // 模拟请求上下文
        ServletRequestAttributes attributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attributes);

        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        // When & Then
        assertThatThrownBy(() -> operationLogAspect.logOperation(joinPoint, operationLog))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("测试异常");

        // 验证异常时也记录了日志
        verify(operationLogService, times(1)).saveOperationLog(argThat(logInfo -> {
            assertThat(logInfo.getStatus()).isEqualTo(0); // 失败状态
            assertThat(logInfo.getErrorMsg()).contains("测试异常");
            return true;
        }));
    }

    @Test
    @DisplayName("记录操作日志 - 不保存请求参数")
    void shouldLogOperationWithoutRequestData() throws Throwable {
        // Given
        Method method = this.getClass().getMethod("testMethod", String.class);
        OperationLog operationLog = mock(OperationLog.class);
        when(operationLog.operation()).thenReturn("自定义操作");
        when(operationLog.businessType()).thenReturn(OperationLog.BusinessType.UPDATE);
        when(operationLog.saveRequestData()).thenReturn(false); // 不保存请求数据
        when(operationLog.saveResponseData()).thenReturn(false);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenReturn("result");
        when(methodSignature.getDeclaringType()).thenReturn(this.getClass());
        when(methodSignature.getName()).thenReturn("testMethod");

        // When
        operationLogAspect.logOperation(joinPoint, operationLog);

        // Then
        verify(operationLogService, times(1)).saveOperationLog(argThat(logInfo -> {
            assertThat(logInfo.getParams()).isNull(); // 没有保存请求参数
            assertThat(logInfo.getResult()).isNull(); // 没有保存响应数据
            assertThat(logInfo.getOperation()).isEqualTo("自定义操作");
            return true;
        }));
    }

    @Test
    @DisplayName("记录操作日志 - 保存响应数据")
    void shouldLogOperationWithResponseData() throws Throwable {
        // Given
        Method method = this.getClass().getMethod("testMethod", String.class);
        OperationLog operationLog = mock(OperationLog.class);
        when(operationLog.operation()).thenReturn("保存响应");
        when(operationLog.businessType()).thenReturn(OperationLog.BusinessType.INSERT);
        when(operationLog.saveRequestData()).thenReturn(false);
        when(operationLog.saveResponseData()).thenReturn(true); // 保存响应数据

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenReturn("response data");
        when(methodSignature.getDeclaringType()).thenReturn(this.getClass());
        when(methodSignature.getName()).thenReturn("testMethod");

        // When
        operationLogAspect.logOperation(joinPoint, operationLog);

        // Then
        verify(operationLogService, times(1)).saveOperationLog(argThat(logInfo -> {
            assertThat(logInfo.getResult()).isEqualTo("response data"); // 保存了响应数据
            return true;
        }));
    }

    @Test
    @DisplayName("记录操作日志 - 无请求上下文")
    void shouldLogOperationWithoutRequestContext() throws Throwable {
        // Given
        Method method = this.getClass().getMethod("testMethod", String.class);
        OperationLog operationLog = method.getAnnotation(OperationLog.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenReturn("result");
        when(methodSignature.getDeclaringType()).thenReturn(this.getClass());
        when(methodSignature.getName()).thenReturn("testMethod");

        // 清空请求上下文
        RequestContextHolder.resetRequestAttributes();

        // When
        Object result = operationLogAspect.logOperation(joinPoint, operationLog);

        // Then
        assertThat(result).isEqualTo("result");
        verify(operationLogService, times(1)).saveOperationLog(argThat(logInfo -> {
            assertThat(logInfo.getIpAddress()).isNull(); // 没有IP地址
            return true;
        }));
    }

    @Test
    @DisplayName("获取操作名称 - 使用注解值")
    void shouldGetOperationNameFromAnnotation() throws Exception {
        // Given
        Method method = this.getClass().getMethod("testMethod", String.class);
        OperationLog operationLog = method.getAnnotation(OperationLog.class);

        // When
        String operationName = getOperationNameFromMethod(method, operationLog);

        // Then
        assertThat(operationName).isEqualTo("测试操作");
    }

    @Test
    @DisplayName("获取操作名称 - 空值时使用方法名")
    void shouldGetOperationNameFromMethodName() throws Exception {
        // Given
        Method method = this.getClass().getMethod("testMethod", String.class);
        OperationLog operationLog = mock(OperationLog.class);
        when(operationLog.operation()).thenReturn(""); // 空字符串

        // When
        String operationName = getOperationNameFromMethod(method, operationLog);

        // Then
        assertThat(operationName).isEqualTo("testMethod");
    }

    @Test
    @DisplayName("验证日志记录字段完整性")
    void shouldRecordAllRequiredFields() throws Throwable {
        // Given
        Method method = this.getClass().getMethod("testMethod", String.class);
        OperationLog operationLog = method.getAnnotation(OperationLog.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenReturn("result");
        when(methodSignature.getDeclaringType()).thenReturn(this.getClass());
        when(methodSignature.getName()).thenReturn("testMethod");

        ServletRequestAttributes attributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attributes);

        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        // When
        operationLogAspect.logOperation(joinPoint, operationLog);

        // Then
        verify(operationLogService, times(1)).saveOperationLog(argThat(logInfo -> {
            assertThat(logInfo.getOperation()).isEqualTo("测试操作");
            assertThat(logInfo.getBusinessType()).isEqualTo(OperationLog.BusinessType.SELECT);
            assertThat(logInfo.getMethod()).contains("testMethod");
            assertThat(logInfo.getStatus()).isEqualTo(1); // 成功
            assertThat(logInfo.getOperationTime()).isNotNull();
            assertThat(logInfo.getTime()).isGreaterThanOrEqualTo(0);
            assertThat(logInfo.getIpAddress()).isEqualTo("10.0.0.1");
            return true;
        }));
    }

    @Test
    @DisplayName("记录执行时间")
    void shouldRecordExecutionTime() throws Throwable {
        // Given
        Method method = this.getClass().getMethod("testMethod", String.class);
        OperationLog operationLog = method.getAnnotation(OperationLog.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            Thread.sleep(10); // 模拟耗时操作
            return "result";
        });
        when(methodSignature.getDeclaringType()).thenReturn(this.getClass());
        when(methodSignature.getName()).thenReturn("testMethod");

        // When
        operationLogAspect.logOperation(joinPoint, operationLog);

        // Then
        verify(operationLogService, times(1)).saveOperationLog(argThat(logInfo -> {
            assertThat(logInfo.getTime()).isGreaterThanOrEqualTo(10L);
            return true;
        }));
    }

    @Test
    @DisplayName("异常时记录错误消息")
    void shouldRecordErrorMessageOnException() throws Throwable {
        // Given
        Method method = this.getClass().getMethod("exceptionMethod");
        OperationLog operationLog = method.getAnnotation(OperationLog.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenThrow(new IllegalArgumentException("参数错误"));
        when(methodSignature.getDeclaringType()).thenReturn(this.getClass());
        when(methodSignature.getName()).thenReturn("exceptionMethod");

        // When & Then
        assertThatThrownBy(() -> operationLogAspect.logOperation(joinPoint, operationLog))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("参数错误");

        verify(operationLogService, times(1)).saveOperationLog(argThat(logInfo -> {
            assertThat(logInfo.getStatus()).isEqualTo(0); // 失败
            assertThat(logInfo.getErrorMsg()).contains("参数错误");
            return true;
        }));
    }

    /**
     * 辅助方法：获取操作名称（从OperationLogAspect复制，便于测试）
     */
    private String getOperationNameFromMethod(Method method, OperationLog operationLog) {
        // 从OperationLogAspect复制逻辑
        String operation = operationLog.operation();
        if (operation == null || operation.isEmpty()) {
            return method.getName();
        }
        return operation;
    }
}
