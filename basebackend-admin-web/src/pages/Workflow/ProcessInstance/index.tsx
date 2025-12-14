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
  Modal,
} from 'antd'
import {
  EyeOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  SyncOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  DeleteOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

import {
  listProcessInstances,
  suspendProcessInstance,
  activateProcessInstance,
  deleteProcessInstance,
} from '@/api/workflow/processInstance'
import { listProcessDefinitions } from '@/api/workflow/processDefinition'
import type { ProcessInstance, ProcessDefinition } from '@/types/workflow'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const { Search } = Input
const { Option } = Select
const { RangePicker } = DatePicker
const { confirm } = Modal

const ProcessInstanceList: React.FC = () => {
  const navigate = useNavigate()

  const [loading, setLoading] = useState(false)
  const [instances, setInstances] = useState<ProcessInstance[]>([])
  const [filteredInstances, setFilteredInstances] = useState<ProcessInstance[]>([])
  const [definitions, setDefinitions] = useState<ProcessDefinition[]>([])
  const [searchText, setSearchText] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('all')
  const [definitionFilter, setDefinitionFilter] = useState<string>('all')
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null)

  // 加载流程定义
  const loadDefinitions = async () => {
    try {
      const response = await listProcessDefinitions()
      if (response.code === 200) {
        setDefinitions(response.data?.records || [])
      }
    } catch (error) {
      console.error('加载流程定义失败', error)
    }
  }

  // 加载流程实例
  const loadInstances = async () => {
    setLoading(true)
    try {
      const response = await listProcessInstances({})
      if (response.code === 200) {
        const instanceList = response.data?.records || []
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
    loadDefinitions()
    loadInstances()
  }, [])

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

    // 流程定义过滤
    if (definitionFilter !== 'all') {
      filtered = filtered.filter((instance) => instance.processDefinitionId === definitionFilter)
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
  }, [searchText, statusFilter, definitionFilter, dateRange, instances])

  // 查看详情
  const handleView = (instance: ProcessInstance) => {
    navigate(`/workflow/instance/${instance.id}`)
  }

  // 挂起流程实例
  const handleSuspend = (instance: ProcessInstance) => {
    confirm({
      title: '确认挂起',
      content: `确定要挂起流程实例 "${instance.businessKey}" 吗？`,
      onOk: async () => {
        try {
          const response = await suspendProcessInstance(instance.id)
          if (response.code === 200) {
            message.success('挂起成功')
            loadInstances()
          } else {
            message.error(response.message || '挂起失败')
          }
        } catch (error) {
          message.error('挂起失败')
          console.error(error)
        }
      },
    })
  }

  // 激活流程实例
  const handleActivate = (instance: ProcessInstance) => {
    confirm({
      title: '确认激活',
      content: `确定要激活流程实例 "${instance.businessKey}" 吗？`,
      onOk: async () => {
        try {
          const response = await activateProcessInstance(instance.id)
          if (response.code === 200) {
            message.success('激活成功')
            loadInstances()
          } else {
            message.error(response.message || '激活失败')
          }
        } catch (error) {
          message.error('激活失败')
          console.error(error)
        }
      },
    })
  }

  // 删除流程实例
  const handleDelete = (instance: ProcessInstance) => {
    confirm({
      title: '确认删除',
      content: `确定要删除流程实例 "${instance.businessKey}" 吗？此操作不可恢复！`,
      okType: 'danger',
      onOk: async () => {
        try {
          const response = await deleteProcessInstance(instance.id)
          if (response.code === 200) {
            message.success('删除成功')
            loadInstances()
          } else {
            message.error(response.message || '删除失败')
          }
        } catch (error) {
          message.error('删除失败')
          console.error(error)
        }
      },
    })
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
        <Tag icon={<PauseCircleOutlined />} color="warning">
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

  const columns: ColumnsType<ProcessInstance> = [
    {
      title: '流程名称',
      dataIndex: 'processDefinitionName',
      key: 'processDefinitionName',
      width: 200,
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
      title: '发起人',
      dataIndex: 'startUserId',
      key: 'startUserId',
      width: 120,
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
          '-'
        ),
    },
    {
      title: '持续时间',
      key: 'duration',
      width: 120,
      render: (_, record) => {
        const endTime = record.ended && record.endTime ? dayjs(record.endTime) : dayjs()
        const duration = endTime.diff(dayjs(record.startTime), 'minute')
        if (duration < 60) {
          return `${duration}分钟`
        } else if (duration < 1440) {
          return `${Math.floor(duration / 60)}小时`
        } else {
          return `${Math.floor(duration / 1440)}天`
        }
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleView(record)}>
            查看
          </Button>
          {!record.ended && (
            <>
              {record.suspended ? (
                <Button
                  type="link"
                  size="small"
                  icon={<PlayCircleOutlined />}
                  onClick={() => handleActivate(record)}
                >
                  激活
                </Button>
              ) : (
                <Button
                  type="link"
                  size="small"
                  icon={<PauseCircleOutlined />}
                  onClick={() => handleSuspend(record)}
                >
                  挂起
                </Button>
              )}
            </>
          )}
          {record.ended && (
            <Button
              type="link"
              size="small"
              danger
              icon={<DeleteOutlined />}
              onClick={() => handleDelete(record)}
            >
              删除
            </Button>
          )}
        </Space>
      ),
    },
  ]

  return (
    <Card
      title="流程实例监控"
      extra={
        <Button onClick={loadInstances}>刷新</Button>
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
        <Col span={5}>
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
        <Col span={5}>
          <Select
            placeholder="流程类型"
            style={{ width: '100%' }}
            value={definitionFilter}
            onChange={setDefinitionFilter}
            showSearch
            filterOption={(input, option) =>
              (option?.children as string).toLowerCase().includes(input.toLowerCase())
            }
          >
            <Option value="all">全部类型</Option>
            {definitions.map((def) => (
              <Option key={def.id} value={def.id}>
                {def.name}
              </Option>
            ))}
          </Select>
        </Col>
        <Col span={6}>
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
              <div style={{ color: '#999' }}>总实例数</div>
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
              <div style={{ fontSize: 24, fontWeight: 'bold', color: '#faad14' }}>
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
        scroll={{ x: 1500 }}
      />
    </Card>
  )
}

export default ProcessInstanceList
