package com.basebackend.scheduler.camunda.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.scheduler.camunda.entity.ProcessTemplateEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流模板 Mapper
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
@Mapper
public interface ProcessTemplateMapper extends BaseMapper<ProcessTemplateEntity> {
}
