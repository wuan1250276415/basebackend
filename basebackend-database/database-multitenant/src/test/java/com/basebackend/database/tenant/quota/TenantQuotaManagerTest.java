package com.basebackend.database.tenant.quota;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TenantQuotaManager 测试")
class TenantQuotaManagerTest {

    private TenantQuotaManager manager;

    @BeforeEach
    void setUp() {
        manager = new TenantQuotaManager();
    }

    @Test
    @DisplayName("设置配额并检查未超限")
    void setQuotaNotExceeded() {
        manager.setQuota("t1", "users", 100);
        assertThat(manager.isExceeded("t1", "users", 50)).isFalse();
    }

    @Test
    @DisplayName("使用量超出配额")
    void exceeded() {
        manager.setQuota("t1", "users", 10);
        manager.increment("t1", "users", 8);
        assertThat(manager.isExceeded("t1", "users", 3)).isTrue();
        assertThat(manager.isExceeded("t1", "users", 2)).isFalse();
    }

    @Test
    @DisplayName("未设置配额的租户不限制")
    void noQuotaNoLimit() {
        assertThat(manager.isExceeded("unknown", "users", 999999)).isFalse();
    }

    @Test
    @DisplayName("increment 累加使用量")
    void incrementUsage() {
        manager.increment("t1", "api_calls", 10);
        manager.increment("t1", "api_calls", 5);
        assertThat(manager.getUsage("t1", "api_calls")).isEqualTo(15);
    }

    @Test
    @DisplayName("getUsage 未使用返回 0")
    void getUsageZero() {
        assertThat(manager.getUsage("t1", "storage")).isZero();
    }

    @Test
    @DisplayName("getLimit 未设置返回 -1")
    void getLimitNotSet() {
        assertThat(manager.getLimit("t1", "users")).isEqualTo(-1);
    }

    @Test
    @DisplayName("getLimit 已设置返回正确值")
    void getLimitSet() {
        manager.setQuota("t1", "users", 500);
        assertThat(manager.getLimit("t1", "users")).isEqualTo(500);
    }

    @Test
    @DisplayName("resetUsage 重置单个资源")
    void resetUsage() {
        manager.increment("t1", "api_calls", 100);
        manager.resetUsage("t1", "api_calls");
        assertThat(manager.getUsage("t1", "api_calls")).isZero();
    }

    @Test
    @DisplayName("resetAllUsage 重置所有资源")
    void resetAllUsage() {
        manager.increment("t1", "api_calls", 100);
        manager.increment("t1", "storage", 50);
        manager.resetAllUsage("t1");
        assertThat(manager.getUsage("t1", "api_calls")).isZero();
        assertThat(manager.getUsage("t1", "storage")).isZero();
    }

    @Test
    @DisplayName("不同租户配额互不影响")
    void differentTenants() {
        manager.setQuota("t1", "users", 10);
        manager.setQuota("t2", "users", 100);
        manager.increment("t1", "users", 10);

        assertThat(manager.isExceeded("t1", "users", 1)).isTrue();
        assertThat(manager.isExceeded("t2", "users", 1)).isFalse();
    }

    @Test
    @DisplayName("不同资源类型互不影响")
    void differentResources() {
        manager.setQuota("t1", "users", 10);
        manager.setQuota("t1", "storage", 1000);
        manager.increment("t1", "users", 10);

        assertThat(manager.isExceeded("t1", "users", 1)).isTrue();
        assertThat(manager.isExceeded("t1", "storage", 500)).isFalse();
    }

    @Test
    @DisplayName("resetUsage 对不存在的租户不报错")
    void resetUsageNonexistent() {
        assertThatCode(() -> manager.resetUsage("unknown", "x"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("刚好等于配额不超限，超过一个单位才超限")
    void exactlyAtLimit() {
        manager.setQuota("t1", "users", 10);
        manager.increment("t1", "users", 9);
        assertThat(manager.isExceeded("t1", "users", 1)).isFalse(); // 9+1=10, 不超
        assertThat(manager.isExceeded("t1", "users", 2)).isTrue();  // 9+2=11, 超
    }
}
