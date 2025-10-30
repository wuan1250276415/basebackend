import { useState, useEffect } from 'react'
import { Card, Form, Select, Switch, Button, message, Space, Alert, Divider, ColorPicker, InputNumber, Radio } from 'antd'
import { SettingOutlined, BulbOutlined, GlobalOutlined, BellOutlined, LayoutOutlined, DatabaseOutlined, DashboardOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { getPreference, updatePreference, UserPreference } from '@/api/preference'
import { useThemeStore } from '@/stores/theme'
import { presetColors } from '@/config/theme'
import type { Color } from 'antd/es/color-picker'

const Preferences = () => {
  const { t, i18n } = useTranslation()
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [preference, setPreference] = useState<UserPreference | null>(null)

  const { mode, primaryColor, layout, menuCollapsed, setMode, setPrimaryColor, setLayout, setMenuCollapsed } = useThemeStore()

  // 加载偏好设置
  const loadPreference = async () => {
    try {
      const response = await getPreference()
      setPreference(response.data)

      // 设置表单值
      form.setFieldsValue({
        ...response.data,
        theme: mode,
        primaryColor: primaryColor,
        layout: layout,
        menuCollapse: menuCollapsed ? 1 : 0,
      })
    } catch (error) {
      message.error(t('preferences.saveFailed'))
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

      // 更新主题 store
      if (values.theme !== mode) {
        setMode(values.theme)
      }
      if (values.primaryColor !== primaryColor) {
        setPrimaryColor(values.primaryColor)
      }
      if (values.layout !== layout) {
        setLayout(values.layout)
      }
      if (values.menuCollapse !== (menuCollapsed ? 1 : 0)) {
        setMenuCollapsed(values.menuCollapse === 1)
      }

      // 更新语言
      if (values.language && values.language !== i18n.language) {
        i18n.changeLanguage(values.language)
      }

      await updatePreference(values)
      message.success(t('preferences.saveSuccess'))
      loadPreference()
    } catch (error: any) {
      if (error.errorFields) {
        message.error(t('validation.required'))
      } else {
        message.error(t('preferences.saveFailed'))
      }
    } finally {
      setLoading(false)
    }
  }

  // 处理主题色变化（实时预览）
  const handlePrimaryColorChange = (color: Color) => {
    const hexColor = color.toHexString()
    form.setFieldValue('primaryColor', hexColor)
    setPrimaryColor(hexColor)
  }

  // 处理主题模式变化（实时预览）
  const handleThemeChange = (value: string) => {
    form.setFieldValue('theme', value)
    setMode(value as any)
  }

  // 处理语言变化（实时预览）
  const handleLanguageChange = (value: string) => {
    form.setFieldValue('language', value)
    i18n.changeLanguage(value)
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Card title={<><SettingOutlined /> {t('preferences.title')}</>}>
        <Alert
          message={t('preferences.title')}
          description={t('common.tips')}
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
            <BulbOutlined /> {t('preferences.appearance')}
          </Divider>

          <Form.Item
            label={t('preferences.theme')}
            name="theme"
            tooltip={t('preferences.theme')}
          >
            <Select size="large" onChange={handleThemeChange}>
              <Select.Option value="light">{t('preferences.light')}</Select.Option>
              <Select.Option value="dark">{t('preferences.dark')}</Select.Option>
              <Select.Option value="auto">{t('preferences.auto')}</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item
            label={t('preferences.primaryColor')}
            name="primaryColor"
            tooltip={t('preferences.primaryColor')}
          >
            <ColorPicker
              value={primaryColor}
              onChange={handlePrimaryColorChange}
              presets={[
                {
                  label: t('preferences.primaryColor'),
                  colors: presetColors.map(c => c.value),
                },
              ]}
              showText
              size="large"
            />
          </Form.Item>

          <Form.Item
            label={t('preferences.layout')}
            name="layout"
            tooltip={t('preferences.layout')}
          >
            <Radio.Group size="large">
              <Radio.Button value="side">{t('preferences.sideMenu')}</Radio.Button>
              <Radio.Button value="top">{t('preferences.topMenu')}</Radio.Button>
              <Radio.Button value="mix">{t('preferences.mixMenu')}</Radio.Button>
            </Radio.Group>
          </Form.Item>

          <Form.Item
            label={t('preferences.menuCollapse')}
            name="menuCollapse"
            valuePropName="checked"
            tooltip={t('preferences.menuCollapse')}
            getValueFromEvent={(checked) => (checked ? 1 : 0)}
            getValueProps={(value) => ({ checked: value === 1 })}
          >
            <Switch />
          </Form.Item>

          {/* 语言与地区 */}
          <Divider orientation="left">
            <GlobalOutlined /> {t('preferences.languageRegion')}
          </Divider>

          <Form.Item
            label={t('preferences.language')}
            name="language"
            tooltip={t('preferences.language')}
          >
            <Select size="large" onChange={handleLanguageChange}>
              <Select.Option value="zh-CN">{t('preferences.chinese')}</Select.Option>
              <Select.Option value="en-US">{t('preferences.english')}</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item
            label={t('preferences.timezone')}
            name="timezone"
            tooltip={t('preferences.timezone')}
          >
            <Select size="large">
              <Select.Option value="Asia/Shanghai">UTC+8 (北京)</Select.Option>
              <Select.Option value="Asia/Tokyo">UTC+9 (东京)</Select.Option>
              <Select.Option value="America/New_York">UTC-5 (纽约)</Select.Option>
              <Select.Option value="Europe/London">UTC+0 (伦敦)</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item
            label={t('preferences.dateFormat')}
            name="dateFormat"
            tooltip={t('preferences.dateFormat')}
          >
            <Select size="large">
              <Select.Option value="YYYY-MM-DD">YYYY-MM-DD</Select.Option>
              <Select.Option value="MM/DD/YYYY">MM/DD/YYYY</Select.Option>
              <Select.Option value="DD/MM/YYYY">DD/MM/YYYY</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item
            label={t('preferences.timeFormat')}
            name="timeFormat"
            tooltip={t('preferences.timeFormat')}
          >
            <Radio.Group size="large">
              <Radio.Button value="12">{t('preferences.hour12')}</Radio.Button>
              <Radio.Button value="24">{t('preferences.hour24')}</Radio.Button>
            </Radio.Group>
          </Form.Item>

          {/* 通知设置 */}
          <Divider orientation="left">
            <BellOutlined /> {t('preferences.notification')}
          </Divider>

          <Form.Item
            label={t('preferences.emailNotification')}
            name="emailNotification"
            valuePropName="checked"
            tooltip={t('preferences.emailNotification')}
            getValueFromEvent={(checked) => (checked ? 1 : 0)}
            getValueProps={(value) => ({ checked: value === 1 })}
          >
            <Switch />
          </Form.Item>

          <Form.Item
            label={t('preferences.smsNotification')}
            name="smsNotification"
            valuePropName="checked"
            tooltip={t('preferences.smsNotification')}
            getValueFromEvent={(checked) => (checked ? 1 : 0)}
            getValueProps={(value) => ({ checked: value === 1 })}
          >
            <Switch />
          </Form.Item>

          <Form.Item
            label={t('preferences.systemNotification')}
            name="systemNotification"
            valuePropName="checked"
            tooltip={t('preferences.systemNotification')}
            getValueFromEvent={(checked) => (checked ? 1 : 0)}
            getValueProps={(value) => ({ checked: value === 1 })}
          >
            <Switch />
          </Form.Item>

          {/* 数据显示 */}
          <Divider orientation="left">
            <DatabaseOutlined /> {t('preferences.dataDisplay')}
          </Divider>

          <Form.Item
            label={t('preferences.pageSize')}
            name="pageSize"
            tooltip={t('preferences.pageSize')}
          >
            <InputNumber
              min={10}
              max={100}
              step={10}
              size="large"
              style={{ width: '100%' }}
            />
          </Form.Item>

          {/* 其他设置 */}
          <Divider orientation="left">
            <DashboardOutlined /> {t('preferences.other')}
          </Divider>

          <Form.Item
            label={t('preferences.autoSave')}
            name="autoSave"
            valuePropName="checked"
            tooltip={t('preferences.autoSave')}
            getValueFromEvent={(checked) => (checked ? 1 : 0)}
            getValueProps={(value) => ({ checked: value === 1 })}
          >
            <Switch />
          </Form.Item>

          <Form.Item style={{ marginTop: 32 }}>
            <Space>
              <Button type="primary" htmlType="submit" loading={loading} size="large">
                {t('common.save')}
              </Button>
              <Button onClick={() => form.resetFields()} size="large">
                {t('common.reset')}
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>
    </Space>
  )
}

export default Preferences
