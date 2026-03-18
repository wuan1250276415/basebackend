import { Search, RefreshCw, Eye } from 'lucide-react';
import { useState } from 'react'
import { Card, Form, Input, Button, Table, Tag, Space, message, Drawer, Descriptions } from 'antd'

import {
  searchTraces,
  getTraceById,
  type TraceDetail,
  type TraceItem,
  type TraceQueryRequest,
} from '@/api/observability/traces'
import dayjs from 'dayjs'

/**
 * 追踪查询页面
 */
const TraceQuery = () => {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [traces, setTraces] = useState<TraceItem[]>([])
  const [total, setTotal] = useState(0)
  const [drawerVisible, setDrawerVisible] = useState(false)
  const [selectedTrace, setSelectedTrace] = useState<TraceItem | null>(null)
  const [traceDetail, setTraceDetail] = useState<TraceDetail | null>(null)

  // 搜索追踪
  const handleSearch = async (values: any) => {
    try {
      setLoading(true)

      const params: TraceQueryRequest = {
        serviceName: values.serviceName,
        minDuration: values.minDuration,
        maxDuration: values.maxDuration,
        limit: 20
      }

      const res = await searchTraces(params)
      setTraces(res.traces)
      setTotal(res.total)
    } catch (error: any) {
      message.error(error.message || '搜索追踪失败')
    } finally {
      setLoading(false)
    }
  }

  // 查看追踪详情
  const handleViewDetail = async (record: TraceItem) => {
    try {
      setSelectedTrace(record)
      setDrawerVisible(true)

      const detail = await getTraceById(record.traceId)
      setTraceDetail(detail)
    } catch (error: any) {
      message.error(error.message || '加载追踪详情失败')
    }
  }

  // 重置表单
  const handleReset = () => {
    form.resetFields()
    setTraces([])
    setTotal(0)
  }

  // 表格列定义
  const columns = [
    {
      title: 'Trace ID',
      dataIndex: 'traceId',
      key: 'traceId',
      width: 250,
      render: (text: string) => <code>{text}</code>
    },
    {
      title: '服务名称',
      dataIndex: 'serviceName',
      key: 'serviceName',
      width: 200
    },
    {
      title: '操作名称',
      dataIndex: 'operationName',
      key: 'operationName',
      width: 250
    },
    {
      title: '持续时间',
      dataIndex: 'duration',
      key: 'duration',
      width: 120,
      render: (duration: number) => {
        const color = duration > 1000 ? 'red' : duration > 500 ? 'orange' : 'green'
        return <Tag color={color}>{duration} ms</Tag>
      }
    },
    {
      title: '开始时间',
      dataIndex: 'startTime',
      key: 'startTime',
      width: 180,
      render: (time: number) => dayjs(time).format('YYYY-MM-DD HH:mm:ss')
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      fixed: 'right' as const,
      render: (_: any, record: any) => (
        <Button
          type="link"
          icon={<Eye />}
          onClick={() => handleViewDetail(record)}
        >
          详情
        </Button>
      )
    }
  ]

  return (
    <div style={{ padding: '24px' }}>
      <Card title="追踪查询" style={{ marginBottom: '24px' }}>
        <Form
          form={form}
          layout="inline"
          onFinish={handleSearch}
        >
          <Form.Item name="serviceName" label="服务名称">
            <Input placeholder="服务名称" style={{ width: 200 }} />
          </Form.Item>

          <Form.Item name="minDuration" label="最小耗时(ms)">
            <Input type="number" placeholder="100" style={{ width: 150 }} />
          </Form.Item>

          <Form.Item name="maxDuration" label="最大耗时(ms)">
            <Input type="number" placeholder="5000" style={{ width: 150 }} />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" icon={<Search />}>
                搜索
              </Button>
              <Button icon={<RefreshCw />} onClick={handleReset}>
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Card title={`搜索结果（共 ${total} 条）`}>
        <Table
          columns={columns}
          dataSource={traces}
          loading={loading}
          rowKey="traceId"
          pagination={{
            total,
            pageSize: 20,
            showSizeChanger: false,
            showTotal: (total) => `共 ${total} 条`
          }}
          scroll={{ x: 1200 }}
        />
      </Card>

      {/* 追踪详情抽屉 */}
      <Drawer
        title="追踪详情"
        width={720}
        open={drawerVisible}
        onClose={() => {
          setDrawerVisible(false)
          setTraceDetail(null)
        }}
      >
        {selectedTrace && (
          <Descriptions column={1} bordered>
            <Descriptions.Item label="Trace ID">
              <code>{selectedTrace.traceId}</code>
            </Descriptions.Item>
            <Descriptions.Item label="服务名称">
              {selectedTrace.serviceName}
            </Descriptions.Item>
            <Descriptions.Item label="操作名称">
              {selectedTrace.operationName}
            </Descriptions.Item>
            <Descriptions.Item label="持续时间">
              {selectedTrace.duration} ms
            </Descriptions.Item>
            <Descriptions.Item label="开始时间">
              {dayjs(selectedTrace.startTime).format('YYYY-MM-DD HH:mm:ss.SSS')}
            </Descriptions.Item>
            <Descriptions.Item label="Span 数量">
              {traceDetail?.spanCount || '-'}
            </Descriptions.Item>
          </Descriptions>
        )}

        {traceDetail?.spans && (
          <div style={{ marginTop: '24px' }}>
            <h4>Spans:</h4>
            <pre style={{ background: '#f5f5f5', padding: '12px', borderRadius: '4px', overflow: 'auto' }}>
              {JSON.stringify(traceDetail.spans, null, 2)}
            </pre>
          </div>
        )}
      </Drawer>
    </div>
  )
}

export default TraceQuery
