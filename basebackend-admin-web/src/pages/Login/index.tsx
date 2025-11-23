import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Form, Input, Button, Card, message } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import { login } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { LoginRequest } from '@/types'
import './index.css'

const Login = () => {
  const navigate = useNavigate()
  const { setToken, setUserInfo, setPermissions, setRoles } = useAuthStore()
  const [loading, setLoading] = useState(false)

  const onFinish = async (values: LoginRequest) => {
    setLoading(true)
    try {
      const response = await login(values)
      const { accessToken, userInfo, permissions, roles } = response.data

      // 保存认证信息
      setToken(accessToken)
      setUserInfo(userInfo)
      setPermissions(permissions)
      setRoles(roles)
      localStorage.setItem('token', accessToken)
      localStorage.setItem('userInfo', JSON.stringify(userInfo))

      message.success('登录成功')
      navigate('/dashboard')
    } catch (error: any) {
      message.error(error.message || '登录失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-container">
      <div className="login-box">
        <Card className="login-card">
          <div className="login-header">
            <h1>后台管理系统</h1>
            <p>BaseBackend Admin System</p>
          </div>
          <Form
            name="login"
            initialValues={{ username: 'admin', password: 'password' }}
            onFinish={onFinish}
            size="large"
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
              <Button type="primary" htmlType="submit" loading={loading} block>
                登录
              </Button>
            </Form.Item>
          </Form>
          <div className="login-footer">
            <p>默认账号: admin / password</p>
          </div>
        </Card>
      </div>
    </div>
  )
}

export default Login
