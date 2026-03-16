import React from 'react';
import { Typography, Row, Col, Card, Statistic, Button, Space } from 'antd';
import { PictureOutlined, FolderOutlined, TeamOutlined, CloudServerOutlined, UploadOutlined, PlusOutlined, UserAddOutlined } from '@ant-design/icons';
import PhotoGrid from '../../components/PhotoGrid';

const { Title } = Typography;

const Home: React.FC = () => {
  const recentPhotos = Array.from({ length: 12 }).map((_, i) => ({ id: `p${i}`, name: `Photo ${i}` }));

  return (
    <div style={{ padding: 24 }}>
      <Title level={2}>欢迎回来，管理员 🐼</Title>
      
      <Space style={{ marginBottom: 24 }}>
        <Button type="primary" icon={<UploadOutlined />}>上传照片</Button>
        <Button icon={<PlusOutlined />}>创建相册</Button>
        <Button icon={<UserAddOutlined />}>邀请家人</Button>
      </Space>

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col span={6}><Card className="card-radius soft-shadow"><Statistic title="照片总数" value={1024} prefix={<PictureOutlined />} /></Card></Col>
        <Col span={6}><Card className="card-radius soft-shadow"><Statistic title="相册数" value={12} prefix={<FolderOutlined />} /></Card></Col>
        <Col span={6}><Card className="card-radius soft-shadow"><Statistic title="家庭数" value={3} prefix={<TeamOutlined />} /></Card></Col>
        <Col span={6}><Card className="card-radius soft-shadow"><Statistic title="存储空间" value="45.5 / 100 GB" prefix={<CloudServerOutlined />} /></Card></Col>
      </Row>

      <Card title="最近上传" className="card-radius soft-shadow">
        <PhotoGrid photos={recentPhotos} />
      </Card>
    </div>
  );
};
export default Home;
