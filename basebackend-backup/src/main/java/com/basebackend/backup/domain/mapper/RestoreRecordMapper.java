package com.basebackend.backup.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.backup.domain.entity.RestoreRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 恢复记录Mapper接口
 */
@Mapper
public interface RestoreRecordMapper extends BaseMapper<RestoreRecord> {

    /**
     * 查询任务的恢复记录（分页）
     */
    IPage<RestoreRecord> selectByTaskIdPage(Page<RestoreRecord> page, @Param("taskId") Long taskId);

    /**
     * 查询最近的恢复记录
     */
    List<RestoreRecord> selectRecentByTaskId(@Param("taskId") Long taskId, @Param("limit") int limit);

    /**
     * 查询成功的恢复记录
     */
    List<RestoreRecord> selectSuccessByTaskId(@Param("taskId") Long taskId);

    /**
     * 查询指定备份的恢复记录
     */
    List<RestoreRecord> selectByHistoryId(@Param("historyId") Long historyId);

    /**
     * 查询指定时间范围内的恢复记录
     */
    List<RestoreRecord> selectByTimeRange(@Param("taskId") Long taskId,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定时间范围内的恢复情况
     */
    int countByTimeRangeAndStatus(@Param("taskId") Long taskId,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime,
                                  @Param("status") String status);

    /**
     * 查询最近的PITR恢复记录
     */
    RestoreRecord selectLatestPITR(@Param("taskId") Long taskId);
}
