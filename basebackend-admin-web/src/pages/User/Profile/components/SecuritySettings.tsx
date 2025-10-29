import { useState, useEffect } from 'react'
import {
  Card,
  Form,
  Input,
  Button,
  message,
  Alert,
  Space,
  Table,
  Tag,
  Popconfirm,
  Tabs,
} from 'antd'
import {
  LockOutlined,
  SafetyOutlined,
  LaptopOutlined,
  HistoryOutlined,
  DeleteOutlined,
  CheckOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { ProfileDetail, ChangePasswordRequest } from '@/types'
import { changePassword } from '@/api/profile'
import {
  getUserDevices,
  removeDevice,
  trustDevice,
  getOperationLogs,
  UserDevice,
  UserOperationLog,
} from '@/api/security'

interface SecuritySettingsProps {
  data: ProfileDetail
}

const SecuritySettings = ({ data }: SecuritySettingsProps) => {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [devices, setDevices] = useState<UserDevice[]>([])
  const [devicesLoading, setDevicesLoading] = useState(false)
  const [logs, setLogs] = useState<UserOperationLog[]>([])
  const [logsLoading, setLogsLoading] = useState(false)

  // 加载设备列表
  const loadDevices = async () => {
    setDevicesLoading(true)
    try {
      const response = await getUserDevices()
      setDevices(response.data)
    } catch (error) {
      message.error('加载设备列表失败')
    } finally {
      setDevicesLoading(false)
    }
  }

  // 加载操作日志
  const loadLogs = async () => {
    setLogsLoading(true)
    try {
      const response = await getOperationLogs(50)
      setLogs(response.data)
    } catch (error) {
      message.error('加载操作日志失败')
    } finally {
      setLogsLoading(false)
    }
  }

  useEffect(() => {
    loadDevices()
    loadLogs()
  }, [])

  // 提交密码修改
  const handleChangePassword = async () => {
    try {
      const values: ChangePasswordRequest = await form.validateFields()
      setLoading(true)

      await changePassword(values)
      message.success('密码修改成功，请重新登录')
      form.resetFields()

      // 可选：3秒后跳转到登录页
      setTimeout(() => {
        window.location.href = '/login'
      }, 3000)
    } catch (error: any) {
      if (error.errorFields) {
        message.error('请填写必填项')
      } else {
        message.error('密码修改失败')
      }
    } finally {
      setLoading(false)
    }
  }

  // 移除设备
  const handleRemoveDevice = async (deviceId: number) => {
    try {
      await removeDevice(deviceId)
      message.success('设备已移除')
      loadDevices()
    } catch (error) {
      message.error('移除设备失败')
    }
  }

  // 信任设备
  const handleTrustDevice = async (deviceId: number) => {
    try {
      await trustDevice(deviceId)
      message.success('设备已设为信任')
      loadDevices()
    } catch (error) {
      message.error('信任设备失败')
    }
  }

  // 设备列表列定义
  const deviceColumns: ColumnsType<UserDevice> = [
    {
      title: '设备类型',
      dataIndex: 'deviceType',
      key: 'deviceType',
      width: 100,
    },
    {
      title: '设备名称',
      dataIndex: 'deviceName',
      key: 'deviceName',
    },
    {
      title: '浏览器',
      dataIndex: 'browser',
      key: 'browser',
      width: 150,
    },
    {
      title: '操作系统',
      dataIndex: 'os',
      key: 'os',
      width: 150,
    },
    {
      title: 'IP地址',
      dataIndex: 'ipAddress',
      key: 'ipAddress',
      width: 150,
    },
    {
      title: '位置',
      dataIndex: 'location',
      key: 'location',
      width: 120,
    },
    {
      title: '状态',
      dataIndex: 'isTrusted',
      key: 'isTrusted',
      width: 100,
      render: (isTrusted: number) =>
        isTrusted === 1 ? (
          <Tag color="green">已信任</Tag>
        ) : (
          <Tag color="default">未信任</Tag>
        ),
    },
    {
      title: '最后活跃',
      dataIndex: 'lastActiveTime',
      key: 'lastActiveTime',
      width: 180,
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_, record) => (
        <Space>
          {record.isTrusted === 0 && (
            <Button
              type="link"
              size="small"
              icon={<CheckOutlined />}
              onClick={() => handleTrustDevice(record.id)}
            >
              信任
            </Button>
          )}
          <Popconfirm
            title="确定要移除此设备吗?"
            onConfirm={() => handleRemoveDevice(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" danger size="small" icon={<DeleteOutlined />}>
              移除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  // 操作日志列定义
  const logColumns: ColumnsType<UserOperationLog> = [
    {
      title: '操作类型',
      dataIndex: 'operationType',
      key: 'operationType',
      width: 120,
    },
    {
      title: '操作描述',
      dataIndex: 'operationDesc',
      key: 'operationDesc',
    },
    {
      title: 'IP地址',
      dataIndex: 'ipAddress',
      key: 'ipAddress',
      width: 150,
    },
    {
      title: '位置',
      dataIndex: 'location',
      key: 'location',
      width: 120,
    },
    {
      title: '浏览器',
      dataIndex: 'browser',
      key: 'browser',
      width: 150,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status: number) =>
        status === 1 ? (
          <Tag color="success">成功</Tag>
        ) : (
          <Tag color="error">失败</Tag>
        ),
    },
    {
      title: '操作时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
    },
  ]

  const tabItems = [
    {
      key: 'password',
      label: (
        <span>
          <LockOutlined />
          修改密码
        </span>
      ),
      children: (
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          {/* 账号安全信息 */}
          <Alert
            message="账号安全提示"
            description={
              <div>
                <p>• 定期修改密码可以提高账号安全性</p>
                <p>• 密码必须包含大小写字母和数字，长度6-20位</p>
                <p>• 不要使用过于简单的密码，如生日、手机号等</p>
                <p>• 最后登录时间: {data.loginTime || '无'}</p>
                <p>• 最后登录IP: {data.loginIp || '无'}</p>
              </div>
            }
            type="info"
            showIcon
          />

          {/* 修改密码表单 */}
          <Form
            form={form}
            layout="vertical"
            style={{ maxWidth: 500 }}
            onFinish={handleChangePassword}
          >
            <Form.Item
              label="当前密码"
              name="oldPassword"
              rules={[{ required: true, message: '请输入当前密码' }]}
            >
              <Input.Password
                placeholder="请输入当前密码"
                prefix={<LockOutlined />}
                size="large"
              />
            </Form.Item>

            <Form.Item
              label="新密码"
              name="newPassword"
              rules={[
                { required: true, message: '请输入新密码' },
                { min: 6, max: 20, message: '密码长度必须在6-20个字符之间' },
                {
                  pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d@$!%*?&]{6,20}$/,
                  message: '密码必须包含大小写字母和数字',
                },
              ]}
            >
              <Input.Password
                placeholder="请输入新密码"
                prefix={<LockOutlined />}
                size="large"
              />
            </Form.Item>

            <Form.Item
              label="确认新密码"
              name="confirmPassword"
              dependencies={['newPassword']}
              rules={[
                { required: true, message: '请确认新密码' },
                ({ getFieldValue }) => ({
                  validator(_, value) {
                    if (!value || getFieldValue('newPassword') === value) {
                      return Promise.resolve()
                    }
                    return Promise.reject(new Error('两次输入的密码不一致'))
                  },
                }),
              ]}
            >
              <Input.Password
                placeholder="请再次输入新密码"
                prefix={<LockOutlined />}
                size="large"
              />
            </Form.Item>

            <Form.Item>
              <Space>
                <Button type="primary" htmlType="submit" loading={loading} size="large">
                  修改密码
                </Button>
                <Button onClick={() => form.resetFields()} size="large">
                  重置
                </Button>
              </Space>
            </Form.Item>
          </Form>
        </Space>
      ),
    },
    {
      key: 'devices',
      label: (
        <span>
          <LaptopOutlined />
          设备管理
        </span>
      ),
      children: (
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          <Alert
            message="设备管理说明"
            description="您可以查看和管理所有登录过您账号的设备，移除可疑设备以保护账号安全。"
            type="info"
            showIcon
          />
          <Table
            columns={deviceColumns}
            dataSource={devices}
            rowKey="id"
            loading={devicesLoading}
            scroll={{ x: 1200 }}
            pagination={{
              pageSize: 10,
              showTotal: (total) => `共 ${total} 台设备`,
            }}
          />
        </Space>
      ),
    },
    {
      key: 'logs',
      label: (
        <span>
          <HistoryOutlined />
          操作日志
        </span>
      ),
      children: (
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          <Alert
            message="操作日志说明"
            description="记录您账号的重要操作，帮助您追踪账号活动。"
            type="info"
            showIcon
          />
          <Table
            columns={logColumns}
            dataSource={logs}
            rowKey="id"
            loading={logsLoading}
            scroll={{ x: 1200 }}
            pagination={{
              pageSize: 10,
              showTotal: (total) => `共 ${total} 条记录`,
            }}
          />
        </Space>
      ),
    },
  ]

  return (
    <Card title={<><SafetyOutlined /> 安全设置</>}>
      <Tabs items={tabItems} defaultActiveKey="password" />
    </Card>
  )
}

export default SecuritySettings
