import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Button,
  Space,
  Input,
  Modal,
  Form,
  Tag,
  Drawer,
  Descriptions,
  Spin,
  Tabs,
  message,
} from 'antd';
import {
  CheckOutlined,
  CloseOutlined,
  RollbackOutlined,
  SwapOutlined,
  EyeOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { ticketApi } from '@/api/ticketApi';
import type { TicketListItem, TicketDetail, TicketQueryDTO } from '@/api/ticketApi';
import TicketStatusTag from '@/components/ticket/TicketStatusTag';
import PriorityBadge from '@/components/ticket/PriorityBadge';
import ApprovalTimeline from '@/components/ticket/ApprovalTimeline';
import StatusLog from '@/components/ticket/StatusLog';
import { Permission } from '@/components/Permission';

type ApprovalAction = 'approve' | 'reject' | 'return' | 'delegate';

const ACTION_CONFIG: Record<ApprovalAction, { label: string; color: string; icon: React.ReactNode }> = {
  approve: { label: '通过', color: '#52c41a', icon: <CheckOutlined /> },
  reject: { label: '拒绝', color: '#ff4d4f', icon: <CloseOutlined /> },
  return: { label: '退回', color: '#fa8c16', icon: <RollbackOutlined /> },
  delegate: { label: '转办', color: '#1890ff', icon: <SwapOutlined /> },
};

const TicketApprovalPage: React.FC = () => {
  const actionRef = useRef<ActionType>();
  const navigate = useNavigate();

  // Drawer state
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerLoading, setDrawerLoading] = useState(false);
  const [currentDetail, setCurrentDetail] = useState<TicketDetail | null>(null);
  const [currentTaskId, setCurrentTaskId] = useState<string | undefined>();

  // Approval modal state
  const [modalOpen, setModalOpen] = useState(false);
  const [currentAction, setCurrentAction] = useState<ApprovalAction>('approve');
  const [actionLoading, setActionLoading] = useState(false);
  const [form] = Form.useForm();

  const openDetail = async (record: TicketListItem) => {
    setDrawerOpen(true);
    setDrawerLoading(true);
    setCurrentTaskId(undefined);
    try {
      const detail = await ticketApi.getDetail(record.id);
      setCurrentDetail(detail);
      // Fetch active Camunda tasks for this ticket
      try {
        const tasks = await ticketApi.getActiveTasks(record.id);
        if (tasks.length > 0) {
          setCurrentTaskId(tasks[0].taskId);
        }
      } catch {
        // No active tasks or API not available
      }
    } finally {
      setDrawerLoading(false);
    }
  };

  const openActionModal = (action: ApprovalAction) => {
    setCurrentAction(action);
    form.resetFields();
    setModalOpen(true);
  };

  const handleAction = async () => {
    const values = await form.validateFields();
    if (!currentDetail) return;

    setActionLoading(true);
    try {
      const ticketId = currentDetail.id;
      const params = {
        taskId: currentTaskId,
        opinion: values.opinion,
      };

      switch (currentAction) {
        case 'approve':
          await ticketApi.approve(ticketId, params);
          break;
        case 'reject':
          await ticketApi.reject(ticketId, params);
          break;
        case 'return':
          await ticketApi.returnTicket(ticketId, params);
          break;
        case 'delegate':
          await ticketApi.delegate(ticketId, {
            taskId: currentTaskId,
            delegateUserId: values.delegateUserId,
            opinion: values.opinion,
          });
          break;
      }

      message.success(`审批${ACTION_CONFIG[currentAction].label}成功`);
      setModalOpen(false);
      setDrawerOpen(false);
      actionRef.current?.reload();
    } catch {
      // handled by interceptor
    } finally {
      setActionLoading(false);
    }
  };

  const columns: ProColumns<TicketListItem>[] = [
    {
      title: '工单编号',
      dataIndex: 'ticketNo',
      width: 180,
      copyable: true,
      render: (_, record) => (
        <a onClick={() => openDetail(record)}>{record.ticketNo}</a>
      ),
    },
    {
      title: '标题',
      dataIndex: 'title',
      ellipsis: true,
    },
    {
      title: '关键词',
      dataIndex: 'keyword',
      hideInTable: true,
      fieldProps: { placeholder: '搜索标题或描述' },
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
    { title: '报告人', dataIndex: 'reporterName', width: 100, hideInSearch: true },
    { title: '处理人', dataIndex: 'assigneeName', width: 100, hideInSearch: true },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      valueType: 'dateTime',
      width: 170,
      hideInSearch: true,
      sorter: true,
    },
    {
      title: '操作',
      valueType: 'option',
      width: 200,
      render: (_, record) => (
        <Space size={4}>
          <a onClick={() => openDetail(record)}>
            <EyeOutlined /> 审批
          </a>
          <a onClick={() => navigate(`/ticket/detail?id=${record.id}`)}>
            详情
          </a>
        </Space>
      ),
    },
  ];

  const actionConfig = ACTION_CONFIG[currentAction];

  return (
    <div>
      <ProTable<TicketListItem>
        headerTitle="待审批工单"
        actionRef={actionRef}
        rowKey="id"
        columns={columns}
        request={async (params) => {
          const { current = 1, pageSize = 10, keyword, ...query } = params;
          const res = await ticketApi.page({
            current,
            size: pageSize,
            keyword,
            status: 'PENDING_APPROVAL',
            ...query,
          } as TicketQueryDTO & { current: number; size: number });
          return { data: res.records, total: res.total, success: true };
        }}
        pagination={{ defaultPageSize: 10, showSizeChanger: true, showTotal: (total) => `共 ${total} 条` }}
        search={{ labelWidth: 'auto', defaultCollapsed: false, span: 6 }}
        toolBarRender={() => [
          <Button key="refresh" icon={<ReloadOutlined />} onClick={() => actionRef.current?.reload()}>
            刷新
          </Button>,
        ]}
      />

      {/* Approval Detail Drawer */}
      <Drawer
        title={currentDetail ? `审批: ${currentDetail.ticketNo}` : '审批详情'}
        width={720}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        extra={
          currentDetail?.status === 'PENDING_APPROVAL' && (
            <Permission code="ticket:approve">
              <Space>
                <Button
                  type="primary"
                  icon={<CheckOutlined />}
                  onClick={() => openActionModal('approve')}
                >
                  通过
                </Button>
                <Button
                  danger
                  icon={<CloseOutlined />}
                  onClick={() => openActionModal('reject')}
                >
                  拒绝
                </Button>
                <Button
                  icon={<RollbackOutlined />}
                  onClick={() => openActionModal('return')}
                >
                  退回
                </Button>
                <Button
                  icon={<SwapOutlined />}
                  onClick={() => openActionModal('delegate')}
                >
                  转办
                </Button>
              </Space>
            </Permission>
          )
        }
      >
        {drawerLoading ? (
          <Spin style={{ display: 'flex', justifyContent: 'center', padding: 60 }} />
        ) : currentDetail ? (
          <>
            <Descriptions column={2} bordered size="small" style={{ marginBottom: 24 }}>
              <Descriptions.Item label="状态">
                <TicketStatusTag status={currentDetail.status} />
              </Descriptions.Item>
              <Descriptions.Item label="优先级">
                <PriorityBadge priority={currentDetail.priority} />
              </Descriptions.Item>
              <Descriptions.Item label="分类">{currentDetail.categoryName}</Descriptions.Item>
              <Descriptions.Item label="来源">{currentDetail.source}</Descriptions.Item>
              <Descriptions.Item label="报告人">{currentDetail.reporterName}</Descriptions.Item>
              <Descriptions.Item label="处理人">
                {currentDetail.assigneeName || <span style={{ color: '#999' }}>未分配</span>}
              </Descriptions.Item>
              <Descriptions.Item label="创建时间">{currentDetail.createTime}</Descriptions.Item>
              <Descriptions.Item label="SLA截止">{currentDetail.slaDeadline || '-'}</Descriptions.Item>
              <Descriptions.Item label="SLA状态">
                {currentDetail.slaBreached ? <Tag color="red">已违约</Tag> : <Tag color="green">正常</Tag>}
              </Descriptions.Item>
              {currentDetail.processInstanceId && (
                <Descriptions.Item label="流程实例">{currentDetail.processInstanceId}</Descriptions.Item>
              )}
              {currentDetail.tags && (
                <Descriptions.Item label="标签" span={2}>
                  {currentDetail.tags.split(',').map((tag) => (
                    <Tag key={tag.trim()}>{tag.trim()}</Tag>
                  ))}
                </Descriptions.Item>
              )}
              <Descriptions.Item label="工单标题" span={2}>
                {currentDetail.title}
              </Descriptions.Item>
              <Descriptions.Item label="描述" span={2}>
                <div style={{ whiteSpace: 'pre-wrap', maxHeight: 200, overflow: 'auto' }}>
                  {currentDetail.description || '-'}
                </div>
              </Descriptions.Item>
            </Descriptions>

            <Tabs
              defaultActiveKey="approvals"
              items={[
                {
                  key: 'approvals',
                  label: `审批记录 (${currentDetail.approvals?.length ?? 0})`,
                  children: <ApprovalTimeline approvals={currentDetail.approvals ?? []} />,
                },
                {
                  key: 'statusLogs',
                  label: `状态日志 (${currentDetail.statusLogs?.length ?? 0})`,
                  children: <StatusLog statusLogs={currentDetail.statusLogs ?? []} />,
                },
              ]}
            />
          </>
        ) : null}
      </Drawer>

      {/* Approval Action Modal */}
      <Modal
        title={
          <Space>
            <span style={{ color: actionConfig.color }}>{actionConfig.icon}</span>
            <span>审批{actionConfig.label}</span>
          </Space>
        }
        open={modalOpen}
        onOk={handleAction}
        onCancel={() => setModalOpen(false)}
        confirmLoading={actionLoading}
        okText={actionConfig.label}
        okButtonProps={{
          danger: currentAction === 'reject',
          type: 'primary',
        }}
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          {currentAction === 'delegate' && (
            <Form.Item
              name="delegateUserId"
              label="转办目标人ID"
              rules={[{ required: true, message: '请输入转办目标人ID' }]}
            >
              <Input placeholder="请输入转办目标人ID" />
            </Form.Item>
          )}
          <Form.Item
            name="opinion"
            label="审批意见"
            rules={
              currentAction === 'reject'
                ? [{ required: true, message: '拒绝时必须填写审批意见' }]
                : undefined
            }
          >
            <Input.TextArea
              rows={4}
              placeholder={
                currentAction === 'reject'
                  ? '请输入拒绝原因（必填）'
                  : '请输入审批意见（可选）'
              }
              maxLength={1000}
              showCount
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default TicketApprovalPage;
