import { useEffect, useState } from 'react';
import { Card, Col, Row, Spin, Statistic, Typography } from 'antd';
import {
  ClockCircleOutlined,
  SyncOutlined,
  CheckCircleOutlined,
  WarningOutlined,
  ExclamationCircleOutlined,
  CloseCircleOutlined,
  FileTextOutlined,
  StopOutlined,
} from '@ant-design/icons';
import { ticketApi } from '@/api/ticketApi';
import type { TicketOverview, TrendPoint, ResolutionTimeStats, SlaCompliance, AssigneeRank } from '@/api/ticketApi';
import TrendChart from './components/TrendChart';
import StatusChart from './components/StatusChart';
import SlaGauge from './components/SlaGauge';
import TopAssignees from './components/TopAssignees';

const { Title } = Typography;

const DashboardPage: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [overview, setOverview] = useState<TicketOverview | null>(null);
  const [trend, setTrend] = useState<TrendPoint[]>([]);
  const [statusData, setStatusData] = useState<Record<string, number>>({});
  const [resolutionTime, setResolutionTime] = useState<ResolutionTimeStats | null>(null);
  const [slaCompliance, setSlaCompliance] = useState<SlaCompliance | null>(null);
  const [topAssignees, setTopAssignees] = useState<AssigneeRank[]>([]);

  useEffect(() => {
    const fetchAll = async () => {
      setLoading(true);
      try {
        const [ov, tr, st, rt, sla, ta] = await Promise.all([
          ticketApi.getOverview(),
          ticketApi.getTrend(30),
          ticketApi.countByStatus(),
          ticketApi.getResolutionTime(),
          ticketApi.getSlaCompliance(),
          ticketApi.getTopAssignees(10),
        ]);
        setOverview(ov);
        setTrend(tr);
        setStatusData(st as unknown as Record<string, number>);
        setResolutionTime(rt);
        setSlaCompliance(sla);
        setTopAssignees(ta);
      } catch {
        // handled by interceptor
      } finally {
        setLoading(false);
      }
    };
    fetchAll();
  }, []);

  if (loading) {
    return <Spin spinning style={{ display: 'flex', justifyContent: 'center', padding: 100 }} />;
  }

  const kpiCards = [
    { title: '全部工单', value: overview?.totalCount ?? 0, icon: <FileTextOutlined />, color: undefined },
    { title: '待处理', value: overview?.openCount ?? 0, icon: <ClockCircleOutlined />, color: '#1890ff' },
    { title: '处理中', value: overview?.inProgressCount ?? 0, icon: <SyncOutlined />, color: '#1890ff' },
    { title: '待审批', value: overview?.pendingApprovalCount ?? 0, icon: <ExclamationCircleOutlined />, color: '#fa8c16' },
    { title: '已解决', value: overview?.resolvedCount ?? 0, icon: <CheckCircleOutlined />, color: '#52c41a' },
    { title: '已关闭', value: overview?.closedCount ?? 0, icon: <StopOutlined />, color: '#8c8c8c' },
    { title: '已拒绝', value: overview?.rejectedCount ?? 0, icon: <CloseCircleOutlined />, color: '#ff4d4f' },
    { title: 'SLA违约', value: overview?.slaBreachedCount ?? 0, icon: <WarningOutlined />, color: (overview?.slaBreachedCount ?? 0) > 0 ? '#ff4d4f' : '#52c41a' },
  ];

  return (
    <div>
      <Title level={4} style={{ marginBottom: 16 }}>工单仪表盘</Title>

      {/* KPI Cards */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        {kpiCards.map((item) => (
          <Col key={item.title} xs={12} sm={6} md={6} lg={3}>
            <Card size="small" hoverable>
              <Statistic
                title={item.title}
                value={item.value}
                prefix={item.icon}
                valueStyle={{ color: item.color, fontSize: 20 }}
              />
            </Card>
          </Col>
        ))}
      </Row>

      {/* Charts Row 1: Trend + Status */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} lg={16}>
          <Card title="工单趋势（近30天）" size="small">
            <TrendChart data={trend} />
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card title="状态分布" size="small">
            <StatusChart data={statusData} />
          </Card>
        </Col>
      </Row>

      {/* Charts Row 2: SLA + Resolution + Top Assignees */}
      <Row gutter={[16, 16]}>
        <Col xs={24} md={8}>
          <Card title="SLA 合规率" size="small">
            <SlaGauge data={slaCompliance} />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card title="解决时间统计" size="small">
            {resolutionTime && (
              <Row gutter={16}>
                <Col span={8}>
                  <Statistic title="平均(h)" value={resolutionTime.avgHours} precision={1} />
                </Col>
                <Col span={8}>
                  <Statistic title="中位(h)" value={resolutionTime.medianHours} precision={1} />
                </Col>
                <Col span={8}>
                  <Statistic title="P90(h)" value={resolutionTime.p90Hours} precision={1} />
                </Col>
              </Row>
            )}
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card title="处理人排名 (Top 10)" size="small">
            <TopAssignees data={topAssignees} />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default DashboardPage;
