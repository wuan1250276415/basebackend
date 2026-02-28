import React from 'react';
import { Typography, Button, Space, Alert } from 'antd';
import { DeleteOutlined } from '@ant-design/icons';
import PhotoGrid from '../../components/PhotoGrid';

const { Title } = Typography;

const Trash: React.FC = () => {
  const photos = Array.from({ length: 4 }).map((_, i) => ({ id: `d${i}`, name: `Deleted ${i}` }));

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={2} style={{ margin: 0 }}>回收站</Title>
        <Button danger icon={<DeleteOutlined />}>清空回收站</Button>
      </div>
      <Alert message="提示：照片将在 30 天后自动清除" type="warning" showIcon style={{ marginBottom: 24 }} />
      <PhotoGrid photos={photos} />
    </div>
  );
};
export default Trash;
