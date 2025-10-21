package com.basebackend.admin.mapper.storage;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.admin.entity.storage.SysFileInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件信息Mapper
 *
 * @author BaseBackend
 */
@Mapper
public interface SysFileInfoMapper extends BaseMapper<SysFileInfo> {
}
