/**
 * 用户下拉菜单组件
 * 显示用户头像和昵称，提供个人中心、修改密码、退出登录等操作
 */
import React, { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { Dropdown, Avatar, Space, Typography } from 'antd';
import type { MenuProps } from 'antd';
import {
  UserOutlined,
  KeyOutlined,
  LogoutOutlined,
} from '@ant-design/icons';
import { useAuthStore } from '@/stores/authStore';

const { Text } = Typography;

/**
 * 用户下拉菜单
 * 放置在 BasicLayout 顶部右侧区域
 * - 显示用户头像和昵称
 * - 下拉菜单：个人中心、修改密码、退出登录
 */
const UserDropdown: React.FC = () => {
  const navigate = useNavigate();
  const { userInfo, logout } = useAuthStore();

  /** 下拉菜单项 */
  const menuItems: MenuProps['items'] = useMemo(
    () => [
      {
        key: 'profile',
        icon: <UserOutlined />,
        label: '个人中心',
        onClick: () => navigate('/profile'),
      },
      {
        key: 'password',
        icon: <KeyOutlined />,
        label: '修改密码',
        onClick: () => navigate('/profile?tab=password'),
      },
      { type: 'divider' as const },
      {
        key: 'logout',
        icon: <LogoutOutlined />,
        label: '退出登录',
        onClick: async () => {
          await logout();
        },
      },
    ],
    [navigate, logout],
  );

  return (
    <Dropdown menu={{ items: menuItems }} placement="bottomRight">
      <Space style={{ cursor: 'pointer' }}>
        <Avatar
          size="small"
          src={userInfo?.avatar}
          icon={!userInfo?.avatar ? <UserOutlined /> : undefined}
        />
        <Text style={{ maxWidth: 100 }} ellipsis>
          {userInfo?.nickname || userInfo?.username || '用户'}
        </Text>
      </Space>
    </Dropdown>
  );
};

export default UserDropdown;
