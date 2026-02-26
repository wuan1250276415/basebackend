package com.basebackend.database.failover.integration;

import com.basebackend.database.failover.DataSourceFailoverHandler;
import com.basebackend.database.failover.DataSourceRecoveryManager;
import com.basebackend.database.health.spi.DatabaseFailoverIntegration;

import javax.sql.DataSource;

/**
 * 故障转移集成实现
 * 桥接 database-core 的 SPI 接口与 database-failover 的具体实现
 */
public class DatabaseFailoverIntegrationImpl implements DatabaseFailoverIntegration {

    private final DataSourceFailoverHandler failoverHandler;
    private final DataSourceRecoveryManager recoveryManager;

    public DatabaseFailoverIntegrationImpl(DataSourceFailoverHandler failoverHandler,
                                           DataSourceRecoveryManager recoveryManager) {
        this.failoverHandler = failoverHandler;
        this.recoveryManager = recoveryManager;
    }

    @Override
    public void handleMasterFailure(DataSource dataSource) {
        failoverHandler.handleMasterFailure(dataSource);
    }

    @Override
    public void performRecoveryCheck() {
        recoveryManager.performRecoveryCheck();
    }
}
