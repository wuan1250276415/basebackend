package com.basebackend.database.config;

import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceBuilder;
import com.basebackend.database.routing.DataSourceType;
import com.basebackend.database.routing.DynamicDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据源配置类
 * 配置主从数据源和动态数据源
 *
 * 配置说明：
 * 1. 主库（Master）：处理所有写操作和强制读主库的操作
 * 2. 从库（Slave）：处理只读查询操作
 * 3. 通过 spring.datasource.read-write-separation.enabled 控制是否启用读写分离
 *
 * 使用方式：
 * 1. 在配置文件中设置 spring.datasource.read-write-separation.enabled=true
 * 2. 配置主库：spring.datasource.master.*
 * 3. 配置从库：spring.datasource.slave.*
 *
 * @author 浮浮酱
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "spring.datasource.read-write-separation", name = "enabled", havingValue = "true")
public class DataSourceConfig {

    /**
     * 主库数据源
     */
    @Bean(name = "masterDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.master")
    public DataSource masterDataSource() {
        log.info("初始化主库数据源（Master）");
        return DruidDataSourceBuilder.create().build();
    }

    /**
     * 从库数据源
     */
    @Bean(name = "slaveDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.slave")
    public DataSource slaveDataSource() {
        log.info("初始化从库数据源（Slave）");
        return DruidDataSourceBuilder.create().build();
    }

    /**
     * 动态数据源
     * 根据上下文自动切换到主库或从库
     */
    @Primary
    @Bean(name = "dynamicDataSource")
    public DataSource dynamicDataSource(
            @Qualifier("masterDataSource") DataSource masterDataSource,
            @Qualifier("slaveDataSource") DataSource slaveDataSource) {

        log.info("初始化动态数据源");

        // 配置数据源映射
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceType.MASTER, masterDataSource);
        targetDataSources.put(DataSourceType.SLAVE, slaveDataSource);

        // 创建动态数据源
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        dynamicDataSource.setTargetDataSources(targetDataSources);
        dynamicDataSource.setDefaultTargetDataSource(masterDataSource); // 默认使用主库

        log.info("动态数据源配置完成 - 主库: {}, 从库: {}",
                DataSourceType.MASTER, DataSourceType.SLAVE);

        return dynamicDataSource;
    }
}
