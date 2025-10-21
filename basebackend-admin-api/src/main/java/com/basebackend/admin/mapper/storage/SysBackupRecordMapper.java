package com.basebackend.admin.mapper.storage;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.admin.entity.storage.SysBackupRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 备份记录Mapper
 *
 * @author BaseBackend
 */
@Mapper
public interface SysBackupRecordMapper extends BaseMapper<SysBackupRecord> {
}
