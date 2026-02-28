import React from 'react';
import { Layout, Menu } from 'antd';
import { Outlet, useNavigate } from 'react-router-dom';
import { HomeOutlined, PictureOutlined, ClockCircleOutlined, TeamOutlined, ShareAltOutlined } from '@ant-design/icons';

const { Header, Content, Sider } = Layout;

export default function MainLayout() {
  const navigate = useNavigate();
  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ background: '#ff8c00', color: '#fff', fontSize: '20px', fontWeight: 'bold' }}>
        🐻 家庭相册
      </Header>
      <Layout>
        <Sider width={200} theme="light">
          <Menu
            mode="inline"
            defaultSelectedKeys={['/']}
            style={{ height: '100%', borderRight: 0 }}
            items={[
              { key: '/', icon: <HomeOutlined />, label: '首页', onClick: () => navigate('/') },
              { key: '/albums', icon: <PictureOutlined />, label: '相册', onClick: () => navigate('/albums') },
              { key: '/timeline', icon: <ClockCircleOutlined />, label: '时间轴', onClick: () => navigate('/timeline') },
              { key: '/family', icon: <TeamOutlined />, label: '家庭', onClick: () => navigate('/family') },
              { key: '/trash', icon: <ShareAltOutlined />, label: '回收站', onClick: () => navigate('/trash') },
            ]}
          />
        </Sider>
        <Layout style={{ padding: '24px' }}>
          <Content style={{ padding: 24, margin: 0, minHeight: 280, background: '#fff', borderRadius: '12px', boxShadow: '0 2px 8px rgba(0,0,0,0.1)' }}>
            <Outlet />
          </Content>
        </Layout>
      </Layout>
    </Layout>
  );
}
