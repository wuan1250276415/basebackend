package com.basebackend.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.file.entity.FileTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件标签Mapper
 */
@Mapper
public interface FileTagMapper extends BaseMapper<FileTag> {
}
