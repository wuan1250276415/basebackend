package com.basebackend.admin.mapper.messaging;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * 消息日志Mapper
 */
@Mapper
public interface SysMessageLogMapper {

    /**
     * 获取消息统计
     */
    @Select("""
            SELECT
                COUNT(*) as total,
                SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pending,
                SUM(CASE WHEN status = 'SENT' THEN 1 ELSE 0 END) as sent,
                SUM(CASE WHEN status = 'CONSUMED' THEN 1 ELSE 0 END) as consumed,
                SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed,
                SUM(CASE WHEN status = 'DEAD_LETTER' THEN 1 ELSE 0 END) as deadLetter
            FROM sys_message_log
            WHERE create_time >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
            """)
    Map<String, Object> getMessageStats();
}
