package com.basebackend.observability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.observability.entity.ServiceDependency;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 服务依赖关系Mapper
 */
@Mapper
public interface ServiceDependencyMapper extends BaseMapper<ServiceDependency> {

    /**
     * 查询指定时间范围的服务依赖
     */
    List<ServiceDependency> selectByTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 查询指定服务的上游依赖
     */
    List<ServiceDependency> selectUpstreamDependencies(@Param("serviceName") String serviceName);

    /**
     * 查询指定服务的下游依赖
     */
    List<ServiceDependency> selectDownstreamDependencies(@Param("serviceName") String serviceName);
}
