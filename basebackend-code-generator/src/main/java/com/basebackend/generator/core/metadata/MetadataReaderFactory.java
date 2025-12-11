package com.basebackend.generator.core.metadata;

import com.basebackend.generator.entity.DatabaseType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * 元数据读取器工厂
 * 
 * 根据数据库类型提供对应的元数据读取器实现
 * 使用工厂模式解耦服务层与具体读取器实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetadataReaderFactory {

    private final MySQLMetadataReader mysqlReader;
    private final PostgreSQLMetadataReader postgresqlReader;
    private final OracleMetadataReader oracleReader;

    /**
     * 元数据读取器缓存
     * 使用EnumMap提高枚举键的查询性能
     */
    private Map<DatabaseType, DatabaseMetadataReader> readerMap;

    /**
     * 根据数据库类型获取元数据读取器
     *
     * @param dbType 数据库类型枚举
     * @return 对应的元数据读取器
     */
    public DatabaseMetadataReader getReader(DatabaseType dbType) {
        if (readerMap == null) {
            initReaderMap();
        }

        DatabaseMetadataReader reader = readerMap.get(dbType);
        if (reader == null) {
            throw new IllegalArgumentException("不支持的数据库类型: " + dbType);
        }

        log.debug("获取{}元数据读取器", dbType.getDisplayName());
        return reader;
    }

    /**
     * 根据数据库类型字符串获取元数据读取器
     *
     * @param dbTypeStr 数据库类型字符串
     * @return 对应的元数据读取器
     */
    public DatabaseMetadataReader getReader(String dbTypeStr) {
        try {
            DatabaseType dbType = DatabaseType.valueOf(dbTypeStr.toUpperCase());
            return getReader(dbType);
        } catch (IllegalArgumentException e) {
            log.error("无效的数据库类型: {}", dbTypeStr);
            throw new IllegalArgumentException("不支持的数据库类型: " + dbTypeStr, e);
        }
    }

    /**
     * 检查是否支持指定的数据库类型
     *
     * @param dbType 数据库类型
     * @return true表示支持，false表示不支持
     */
    public boolean isSupported(DatabaseType dbType) {
        if (readerMap == null) {
            initReaderMap();
        }
        return readerMap.containsKey(dbType);
    }

    /**
     * 检查是否支持指定的数据库类型
     *
     * @param dbTypeStr 数据库类型字符串
     * @return true表示支持，false表示不支持
     */
    public boolean isSupported(String dbTypeStr) {
        try {
            DatabaseType dbType = DatabaseType.valueOf(dbTypeStr.toUpperCase());
            return isSupported(dbType);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 初始化读取器映射
     */
    private synchronized void initReaderMap() {
        if (readerMap == null) {
            readerMap = new EnumMap<>(DatabaseType.class);
            readerMap.put(DatabaseType.MYSQL, mysqlReader);
            readerMap.put(DatabaseType.POSTGRESQL, postgresqlReader);
            readerMap.put(DatabaseType.ORACLE, oracleReader);
            log.info("初始化元数据读取器工厂，支持 {} 种数据库类型", readerMap.size());
        }
    }
}
