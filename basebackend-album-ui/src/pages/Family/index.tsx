import React, { useState } from 'react';
import { Row, Col, Card, Button, Modal, Drawer, Avatar, List, Tag, Typography } from 'antd';
import { PlusOutlined, UsergroupAddOutlined } from '@ant-design/icons';

const { Title } = Typography;

const Family: React.FC = () => {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const families = [{ id: 1, name: '温暖的家', count: 4 }];

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 24 }}>
        <Title level={2}>我的家庭</Title>
        <div>
          <Button icon={<PlusOutlined />} type="primary" style={{ marginRight: 8 }}>创建家庭</Button>
          <Button icon={<UsergroupAddOutlined />}>加入家庭</Button>
        </div>
      </div>

      <Row gutter={[16, 16]}>
        {families.map(f => (
          <Col span={8} key={f.id}>
            <Card title={f.name} extra={<Button type="link" onClick={() => setDrawerOpen(true)}>详情</Button>} className="card-radius soft-shadow">
              <p>成员数量: {f.count} 人</p>
            </Card>
          </Col>
        ))}
      </Row>

      <Drawer title="家庭详情" placement="right" onClose={() => setDrawerOpen(false)} open={drawerOpen} width={400}>
        <div style={{ marginBottom: 24 }}>
          <Button type="primary" block>生成邀请码</Button>
        </div>
        <Title level={5}>成员列表</Title>
        <List
          itemLayout="horizontal"
          dataSource={[{name: '我', role: '创建者'}, {name: '妈妈', role: '成员'}]}
          renderItem={item => (
            <List.Item actions={[<a key="edit">设为管理员</a>, <a key="del" style={{color: 'red'}}>移除</a>]}>
              <List.Item.Meta avatar={<Avatar>{item.name[0]}</Avatar>} title={item.name} description={<Tag color={item.role === '成员' ? 'blue' : 'gold'}>{item.role}</Tag>} />
            </List.Item>
          )}
        />
      </Drawer>
    </div>
  );
};
export default Family;
