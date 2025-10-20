import { useState, useEffect } from 'react'
import { Card, Form, Input, Select, DatePicker, Button, Table, Tag, Space, message } from 'antd'
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons'
import { queryLogs, LogQueryRequest } from '@/api/observability/logs'
import dayjs from 'dayjs'

const { RangePicker } = DatePicker
const { Option } = Select

/**
 * 日志查询页面
 */
const LogQuery = () => {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [logs, setLogs] = useState<any[]>([])
  const [total, setTotal] = useState(0)

  // 查询日志
  const handleSearch = async (values: any) => {
    try {
      setLoading(true)

      const params: LogQueryRequest = {
        keyword: values.keyword,
        level: values.level,
        traceId: values.traceId,
        application: values.application,
        limit: 100
      }

      if (values.timeRange) {
        params.startTime = values.timeRange[0].toISOString()
        params.endTime = values.timeRange[1].toISOString()
      }

      const res = await queryLogs(params)

      if (res.data?.logs) {
        setLogs(res.data.logs)
        setTotal(res.data.total || 0)
      } else {
        setLogs([])
        setTotal(0)
      }
    } catch (error: any) {
      message.error(error.message || '查询日志失败')
    } finally {
      setLoading(false)
    }
  }

  // 重置表单
  const handleReset = () => {
    form.resetFields()
    setLogs([])
    setTotal(0)
  }

  // 表格列定义
  const columns = [
    {
      title: '时间',
      dataIndex: 'timestamp',
      key: 'timestamp',
      width: 180,
      render: (timestamp: number) => dayjs(timestamp / 1000000).format('YYYY-MM-DD HH:mm:ss.SSS')
    },
    {
      title: '日志内容',
      dataIndex: 'line',
      key: 'line',
      ellipsis: true,
      render: (text: string) => {
        try {
          const log = JSON.parse(text)
          return (
            <div>
              <Tag color={getLevelColor(log.level)}>{log.level}</Tag>
              <span>{log.message || text}</span>
            </div>
          )
        } catch {
          return text
        }
      }
    }
  ]

  // 获取日志级别颜色
  const getLevelColor = (level: string) => {
    const colors: Record<string, string> = {
      ERROR: 'red',
      WARN: 'orange',
      INFO: 'blue',
      DEBUG: 'green'
    }
    return colors[level] || 'default'
  }

  // 初始化：设置默认时间范围为最近1小时
  useEffect(() => {
    form.setFieldsValue({
      timeRange: [dayjs().subtract(1, 'hour'), dayjs()]
    })
  }, [form])

  return (
    <div style={{ padding: '24px' }}>
      <Card title="日志查询" style={{ marginBottom: '24px' }}>
        <Form
          form={form}
          layout="inline"
          onFinish={handleSearch}
        >
          <Form.Item name="keyword" label="关键词">
            <Input placeholder="搜索关键词" style={{ width: 200 }} />
          </Form.Item>

          <Form.Item name="level" label="日志级别">
            <Select placeholder="选择级别" style={{ width: 120 }} allowClear>
              <Option value="INFO">INFO</Option>
              <Option value="WARN">WARN</Option>
              <Option value="ERROR">ERROR</Option>
              <Option value="DEBUG">DEBUG</Option>
            </Select>
          </Form.Item>

          <Form.Item name="traceId" label="TraceId">
            <Input placeholder="TraceId" style={{ width: 200 }} />
          </Form.Item>

          <Form.Item name="application" label="应用">
            <Input placeholder="应用名称" style={{ width: 200 }} />
          </Form.Item>

          <Form.Item name="timeRange" label="时间范围">
            <RangePicker
              showTime
              format="YYYY-MM-DD HH:mm:ss"
            />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
                查询
              </Button>
              <Button icon={<ReloadOutlined />} onClick={handleReset}>
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Card title={`查询结果（共 ${total} 条）`}>
        <Table
          columns={columns}
          dataSource={logs}
          loading={loading}
          rowKey={(record, index) => `${record.timestamp}-${index}`}
          pagination={{
            total,
            pageSize: 100,
            showSizeChanger: false,
            showTotal: (total) => `共 ${total} 条`
          }}
          scroll={{ x: 1000 }}
        />
      </Card>
    </div>
  )
}

export default LogQuery
