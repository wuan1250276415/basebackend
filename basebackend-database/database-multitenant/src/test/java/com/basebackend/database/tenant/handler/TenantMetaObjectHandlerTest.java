package com.basebackend.database.tenant.handler;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.exception.TenantContextException;
import com.basebackend.database.tenant.context.TenantContext;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TenantMetaObjectHandler 测试")
class TenantMetaObjectHandlerTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("默认 tenant_id 配置可填充到实体 tenantId 属性")
    void shouldAutoFillTenantIdWhenUsingDefaultTenantColumn() {
        DatabaseEnhancedProperties properties = new DatabaseEnhancedProperties();
        properties.getMultiTenancy().setEnabled(true);
        TenantMetaObjectHandler handler = new TestableTenantMetaObjectHandler(properties);

        TenantEntity entity = new TenantEntity();
        TenantContext.setTenantId("tenant_001");

        handler.insertFill(SystemMetaObject.forObject(entity));

        assertThat(entity.getTenantId()).isEqualTo("tenant_001");
    }

    @Test
    @DisplayName("实体 tenantId 与上下文不一致时抛出异常")
    void shouldThrowWhenEntityTenantIdMismatchContext() {
        DatabaseEnhancedProperties properties = new DatabaseEnhancedProperties();
        properties.getMultiTenancy().setEnabled(true);
        TenantMetaObjectHandler handler = new TestableTenantMetaObjectHandler(properties);

        TenantEntity entity = new TenantEntity();
        entity.setTenantId("tenant_002");
        TenantContext.setTenantId("tenant_001");

        assertThatThrownBy(() -> handler.insertFill(SystemMetaObject.forObject(entity)))
                .isInstanceOf(TenantContextException.class)
                .hasMessageContaining("does not match context tenant ID");
    }

    private static class TenantEntity {
        private String tenantId;

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }
    }

    private static class TestableTenantMetaObjectHandler extends TenantMetaObjectHandler {

        private TestableTenantMetaObjectHandler(DatabaseEnhancedProperties properties) {
            super(properties);
        }

        @Override
        public <T, E extends T> TenantMetaObjectHandler strictInsertFill(MetaObject metaObject, String fieldName,
                                                                         Class<T> fieldType, E fieldVal) {
            if (metaObject.getValue(fieldName) == null) {
                metaObject.setValue(fieldName, fieldVal);
            }
            return this;
        }
    }
}
