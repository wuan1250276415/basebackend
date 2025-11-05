import { useState, useEffect } from 'react';
import { Badge, Dropdown, Button, List, Typography, Space, Empty, Spin } from 'antd';
import { BellOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useQuery, useMutation, useQueryClient } from 'react-query';
import { useNavigate } from 'react-router-dom';
import { getNotifications, markAsRead, markAllAsRead } from '@/api/notification';
import type { UserNotificationDTO } from '@/api/notification';
import { useNotificationStore } from '@/stores/notification';
import { formatNotificationTime, getNotificationLevelColor } from '@/utils/notification';
import styles from './NotificationBell.module.scss';

const { Text, Paragraph } = Typography;

/**
 * 通知铃铛组件
 */
const NotificationBell = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [shake, setShake] = useState(false);

  const { unreadCount, setUnreadCount, decrementUnreadCount } = useNotificationStore();

  // 获取通知列表
  const { data: notifications = [], isLoading } = useQuery(
    'notification-list',
    () => getNotifications(10),
    {
      enabled: open, // 只在打开时加载
      onSuccess: (data) => {
        // 更新未读数量
        const unread = data.filter((n) => n.isRead === 0).length;
        setUnreadCount(unread);
      },
    }
  );

  // 监听未读数量变化，触发摇晃动画
  useEffect(() => {
    if (unreadCount > 0) {
      setShake(true);
      const timer = setTimeout(() => setShake(false), 2000);
      return () => clearTimeout(timer);
    }
  }, [unreadCount]);

  // 标记已读
  const markAsReadMutation = useMutation(markAsRead, {
    onSuccess: () => {
      queryClient.invalidateQueries('notification-list');
      decrementUnreadCount();
    },
  });

  // 批量标记已读
  const markAllAsReadMutation = useMutation(markAllAsRead, {
    onSuccess: () => {
      queryClient.invalidateQueries('notification-list');
      setUnreadCount(0);
    },
  });

  // 处理通知点击
  const handleNotificationClick = (notification: UserNotificationDTO) => {
    if (notification.isRead === 0) {
      markAsReadMutation.mutate(notification.id);
    }
    setOpen(false);
    if (notification.linkUrl) {
      navigate(notification.linkUrl);
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
                        style={{ backgroundColor: getNotificationLevelColor(item.level) }}
                      />
                      <Text strong>{item.title}</Text>
                    </Space>
                    <Text type="secondary" className={styles.time}>
                      {formatNotificationTime(item.createTime)}
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
          <Button
            type="link"
            block
            onClick={() => {
              setOpen(false);
              navigate('/notification/center');
            }}
          >
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
          className={`${styles.bellButton} ${shake ? styles.shake : ''}`}
        />
      </Badge>
    </Dropdown>
  );
};

export default NotificationBell;
