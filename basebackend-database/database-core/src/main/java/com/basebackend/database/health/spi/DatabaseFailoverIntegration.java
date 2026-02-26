package com.basebackend.database.health.spi;

import javax.sql.DataSource;

/**
 * 故障转移集成接口
 * 定义在 database-core 中，由 database-failover 模块提供实现。
 * 使 HealthCheckScheduler 无需直接依赖 failover 模块的类型。
 */
public interface DatabaseFailoverIntegration {

    /**
     * 处理主库连接故障
     *
     * @param dataSource 主库数据源
     */
    void handleMasterFailure(DataSource dataSource);

    /**
     * 执行从库恢复检测
     */
    void performRecoveryCheck();
}
