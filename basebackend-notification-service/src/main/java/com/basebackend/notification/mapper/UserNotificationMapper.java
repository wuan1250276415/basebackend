package com.basebackend.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.notification.entity.UserNotification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户通知 Mapper 接口
 *
 * @author BaseBackend Team
 * @since 2025-11-14
 */
@Mapper
public interface UserNotificationMapper extends BaseMapper<UserNotification> {
}
