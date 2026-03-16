import React from 'react';
import { Switch, Button, message } from 'antd';
import { LogoutOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useThemeStore } from '@/stores/useThemeStore';
import { useAuthStore } from '@/stores/useAuthStore';
import { useWebSocketStore } from '@/stores/useWebSocketStore';

/** 设置页 */
const SettingsPage: React.FC = () => {
  const navigate = useNavigate();
  const { theme, toggleTheme } = useThemeStore();
  const logout = useAuthStore((s) => s.logout);
  const disconnect = useWebSocketStore((s) => s.disconnect);

  const handleLogout = () => {
    disconnect();
    logout();
    message.info('已退出登录');
    navigate('/login', { replace: true });
  };

  return (
    <div className="contacts-page">
      <div className="contacts-header">设置</div>
      <div className="settings-content">
        <div className="settings-section">
          <div className="settings-section-title">外观</div>
          <div className="settings-row">
            <span className="settings-label">暗色模式</span>
            <Switch checked={theme === 'dark'} onChange={toggleTheme} />
          </div>
        </div>

        <div className="settings-section">
          <div className="settings-section-title">账号</div>
          <div className="settings-row">
            <span className="settings-label">退出登录</span>
            <Button danger icon={<LogoutOutlined />} onClick={handleLogout}>
              退出
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SettingsPage;
