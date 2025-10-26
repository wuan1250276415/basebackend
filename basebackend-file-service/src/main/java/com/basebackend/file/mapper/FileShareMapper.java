package com.basebackend.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.file.entity.FileShare;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件分享Mapper
 */
@Mapper
public interface FileShareMapper extends BaseMapper<FileShare> {
}
