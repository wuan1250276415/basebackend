import { Progress, Statistic, Space } from 'antd';
import type { SlaCompliance } from '@/api/ticketApi';

interface SlaGaugeProps {
  data: SlaCompliance | null;
}

const SlaGauge: React.FC<SlaGaugeProps> = ({ data }) => {
  if (!data) return null;

  const rate = data.complianceRate;
  const color = rate >= 95 ? '#52c41a' : rate >= 80 ? '#fa8c16' : '#ff4d4f';

  return (
    <div style={{ textAlign: 'center', padding: '16px 0' }}>
      <Progress
        type="dashboard"
        percent={rate}
        strokeColor={color}
        format={(pct) => `${pct?.toFixed(1)}%`}
        size={180}
      />
      <Space size={32} style={{ marginTop: 16 }}>
        <Statistic title="有SLA工单" value={data.totalCount} />
        <Statistic title="已违约" value={data.breachedCount} valueStyle={{ color: data.breachedCount > 0 ? '#ff4d4f' : undefined }} />
      </Space>
    </div>
  );
};

export default SlaGauge;
