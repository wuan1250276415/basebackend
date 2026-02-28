/**
 * 应用根组件
 * 集成 BrowserRouter、Ant Design ConfigProvider（全局主题）和路由配置
 */
import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { ConfigProvider, theme } from 'antd';
import { useSettingStore } from '@/stores/settingStore';
import AppRoutes from '@/router';

const App: React.FC = () => {
  const { theme: appTheme, primaryColor } = useSettingStore();

  return (
    <ConfigProvider
      theme={{
        /* 根据用户偏好切换暗色/亮色算法 */
        algorithm: appTheme === 'dark' ? theme.darkAlgorithm : theme.defaultAlgorithm,
        token: {
          colorPrimary: primaryColor, // #1677ff
          borderRadius: 8,
        },
      }}
    >
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </ConfigProvider>
  );
};

export default App;
