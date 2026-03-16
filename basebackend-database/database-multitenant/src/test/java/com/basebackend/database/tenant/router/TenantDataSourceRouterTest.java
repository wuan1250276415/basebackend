package com.basebackend.database.tenant.router;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.exception.TenantContextException;
import com.basebackend.database.tenant.context.TenantContext;
import com.basebackend.database.tenant.entity.TenantConfig;
import com.basebackend.database.tenant.service.TenantConfigService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("TenantDataSourceRouter 测试")
class TenantDataSourceRouterTest {

    private TenantConfigService tenantConfigService;
    private TestableTenantDataSourceRouter router;

    @BeforeEach
    void setUp() {
        tenantConfigService = mock(TenantConfigService.class);

        DatabaseEnhancedProperties properties = new DatabaseEnhancedProperties();
        properties.getMultiTenancy().setEnabled(true);
        properties.getDynamicDatasource().setPrimary("master");

        router = new TestableTenantDataSourceRouter(tenantConfigService, properties);
        router.setDefaultTargetDataSource(mock(DataSource.class));
        router.init();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("SEPARATE_DB 路由键使用 dataSourceKey")
    void shouldUseDataSourceKeyAsLookupKeyInSeparateDbMode() {
        TenantConfig tenantConfig = new TenantConfig();
        tenantConfig.setTenantId("tenant_001");
        tenantConfig.setStatus("ACTIVE");
        tenantConfig.setIsolationMode("SEPARATE_DB");
        tenantConfig.setDataSourceKey("tenant_ds_001");

        when(tenantConfigService.getByTenantId("tenant_001")).thenReturn(tenantConfig);
        TenantContext.setTenantId("tenant_001");

        assertThat(router.currentLookupKey()).isEqualTo("tenant_ds_001");
    }

    @Test
    @DisplayName("租户与路由键映射一致时可正确增删查")
    void shouldRegisterAndRemoveDataSourceByTenantAndLookupKey() {
        DataSource dataSource = mock(DataSource.class);
        router.addTenantDataSource("tenant_001", "tenant_ds_001", dataSource);

        assertThat(router.getTenantDataSource("tenant_001")).isSameAs(dataSource);
        assertThat(router.hasTenantDataSource("tenant_001")).isTrue();

        router.removeTenantDataSource("tenant_001");
        assertThat(router.getTenantDataSource("tenant_001")).isNull();
        assertThat(router.hasTenantDataSource("tenant_001")).isFalse();
    }

    @Test
    @DisplayName("租户未激活时拒绝路由")
    void shouldRejectInactiveTenant() {
        TenantConfig tenantConfig = new TenantConfig();
        tenantConfig.setTenantId("tenant_001");
        tenantConfig.setStatus("INACTIVE");
        tenantConfig.setIsolationMode("SEPARATE_DB");
        tenantConfig.setDataSourceKey("tenant_ds_001");

        when(tenantConfigService.getByTenantId("tenant_001")).thenReturn(tenantConfig);
        TenantContext.setTenantId("tenant_001");

        assertThatThrownBy(() -> router.currentLookupKey())
                .isInstanceOf(TenantContextException.class)
                .hasMessageContaining("Tenant is not active");
    }

    static class TestableTenantDataSourceRouter extends TenantDataSourceRouter {
        TestableTenantDataSourceRouter(TenantConfigService tenantConfigService, DatabaseEnhancedProperties properties) {
            super(tenantConfigService, properties);
        }

        Object currentLookupKey() {
            return super.determineCurrentLookupKey();
        }
    }
}

