package com.basebackend.common.audit;

import com.basebackend.common.audit.aspect.AuditLogAspect;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditLogAspectTest {

    private final AuditEventPublisher publisher = mock(AuditEventPublisher.class);
    private final AuditLogAspect aspect = new AuditLogAspect(publisher, null);

    @Test
    void resolveDescription_plainText_returnsAsIs() throws NoSuchMethodException {
        Method method = SampleService.class.getMethod("delete", Long.class);
        String result = aspect.resolveDescription("plain text", method, new Object[]{1L});
        assertEquals("plain text", result);
    }

    @Test
    void resolveDescription_spelExpression_resolvesVariable() throws NoSuchMethodException {
        Method method = SampleService.class.getMethod("delete", Long.class);
        String result = aspect.resolveDescription("delete user #{#id}", method, new Object[]{42L});
        assertEquals("delete user 42", result);
    }

    @Test
    void resolveDescription_emptyTemplate_returnsEmpty() throws NoSuchMethodException {
        Method method = SampleService.class.getMethod("delete", Long.class);
        String result = aspect.resolveDescription("", method, new Object[]{1L});
        assertEquals("", result);
    }

    @Test
    void resolveDescription_invalidSpel_returnsTemplate() throws NoSuchMethodException {
        Method method = SampleService.class.getMethod("delete", Long.class);
        String result = aspect.resolveDescription("#{#nonexistent.bad}", method, new Object[]{1L});
        assertEquals("#{#nonexistent.bad}", result);
    }

    public static class SampleService {
        public void delete(Long id) {}
    }
}
