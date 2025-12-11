package com.basebackend.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.file.audit.AuditAction;
import com.basebackend.file.audit.FileShareAuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件分享审计日志 Mapper 接口
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-28
 */
@Mapper
public interface FileShareAuditLogMapper extends BaseMapper<FileShareAuditLog> {

    /**
     * 根据分享码查询审计日志
     *
     * @param shareCode 分享码
     * @param limit 限制返回数量
     * @return 审计日志列表
     */
    List<FileShareAuditLog> selectByShareCode(
            @Param("shareCode") String shareCode,
            @Param("limit") Integer limit
    );

    /**
     * 根据用户ID查询审计日志
     *
     * @param userId 用户ID
     * @param limit 限制返回数量
     * @return 审计日志列表
     */
    List<FileShareAuditLog> selectByUserId(
            @Param("userId") String userId,
            @Param("limit") Integer limit
    );

    /**
     * 根据操作类型查询审计日志
     *
     * @param action 操作类型
     * @param limit 限制返回数量
     * @return 审计日志列表
     */
    List<FileShareAuditLog> selectByAction(
            @Param("action") AuditAction action,
            @Param("limit") Integer limit
    );

    /**
     * 查询失败的审计日志（用于安全告警）
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param action 特定操作类型（可选）
     * @param userId 特定用户ID（可选）
     * @param shareCode 特定分享码（可选）
     * @return 失败的审计日志列表
     */
    List<FileShareAuditLog> selectFailedLogs(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("action") AuditAction action,
            @Param("userId") String userId,
            @Param("shareCode") String shareCode
    );
}
