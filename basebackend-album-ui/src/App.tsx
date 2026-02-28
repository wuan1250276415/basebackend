import React from 'react';
import { RouterProvider } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import router from './router';

function App() {
  return (
    <ConfigProvider theme={{ token: { colorPrimary: '#ff8c00', borderRadius: 12 } }}>
      <RouterProvider router={router} />
    </ConfigProvider>
  );
}

export default App;
