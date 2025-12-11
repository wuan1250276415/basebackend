package com.basebackend.scheduler.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.scheduler.persistence.entity.WorkflowInstanceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

/**
 * 工作流实例数据访问层
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Mapper
public interface WorkflowInstanceMapper extends BaseMapper<WorkflowInstanceEntity> {

    /**
     * 根据定义ID查询运行中的实例
     */
    List<WorkflowInstanceEntity> selectRunningByDefinitionId(@Param("definitionId") String definitionId);

    /**
     * 查询超时未完成的实例
     */
    List<WorkflowInstanceEntity> selectTimeoutInstances(@Param("timeoutTime") Instant timeoutTime);

    /**
     * 批量更新实例状态
     */
    int batchUpdateStatus(@Param("ids") List<String> ids,
                          @Param("status") String status,
                          @Param("endTime") Instant endTime,
                          @Param("errorMessage") String errorMessage);

    /**
     * 更新活跃节点
     */
    int updateActiveNodes(@Param("id") String id,
                          @Param("activeNodesJson") String activeNodesJson,
                          @Param("version") Long version);

    /**
     * 更新上下文
     */
    int updateContext(@Param("id") String id,
                      @Param("contextJson") String contextJson,
                      @Param("version") Long version);

    /**
     * 查询活跃实例统计
     */
    int countActiveInstances();

    /**
     * 查询最近N分钟内失败的实例
     */
    List<WorkflowInstanceEntity> selectFailedRecentMinutes(@Param("minutes") int minutes);
}
