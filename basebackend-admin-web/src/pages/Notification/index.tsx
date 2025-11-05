import { useState, useEffect } from 'react';
import {
  Card,
  Table,
  Button,
  Space,
  Tag,
  Input,
  Select,
  Popconfirm,
  message,
  Typography,
  Row,
  Col,
  Statistic,
  Tooltip,
  Checkbox,
  Dropdown,
  Empty,
} from 'antd';
import type { TableProps, MenuProps } from 'antd';
import {
  ReloadOutlined,
  DeleteOutlined,
  CheckOutlined,
  SearchOutlined,
  BellOutlined,
  DownOutlined,
} from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from 'react-query';
import { useTranslation } from 'react-i18next';
import {
  getNotificationList,
  markAsRead,
  markAllAsRead,
  deleteNotification,
  batchDeleteNotifications,
  type UserNotificationDTO,
  type NotificationQueryParams,
  type NotificationType,
  type NotificationLevel,
} from '@/api/notification';
import { useNotificationStore } from '@/stores/notification';
import {
  formatNotificationTime,
  getNotificationLevelColor,
  getNotificationTypeColor,
} from '@/utils/notification';
import styles from './index.module.scss';

const { Search } = Input;
const { Text, Title } = Typography;

/**
 * 通知中心页面
 */
const NotificationCenter = () => {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const { setUnreadCount } = useNotificationStore();

  // 筛选参数
  const [filters, setFilters] = useState<NotificationQueryParams>({
    page: 1,
    pageSize: 10,
    type: 'all',
    level: 'all',
    isRead: 'all',
    keyword: '',
  });

  // 选中的通知 ID
  const [selectedIds, setSelectedIds] = useState<number[]>([]);

  // 获取通知列表
  const {
    data: notificationData,
    isLoading,
    refetch,
  } = useQuery(
    ['notification-center-list', filters],
    () => getNotificationList(filters),
    {
      onSuccess: (data) => {
        // 更新未读数量
        const unreadCount = data.records.filter((n) => n.isRead === 0).length;
        setUnreadCount(unreadCount);
      },
    }
  );

  const notifications = notificationData?.records || [];
  const total = notificationData?.total || 0;

  // 统计数据
  const stats = {
    total: total,
    unread: notifications.filter((n) => n.isRead === 0).length,
    system: notifications.filter((n) => n.type === 'system').length,
    announcement: notifications.filter((n) => n.type === 'announcement').length,
  };

  // 标记已读
  const markAsReadMutation = useMutation(markAsRead, {
    onSuccess: () => {
      message.success('标记成功');
      refetch();
      queryClient.invalidateQueries('notification-unread-count');
    },
  });

  // 批量标记已读
  const markAllAsReadMutation = useMutation(markAllAsRead, {
    onSuccess: () => {
      message.success('批量标记成功');
      setSelectedIds([]);
      refetch();
      queryClient.invalidateQueries('notification-unread-count');
    },
  });

  // 删除通知
  const deleteNotificationMutation = useMutation(deleteNotification, {
    onSuccess: () => {
      message.success('删除成功');
      refetch();
      queryClient.invalidateQueries('notification-unread-count');
    },
  });

  // 批量删除
  const batchDeleteMutation = useMutation(batchDeleteNotifications, {
    onSuccess: () => {
      message.success('批量删除成功');
      setSelectedIds([]);
      refetch();
      queryClient.invalidateQueries('notification-unread-count');
    },
  });

  // 处理筛选变化
  const handleFilterChange = (key: keyof NotificationQueryParams, value: any) => {
    setFilters((prev) => ({
      ...prev,
      [key]: value,
      page: key === 'page' ? value : 1, // 筛选条件变化时重置到第一页
    }));
  };

  // 处理搜索
  const handleSearch = (value: string) => {
    handleFilterChange('keyword', value);
  };

  // 处理通知点击
  const handleNotificationClick = (notification: UserNotificationDTO) => {
    if (notification.isRead === 0) {
      markAsReadMutation.mutate(notification.id);
    }
    if (notification.linkUrl) {
      window.location.href = notification.linkUrl;
    }
  };

  // 批量操作菜单
  const batchActionMenuItems: MenuProps['items'] = [
    {
      key: 'mark-read',
      icon: <CheckOutlined />,
      label: '标记已读',
      disabled: selectedIds.length === 0,
      onClick: () => {
        markAllAsReadMutation.mutate(selectedIds);
      },
    },
    {
      key: 'delete',
      icon: <DeleteOutlined />,
      label: '批量删除',
      danger: true,
      disabled: selectedIds.length === 0,
      onClick: () => {
        batchDeleteMutation.mutate(selectedIds);
      },
    },
  ];

  // 表格列定义
  const columns: TableProps<UserNotificationDTO>['columns'] = [
    {
      title: '通知内容',
      key: 'content',
      render: (_, record) => (
        <div
          className={styles.notificationContent}
          onClick={() => handleNotificationClick(record)}
        >
          <div className={styles.contentHeader}>
            <Space>
              <div
                className={styles.levelIndicator}
                style={{ backgroundColor: getNotificationLevelColor(record.level) }}
              />
              <Text strong>{record.title}</Text>
              {record.isRead === 0 && (
                <Tag color="red" style={{ marginLeft: 8 }}>
                  未读
                </Tag>
              )}
            </Space>
          </div>
          <div className={styles.contentBody}>
            <Text type="secondary">{record.content}</Text>
          </div>
        </div>
      ),
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      width: 100,
      render: (type: NotificationType) => (
        <Tag color={getNotificationTypeColor(type)}>
          {type === 'system' && '系统'}
          {type === 'announcement' && '公告'}
          {type === 'reminder' && '提醒'}
        </Tag>
      ),
    },
    {
      title: '级别',
      dataIndex: 'level',
      key: 'level',
      width: 100,
      render: (level: NotificationLevel) => (
        <Tag color={getNotificationLevelColor(level)}>
          {level === 'info' && '信息'}
          {level === 'success' && '成功'}
          {level === 'warning' && '警告'}
          {level === 'error' && '错误'}
        </Tag>
      ),
    },
    {
      title: '时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 150,
      render: (time: string) => (
        <Tooltip title={time}>
          <Text type="secondary">{formatNotificationTime(time)}</Text>
        </Tooltip>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          {record.isRead === 0 && (
            <Button
              type="link"
              size="small"
              icon={<CheckOutlined />}
              onClick={() => markAsReadMutation.mutate(record.id)}
              loading={markAsReadMutation.isLoading}
            >
              标记已读
            </Button>
          )}
          <Popconfirm
            title="确定删除此通知吗？"
            onConfirm={() => deleteNotificationMutation.mutate(record.id)}
          >
            <Button
              type="link"
              size="small"
              danger
              icon={<DeleteOutlined />}
              loading={deleteNotificationMutation.isLoading}
            >
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  // 行选择配置
  const rowSelection = {
    selectedRowKeys: selectedIds,
    onChange: (selectedRowKeys: React.Key[]) => {
      setSelectedIds(selectedRowKeys as number[]);
    },
  };

  return (
    <div className={styles.notificationCenter}>
      {/* 统计卡片 */}
      <Row gutter={16} className={styles.statistics}>
        <Col span={6}>
          <Card>
            <Statistic
              title="总通知"
              value={stats.total}
              prefix={<BellOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="未读通知"
              value={stats.unread}
              valueStyle={{ color: '#ff4d4f' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="系统通知"
              value={stats.system}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="公告通知"
              value={stats.announcement}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
      </Row>

      {/* 主卡片 */}
      <Card
        title={
          <Space>
            <BellOutlined />
            <span>通知中心</span>
          </Space>
        }
        extra={
          <Button
            type="text"
            icon={<ReloadOutlined />}
            onClick={() => refetch()}
            loading={isLoading}
          >
            刷新
          </Button>
        }
      >
        {/* 筛选区域 */}
        <div className={styles.filterSection}>
          <Space wrap>
            <Select
              style={{ width: 120 }}
              value={filters.type}
              onChange={(value) => handleFilterChange('type', value)}
              options={[
                { label: '全部类型', value: 'all' },
                { label: '系统通知', value: 'system' },
                { label: '公告通知', value: 'announcement' },
                { label: '提醒通知', value: 'reminder' },
              ]}
            />
            <Select
              style={{ width: 120 }}
              value={filters.level}
              onChange={(value) => handleFilterChange('level', value)}
              options={[
                { label: '全部级别', value: 'all' },
                { label: '信息', value: 'info' },
                { label: '成功', value: 'success' },
                { label: '警告', value: 'warning' },
                { label: '错误', value: 'error' },
              ]}
            />
            <Select
              style={{ width: 120 }}
              value={filters.isRead}
              onChange={(value) => handleFilterChange('isRead', value)}
              options={[
                { label: '全部状态', value: 'all' },
                { label: '未读', value: 0 },
                { label: '已读', value: 1 },
              ]}
            />
            <Search
              placeholder="搜索通知标题或内容"
              allowClear
              style={{ width: 250 }}
              onSearch={handleSearch}
              enterButton={<SearchOutlined />}
            />
          </Space>

          {/* 批量操作 */}
          <Space>
            {selectedIds.length > 0 && (
              <Text type="secondary">已选择 {selectedIds.length} 项</Text>
            )}
            <Dropdown menu={{ items: batchActionMenuItems }} disabled={selectedIds.length === 0}>
              <Button>
                批量操作 <DownOutlined />
              </Button>
            </Dropdown>
          </Space>
        </div>

        {/* 通知列表 */}
        <Table
          rowKey="id"
          columns={columns}
          dataSource={notifications}
          loading={isLoading}
          rowSelection={rowSelection}
          pagination={{
            current: filters.page,
            pageSize: filters.pageSize,
            total: total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => {
              handleFilterChange('page', page);
              handleFilterChange('pageSize', pageSize);
            },
          }}
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="暂无通知"
              />
            ),
          }}
          className={styles.notificationTable}
        />
      </Card>
    </div>
  );
};

export default NotificationCenter;
