package com.basebackend.scheduler.camunda.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.scheduler.camunda.entity.TaskCCEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务抄送 Mapper
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
@Mapper
public interface TaskCCMapper extends BaseMapper<TaskCCEntity> {
}
