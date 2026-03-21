package com.basebackend.generator.dto;

import com.basebackend.generator.entity.GenDataSource;

import java.time.LocalDateTime;

/**
 * 数据源展示模型。
 *
 * 对外返回时不透出解密后的数据库密码，仅暴露是否已配置密码。
 */
public record GenDataSourceView(
        Long id,
        String name,
        String dbType,
        String host,
        Integer port,
        String databaseName,
        String username,
        Boolean passwordConfigured,
        String connectionParams,
        Integer status,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        Long createBy,
        Long updateBy,
        Integer deleted
) {

    public static GenDataSourceView from(GenDataSource dataSource) {
        if (dataSource == null) {
            return null;
        }

        return new GenDataSourceView(
                dataSource.getId(),
                dataSource.getName(),
                dataSource.getDbType(),
                dataSource.getHost(),
                dataSource.getPort(),
                dataSource.getDatabaseName(),
                dataSource.getUsername(),
                dataSource.getPassword() != null && !dataSource.getPassword().isBlank(),
                dataSource.getConnectionParams(),
                dataSource.getStatus(),
                dataSource.getCreateTime(),
                dataSource.getUpdateTime(),
                dataSource.getCreateBy(),
                dataSource.getUpdateBy(),
                dataSource.getDeleted()
        );
    }
}
