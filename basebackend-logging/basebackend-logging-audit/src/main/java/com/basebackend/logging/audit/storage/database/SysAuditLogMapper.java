package com.basebackend.logging.audit.storage.database;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 审计日志 Mapper 接口
 *
 * @author basebackend team
 * @since 2025-12-10
 */
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {

    /**
     * 按时间范围查询
     */
    @Select("SELECT * FROM sys_audit_log WHERE timestamp >= #{startTime} AND timestamp <= #{endTime} ORDER BY timestamp DESC LIMIT #{limit}")
    List<SysAuditLog> selectByTimeRange(@Param("startTime") java.time.Instant startTime,
                                         @Param("endTime") java.time.Instant endTime,
                                         @Param("limit") int limit);

    /**
     * 查询存储统计信息
     */
    @Select("SELECT COUNT(*) AS total_entries, MIN(timestamp) AS oldest_entry_time, MAX(timestamp) AS newest_entry_time FROM sys_audit_log")
    Map<String, Object> selectStats();

    /**
     * 删除过期数据
     */
    @Delete("DELETE FROM sys_audit_log WHERE timestamp < #{expireTime}")
    int deleteExpired(@Param("expireTime") java.time.Instant expireTime);
}
