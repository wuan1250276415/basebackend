import React from 'react';
import { Timeline, Tag } from 'antd';
import { CheckCircleOutlined, CloseCircleOutlined, RollbackOutlined, SwapOutlined } from '@ant-design/icons';
import type { ApprovalItem } from '@/api/ticketApi';

interface ApprovalTimelineProps {
  approvals: ApprovalItem[];
}

const ACTION_CONFIG: Record<string, { color: string; icon: React.ReactNode; label: string }> = {
  APPROVE: { color: 'green', icon: <CheckCircleOutlined />, label: '通过' },
  REJECT: { color: 'red', icon: <CloseCircleOutlined />, label: '拒绝' },
  RETURN: { color: 'orange', icon: <RollbackOutlined />, label: '退回' },
  DELEGATE: { color: 'blue', icon: <SwapOutlined />, label: '转办' },
};

const ApprovalTimeline: React.FC<ApprovalTimelineProps> = ({ approvals }) => {
  if (!approvals.length) {
    return <div style={{ color: '#999', textAlign: 'center', padding: 16 }}>暂无审批记录</div>;
  }

  return (
    <Timeline
      items={approvals.map((item) => {
        const config = ACTION_CONFIG[item.action] ?? { color: 'gray', icon: null, label: item.action };
        return {
          key: item.id,
          color: config.color,
          dot: config.icon,
          children: (
            <div>
              <div>
                <strong>{item.approverName}</strong>
                <Tag color={config.color} style={{ marginLeft: 8 }}>{config.label}</Tag>
                {item.taskName && <span style={{ color: '#999' }}>({item.taskName})</span>}
              </div>
              {item.opinion && <div style={{ marginTop: 4, color: '#666' }}>{item.opinion}</div>}
              {item.delegateToName && (
                <div style={{ marginTop: 4, color: '#1890ff' }}>转办给: {item.delegateToName}</div>
              )}
              <div style={{ marginTop: 4, fontSize: 12, color: '#999' }}>{item.createTime}</div>
            </div>
          ),
        };
      })}
    />
  );
};

export default ApprovalTimeline;
