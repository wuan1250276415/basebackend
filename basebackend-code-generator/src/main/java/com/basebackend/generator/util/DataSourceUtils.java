package com.basebackend.generator.util;

import com.alibaba.druid.pool.DruidDataSource;
import com.basebackend.generator.entity.DatabaseType;
import com.basebackend.generator.entity.GenDataSource;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据源工具类
 */
@Slf4j
public class DataSourceUtils {

    /**
     * 创建数据源
     */
    public static DataSource createDataSource(GenDataSource config) {
        DatabaseType dbType = DatabaseType.valueOf(config.getDbType());
        String url = dbType.buildUrl(config.getHost(), config.getPort(), config.getDatabaseName());

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(config.getUsername());
        dataSource.setPassword(config.getPassword());
        dataSource.setDriverClassName(dbType.getDriverClass());
        
        // 连接池配置
        dataSource.setInitialSize(1);
        dataSource.setMinIdle(1);
        dataSource.setMaxActive(5);
        dataSource.setMaxWait(60000);
        dataSource.setTestOnBorrow(true);
        dataSource.setValidationQuery("SELECT 1");

        return dataSource;
    }

    /**
     * 测试数据源连接
     */
    public static boolean testConnection(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            log.error("数据源连接测试失败", e);
            return false;
        }
    }

    /**
     * 关闭数据源
     */
    public static void closeDataSource(DataSource dataSource) {
        if (dataSource instanceof DruidDataSource) {
            ((DruidDataSource) dataSource).close();
        }
    }
}
