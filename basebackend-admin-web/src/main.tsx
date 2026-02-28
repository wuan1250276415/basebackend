// 应用入口文件
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './styles/global.less';

// 使用 React 18 createRoot API 渲染应用
ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
