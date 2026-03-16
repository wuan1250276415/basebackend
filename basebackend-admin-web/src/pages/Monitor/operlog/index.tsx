/**
 * 操作日志页面
 * 展示系统操作日志列表，支持搜索过滤、查看详情、批量删除和清空操作
 * 使用 ProTable 实现服务端分页和搜索过滤
 */
import { useRef, useState } from 'react';
import { Button, Modal, Tag, Drawer, Descriptions, message } from 'antd';
import {
  DeleteOutlined,
  ExclamationCircleOutlined,
  ClearOutlined,
  EyeOutlined,
} from '@ant-design/icons';
import { ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { logApi } from '@/api/logApi';
import { Permission } from '@/components/Permission';
import type { OperationLogDTO } from '@/types';

/**
 * 操作日志管理主页面
 * 展示操作日志列表，支持查看详情、批量删除和清空
 */
const OperlogPage: React.FC = () => {
  const actionRef = useRef<ActionType>();
  /** 选中的行 key 列表 */
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  /** 详情抽屉可见状态 */
  const [detailOpen, setDetailOpen] = useState(false);
  /** 当前查看的日志详情 */
  const [currentLog, setCurrentLog] = useState<OperationLogDTO | null>(null);
  /** 详情加载状态 */
  const [detailLoading, setDetailLoading] = useState(false);

  /** 查看日志详情 */
  const handleViewDetail = async (record: OperationLogDTO) => {
    setDetailOpen(true);
    setDetailLoading(true);
    try {
      const detail = await logApi.logDetail(record.id);
      setCurrentLog(detail);
    } catch {
      message.error('获取日志详情失败');
    } finally {
      setDetailLoading(false);
    }
  };

  /** 批量删除操作日志 */
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
        await logApi.batchDeleteOperationLog(selectedRowKeys as string[]);
        message.success('删除成功');
        setSelectedRowKeys([]);
        actionRef.current?.reload();
      },
    });
  };

  /** 清空所有操作日志 */
  const handleCleanAll = () => {
    Modal.confirm({
      title: '确认清空',
      icon: <ExclamationCircleOutlined />,
      content: '确定要清空所有操作日志吗？此操作不可恢复！',
      okText: '确定',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        await logApi.cleanOperationLog();
        message.success('清空成功');
        setSelectedRowKeys([]);
        actionRef.current?.reload();
      },
    });
  };

  /** 表格列定义 */
  const columns: ProColumns<OperationLogDTO>[] = [
    {
      title: '操作描述',
      dataIndex: 'operation',
      ellipsis: true,
    },
    {
      title: '操作人员',
      dataIndex: 'username',
      ellipsis: true,
    },
    {
      title: '请求方法',
      dataIndex: 'method',
      hideInSearch: true,
      ellipsis: true,
      width: 200,
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
      title: '操作时间',
      dataIndex: 'operationTime',
      valueType: 'dateTime',
      hideInSearch: true,
      width: 180,
    },
    {
      title: '操作时间',
      dataIndex: 'operationTimeRange',
      valueType: 'dateRange',
      hideInTable: true,
      search: {
        transform: (value: [string, string]) => ({
          beginTime: value[0],
          endTime: value[1],
        }),
      },
    },
    {
      title: '耗时(ms)',
      dataIndex: 'time',
      hideInSearch: true,
      width: 100,
      render: (_, record) => `${record.time}ms`,
    },
    {
      title: '操作',
      valueType: 'option',
      width: 80,
      render: (_, record) => (
        <Button
          type="link"
          icon={<EyeOutlined />}
          size="small"
          onClick={() => handleViewDetail(record)}
        />
      ),
    },
  ];

  return (
    <>
      <ProTable<OperationLogDTO>
        headerTitle="操作日志"
        actionRef={actionRef}
        rowKey="id"
        columns={columns}
        rowSelection={{
          selectedRowKeys,
          onChange: (keys) => setSelectedRowKeys(keys),
        }}
        request={async (params) => {
          const { current = 1, pageSize = 10, ...query } = params;
          const res = await logApi.operationLogPage({
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
          <Permission key="batchDelete" code="monitor:operlog:delete">
            <Button
              danger
              icon={<DeleteOutlined />}
              disabled={selectedRowKeys.length === 0}
              onClick={handleBatchDelete}
            >
              批量删除
            </Button>
          </Permission>,
          <Permission key="clean" code="monitor:operlog:clean">
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

      {/* 操作日志详情抽屉 */}
      <Drawer
        title="操作日志详情"
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        width={600}
        loading={detailLoading}
      >
        {currentLog && (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="操作描述">
              {currentLog.operation}
            </Descriptions.Item>
            <Descriptions.Item label="操作人员">
              {currentLog.username}
            </Descriptions.Item>
            <Descriptions.Item label="请求方法">
              {currentLog.method}
            </Descriptions.Item>
            <Descriptions.Item label="请求参数">
              <pre style={{ margin: 0, whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
                {currentLog.params || '-'}
              </pre>
            </Descriptions.Item>
            <Descriptions.Item label="IP地址">
              {currentLog.ipAddress || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="操作地点">
              {currentLog.location || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag color={currentLog.status === 1 ? 'success' : 'error'}>
                {currentLog.status === 1 ? '成功' : '失败'}
              </Tag>
            </Descriptions.Item>
            {currentLog.status === 0 && currentLog.errorMsg && (
              <Descriptions.Item label="错误信息">
                <pre style={{ margin: 0, whiteSpace: 'pre-wrap', wordBreak: 'break-all', color: '#ff4d4f' }}>
                  {currentLog.errorMsg}
                </pre>
              </Descriptions.Item>
            )}
            <Descriptions.Item label="操作时间">
              {currentLog.operationTime}
            </Descriptions.Item>
            <Descriptions.Item label="耗时">
              {currentLog.time}ms
            </Descriptions.Item>
          </Descriptions>
        )}
      </Drawer>
    </>
  );
};

export default OperlogPage;
