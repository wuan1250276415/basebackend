package com.basebackend.common.datascope.aspect;

import com.basebackend.common.context.UserContext;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.datascope.annotation.DataScope;
import com.basebackend.common.datascope.config.DataScopeProperties;
import com.basebackend.common.datascope.context.DataScopeContext;
import com.basebackend.common.datascope.enums.DataScopeType;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * DataScopeAspect 单元测试
 */
class DataScopeAspectTest {

    private DataScopeAspect aspect;

    @BeforeEach
    void setUp() {
        DataScopeProperties properties = new DataScopeProperties();
        aspect = new DataScopeAspect(properties);

        UserContextHolder.set(UserContext.builder()
                .userId(100L)
                .deptId(10L)
                .build());
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
        DataScopeContext.clear();
    }

    @Test
    @DisplayName("类级 @DataScope 应生效")
    void shouldApplyClassLevelAnnotation() throws Throwable {
        ClassLevelScopeService target = new ClassLevelScopeService();
        ProceedingJoinPoint joinPoint = mockJoinPoint(target, "query");
        AtomicReference<String> conditionInProceed = new AtomicReference<>();

        when(joinPoint.proceed()).thenAnswer(invocation -> {
            conditionInProceed.set(DataScopeContext.get());
            return "ok";
        });

        Object result = aspect.around(joinPoint);

        assertThat(result).isEqualTo("ok");
        assertThat(conditionInProceed.get()).isEqualTo("d.dept_id = 10");
        assertThat(DataScopeContext.get()).isNull();
    }

    @Test
    @DisplayName("方法级 @DataScope 优先于类级")
    void shouldPreferMethodAnnotationOverClassAnnotation() throws Throwable {
        MixedScopeService target = new MixedScopeService();
        ProceedingJoinPoint joinPoint = mockJoinPoint(target, "query");
        AtomicReference<String> conditionInProceed = new AtomicReference<>();

        when(joinPoint.proceed()).thenAnswer(invocation -> {
            conditionInProceed.set(DataScopeContext.get());
            return "ok";
        });

        Object result = aspect.around(joinPoint);

        assertThat(result).isEqualTo("ok");
        assertThat(conditionInProceed.get()).isEqualTo("u.create_by = 100");
        assertThat(DataScopeContext.get()).isNull();
    }

    private ProceedingJoinPoint mockJoinPoint(Object target, String methodName) throws Exception {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = target.getClass().getMethod(methodName);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(target);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getDeclaringType()).thenReturn(target.getClass());

        return joinPoint;
    }

    @DataScope(type = DataScopeType.DEPT, deptAlias = "d", deptField = "dept_id")
    private static class ClassLevelScopeService {
        public String query() {
            return "ok";
        }
    }

    @DataScope(type = DataScopeType.DEPT, deptAlias = "d", deptField = "dept_id")
    private static class MixedScopeService {

        @DataScope(type = DataScopeType.SELF, userAlias = "u", userField = "create_by")
        public String query() {
            return "ok";
        }
    }
}
