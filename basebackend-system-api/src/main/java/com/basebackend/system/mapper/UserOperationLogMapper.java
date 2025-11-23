package com.basebackend.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.system.entity.UserOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户操作日志 Mapper 接口
 *
 * @author Claude Code
 * @since 2025-10-30
 */
@Mapper
public interface UserOperationLogMapper extends BaseMapper<UserOperationLog> {
}
