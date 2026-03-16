import React, { useEffect, useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { Avatar, Badge, Tooltip } from 'antd';
import {
  MessageOutlined,
  ContactsOutlined,
  SettingOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { useAuthStore } from '@/stores/useAuthStore';
import { useWebSocketStore } from '@/stores/useWebSocketStore';
import { useConversationStore } from '@/stores/useConversationStore';

type NavKey = 'chat' | 'contacts' | 'settings';

const navItems: { key: NavKey; icon: React.ReactNode; label: string; path: string }[] = [
  { key: 'chat', icon: <MessageOutlined />, label: '消息', path: '/chat' },
  { key: 'contacts', icon: <ContactsOutlined />, label: '通讯录', path: '/contacts' },
  { key: 'settings', icon: <SettingOutlined />, label: '设置', path: '/settings' },
];

/** 三栏主布局：侧边栏 + 内容区 */
const ChatLayout: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const user = useAuthStore((s) => s.user);
  const token = useAuthStore((s) => s.token);
  const connected = useWebSocketStore((s) => s.connected);
  const wsConnect = useWebSocketStore((s) => s.connect);
  const conversations = useConversationStore((s) => s.conversations);

  // 计算总未读数
  const totalUnread = conversations.reduce((sum, c) => sum + (c.isMuted ? 0 : c.unreadCount), 0);

  // 当前激活的导航
  const [activeNav, setActiveNav] = useState<NavKey>(() => {
    if (location.pathname.startsWith('/contacts')) return 'contacts';
    if (location.pathname.startsWith('/settings')) return 'settings';
    return 'chat';
  });

  // 连接 WebSocket
  useEffect(() => {
    if (token && !connected) {
      wsConnect(token);
    }
  }, [token, connected, wsConnect]);

  const handleNav = (item: typeof navItems[number]) => {
    setActiveNav(item.key);
    navigate(item.path);
  };

  return (
    <div className="app-layout">
      {/* 侧边导航栏 */}
      <div className="sidebar">
        <Avatar
          className="sidebar-avatar"
          size={40}
          src={user?.avatar || undefined}
          icon={!user?.avatar ? <UserOutlined /> : undefined}
          shape="square"
        />

        {navItems.map((item) => (
          <Tooltip key={item.key} title={item.label} placement="right">
            <div
              className={`sidebar-nav-item${activeNav === item.key ? ' active' : ''}`}
              onClick={() => handleNav(item)}
            >
              {item.key === 'chat' && totalUnread > 0 ? (
                <Badge count={totalUnread} size="small" offset={[8, -4]}>
                  {item.icon}
                </Badge>
              ) : (
                item.icon
              )}
            </div>
          </Tooltip>
        ))}

        <div className="sidebar-spacer" />

        {/* 连接状态指示 */}
        <Tooltip title={connected ? '已连接' : '未连接'} placement="right">
          <div className="sidebar-nav-item">
            <span className={`online-dot ${connected ? 'online' : 'offline'}`} />
          </div>
        </Tooltip>
      </div>

      {/* 内容区域 */}
      <Outlet />
    </div>
  );
};

export default ChatLayout;
