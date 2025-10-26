import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Card,
  Table,
  Tag,
  Button,
  Input,
  Space,
  message,
  Tooltip,
  Select,
  DatePicker,
  Row,
  Col,
} from 'antd'
import {
  EyeOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  SyncOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

import { listProcessInstances } from '@/api/workflow/processInstance'
import { useAuthStore } from '@/stores/auth'
import type { ProcessInstance } from '@/types/workflow'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const { Search } = Input
const { Option } = Select
const { RangePicker } = DatePicker

const MyInitiated: React.FC = () => {
  const navigate = useNavigate()
  const { user } = useAuthStore()

  const [loading, setLoading] = useState(false)
  const [instances, setInstances] = useState<ProcessInstance[]>([])
  const [filteredInstances, setFilteredInstances] = useState<ProcessInstance[]>([])
  const [searchText, setSearchText] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('all')
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null)

  // 加载流程实例
  const loadInstances = async () => {
    if (!user) return

    setLoading(true)
    try {
      const response = await listProcessInstances({
        startedBy: user.username,
      })
      if (response.success) {
        const instanceList = response.data?.list || []
        setInstances(instanceList)
        setFilteredInstances(instanceList)
      } else {
        message.error(response.message || '加载流程实例失败')
      }
    } catch (error) {
      message.error('加载流程实例失败')
      console.error(error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadInstances()
  }, [user])

  // 过滤逻辑
  useEffect(() => {
    let filtered = instances

    // 搜索过滤
    if (searchText) {
      filtered = filtered.filter(
        (instance) =>
          instance.processDefinitionName?.toLowerCase().includes(searchText.toLowerCase()) ||
          instance.businessKey?.toLowerCase().includes(searchText.toLowerCase()) ||
          instance.id?.toLowerCase().includes(searchText.toLowerCase())
      )
    }

    // 状态过滤
    if (statusFilter !== 'all') {
      if (statusFilter === 'active') {
        filtered = filtered.filter((instance) => !instance.ended && !instance.suspended)
      } else if (statusFilter === 'suspended') {
        filtered = filtered.filter((instance) => instance.suspended)
      } else if (statusFilter === 'completed') {
        filtered = filtered.filter((instance) => instance.ended)
      }
    }

    // 日期范围过滤
    if (dateRange) {
      const [start, end] = dateRange
      filtered = filtered.filter((instance) => {
        const startTime = dayjs(instance.startTime)
        return startTime.isAfter(start) && startTime.isBefore(end.add(1, 'day'))
      })
    }

    setFilteredInstances(filtered)
  }, [searchText, statusFilter, dateRange, instances])

  // 查看详情
  const handleView = (instance: ProcessInstance) => {
    navigate(`/workflow/instance/${instance.id}`)
  }

  // 获取流程状态标签
  const getStatusTag = (instance: ProcessInstance) => {
    if (instance.ended) {
      return (
        <Tag icon={<CheckCircleOutlined />} color="success">
          已完成
        </Tag>
      )
    } else if (instance.suspended) {
      return (
        <Tag icon={<CloseCircleOutlined />} color="error">
          已挂起
        </Tag>
      )
    } else {
      return (
        <Tag icon={<SyncOutlined spin />} color="processing">
          进行中
        </Tag>
      )
    }
  }

  // 获取流程类型标签
  const getProcessTypeTag = (name: string) => {
    if (name.includes('请假') || name.toLowerCase().includes('leave')) {
      return <Tag color="blue">请假审批</Tag>
    } else if (name.includes('报销') || name.toLowerCase().includes('expense')) {
      return <Tag color="green">报销审批</Tag>
    } else if (name.includes('采购') || name.toLowerCase().includes('purchase')) {
      return <Tag color="orange">采购审批</Tag>
    } else {
      return <Tag color="default">其他流程</Tag>
    }
  }

  const columns: ColumnsType<ProcessInstance> = [
    {
      title: '流程名称',
      dataIndex: 'processDefinitionName',
      key: 'processDefinitionName',
      width: 200,
      render: (text) => (
        <Space>
          {text}
          {getProcessTypeTag(text)}
        </Space>
      ),
    },
    {
      title: '业务键',
      dataIndex: 'businessKey',
      key: 'businessKey',
      width: 180,
      render: (text) => (
        <Tooltip title={text}>
          <span
            style={{
              maxWidth: 150,
              display: 'inline-block',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
            }}
          >
            {text}
          </span>
        </Tooltip>
      ),
    },
    {
      title: '流程实例ID',
      dataIndex: 'id',
      key: 'id',
      width: 180,
      render: (text) => (
        <Tooltip title={text}>
          <span
            style={{
              maxWidth: 150,
              display: 'inline-block',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              fontFamily: 'monospace',
              fontSize: 12,
            }}
          >
            {text}
          </span>
        </Tooltip>
      ),
    },
    {
      title: '状态',
      key: 'status',
      width: 120,
      render: (_, record) => getStatusTag(record),
    },
    {
      title: '开始时间',
      dataIndex: 'startTime',
      key: 'startTime',
      width: 180,
      sorter: (a, b) => dayjs(a.startTime).unix() - dayjs(b.startTime).unix(),
      render: (text) => (
        <Tooltip title={dayjs(text).format('YYYY-MM-DD HH:mm:ss')}>
          {dayjs(text).fromNow()}
        </Tooltip>
      ),
    },
    {
      title: '结束时间',
      dataIndex: 'endTime',
      key: 'endTime',
      width: 180,
      render: (text) =>
        text ? (
          <Tooltip title={dayjs(text).format('YYYY-MM-DD HH:mm:ss')}>
            {dayjs(text).fromNow()}
          </Tooltip>
        ) : (
          <Tag icon={<ClockCircleOutlined />} color="processing">
            进行中
          </Tag>
        ),
    },
    {
      title: '持续时间',
      key: 'duration',
      width: 120,
      render: (_, record) => {
        if (record.ended && record.endTime) {
          const duration = dayjs(record.endTime).diff(dayjs(record.startTime), 'minute')
          if (duration < 60) {
            return `${duration}分钟`
          } else if (duration < 1440) {
            return `${Math.floor(duration / 60)}小时`
          } else {
            return `${Math.floor(duration / 1440)}天`
          }
        } else {
          const duration = dayjs().diff(dayjs(record.startTime), 'minute')
          if (duration < 60) {
            return `${duration}分钟`
          } else if (duration < 1440) {
            return `${Math.floor(duration / 60)}小时`
          } else {
            return `${Math.floor(duration / 1440)}天`
          }
        }
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      fixed: 'right',
      render: (_, record) => (
        <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleView(record)}>
          查看
        </Button>
      ),
    },
  ]

  return (
    <Card
      title="我发起的流程"
      extra={
        <Space>
          <Button onClick={loadInstances}>刷新</Button>
          <Button type="primary" onClick={() => navigate('/workflow/template')}>
            发起新流程
          </Button>
        </Space>
      }
    >
      {/* 筛选区域 */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={8}>
          <Search
            placeholder="搜索流程名称、业务键或实例ID"
            allowClear
            onSearch={setSearchText}
            onChange={(e) => setSearchText(e.target.value)}
          />
        </Col>
        <Col span={6}>
          <Select
            placeholder="流程状态"
            style={{ width: '100%' }}
            value={statusFilter}
            onChange={setStatusFilter}
          >
            <Option value="all">全部状态</Option>
            <Option value="active">进行中</Option>
            <Option value="completed">已完成</Option>
            <Option value="suspended">已挂起</Option>
          </Select>
        </Col>
        <Col span={10}>
          <RangePicker
            style={{ width: '100%' }}
            placeholder={['开始日期', '结束日期']}
            value={dateRange}
            onChange={(dates) => setDateRange(dates as [dayjs.Dayjs, dayjs.Dayjs] | null)}
          />
        </Col>
      </Row>

      {/* 统计信息 */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card size="small">
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 24, fontWeight: 'bold', color: '#1890ff' }}>
                {instances.length}
              </div>
              <div style={{ color: '#999' }}>总流程数</div>
            </div>
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 24, fontWeight: 'bold', color: '#52c41a' }}>
                {instances.filter((i) => !i.ended && !i.suspended).length}
              </div>
              <div style={{ color: '#999' }}>进行中</div>
            </div>
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 24, fontWeight: 'bold', color: '#13c2c2' }}>
                {instances.filter((i) => i.ended).length}
              </div>
              <div style={{ color: '#999' }}>已完成</div>
            </div>
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 24, fontWeight: 'bold', color: '#f5222d' }}>
                {instances.filter((i) => i.suspended).length}
              </div>
              <div style={{ color: '#999' }}>已挂起</div>
            </div>
          </Card>
        </Col>
      </Row>

      {/* 表格 */}
      <Table
        columns={columns}
        dataSource={filteredInstances}
        rowKey="id"
        loading={loading}
        pagination={{
          total: filteredInstances.length,
          pageSize: 10,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 条`,
        }}
        scroll={{ x: 1400 }}
      />
    </Card>
  )
}

export default MyInitiated
