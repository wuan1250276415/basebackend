import { Line } from '@ant-design/charts';
import type { TrendPoint } from '@/api/ticketApi';

interface TrendChartProps {
  data: TrendPoint[];
}

const TrendChart: React.FC<TrendChartProps> = ({ data }) => {
  const chartData = data.flatMap((item) => [
    { date: item.date, type: '新建', count: item.openCount },
    { date: item.date, type: '已解决', count: item.resolvedCount },
    { date: item.date, type: '已关闭', count: item.closedCount },
  ]);

  return (
    <Line
      data={chartData}
      xField="date"
      yField="count"
      seriesField="type"
      color={['#1890ff', '#52c41a', '#8c8c8c']}
      smooth
      height={280}
      point={{ size: 2, shape: 'circle' }}
      legend={{ position: 'top' }}
      xAxis={{ label: { autoRotate: true, autoHide: true } }}
      yAxis={{ min: 0 }}
      tooltip={{ shared: true }}
    />
  );
};

export default TrendChart;
