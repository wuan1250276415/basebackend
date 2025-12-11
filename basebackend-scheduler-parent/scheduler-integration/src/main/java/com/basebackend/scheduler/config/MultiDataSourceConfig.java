package com.basebackend.scheduler.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * 多数据源配置
 * <p>
 * 配置主数据源（Primary）用于应用业务数据，
 * 以及 PowerJob 数据源用于存储任务执行记录。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Configuration
@MapperScan(basePackages = "com.basebackend.scheduler.**.mapper", sqlSessionTemplateRef = "primarySqlSessionTemplate")
public class MultiDataSourceConfig {

    /**
     * 主数据源（Primary）
     * 用于应用业务数据
     */
    @Bean(name = "primaryDataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.primary")
    public DataSource primaryDataSource() {
        return new DruidDataSource();
    }

    /**
     * PowerJob 数据源
     * 用于存储 PowerJob 任务执行记录
     */
    @Bean(name = "powerjobDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.powerjob")
    @ConditionalOnProperty(prefix = "spring.datasource.powerjob", name = "url")
    public DataSource powerjobDataSource() {
        return new DruidDataSource();
    }

    /**
     * 主数据源 SqlSessionFactory
     */
    @Bean(name = "primarySqlSessionFactory")
    @Primary
    public SqlSessionFactory primarySqlSessionFactory(@Qualifier("primaryDataSource") DataSource dataSource)
            throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/**/*.xml"));

        // 配置 MyBatis
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setCallSettersOnNulls(true);
        bean.setConfiguration(configuration);

        return bean.getObject();
    }

    /**
     * 主数据源事务管理器
     */
    @Bean(name = "primaryTransactionManager")
    @Primary
    public DataSourceTransactionManager primaryTransactionManager(
            @Qualifier("primaryDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    /**
     * 主数据源 SqlSessionTemplate
     */
    @Bean(name = "primarySqlSessionTemplate")
    @Primary
    public SqlSessionTemplate primarySqlSessionTemplate(
            @Qualifier("primarySqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
