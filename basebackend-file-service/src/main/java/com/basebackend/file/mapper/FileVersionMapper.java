package com.basebackend.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.file.entity.FileVersion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件版本Mapper
 */
@Mapper
public interface FileVersionMapper extends BaseMapper<FileVersion> {
}
