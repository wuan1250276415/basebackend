import React, { useState } from 'react';
import { Tabs, Row, Col, Card, Button, Modal, Form, Input, Select } from 'antd';
import { PlusOutlined } from '@ant-design/icons';

const Albums: React.FC = () => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  
  const albums = Array.from({ length: 8 }).map((_, i) => ({ id: i, name: `相册 ${i}`, count: 20, date: '2026-02-28' }));

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Tabs defaultActiveKey="1" items={[{ key: '1', label: '全部' }, { key: '2', label: '个人' }, { key: '3', label: '家庭' }]} />
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setIsModalOpen(true)}>创建相册</Button>
      </div>

      <Row gutter={[16, 16]}>
        {albums.map(album => (
          <Col xs={24} sm={12} md={8} lg={6} key={album.id}>
            <Card hoverable cover={<div style={{ height: 160, background: '#e6f7ff' }} />} className="card-radius soft-shadow">
              <Card.Meta title={album.name} description={`${album.count} 张照片 · ${album.date}`} />
            </Card>
          </Col>
        ))}
      </Row>

      <Modal title="创建相册" open={isModalOpen} onOk={() => setIsModalOpen(false)} onCancel={() => setIsModalOpen(false)}>
        <Form layout="vertical">
          <Form.Item label="相册名称" required><Input placeholder="输入相册名称" /></Form.Item>
          <Form.Item label="描述"><Input.TextArea placeholder="输入描述" /></Form.Item>
          <Form.Item label="可见性"><Select defaultValue="private" options={[{label:'私密', value:'private'}, {label:'家庭可见', value:'family'}]} /></Form.Item>
        </Form>
      </Modal>
    </div>
  );
};
export default Albums;
