import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import {
  Descriptions,
  Card,
  Tabs,
  Spin,
  Button,
  Space,
  Tag,
  Modal,
  Input,
  Select,
  Dropdown,
  message,
} from 'antd';
import {
  ArrowLeftOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
  MoreOutlined,
  UserSwitchOutlined,
} from '@ant-design/icons';
import type { MenuProps } from 'antd';
import { useTicketStore } from '@/stores/ticketStore';
import TicketStatusTag from '@/components/ticket/TicketStatusTag';
import PriorityBadge from '@/components/ticket/PriorityBadge';
import CommentSection from '@/components/ticket/CommentSection';
import AttachmentList from '@/components/ticket/AttachmentList';
import ApprovalTimeline from '@/components/ticket/ApprovalTimeline';
import StatusLog from '@/components/ticket/StatusLog';
import { ticketApi } from '@/api/ticketApi';
import { Permission } from '@/components/Permission';

const STATUS_TRANSITIONS: Record<string, { label: string; value: string }[]> = {
  OPEN: [
    { label: '开始处理', value: 'IN_PROGRESS' },
    { label: '关闭', value: 'CLOSED' },
  ],
  IN_PROGRESS: [
    { label: '标记解决', value: 'RESOLVED' },
    { label: '提交审批', value: 'PENDING_APPROVAL' },
    { label: '关闭', value: 'CLOSED' },
  ],
  PENDING_APPROVAL: [],
  APPROVED: [
    { label: '标记解决', value: 'RESOLVED' },
    { label: '关闭', value: 'CLOSED' },
  ],
  REJECTED: [
    { label: '重新处理', value: 'IN_PROGRESS' },
    { label: '关闭', value: 'CLOSED' },
  ],
  RESOLVED: [
    { label: '关闭', value: 'CLOSED' },
    { label: '重新打开', value: 'OPEN' },
  ],
  CLOSED: [
    { label: '重新打开', value: 'OPEN' },
  ],
};

const TicketDetailPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const id = Number(searchParams.get('id'));
  const { detail, detailLoading, fetchDetail } = useTicketStore();

  const [statusModalOpen, setStatusModalOpen] = useState(false);
  const [targetStatus, setTargetStatus] = useState('');
  const [statusRemark, setStatusRemark] = useState('');
  const [assignModalOpen, setAssignModalOpen] = useState(false);
  const [assigneeId, setAssigneeId] = useState<number | undefined>();
  const [assigneeName, setAssigneeName] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  useEffect(() => {
    if (id) fetchDetail(id);
  }, [id, fetchDetail]);

  const handleStatusChange = (toStatus: string) => {
    setTargetStatus(toStatus);
    setStatusRemark('');
    setStatusModalOpen(true);
  };

  const confirmStatusChange = async () => {
    setActionLoading(true);
    try {
      if (targetStatus === 'CLOSED') {
        await ticketApi.close(id, statusRemark || undefined);
      } else {
        await ticketApi.changeStatus(id, { toStatus: targetStatus, remark: statusRemark || undefined });
      }
      message.success('状态变更成功');
      setStatusModalOpen(false);
      fetchDetail(id);
    } catch {
      // handled by interceptor
    } finally {
      setActionLoading(false);
    }
  };

  const confirmAssign = async () => {
    if (!assigneeId || !assigneeName.trim()) {
      message.warning('请输入处理人信息');
      return;
    }
    setActionLoading(true);
    try {
      await ticketApi.assign(id, { assigneeId, assigneeName: assigneeName.trim() });
      message.success('分配成功');
      setAssignModalOpen(false);
      fetchDetail(id);
    } catch {
      // handled by interceptor
    } finally {
      setActionLoading(false);
    }
  };

  const handleDelete = () => {
    Modal.confirm({
      title: '确认删除',
      icon: <ExclamationCircleOutlined />,
      content: `确认删除工单 ${detail?.ticketNo}？此操作不可撤销。`,
      okType: 'danger',
      onOk: async () => {
        await ticketApi.delete(id);
        message.success('删除成功');
        navigate('/ticket');
      },
    });
  };

  if (detailLoading || !detail) {
    return <Spin spinning={detailLoading} style={{ display: 'flex', justifyContent: 'center', padding: 100 }} />;
  }

  const transitions = STATUS_TRANSITIONS[detail.status] ?? [];

  const moreMenuItems: MenuProps['items'] = [
    {
      key: 'assign',
      icon: <UserSwitchOutlined />,
      label: '分配处理人',
      onClick: () => {
        setAssigneeId(undefined);
        setAssigneeName('');
        setAssignModalOpen(true);
      },
    },
    {
      key: 'edit',
      icon: <EditOutlined />,
      label: '编辑工单',
      onClick: () => navigate(`/ticket/create?edit=${id}`),
    },
    { type: 'divider' },
    {
      key: 'delete',
      label: '删除工单',
      danger: true,
      onClick: handleDelete,
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/ticket')}>
          返回列表
        </Button>
        <Space>
          {transitions.map((t) => (
            <Button
              key={t.value}
              type={t.value === 'CLOSED' ? 'default' : 'primary'}
              danger={t.value === 'CLOSED'}
              onClick={() => handleStatusChange(t.value)}
            >
              {t.label}
            </Button>
          ))}
          <Permission code="ticket:delete">
            <Dropdown menu={{ items: moreMenuItems }} trigger={['click']}>
              <Button icon={<MoreOutlined />} />
            </Dropdown>
          </Permission>
        </Space>
      </div>

      <Card
        title={
          <Space>
            <span>{detail.ticketNo}</span>
            <span style={{ fontWeight: 'normal', color: '#333' }}>-</span>
            <span>{detail.title}</span>
          </Space>
        }
        extra={<TicketStatusTag status={detail.status} />}
        style={{ marginBottom: 16 }}
      >
        <Descriptions column={3} bordered size="small">
          <Descriptions.Item label="优先级"><PriorityBadge priority={detail.priority} /></Descriptions.Item>
          <Descriptions.Item label="分类">{detail.categoryName}</Descriptions.Item>
          <Descriptions.Item label="来源">{detail.source}</Descriptions.Item>
          <Descriptions.Item label="报告人">{detail.reporterName}</Descriptions.Item>
          <Descriptions.Item label="处理人">{detail.assigneeName || <span style={{ color: '#999' }}>未分配</span>}</Descriptions.Item>
          <Descriptions.Item label="创建时间">{detail.createTime}</Descriptions.Item>
          <Descriptions.Item label="SLA截止">{detail.slaDeadline || '-'}</Descriptions.Item>
          <Descriptions.Item label="SLA状态">
            {detail.slaBreached ? <Tag color="red">已违约</Tag> : <Tag color="green">正常</Tag>}
          </Descriptions.Item>
          <Descriptions.Item label="更新时间">{detail.updateTime}</Descriptions.Item>
          {detail.resolvedAt && (
            <Descriptions.Item label="解决时间">{detail.resolvedAt}</Descriptions.Item>
          )}
          {detail.closedAt && (
            <Descriptions.Item label="关闭时间">{detail.closedAt}</Descriptions.Item>
          )}
          {detail.processInstanceId && (
            <Descriptions.Item label="流程实例">{detail.processInstanceId}</Descriptions.Item>
          )}
          {detail.tags && (
            <Descriptions.Item label="标签" span={3}>
              {detail.tags.split(',').map((tag) => (
                <Tag key={tag.trim()}>{tag.trim()}</Tag>
              ))}
            </Descriptions.Item>
          )}
          <Descriptions.Item label="描述" span={3}>
            <div style={{ whiteSpace: 'pre-wrap' }}>{detail.description || '-'}</div>
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Card>
        <Tabs
          defaultActiveKey="comments"
          items={[
            {
              key: 'comments',
              label: `评论 (${detail.comments?.length ?? 0})`,
              children: (
                <CommentSection
                  ticketId={detail.id}
                  comments={detail.comments ?? []}
                  onRefresh={() => fetchDetail(id)}
                />
              ),
            },
            {
              key: 'attachments',
              label: `附件 (${detail.attachments?.length ?? 0})`,
              children: (
                <AttachmentList
                  ticketId={detail.id}
                  attachments={detail.attachments ?? []}
                  onRefresh={() => fetchDetail(id)}
                />
              ),
            },
            {
              key: 'approvals',
              label: `审批记录 (${detail.approvals?.length ?? 0})`,
              children: <ApprovalTimeline approvals={detail.approvals ?? []} />,
            },
            {
              key: 'statusLogs',
              label: `状态日志 (${detail.statusLogs?.length ?? 0})`,
              children: <StatusLog statusLogs={detail.statusLogs ?? []} />,
            },
          ]}
        />
      </Card>

      {/* Status Change Modal */}
      <Modal
        title="状态变更"
        open={statusModalOpen}
        onOk={confirmStatusChange}
        onCancel={() => setStatusModalOpen(false)}
        confirmLoading={actionLoading}
      >
        <div style={{ marginBottom: 16 }}>
          <span>目标状态：</span>
          <TicketStatusTag status={targetStatus} />
        </div>
        <Input.TextArea
          rows={3}
          placeholder="请输入变更说明（可选）"
          value={statusRemark}
          onChange={(e) => setStatusRemark(e.target.value)}
          maxLength={500}
          showCount
        />
      </Modal>

      {/* Assign Modal */}
      <Modal
        title="分配处理人"
        open={assignModalOpen}
        onOk={confirmAssign}
        onCancel={() => setAssignModalOpen(false)}
        confirmLoading={actionLoading}
      >
        <Space direction="vertical" style={{ width: '100%' }}>
          <div>
            <label>处理人ID：</label>
            <Select
              style={{ width: '100%' }}
              placeholder="请输入处理人ID"
              showSearch
              onChange={(val) => setAssigneeId(val)}
              value={assigneeId}
              options={[]}
              notFoundContent="请输入ID搜索"
            />
          </div>
          <div>
            <label>处理人姓名：</label>
            <Input
              placeholder="请输入处理人姓名"
              value={assigneeName}
              onChange={(e) => setAssigneeName(e.target.value)}
            />
          </div>
        </Space>
      </Modal>
    </div>
  );
};

export default TicketDetailPage;
