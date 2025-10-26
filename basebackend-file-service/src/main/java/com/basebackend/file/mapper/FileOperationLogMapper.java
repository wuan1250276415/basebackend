package com.basebackend.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.file.entity.FileOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件操作日志Mapper
 */
@Mapper
public interface FileOperationLogMapper extends BaseMapper<FileOperationLog> {
}
