import request from '@/utils/request';

/**
 * 通知类型
 */
export type NotificationType = 'system' | 'announcement' | 'reminder';

/**
 * 通知级别
 */
export type NotificationLevel = 'info' | 'warning' | 'error' | 'success';

/**
 * 用户通知 DTO
 */
export interface UserNotificationDTO {
  id: number;
  userId: number;
  title: string;
  content: string;
  type: NotificationType;
  level: NotificationLevel;
  isRead: number;
  linkUrl?: string;
  extraData?: string;
  createTime: string;
  readTime?: string;
}

/**
 * 创建通知 DTO
 */
export interface CreateNotificationDTO {
  userId?: number;
  title: string;
  content: string;
  type: NotificationType;
  level: NotificationLevel;
  linkUrl?: string;
  extraData?: string;
}

/**
 * 获取通知列表
 */
export const getNotifications = (limit?: number) => {
  return request<UserNotificationDTO[]>({
    url: '/admin/notifications',
    method: 'GET',
    params: { limit },
  });
};

/**
 * 获取未读数量
 */
export const getUnreadCount = () => {
  return request<number>({
    url: '/admin/notifications/unread-count',
    method: 'GET',
  });
};

/**
 * 标记已读
 */
export const markAsRead = (id: number) => {
  return request<void>({
    url: `/admin/notifications/${id}/read`,
    method: 'PUT',
  });
};

/**
 * 批量标记已读
 */
export const markAllAsRead = (ids: number[]) => {
  return request<void>({
    url: '/admin/notifications/read-all',
    method: 'PUT',
    data: ids,
  });
};

/**
 * 删除通知
 */
export const deleteNotification = (id: number) => {
  return request<void>({
    url: `/admin/notifications/${id}`,
    method: 'DELETE',
  });
};

/**
 * 创建通知（管理员）
 */
export const createNotification = (data: CreateNotificationDTO) => {
  return request<void>({
    url: '/admin/notifications',
    method: 'POST',
    data,
  });
};
