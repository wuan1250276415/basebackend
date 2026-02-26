package com.basebackend.database.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.database.audit.entity.AuditLogArchive;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志归档 Mapper
 */
@Mapper
public interface AuditLogArchiveMapper extends BaseMapper<AuditLogArchive> {
}
