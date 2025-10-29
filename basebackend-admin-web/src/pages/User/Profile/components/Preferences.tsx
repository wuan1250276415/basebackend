import { useState, useEffect } from 'react'
import { Card, Form, Select, Switch, Button, message, Space, Alert, Divider } from 'antd'
import { SettingOutlined, BulbOutlined, GlobalOutlined, BellOutlined } from '@ant-design/icons'
import { getPreference, updatePreference, UserPreference } from '@/api/preference'

const Preferences = () => {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [preference, setPreference] = useState<UserPreference | null>(null)

  // 加载偏好设置
  const loadPreference = async () => {
    try {
      const response = await getPreference()
      setPreference(response.data)
      form.setFieldsValue(response.data)
    } catch (error) {
      message.error('加载偏好设置失败')
    }
  }

  useEffect(() => {
    loadPreference()
  }, [])

  // 提交更新
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)

      await updatePreference(values)
      message.success('偏好设置已保存')
      loadPreference()
    } catch (error: any) {
      if (error.errorFields) {
        message.error('请填写必填项')
      } else {
        message.error('保存偏好设置失败')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Card title={<><SettingOutlined /> 个性化设置</>}>
        <Alert
          message="个性化您的体验"
          description="自定义界面主题、语言和通知偏好，让系统更贴合您的使用习惯。"
          type="info"
          showIcon
          style={{ marginBottom: 24 }}
        />

        <Form
          form={form}
          layout="vertical"
          style={{ maxWidth: 600 }}
          onFinish={handleSubmit}
        >
          {/* 外观设置 */}
          <Divider orientation="left">
            <BulbOutlined /> 外观设置
          </Divider>

          <Form.Item
            label="主题模式"
            name="theme"
            tooltip="选择您喜欢的主题模式"
          >
            <Select size="large">
              <Select.Option value="light">浅色模式</Select.Option>
              <Select.Option value="dark">深色模式</Select.Option>
              <Select.Option value="auto">跟随系统</Select.Option>
            </Select>
          </Form.Item>

          {/* 语言设置 */}
          <Divider orientation="left">
            <GlobalOutlined /> 语言设置
          </Divider>

          <Form.Item
            label="界面语言"
            name="language"
            tooltip="选择系统界面显示语言"
          >
            <Select size="large">
              <Select.Option value="zh-CN">简体中文</Select.Option>
              <Select.Option value="en-US">English</Select.Option>
            </Select>
          </Form.Item>

          {/* 通知设置 */}
          <Divider orientation="left">
            <BellOutlined /> 通知设置
          </Divider>

          <Form.Item
            label="邮件通知"
            name="emailNotification"
            valuePropName="checked"
            tooltip="接收重要消息的邮件通知"
            getValueFromEvent={(checked) => (checked ? 1 : 0)}
            getValueProps={(value) => ({ checked: value === 1 })}
          >
            <Switch />
          </Form.Item>

          <Form.Item
            label="短信通知"
            name="smsNotification"
            valuePropName="checked"
            tooltip="接收重要消息的短信通知"
            getValueFromEvent={(checked) => (checked ? 1 : 0)}
            getValueProps={(value) => ({ checked: value === 1 })}
          >
            <Switch />
          </Form.Item>

          <Form.Item
            label="系统通知"
            name="systemNotification"
            valuePropName="checked"
            tooltip="在系统内接收消息通知"
            getValueFromEvent={(checked) => (checked ? 1 : 0)}
            getValueProps={(value) => ({ checked: value === 1 })}
          >
            <Switch />
          </Form.Item>

          <Form.Item style={{ marginTop: 32 }}>
            <Space>
              <Button type="primary" htmlType="submit" loading={loading} size="large">
                保存设置
              </Button>
              <Button onClick={() => form.resetFields()} size="large">
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>
    </Space>
  )
}

export default Preferences
