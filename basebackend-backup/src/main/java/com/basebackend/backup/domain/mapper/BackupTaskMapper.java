package com.basebackend.backup.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.backup.domain.entity.BackupTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 备份任务Mapper接口
 */
@Mapper
public interface BackupTaskMapper extends BaseMapper<BackupTask> {

    /**
     * 查询启用的任务列表
     */
    List<BackupTask> selectEnabledTasks();

    /**
     * 根据数据源类型查询任务
     */
    List<BackupTask> selectByDatasourceType(@Param("datasourceType") String datasourceType);

    /**
     * 查询需要执行的任务（基于CRON调度）
     */
    List<BackupTask> selectTasksToExecute(@Param("now") String now);
}
