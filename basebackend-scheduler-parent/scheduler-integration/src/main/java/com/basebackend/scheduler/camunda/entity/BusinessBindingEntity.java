package com.basebackend.scheduler.camunda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务绑定配置实体
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scheduler_business_binding")
public class BusinessBindingEntity extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 流程标识
     */
    private String processKey;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 关联业务表
     */
    private String businessTable;

    /**
     * 详情页URL模板
     */
    private String detailUrlTemplate;

    /**
     * 回调服务名
     */
    private String callbackService;

    /**
     * 租户ID
     */
    private String tenantId;
}
