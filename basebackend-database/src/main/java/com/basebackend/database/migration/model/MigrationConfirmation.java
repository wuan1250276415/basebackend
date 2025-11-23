package com.basebackend.database.migration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 迁移确认请求模型
 * 用于生产环境迁移确认
 * 
 * @author basebackend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationConfirmation {

    /**
     * 确认令牌
     */
    private String confirmationToken;

    /**
     * 确认人
     */
    private String confirmedBy;

    /**
     * 确认原因/说明
     */
    private String reason;

    /**
     * 是否创建备份
     */
    private Boolean createBackup;
}
