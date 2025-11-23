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
 * 通知列表查询参数
 */
export interface NotificationQueryParams {
  page?: number;
  pageSize?: number;
  type?: NotificationType | 'all';
  level?: NotificationLevel | 'all';
  isRead?: number | 'all';
  keyword?: string;
}

/**
 * 通知列表响应
 */
export interface NotificationListResponse {
  records: UserNotificationDTO[];
  total: number;
  page: number;
  pageSize: number;
}

/**
 * 获取通知列表（简化版，用于铃铛下拉）
 */
export const getNotifications = (limit?: number) => {
  return request<UserNotificationDTO[]>({
    url: '/basebackend-notification-service/api/notifications',
    method: 'GET',
    params: { limit },
  });
};

/**
 * 获取通知列表（完整版，支持分页和过滤）
 */
export const getNotificationList = (params: NotificationQueryParams) => {
  return request<NotificationListResponse>({
    url: '/basebackend-notification-service/api/notifications/list',
    method: 'GET',
    params,
  });
};

/**
 * 获取未读数量
 */
export const getUnreadCount = () => {
  return request<number>({
    url: '/basebackend-notification-service/api/notifications/unread-count',
    method: 'GET',
  });
};

/**
 * 标记已读
 */
export const markAsRead = (id: number) => {
  return request<void>({
    url: `/basebackend-notification-service/api/notifications/${id}/read`,
    method: 'PUT',
  });
};

/**
 * 批量标记已读
 */
export const markAllAsRead = (ids: number[]) => {
  return request<void>({
    url: '/basebackend-notification-service/api/notifications/read-all',
    method: 'PUT',
    data: ids,
  });
};

/**
 * 删除通知
 */
export const deleteNotification = (id: number) => {
  return request<void>({
    url: `/basebackend-notification-service/api/notifications/${id}`,
    method: 'DELETE',
  });
};

/**
 * 批量删除通知
 */
export const batchDeleteNotifications = (ids: number[]) => {
  return request<void>({
    url: '/basebackend-notification-service/api/notifications/batch-delete',
    method: 'DELETE',
    data: ids,
  });
};

/**
 * 创建通知（管理员）
 */
export const createNotification = (data: CreateNotificationDTO) => {
  return request<void>({
    url: '/basebackend-notification-service/api/notifications',
    method: 'POST',
    data,
  });
};
