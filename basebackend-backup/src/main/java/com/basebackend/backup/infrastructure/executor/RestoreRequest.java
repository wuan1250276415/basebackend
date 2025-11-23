package com.basebackend.backup.infrastructure.executor;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 恢复请求对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestoreRequest {

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 备份历史ID
     */
    private Long historyId;

    /**
     * 目标时间点（PITR）
     */
    private LocalDateTime targetTime;

    /**
     * 目标数据库名（可选）
     */
    private String targetDatabase;

    /**
     * 操作者
     */
    private String operator;

    /**
     * 恢复备注
     */
    private String remark;

    /**
     * 数据源配置
     */
    private DataSourceConfig dataSourceConfig;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataSourceConfig {
        private String host;
        private int port;
        private String username;
        private String password;
        private String database;
        private String datasourceType; // mysql/postgres/redis
    }
}
