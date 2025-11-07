package com.basebackend.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.admin.dto.notification.CreateNotificationDTO;
import com.basebackend.admin.dto.notification.NotificationQueryDTO;
import com.basebackend.admin.dto.notification.UserNotificationDTO;

import java.util.List;

/**
 * 通知服务接口
 *
 * @author Claude Code
 * @since 2025-10-30
 */
public interface NotificationService {

    /**
     * 发送邮件通知
     *
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容（HTML格式）
     */
    void sendEmailNotification(String to, String subject, String content);

    /**
     * 使用模板发送邮件
     *
     * @param to 收件人邮箱
     * @param templateCode 模板编码
     * @param variables 模板变量
     */
    void sendEmailByTemplate(String to, String templateCode, Object variables);

    /**
     * 创建系统通知
     *
     * @param dto 创建通知请求
     */
    void createSystemNotification(CreateNotificationDTO dto);

    /**
     * 获取当前用户的通知列表
     *
     * @param limit 限制数量
     * @return 通知列表
     */
    List<UserNotificationDTO> getCurrentUserNotifications(Integer limit);

    /**
     * 分页查询当前用户通知列表
     *
     * @param queryDTO 查询参数
     * @return 分页结果
     */
    Page<UserNotificationDTO> getNotificationPage(NotificationQueryDTO queryDTO);

    /**
     * 获取当前用户未读通知数量
     *
     * @return 未读数量
     */
    Long getUnreadCount();

    /**
     * 标记通知为已读
     *
     * @param notificationId 通知ID
     */
    void markAsRead(Long notificationId);

    /**
     * 批量标记已读
     *
     * @param notificationIds 通知ID列表
     */
    void markAllAsRead(List<Long> notificationIds);

    /**
     * 删除通知
     *
     * @param notificationId 通知ID
     */
    void deleteNotification(Long notificationId);

    /**
     * 批量删除通知
     *
     * @param notificationIds 通知ID列表
     */
    void batchDeleteNotifications(List<Long> notificationIds);
}
