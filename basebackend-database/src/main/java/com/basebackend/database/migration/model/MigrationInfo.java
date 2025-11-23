package com.basebackend.database.migration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 迁移信息模型
 * 
 * @author basebackend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationInfo {

    /**
     * 版本号
     */
    private String version;

    /**
     * 描述
     */
    private String description;

    /**
     * 脚本类型 (SQL, JAVA)
     */
    private String type;

    /**
     * 脚本路径
     */
    private String script;

    /**
     * 校验和
     */
    private Integer checksum;

    /**
     * 安装顺序
     */
    private Integer installedRank;

    /**
     * 安装时间
     */
    private LocalDateTime installedOn;

    /**
     * 执行人
     */
    private String installedBy;

    /**
     * 执行时间（毫秒）
     */
    private Integer executionTime;

    /**
     * 状态 (SUCCESS, FAILED, PENDING, etc.)
     */
    private String state;

    /**
     * 是否成功
     */
    private Boolean success;
}
