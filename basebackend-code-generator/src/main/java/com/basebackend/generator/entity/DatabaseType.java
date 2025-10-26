package com.basebackend.generator.entity;

import lombok.Getter;

/**
 * 数据库类型枚举
 */
@Getter
public enum DatabaseType {
    
    MYSQL("MySQL", "com.mysql.cj.jdbc.Driver", "jdbc:mysql://%s:%d/%s"),
    POSTGRESQL("PostgreSQL", "org.postgresql.Driver", "jdbc:postgresql://%s:%d/%s"),
    ORACLE("Oracle", "oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@%s:%d:%s");

    private final String displayName;
    private final String driverClass;
    private final String urlTemplate;

    DatabaseType(String displayName, String driverClass, String urlTemplate) {
        this.displayName = displayName;
        this.driverClass = driverClass;
        this.urlTemplate = urlTemplate;
    }

    public String buildUrl(String host, int port, String database) {
        return String.format(urlTemplate, host, port, database);
    }
}
