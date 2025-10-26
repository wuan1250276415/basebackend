package com.basebackend.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.file.entity.FilePermission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件权限Mapper
 */
@Mapper
public interface FilePermissionMapper extends BaseMapper<FilePermission> {
}
