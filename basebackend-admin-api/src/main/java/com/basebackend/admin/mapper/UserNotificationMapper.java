package com.basebackend.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.admin.entity.UserNotification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户通知 Mapper 接口
 *
 * @author Claude Code
 * @since 2025-10-30
 */
@Mapper
public interface UserNotificationMapper extends BaseMapper<UserNotification> {
}
