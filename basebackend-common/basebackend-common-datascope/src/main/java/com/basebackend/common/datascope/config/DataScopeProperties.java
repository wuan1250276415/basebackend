package com.basebackend.common.datascope.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 数据权限配置属性
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "basebackend.datascope")
public class DataScopeProperties {

    /**
     * 是否启用数据权限
     */
    private boolean enabled = true;

    /**
     * 超管是否跳过数据权限过滤
     */
    private boolean superAdminSkip = true;

    /**
     * 部门表名
     */
    private String deptTableName = "sys_dept";
}
