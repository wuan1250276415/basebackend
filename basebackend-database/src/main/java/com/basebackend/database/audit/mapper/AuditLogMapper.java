package com.basebackend.database.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.database.audit.entity.AuditLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 审计日志 Mapper
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {

    /**
     * 批量插入审计日志
     * 用于高并发场景下的性能优化
     * 注意：调用前需要确保每个 AuditLog 对象的 id 已经设置
     * 
     * @param auditLogs 审计日志列表
     * @return 插入的记录数
     */
    @Insert({
            "<script>",
            "INSERT INTO sys_audit_log (id, operation_type, table_name, primary_key, before_data, after_data, ",
            "changed_fields, operator_id, operator_name, operator_ip, tenant_id, operate_time) VALUES ",
            "<foreach collection='list' item='item' separator=','>",
            "(#{item.id}, #{item.operationType}, #{item.tableName}, #{item.primaryKey}, #{item.beforeData}, ",
            "#{item.afterData}, #{item.changedFields}, #{item.operatorId}, #{item.operatorName}, ",
            "#{item.operatorIp}, #{item.tenantId}, #{item.operateTime})",
            "</foreach>",
            "</script>"
    })
    int insertBatch(@Param("list") List<AuditLog> auditLogs);
}
