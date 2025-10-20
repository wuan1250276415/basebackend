import { useState, useEffect } from 'react'
import { Card, Button, Table, Modal, Form, Input, Select, InputNumber, Switch, Space, message, Tag, Popconfirm } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, ExperimentOutlined } from '@ant-design/icons'
import {
  getAllAlertRules,
  registerAlertRule,
  deleteAlertRule,
  getRecentAlerts,
  testAlertRule,
  AlertRule,
  AlertEvent
} from '@/api/observability/alerts'
import dayjs from 'dayjs'

const { Option } = Select
const { TextArea } = Input

/**
 * 告警管理页面
 */
const AlertManagement = () => {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [rules, setRules] = useState<AlertRule[]>([])
  const [events, setEvents] = useState<AlertEvent[]>([])
  const [modalVisible, setModalVisible] = useState(false)
  const [editingRule, setEditingRule] = useState<AlertRule | null>(null)

  // 加载告警规则
  const loadRules = async () => {
    try {
      setLoading(true)
      const res = await getAllAlertRules()
      setRules(res.data || [])
    } catch (error: any) {
      message.error(error.message || '加载告警规则失败')
    } finally {
      setLoading(false)
    }
  }

  // 加载告警事件
  const loadEvents = async () => {
    try {
      const res = await getRecentAlerts()
      setEvents(res.data || [])
    } catch (error: any) {
      console.error('加载告警事件失败:', error)
    }
  }

  useEffect(() => {
    loadRules()
    loadEvents()
    // 每30秒刷新事件
    const interval = setInterval(loadEvents, 30000)
    return () => clearInterval(interval)
  }, [])

  // 打开新增/编辑弹窗
  const handleOpenModal = (rule?: AlertRule) => {
    if (rule) {
      setEditingRule(rule)
      form.setFieldsValue(rule)
    } else {
      setEditingRule(null)
      form.resetFields()
    }
    setModalVisible(true)
  }

  // 保存规则
  const handleSaveRule = async (values: any) => {
    try {
      await registerAlertRule(values)
      message.success('保存成功')
      setModalVisible(false)
      form.resetFields()
      loadRules()
    } catch (error: any) {
      message.error(error.message || '保存失败')
    }
  }

  // 删除规则
  const handleDeleteRule = async (ruleId: number) => {
    try {
      await deleteAlertRule(ruleId)
      message.success('删除成功')
      loadRules()
    } catch (error: any) {
      message.error(error.message || '删除失败')
    }
  }

  // 测试规则
  const handleTestRule = async (rule: AlertRule) => {
    try {
      const res = await testAlertRule(rule)
      Modal.info({
        title: '测试结果',
        content: (
          <pre style={{ background: '#f5f5f5', padding: '12px', borderRadius: '4px' }}>
            {JSON.stringify(res.data, null, 2)}
          </pre>
        ),
        width: 600
      })
    } catch (error: any) {
      message.error(error.message || '测试失败')
    }
  }

  // 规则表格列
  const ruleColumns = [
    {
      title: '规则名称',
      dataIndex: 'ruleName',
      key: 'ruleName',
      width: 200
    },
    {
      title: '类型',
      dataIndex: 'ruleType',
      key: 'ruleType',
      width: 120,
      render: (type: string) => <Tag>{type}</Tag>
    },
    {
      title: '指标',
      dataIndex: 'metricName',
      key: 'metricName',
      width: 200
    },
    {
      title: '阈值',
      key: 'threshold',
      width: 150,
      render: (_: any, record: AlertRule) => (
        <span>{record.comparisonOperator} {record.thresholdValue}</span>
      )
    },
    {
      title: '级别',
      dataIndex: 'severity',
      key: 'severity',
      width: 100,
      render: (severity: string) => (
        <Tag color={getSeverityColor(severity)}>{severity}</Tag>
      )
    },
    {
      title: '通知渠道',
      dataIndex: 'notifyChannels',
      key: 'notifyChannels',
      width: 150
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      key: 'enabled',
      width: 80,
      render: (enabled: boolean) => (
        <Tag color={enabled ? 'green' : 'red'}>
          {enabled ? '启用' : '禁用'}
        </Tag>
      )
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      fixed: 'right' as const,
      render: (_: any, record: AlertRule) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<ExperimentOutlined />}
            onClick={() => handleTestRule(record)}
          >
            测试
          </Button>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleOpenModal(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定删除此规则？"
            onConfirm={() => handleDeleteRule(record.id!)}
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      )
    }
  ]

  // 事件表格列
  const eventColumns = [
    {
      title: '时间',
      dataIndex: 'alertTime',
      key: 'alertTime',
      width: 180,
      render: (time: string) => dayjs(time).format('YYYY-MM-DD HH:mm:ss')
    },
    {
      title: '规则',
      dataIndex: 'ruleName',
      key: 'ruleName',
      width: 200
    },
    {
      title: '级别',
      dataIndex: 'severity',
      key: 'severity',
      width: 100,
      render: (severity: string) => (
        <Tag color={getSeverityColor(severity)}>{severity}</Tag>
      )
    },
    {
      title: '消息',
      dataIndex: 'message',
      key: 'message',
      ellipsis: true
    },
    {
      title: '触发值',
      dataIndex: 'triggerValue',
      key: 'triggerValue',
      width: 100
    },
    {
      title: '通知状态',
      dataIndex: 'notifyStatus',
      key: 'notifyStatus',
      width: 100,
      render: (status: string) => (
        <Tag color={status === 'SUCCESS' ? 'green' : 'red'}>{status}</Tag>
      )
    }
  ]

  const getSeverityColor = (severity: string) => {
    const colors: Record<string, string> = {
      CRITICAL: 'red',
      ERROR: 'orange',
      WARNING: 'gold',
      INFO: 'blue'
    }
    return colors[severity] || 'default'
  }

  return (
    <div style={{ padding: '24px' }}>
      {/* 告警规则 */}
      <Card
        title="告警规则"
        extra={
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => handleOpenModal()}
          >
            新增规则
          </Button>
        }
        style={{ marginBottom: '24px' }}
      >
        <Table
          columns={ruleColumns}
          dataSource={rules}
          loading={loading}
          rowKey="id"
          scroll={{ x: 1200 }}
        />
      </Card>

      {/* 最近告警事件 */}
      <Card title="最近告警事件">
        <Table
          columns={eventColumns}
          dataSource={events}
          rowKey="id"
          scroll={{ x: 1000 }}
        />
      </Card>

      {/* 新增/编辑规则弹窗 */}
      <Modal
        title={editingRule ? '编辑告警规则' : '新增告警规则'}
        open={modalVisible}
        onOk={() => form.submit()}
        onCancel={() => {
          setModalVisible(false)
          form.resetFields()
        }}
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSaveRule}
          initialValues={{
            ruleType: 'THRESHOLD',
            comparisonOperator: '>',
            severity: 'WARNING',
            enabled: true,
            notifyChannels: 'email'
          }}
        >
          <Form.Item name="id" hidden>
            <Input />
          </Form.Item>

          <Form.Item
            name="ruleName"
            label="规则名称"
            rules={[{ required: true, message: '请输入规则名称' }]}
          >
            <Input placeholder="如：CPU使用率过高" />
          </Form.Item>

          <Form.Item
            name="ruleType"
            label="规则类型"
            rules={[{ required: true }]}
          >
            <Select>
              <Option value="THRESHOLD">阈值告警</Option>
              <Option value="LOG">日志告警</Option>
              <Option value="CUSTOM">自定义告警</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="metricName"
            label="指标名称"
            rules={[{ required: true, message: '请输入指标名称' }]}
          >
            <Input placeholder="如：system_cpu_usage" />
          </Form.Item>

          <Form.Item label="阈值条件">
            <Input.Group compact>
              <Form.Item
                name="comparisonOperator"
                noStyle
                rules={[{ required: true }]}
              >
                <Select style={{ width: '30%' }}>
                  <Option value=">">{'>'}</Option>
                  <Option value=">=">{'>='}</Option>
                  <Option value="<">{'<'}</Option>
                  <Option value="<=">{'<='}</Option>
                  <Option value="==">{'=='}</Option>
                </Select>
              </Form.Item>
              <Form.Item
                name="thresholdValue"
                noStyle
                rules={[{ required: true, message: '请输入阈值' }]}
              >
                <InputNumber
                  style={{ width: '70%' }}
                  placeholder="阈值"
                />
              </Form.Item>
            </Input.Group>
          </Form.Item>

          <Form.Item
            name="severity"
            label="告警级别"
            rules={[{ required: true }]}
          >
            <Select>
              <Option value="INFO">INFO</Option>
              <Option value="WARNING">WARNING</Option>
              <Option value="ERROR">ERROR</Option>
              <Option value="CRITICAL">CRITICAL</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="notifyChannels"
            label="通知渠道"
            rules={[{ required: true, message: '请输入通知渠道' }]}
          >
            <Input placeholder="如：email,dingtalk,wechat" />
          </Form.Item>

          <Form.Item name="description" label="描述">
            <TextArea rows={3} placeholder="规则描述" />
          </Form.Item>

          <Form.Item name="enabled" label="启用" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default AlertManagement
