package com.basebackend.common.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TenantContextHolder 单元测试
 */
class TenantContextHolderTest {

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Nested
    @DisplayName("ignoreTenant")
    class IgnoreTenantTests {

        @Test
        @DisplayName("有备份上下文时应恢复原上下文")
        void shouldRestoreBackupContextAfterRunnable() {
            TenantContextHolder.set(new TestTenantContext(1L));

            TenantContextHolder.ignoreTenant(() -> {
                assertThat(TenantContextHolder.get()).isNull();
                TenantContextHolder.set(new TestTenantContext(2L));
            });

            assertThat(TenantContextHolder.getTenantId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("无备份上下文时应清除执行中设置的上下文")
        void shouldClearContextWhenNoBackupAfterRunnable() {
            TenantContextHolder.ignoreTenant(() -> {
                TenantContextHolder.set(new TestTenantContext(3L));
                assertThat(TenantContextHolder.getTenantId()).isEqualTo(3L);
            });

            assertThat(TenantContextHolder.get()).isNull();
        }

        @Test
        @DisplayName("Supplier 版本应在返回后恢复上下文")
        void shouldRestoreBackupContextAfterSupplier() {
            TenantContextHolder.set(new TestTenantContext(10L));

            String result = TenantContextHolder.ignoreTenant(() -> {
                assertThat(TenantContextHolder.get()).isNull();
                TenantContextHolder.set(new TestTenantContext(20L));
                return "ok";
            });

            assertThat(result).isEqualTo("ok");
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("action 为空应抛出空指针异常")
        void shouldThrowWhenRunnableIsNull() {
            assertThatThrownBy(() -> TenantContextHolder.ignoreTenant((Runnable) null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("action");
        }

        @Test
        @DisplayName("supplier 为空应抛出空指针异常")
        void shouldThrowWhenSupplierIsNull() {
            assertThatThrownBy(() -> TenantContextHolder.ignoreTenant((java.util.function.Supplier<?>) null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("supplier");
        }
    }

    private static final class TestTenantContext implements TenantContextInfo {
        private static final long serialVersionUID = 1L;

        private final Long tenantId;

        private TestTenantContext(Long tenantId) {
            this.tenantId = tenantId;
        }

        @Override
        public Long getTenantId() {
            return tenantId;
        }
    }
}
