import React from 'react';
import { Tag } from 'antd';
import {
  ClockCircleOutlined,
  SyncOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ExclamationCircleOutlined,
  AuditOutlined,
  StopOutlined,
} from '@ant-design/icons';

const STATUS_MAP: Record<string, { color: string; label: string; icon: React.ReactNode }> = {
  OPEN: { color: 'blue', label: '待处理', icon: <ClockCircleOutlined /> },
  IN_PROGRESS: { color: 'processing', label: '处理中', icon: <SyncOutlined spin /> },
  PENDING_APPROVAL: { color: 'orange', label: '待审批', icon: <AuditOutlined /> },
  APPROVED: { color: 'cyan', label: '已审批', icon: <CheckCircleOutlined /> },
  REJECTED: { color: 'red', label: '已拒绝', icon: <CloseCircleOutlined /> },
  RESOLVED: { color: 'green', label: '已解决', icon: <CheckCircleOutlined /> },
  CLOSED: { color: 'default', label: '已关闭', icon: <StopOutlined /> },
  REOPENED: { color: 'warning', label: '重新打开', icon: <ExclamationCircleOutlined /> },
};

interface TicketStatusTagProps {
  status: string;
}

const TicketStatusTag: React.FC<TicketStatusTagProps> = ({ status }) => {
  const config = STATUS_MAP[status] ?? { color: 'default', label: status, icon: null };
  return (
    <Tag color={config.color} icon={config.icon}>
      {config.label}
    </Tag>
  );
};

export default TicketStatusTag;
