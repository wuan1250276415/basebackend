import React from 'react';
import { Timeline } from 'antd';
import { ArrowRightOutlined } from '@ant-design/icons';
import type { StatusLogItem } from '@/api/ticketApi';
import TicketStatusTag from './TicketStatusTag';

interface StatusLogProps {
  statusLogs: StatusLogItem[];
}

const StatusLog: React.FC<StatusLogProps> = ({ statusLogs }) => {
  if (!statusLogs.length) {
    return <div style={{ color: '#999', textAlign: 'center', padding: 16 }}>暂无状态变更记录</div>;
  }

  return (
    <Timeline
      items={statusLogs.map((item) => ({
        key: item.id,
        children: (
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <TicketStatusTag status={item.fromStatus} />
              <ArrowRightOutlined />
              <TicketStatusTag status={item.toStatus} />
              <span style={{ color: '#666' }}>by {item.operatorName}</span>
            </div>
            {item.remark && <div style={{ marginTop: 4, color: '#666' }}>{item.remark}</div>}
            <div style={{ marginTop: 4, fontSize: 12, color: '#999' }}>{item.createTime}</div>
          </div>
        ),
      }))}
    />
  );
};

export default StatusLog;
