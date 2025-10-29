package com.basebackend.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.admin.entity.UserDevice;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户设备 Mapper 接口
 *
 * @author Claude Code
 * @since 2025-10-30
 */
@Mapper
public interface UserDeviceMapper extends BaseMapper<UserDevice> {
}
