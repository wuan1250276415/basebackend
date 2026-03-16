/**
 * 登录页面
 * 居中登录卡片 + 动画渐变背景
 * 使用 Ant Design Form 进行表单验证和提交
 */
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, Button, Card, Alert } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useAuthStore } from '@/stores/authStore';
import './index.css';

const Login: React.FC = () => {
  const navigate = useNavigate();
  const login = useAuthStore((s) => s.login);
  const [loading, setLoading] = useState(false);
  /** 服务端返回的错误信息 */
  const [errorMsg, setErrorMsg] = useState<string>('');

  /** 表单提交：调用 authStore.login，成功跳转首页，失败显示错误 */
  const onFinish = async (values: { username: string; password: string }) => {
    setErrorMsg('');
    setLoading(true);
    try {
      await login(values.username, values.password);
      // 登录成功，跳转到仪表盘首页
      navigate('/', { replace: true });
    } catch (err: unknown) {
      // authStore.login 失败时抛出异常，提取错误信息显示在表单上
      const message =
        err instanceof Error ? err.message : '登录失败，请稍后重试';
      setErrorMsg(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-box">
        <Card className="login-card" bordered={false}>
          <div className="login-header">
            <h1>后台管理系统</h1>
            <p>BaseBackend Admin System</p>
          </div>

          {/* 服务端错误信息展示 */}
          {errorMsg && (
            <Alert
              message={errorMsg}
              type="error"
              showIcon
              closable
              onClose={() => setErrorMsg('')}
              style={{ marginBottom: 24 }}
            />
          )}

          <Form
            name="login"
            onFinish={onFinish}
            size="large"
            autoComplete="off"
          >
            <Form.Item
              name="username"
              rules={[{ required: true, message: '请输入用户名' }]}
            >
              <Input
                prefix={<UserOutlined />}
                placeholder="用户名"
                autoComplete="username"
              />
            </Form.Item>

            <Form.Item
              name="password"
              rules={[{ required: true, message: '请输入密码' }]}
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder="密码"
                autoComplete="current-password"
              />
            </Form.Item>

            <Form.Item>
              <Button
                type="primary"
                htmlType="submit"
                loading={loading}
                block
              >
                登 录
              </Button>
            </Form.Item>
          </Form>
        </Card>
      </div>
    </div>
  );
};

export default Login;
