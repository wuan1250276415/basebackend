import { useState, useEffect } from 'react';
import { Badge, Dropdown, Button, List, Typography, Space, Empty, Spin } from 'antd';
import { BellOutlined, CheckOutlined, DeleteOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useQuery, useMutation, useQueryClient } from 'react-query';
import { getNotifications, getUnreadCount, markAsRead, markAllAsRead } from '@/api/notification';
import type { UserNotificationDTO } from '@/api/notification';
import styles from './NotificationBell.module.scss';

const { Text, Paragraph } = Typography;

/**
 * 通知铃铛组件
 */
const NotificationBell = () => {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);

  // 获取未读数量
  const { data: unreadCount = 0 } = useQuery(
    'unreadCount',
    getUnreadCount,
    {
      refetchInterval: 30000, // 每30秒刷新
    }
  );

  // 获取通知列表
  const { data: notifications = [], isLoading } = useQuery(
    'notifications',
    () => getNotifications(10),
    {
      enabled: open, // 只在打开时加载
    }
  );

  // 标记已读
  const markAsReadMutation = useMutation(markAsRead, {
    onSuccess: () => {
      queryClient.invalidateQueries('notifications');
      queryClient.invalidateQueries('unreadCount');
    },
  });

  // 批量标记已读
  const markAllAsReadMutation = useMutation(markAllAsRead, {
    onSuccess: () => {
      queryClient.invalidateQueries('notifications');
      queryClient.invalidateQueries('unreadCount');
    },
  });

  // 处理通知点击
  const handleNotificationClick = (notification: UserNotificationDTO) => {
    if (notification.isRead === 0) {
      markAsReadMutation.mutate(notification.id);
    }
    if (notification.linkUrl) {
      window.location.href = notification.linkUrl;
    }
  };

  // 标记全部已读
  const handleMarkAllAsRead = () => {
    const unreadIds = notifications
      .filter((n) => n.isRead === 0)
      .map((n) => n.id);
    if (unreadIds.length > 0) {
      markAllAsReadMutation.mutate(unreadIds);
    }
  };

  // 获取通知级别对应的颜色
  const getLevelColor = (level: string) => {
    switch (level) {
      case 'error':
        return '#ff4d4f';
      case 'warning':
        return '#faad14';
      case 'success':
        return '#52c41a';
      default:
        return '#1890ff';
    }
  };

  // 下拉菜单内容
  const dropdownContent = (
    <div className={styles.notificationDropdown}>
      <div className={styles.header}>
        <Text strong>{t('notification.title')}</Text>
        {unreadCount > 0 && (
          <Button
            type="link"
            size="small"
            onClick={handleMarkAllAsRead}
            loading={markAllAsReadMutation.isLoading}
          >
            {t('notification.markAllAsRead')}
          </Button>
        )}
      </div>
      <div className={styles.content}>
        {isLoading ? (
          <div className={styles.loading}>
            <Spin />
          </div>
        ) : notifications.length === 0 ? (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description={t('notification.noNotification')}
          />
        ) : (
          <List
            dataSource={notifications}
            renderItem={(item) => (
              <List.Item
                className={`${styles.notificationItem} ${
                  item.isRead === 0 ? styles.unread : ''
                }`}
                onClick={() => handleNotificationClick(item)}
              >
                <div className={styles.itemContent}>
                  <div className={styles.itemHeader}>
                    <Space>
                      <div
                        className={styles.levelIndicator}
                        style={{ backgroundColor: getLevelColor(item.level) }}
                      />
                      <Text strong>{item.title}</Text>
                    </Space>
                    <Text type="secondary" className={styles.time}>
                      {new Date(item.createTime).toLocaleString()}
                    </Text>
                  </div>
                  <Paragraph
                    ellipsis={{ rows: 2 }}
                    className={styles.itemDesc}
                  >
                    {item.content}
                  </Paragraph>
                </div>
              </List.Item>
            )}
          />
        )}
      </div>
      {notifications.length > 0 && (
        <div className={styles.footer}>
          <Button type="link" block onClick={() => setOpen(false)}>
            {t('notification.viewDetail')}
          </Button>
        </div>
      )}
    </div>
  );

  return (
    <Dropdown
      dropdownRender={() => dropdownContent}
      trigger={['click']}
      open={open}
      onOpenChange={setOpen}
      placement="bottomRight"
    >
      <Badge count={unreadCount} overflowCount={99}>
        <Button
          type="text"
          icon={<BellOutlined style={{ fontSize: 18 }} />}
          className={styles.bellButton}
        />
      </Badge>
    </Dropdown>
  );
};

export default NotificationBell;
