package com.basebackend.nacos.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 配置历史记录模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigHistory implements Serializable {

    /**
     * 历史记录ID
     */
    private Long id;

    /**
     * 配置ID
     */
    private Long configId;

    /**
     * 配置Data ID
     */
    private String dataId;

    /**
     * 配置分组
     */
    private String group;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * 配置内容
     */
    private String content;

    /**
     * 配置版本
     */
    private Integer version;

    /**
     * 操作类型（create/update/delete/rollback/publish）
     */
    private String operationType;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 回滚来源版本
     */
    private Integer rollbackFrom;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * MD5值
     */
    private String md5;
}
