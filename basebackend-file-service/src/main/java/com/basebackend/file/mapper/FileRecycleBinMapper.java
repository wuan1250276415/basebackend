package com.basebackend.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.file.entity.FileRecycleBin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 回收站Mapper
 */
@Mapper
public interface FileRecycleBinMapper extends BaseMapper<FileRecycleBin> {
}
