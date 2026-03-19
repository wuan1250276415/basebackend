import React, { useState } from 'react';
import { Form, Input, Button, message } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/useAuthStore';
import http from '@/services/api';
import type { ApiResult } from '@/types';

interface LoginForm {
  username: string;
  password: string;
}

/** 登录页 */
const LoginPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const login = useAuthStore((s) => s.login);

  const handleLogin = async (values: LoginForm) => {
    setLoading(true);
    try {
      // 调用平台登录接口（由 user-api 提供）
      const resp = await http.post<ApiResult<{
        token: string;
        userId: number;
        nickname: string;
        avatar: string;
        tenantId: string;
      }>>('/auth/login', values, { baseURL: '/api' });

      const { token, userId, nickname, avatar, tenantId } = resp.data.data;
      login({ userId, nickname, avatar, tenantId: tenantId ?? '0' }, token);
      message.success('登录成功');
      navigate('/chat', { replace: true });
    } catch (err) {
      console.error(err);
      message.error('登录失败，请检查用户名和密码');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-card">
        <h1 className="login-title">BaseBackend Chat</h1>
        <Form<LoginForm> onFinish={handleLogin} autoComplete="off" size="large">
          <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input prefix={<UserOutlined />} placeholder="用户名" />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="密码" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block>
              登录
            </Button>
          </Form.Item>
        </Form>
      </div>
    </div>
  );
};

export default LoginPage;
