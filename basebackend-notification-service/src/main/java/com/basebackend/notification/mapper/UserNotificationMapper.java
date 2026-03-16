package com.basebackend.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.notification.entity.UserNotification;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户通知 Mapper 接口
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */
@Mapper
public interface UserNotificationMapper extends BaseMapper<UserNotification> {

    /**
     * 批量插入通知（单条 SQL，性能优于逐条 INSERT）
     *
     * @param notifications 通知列表
     * @return 插入行数
     */
    @Insert("<script>INSERT INTO user_notification " +
            "(user_id, title, content, type, level, link_url, is_read, create_time) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.userId}, #{item.title}, #{item.content}, #{item.type}, #{item.level}," +
            " #{item.linkUrl}, #{item.isRead}, #{item.createTime})" +
            "</foreach></script>")
    int insertBatch(@Param("list") List<UserNotification> notifications);
}
