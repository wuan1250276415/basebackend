import { useCallback, useEffect, useState } from 'react';
import { Card, Tag, Avatar, Tooltip, Space, Typography, Spin, message, Badge } from 'antd';
import { UserOutlined, WarningOutlined, ClockCircleOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { ticketApi } from '@/api/ticketApi';
import type { TicketListItem } from '@/api/ticketApi';
import PriorityBadge from '@/components/ticket/PriorityBadge';

const KANBAN_COLUMNS = [
  { key: 'OPEN', title: '待处理', color: '#1890ff' },
  { key: 'IN_PROGRESS', title: '处理中', color: '#13c2c2' },
  { key: 'PENDING_APPROVAL', title: '待审批', color: '#fa8c16' },
  { key: 'RESOLVED', title: '已解决', color: '#52c41a' },
  { key: 'CLOSED', title: '已关闭', color: '#8c8c8c' },
];

const { Text } = Typography;

const KanbanPage: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [tickets, setTickets] = useState<TicketListItem[]>([]);
  const [dragOverColumn, setDragOverColumn] = useState<string | null>(null);

  const fetchTickets = useCallback(async () => {
    setLoading(true);
    try {
      const res = await ticketApi.page({ current: 1, size: 200 });
      setTickets(res.records);
    } catch {
      // handled by interceptor
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchTickets();
  }, [fetchTickets]);

  const getColumnTickets = (status: string) =>
    tickets.filter((t) => t.status === status);

  const handleDragStart = (e: React.DragEvent, ticket: TicketListItem) => {
    e.dataTransfer.setData('ticketId', String(ticket.id));
    e.dataTransfer.setData('fromStatus', ticket.status);
  };

  const handleDragOver = (e: React.DragEvent, status: string) => {
    e.preventDefault();
    setDragOverColumn(status);
  };

  const handleDragLeave = () => {
    setDragOverColumn(null);
  };

  const handleDrop = async (e: React.DragEvent, toStatus: string) => {
    e.preventDefault();
    setDragOverColumn(null);

    const ticketId = Number(e.dataTransfer.getData('ticketId'));
    const fromStatus = e.dataTransfer.getData('fromStatus');

    if (fromStatus === toStatus) return;

    try {
      if (toStatus === 'CLOSED') {
        await ticketApi.close(ticketId, '看板拖拽关闭');
      } else {
        await ticketApi.changeStatus(ticketId, { toStatus, remark: '看板拖拽变更' });
      }
      message.success('状态变更成功');
      fetchTickets();
    } catch {
      // handled by interceptor
    }
  };

  if (loading) {
    return <Spin spinning style={{ display: 'flex', justifyContent: 'center', padding: 100 }} />;
  }

  return (
    <div>
      <Typography.Title level={4} style={{ marginBottom: 16 }}>工单看板</Typography.Title>
      <div style={{ display: 'flex', gap: 12, overflowX: 'auto', paddingBottom: 16 }}>
        {KANBAN_COLUMNS.map((col) => {
          const colTickets = getColumnTickets(col.key);
          return (
            <div
              key={col.key}
              onDragOver={(e) => handleDragOver(e, col.key)}
              onDragLeave={handleDragLeave}
              onDrop={(e) => handleDrop(e, col.key)}
              style={{
                flex: '0 0 280px',
                minHeight: 500,
                background: dragOverColumn === col.key ? '#e6f7ff' : '#f5f5f5',
                borderRadius: 8,
                padding: 12,
                transition: 'background 0.2s',
              }}
            >
              {/* Column Header */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                <Space>
                  <div style={{ width: 4, height: 16, background: col.color, borderRadius: 2 }} />
                  <Text strong>{col.title}</Text>
                </Space>
                <Badge count={colTickets.length} style={{ backgroundColor: col.color }} />
              </div>

              {/* Cards */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                {colTickets.map((ticket) => (
                  <Card
                    key={ticket.id}
                    size="small"
                    hoverable
                    draggable
                    onDragStart={(e) => handleDragStart(e, ticket)}
                    onClick={() => navigate(`/ticket/detail?id=${ticket.id}`)}
                    style={{ cursor: 'grab' }}
                    styles={{ body: { padding: '8px 12px' } }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                      <Text type="secondary" style={{ fontSize: 12 }}>{ticket.ticketNo}</Text>
                      <PriorityBadge priority={ticket.priority} />
                    </div>
                    <div style={{ marginBottom: 8 }}>
                      <Text ellipsis style={{ fontSize: 13, fontWeight: 500 }}>{ticket.title}</Text>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <Space size={4}>
                        {ticket.assigneeName ? (
                          <Tooltip title={ticket.assigneeName}>
                            <Avatar size="small" icon={<UserOutlined />} />
                          </Tooltip>
                        ) : (
                          <Text type="secondary" style={{ fontSize: 11 }}>未分配</Text>
                        )}
                      </Space>
                      <Space size={4}>
                        {ticket.slaBreached === 1 && (
                          <Tooltip title="SLA 已违约">
                            <Tag color="red" style={{ margin: 0, fontSize: 10 }} icon={<WarningOutlined />}>SLA</Tag>
                          </Tooltip>
                        )}
                        {ticket.slaDeadline && ticket.slaBreached !== 1 && (
                          <Tooltip title={`SLA截止: ${ticket.slaDeadline}`}>
                            <ClockCircleOutlined style={{ color: '#8c8c8c', fontSize: 12 }} />
                          </Tooltip>
                        )}
                      </Space>
                    </div>
                  </Card>
                ))}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default KanbanPage;
