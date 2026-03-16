/**
 * 在线用户监控页面
 * 展示当前在线用户列表，支持按用户名搜索和强制下线操作
 * 使用 ProTable 展示在线用户数据，从 monitorApi.onlineUsers 获取
 */
import { useRef } from 'react';
import { Button, Modal, message } from 'antd';
import {
  ExclamationCircleOutlined,
  PoweroffOutlined,
} from '@ant-design/icons';
import { ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { monitorApi } from '@/api/monitorApi';
import { Permission } from '@/components/Permission';
import type { OnlineUserDTO } from '@/types';

/**
 * 在线用户监控主页面
 * 展示在线用户列表，支持强制下线操作
 */
const OnlineUserPage: React.FC = () => {
  const actionRef = useRef<ActionType>();

  /** 强制下线用户 */
  const handleForceLogout = (record: OnlineUserDTO) => {
    Modal.confirm({
      title: '确认强制下线',
      icon: <ExclamationCircleOutlined />,
      content: `确定要将用户「${record.username}」强制下线吗？`,
      okText: '确定',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        await monitorApi.forceLogout(record.token);
        message.success('已将该用户强制下线');
        actionRef.current?.reload();
      },
    });
  };

  /** 表格列定义 */
  const columns: ProColumns<OnlineUserDTO>[] = [
    {
      title: '用户名',
      dataIndex: 'username',
      ellipsis: true,
    },
    {
      title: 'IP地址',
      dataIndex: 'loginIp',
      hideInSearch: true,
      ellipsis: true,
    },
    {
      title: '登录地点',
      dataIndex: 'loginLocation',
      hideInSearch: true,
      ellipsis: true,
    },
    {
      title: '浏览器',
      dataIndex: 'browser',
      hideInSearch: true,
      ellipsis: true,
    },
    {
      title: '操作系统',
      dataIndex: 'os',
      hideInSearch: true,
      ellipsis: true,
    },
    {
      title: '登录时间',
      dataIndex: 'loginTime',
      valueType: 'dateTime',
      hideInSearch: true,
      width: 180,
    },
    {
      title: '操作',
      valueType: 'option',
      width: 100,
      render: (_, record) => (
        <Permission code="monitor:online:forceLogout">
          <Button
            type="link"
            danger
            icon={<PoweroffOutlined />}
            size="small"
            onClick={() => handleForceLogout(record)}
          >
            强制下线
          </Button>
        </Permission>
      ),
    },
  ];

  return (
    <ProTable<OnlineUserDTO>
      headerTitle="在线用户"
      actionRef={actionRef}
      rowKey="token"
      columns={columns}
      request={async (params) => {
        const { username } = params;
        const data = await monitorApi.onlineUsers(
          username ? { username } : undefined,
        );
        return {
          data,
          total: data.length,
          success: true,
        };
      }}
      pagination={false}
      search={{ labelWidth: 'auto' }}
    />
  );
};

export default OnlineUserPage;
