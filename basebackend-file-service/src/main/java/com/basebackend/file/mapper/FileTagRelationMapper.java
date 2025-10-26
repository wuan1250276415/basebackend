package com.basebackend.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.file.entity.FileTagRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件标签关联Mapper
 */
@Mapper
public interface FileTagRelationMapper extends BaseMapper<FileTagRelation> {
}
