import { useEffect, useRef, useState } from 'react';
import { Button, Space, Modal, message, Card, Row, Col, Statistic, Tag, Tooltip } from 'antd';
import {
  PlusOutlined,
  DeleteOutlined,
  EyeOutlined,
  ExclamationCircleOutlined,
  ClockCircleOutlined,
  SyncOutlined,
  CheckCircleOutlined,
  WarningOutlined,
  ReloadOutlined,
  DownloadOutlined,
} from '@ant-design/icons';
import { ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { useNavigate } from 'react-router-dom';
import { ticketApi } from '@/api/ticketApi';
import type { TicketListItem, TicketQueryDTO, TicketOverview } from '@/api/ticketApi';
import { Permission } from '@/components/Permission';
import TicketStatusTag from '@/components/ticket/TicketStatusTag';
import PriorityBadge from '@/components/ticket/PriorityBadge';

const TicketPage: React.FC = () => {
  const actionRef = useRef<ActionType>();
  const navigate = useNavigate();
  const [overview, setOverview] = useState<TicketOverview | null>(null);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [exportLoading, setExportLoading] = useState(false);

  const fetchOverview = async () => {
    try {
      const data = await ticketApi.getOverview();
      setOverview(data);
    } catch {
      // ignore
    }
  };

  useEffect(() => {
    fetchOverview();
  }, []);

  const handleBatchDelete = () => {
    if (!selectedRowKeys.length) return;
    Modal.confirm({
      title: '批量删除',
      icon: <ExclamationCircleOutlined />,
      content: `确认删除选中的 ${selectedRowKeys.length} 条工单？`,
      onOk: async () => {
        await Promise.all(selectedRowKeys.map((id) => ticketApi.delete(id as number)));
        message.success('批量删除成功');
        setSelectedRowKeys([]);
        actionRef.current?.reload();
        fetchOverview();
      },
    });
  };

  const handleBatchClose = () => {
    if (!selectedRowKeys.length) return;
    Modal.confirm({
      title: '批量关闭',
      content: `确认关闭选中的 ${selectedRowKeys.length} 条工单？`,
      onOk: async () => {
        await Promise.all(selectedRowKeys.map((id) => ticketApi.close(id as number, '批量关闭')));
        message.success('批量关闭成功');
        setSelectedRowKeys([]);
        actionRef.current?.reload();
        fetchOverview();
      },
    });
  };

  const handleExport = async (format: 'csv' | 'excel') => {
    setExportLoading(true);
    try {
      const blob = await ticketApi.exportTickets({}, format);
      const url = window.URL.createObjectURL(new Blob([blob]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `工单导出.${format === 'csv' ? 'csv' : 'xlsx'}`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      message.success('导出成功');
    } catch {
      // handled by interceptor
    } finally {
      setExportLoading(false);
    }
  };

  const columns: ProColumns<TicketListItem>[] = [
    {
      title: '工单编号',
      dataIndex: 'ticketNo',
      width: 180,
      copyable: true,
      render: (_, record) => (
        <a onClick={() => navigate(`/ticket/detail?id=${record.id}`)}>{record.ticketNo}</a>
      ),
    },
    {
      title: '标题',
      dataIndex: 'title',
      ellipsis: true,
      render: (_, record) => (
        <Space>
          <span>{record.title}</span>
          {record.slaBreached === 1 && (
            <Tooltip title="SLA 已违约">
              <Tag color="red" icon={<WarningOutlined />}>SLA</Tag>
            </Tooltip>
          )}
        </Space>
      ),
    },
    {
      title: '关键词',
      dataIndex: 'keyword',
      hideInTable: true,
      fieldProps: { placeholder: '搜索标题或描述' },
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 120,
      valueType: 'select',
      valueEnum: {
        OPEN: { text: '待处理' },
        IN_PROGRESS: { text: '处理中' },
        PENDING_APPROVAL: { text: '待审批' },
        APPROVED: { text: '已审批' },
        REJECTED: { text: '已拒绝' },
        RESOLVED: { text: '已解决' },
        CLOSED: { text: '已关闭' },
      },
      render: (_, record) => <TicketStatusTag status={record.status} />,
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      width: 80,
      valueType: 'select',
      valueEnum: {
        1: { text: '紧急' },
        2: { text: '高' },
        3: { text: '中' },
        4: { text: '低' },
        5: { text: '最低' },
      },
      render: (_, record) => <PriorityBadge priority={record.priority} />,
    },
    { title: '分类', dataIndex: 'categoryName', width: 120, hideInSearch: true },
    { title: '处理人', dataIndex: 'assigneeName', width: 100 },
    { title: '报告人', dataIndex: 'reporterName', width: 100, hideInSearch: true },
    {
      title: '评论',
      dataIndex: 'commentCount',
      width: 60,
      hideInSearch: true,
      align: 'center',
      render: (_, record) => record.commentCount > 0 ? <Tag>{record.commentCount}</Tag> : '-',
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      valueType: 'dateTime',
      width: 170,
      hideInSearch: true,
      sorter: true,
    },
    {
      title: '创建日期',
      dataIndex: 'dateRange',
      valueType: 'dateRange',
      hideInTable: true,
      search: {
        transform: (value: [string, string]) => ({
          startDate: value[0],
          endDate: value[1],
        }),
      },
    },
    {
      title: '操作',
      valueType: 'option',
      width: 120,
      render: (_, record) => (
        <Space size={4}>
          <a onClick={() => navigate(`/ticket/detail?id=${record.id}`)}>
            <EyeOutlined /> 详情
          </a>
          <Permission code="ticket:delete">
            <a
              style={{ color: '#ff4d4f' }}
              onClick={() => {
                Modal.confirm({
                  title: '确认删除',
                  content: `确认删除工单 ${record.ticketNo}？`,
                  onOk: async () => {
                    await ticketApi.delete(record.id);
                    message.success('删除成功');
                    actionRef.current?.reload();
                    fetchOverview();
                  },
                });
              }}
            >
              <DeleteOutlined />
            </a>
          </Permission>
        </Space>
      ),
    },
  ];

  return (
    <div>
      {/* Overview Statistics */}
      {overview && (
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={3}>
            <Card size="small" hoverable onClick={() => actionRef.current?.reload()}>
              <Statistic title="全部" value={overview.totalCount} valueStyle={{ fontSize: 20 }} />
            </Card>
          </Col>
          <Col span={3}>
            <Card size="small" hoverable>
              <Statistic
                title="待处理"
                value={overview.openCount}
                valueStyle={{ color: '#1890ff', fontSize: 20 }}
                prefix={<ClockCircleOutlined />}
              />
            </Card>
          </Col>
          <Col span={3}>
            <Card size="small" hoverable>
              <Statistic
                title="处理中"
                value={overview.inProgressCount}
                valueStyle={{ color: '#1890ff', fontSize: 20 }}
                prefix={<SyncOutlined />}
              />
            </Card>
          </Col>
          <Col span={3}>
            <Card size="small" hoverable>
              <Statistic
                title="待审批"
                value={overview.pendingApprovalCount}
                valueStyle={{ color: '#fa8c16', fontSize: 20 }}
                prefix={<ExclamationCircleOutlined />}
              />
            </Card>
          </Col>
          <Col span={3}>
            <Card size="small" hoverable>
              <Statistic
                title="已解决"
                value={overview.resolvedCount}
                valueStyle={{ color: '#52c41a', fontSize: 20 }}
                prefix={<CheckCircleOutlined />}
              />
            </Card>
          </Col>
          <Col span={3}>
            <Card size="small" hoverable>
              <Statistic
                title="已关闭"
                value={overview.closedCount}
                valueStyle={{ fontSize: 20 }}
              />
            </Card>
          </Col>
          <Col span={3}>
            <Card size="small" hoverable>
              <Statistic
                title="已拒绝"
                value={overview.rejectedCount}
                valueStyle={{ color: '#ff4d4f', fontSize: 20 }}
              />
            </Card>
          </Col>
          <Col span={3}>
            <Card size="small" hoverable>
              <Statistic
                title="SLA违约"
                value={overview.slaBreachedCount}
                valueStyle={{ color: overview.slaBreachedCount > 0 ? '#ff4d4f' : '#52c41a', fontSize: 20 }}
                prefix={<WarningOutlined />}
              />
            </Card>
          </Col>
        </Row>
      )}

      {/* Ticket Table */}
      <ProTable<TicketListItem>
        headerTitle="工单管理"
        actionRef={actionRef}
        rowKey="id"
        columns={columns}
        rowSelection={{
          selectedRowKeys,
          onChange: setSelectedRowKeys,
        }}
        tableAlertOptionRender={() => (
          <Space>
            <Permission code="ticket:delete">
              <Button size="small" danger onClick={handleBatchDelete}>
                批量删除
              </Button>
            </Permission>
            <Button size="small" onClick={handleBatchClose}>
              批量关闭
            </Button>
            <a onClick={() => setSelectedRowKeys([])}>取消选择</a>
          </Space>
        )}
        request={async (params) => {
          const { current = 1, pageSize = 10, keyword, startDate, endDate, ...query } = params;
          const res = await ticketApi.page({
            current,
            size: pageSize,
            keyword,
            startDate,
            endDate,
            ...query,
          } as TicketQueryDTO & { current: number; size: number });
          return { data: res.records, total: res.total, success: true };
        }}
        onLoad={() => fetchOverview()}
        pagination={{ defaultPageSize: 10, showSizeChanger: true, showTotal: (total) => `共 ${total} 条` }}
        search={{ labelWidth: 'auto', defaultCollapsed: false, span: 6 }}
        toolBarRender={() => [
          <Button key="export-excel" icon={<DownloadOutlined />} loading={exportLoading} onClick={() => handleExport('excel')}>
            导出 Excel
          </Button>,
          <Button key="export-csv" loading={exportLoading} onClick={() => handleExport('csv')}>
            导出 CSV
          </Button>,
          <Button key="refresh" icon={<ReloadOutlined />} onClick={() => { actionRef.current?.reload(); fetchOverview(); }}>
            刷新
          </Button>,
          <Permission key="add" code="ticket:create">
            <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/ticket/create')}>
              创建工单
            </Button>
          </Permission>,
        ]}
      />
    </div>
  );
};

export default TicketPage;
