package com.basebackend.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.file.entity.FileMetadata;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件元数据Mapper
 */
@Mapper
public interface FileMetadataMapper extends BaseMapper<FileMetadata> {
}
