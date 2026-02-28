import React from 'react';
import { Typography, Divider } from 'antd';
import PhotoGrid from '../../components/PhotoGrid';

const { Title, Text } = Typography;

const Timeline: React.FC = () => {
  const dates = ['2026年2月28日 星期六', '2026年2月27日 星期五'];

  return (
    <div style={{ padding: 24 }}>
      <Title level={2}>时间轴</Title>
      {dates.map((date, idx) => (
        <div key={idx} style={{ marginBottom: 32 }}>
          <div style={{ display: 'flex', alignItems: 'center', marginBottom: 16 }}>
            <Title level={4} style={{ margin: 0, marginRight: 16 }}>{date}</Title>
            <Text type="secondary">{idx === 0 ? 5 : 8} 张照片</Text>
          </div>
          <PhotoGrid photos={Array.from({ length: idx === 0 ? 5 : 8 }).map((_, i) => ({ id: `t${idx}_${i}` }))} />
        </div>
      ))}
      <div style={{ textAlign: 'center', color: '#999', marginTop: 24 }}>正在加载更多...</div>
    </div>
  );
};
export default Timeline;
