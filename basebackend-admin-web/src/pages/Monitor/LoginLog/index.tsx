/**
 * 登录日志页面
 * 展示系统登录日志列表，支持搜索过滤、批量删除和清空操作
 * 使用 ProTable 实现服务端分页和搜索过滤
 */
import { useRef, useState } from 'react';
import { Button, Modal, Tag, message } from 'antd';
import {
  DeleteOutlined,
  ExclamationCircleOutlined,
  ClearOutlined,
} from '@ant-design/icons';
import { ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { logApi } from '@/api/logApi';
import { Permission } from '@/components/Permission';
import type { LoginLogDTO } from '@/types';

/**
 * 登录日志管理主页面
 * 展示登录日志列表，支持批量删除和清空
 */
const LoginlogPage: React.FC = () => {
  const actionRef = useRef<ActionType>();
  /** 选中的行 key 列表 */
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

  /** 批量删除登录日志 */
  const handleBatchDelete = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请选择要删除的日志');
      return;
    }
    Modal.confirm({
      title: '确认删除',
      icon: <ExclamationCircleOutlined />,
      content: `确定要删除选中的 ${selectedRowKeys.length} 条日志吗？`,
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        await logApi.batchDeleteLoginLog(selectedRowKeys as string[]);
        message.success('删除成功');
        setSelectedRowKeys([]);
        actionRef.current?.reload();
      },
    });
  };

  /** 清空所有登录日志 */
  const handleCleanAll = () => {
    Modal.confirm({
      title: '确认清空',
      icon: <ExclamationCircleOutlined />,
      content: '确定要清空所有登录日志吗？此操作不可恢复！',
      okText: '确定',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        await logApi.cleanLoginLog();
        message.success('清空成功');
        setSelectedRowKeys([]);
        actionRef.current?.reload();
      },
    });
  };

  /** 表格列定义 */
  const columns: ProColumns<LoginLogDTO>[] = [
    {
      title: '用户名',
      dataIndex: 'username',
      ellipsis: true,
    },
    {
      title: 'IP地址',
      dataIndex: 'ipAddress',
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
      width: 120,
    },
    {
      title: '操作系统',
      dataIndex: 'os',
      hideInSearch: true,
      ellipsis: true,
      width: 140,
    },
    {
      title: '状态',
      dataIndex: 'status',
      valueType: 'select',
      valueEnum: {
        1: { text: '成功', status: 'Success' },
        0: { text: '失败', status: 'Error' },
      },
      width: 100,
      render: (_, record) => (
        <Tag color={record.status === 1 ? 'success' : 'error'}>
          {record.status === 1 ? '成功' : '失败'}
        </Tag>
      ),
    },
    {
      title: '登录时间',
      dataIndex: 'loginTime',
      valueType: 'dateTime',
      hideInSearch: true,
      width: 180,
    },
    {
      title: '登录时间',
      dataIndex: 'loginTimeRange',
      valueType: 'dateRange',
      hideInTable: true,
      search: {
        transform: (value: [string, string]) => ({
          beginTime: value[0],
          endTime: value[1],
        }),
      },
    },
  ];

  return (
    <ProTable<LoginLogDTO>
      headerTitle="登录日志"
      actionRef={actionRef}
      rowKey="id"
      columns={columns}
      rowSelection={{
        selectedRowKeys,
        onChange: (keys) => setSelectedRowKeys(keys),
      }}
      request={async (params) => {
        const { current = 1, pageSize = 10, ...query } = params;
        const res = await logApi.loginLogPage({
          current,
          size: pageSize,
          ...query,
        });
        return {
          data: res.records,
          total: res.total,
          success: true,
        };
      }}
      pagination={{ defaultPageSize: 10, showSizeChanger: true }}
      search={{ labelWidth: 'auto' }}
      toolBarRender={() => [
        <Permission key="batchDelete" code="monitor:loginlog:delete">
          <Button
            danger
            icon={<DeleteOutlined />}
            disabled={selectedRowKeys.length === 0}
            onClick={handleBatchDelete}
          >
            批量删除
          </Button>
        </Permission>,
        <Permission key="clean" code="monitor:loginlog:clean">
          <Button
            danger
            icon={<ClearOutlined />}
            onClick={handleCleanAll}
          >
            清空
          </Button>
        </Permission>,
      ]}
    />
  );
};

export default LoginlogPage;
