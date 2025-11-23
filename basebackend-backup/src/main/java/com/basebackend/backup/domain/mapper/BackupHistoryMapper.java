package com.basebackend.backup.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.backup.domain.entity.BackupHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 备份历史Mapper接口
 */
@Mapper
public interface BackupHistoryMapper extends BaseMapper<BackupHistory> {

    /**
     * 查询任务的历史记录（分页）
     */
    IPage<BackupHistory> selectByTaskIdPage(Page<BackupHistory> page, @Param("taskId") Long taskId);

    /**
     * 查询最近的备份记录
     */
    List<BackupHistory> selectRecentByTaskId(@Param("taskId") Long taskId, @Param("limit") int limit);

    /**
     * 查询成功的备份记录
     */
    List<BackupHistory> selectSuccessByTaskId(@Param("taskId") Long taskId);

    /**
     * 查询指定时间范围内的备份记录
     */
    List<BackupHistory> selectByTimeRange(@Param("taskId") Long taskId,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 查询最近的增量备份（用于PITR）
     */
    BackupHistory selectLatestIncrementalByBaseFullId(@Param("baseFullId") Long baseFullId);

    /**
     * 查询增量链
     */
    List<BackupHistory> selectIncrementalChain(@Param("baseFullId") Long baseFullId);

    /**
     * 统计指定时间范围内的备份情况
     */
    int countByTimeRangeAndStatus(@Param("taskId") Long taskId,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime,
                                  @Param("status") String status);

    /**
     * 查询最新的全量备份
     */
    BackupHistory selectLatestFullBackup(@Param("taskId") Long taskId);
}
