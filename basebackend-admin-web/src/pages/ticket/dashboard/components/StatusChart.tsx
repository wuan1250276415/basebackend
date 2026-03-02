import { Pie } from '@ant-design/charts';

const STATUS_LABELS: Record<string, string> = {
  OPEN: '待处理',
  IN_PROGRESS: '处理中',
  PENDING_APPROVAL: '待审批',
  APPROVED: '已审批',
  REJECTED: '已拒绝',
  RESOLVED: '已解决',
  CLOSED: '已关闭',
};

const STATUS_COLORS: Record<string, string> = {
  OPEN: '#1890ff',
  IN_PROGRESS: '#13c2c2',
  PENDING_APPROVAL: '#fa8c16',
  APPROVED: '#36cfc9',
  REJECTED: '#ff4d4f',
  RESOLVED: '#52c41a',
  CLOSED: '#8c8c8c',
};

interface StatusChartProps {
  data: Record<string, number>;
}

const StatusChart: React.FC<StatusChartProps> = ({ data }) => {
  const chartData = Object.entries(data)
    .filter(([, value]) => value > 0)
    .map(([key, value]) => ({
      status: STATUS_LABELS[key] || key,
      count: value,
      color: STATUS_COLORS[key] || '#8c8c8c',
    }));

  return (
    <Pie
      data={chartData}
      angleField="count"
      colorField="status"
      color={chartData.map((d) => d.color)}
      radius={0.9}
      innerRadius={0.6}
      height={280}
      label={{ type: 'spider', content: '{name}\n{value}' }}
      legend={{ position: 'bottom' }}
      statistic={{
        title: { content: '总计' },
        content: {
          content: String(chartData.reduce((sum, d) => sum + d.count, 0)),
        },
      }}
    />
  );
};

export default StatusChart;
