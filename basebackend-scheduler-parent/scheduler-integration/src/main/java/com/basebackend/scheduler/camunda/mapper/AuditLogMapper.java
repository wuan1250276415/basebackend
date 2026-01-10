package com.basebackend.scheduler.camunda.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.scheduler.camunda.entity.AuditLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 审计日志 Mapper (Camunda 工作流专用)
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
@Mapper
@Repository("camundaAuditLogMapper")
public interface AuditLogMapper extends BaseMapper<AuditLogEntity> {
}
