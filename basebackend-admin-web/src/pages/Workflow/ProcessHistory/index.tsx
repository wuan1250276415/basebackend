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
  Timeline,
  Modal,
  Descriptions,
} from 'antd'
import {
  EyeOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  HistoryOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

import { listHistoricProcessInstances } from '@/api/workflow/processInstance'
import { listHistoricTasksByProcessInstanceId } from '@/api/workflow/task'
import type { ProcessInstance } from '@/types/workflow'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const { Search } = Input
const { Option } = Select
const { RangePicker } = DatePicker

const ProcessHistory: React.FC = () => {
  const navigate = useNavigate()

  const [loading, setLoading] = useState(false)
  const [instances, setInstances] = useState<ProcessInstance[]>([])
  const [filteredInstances, setFilteredInstances] = useState<ProcessInstance[]>([])
  const [searchText, setSearchText] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('all')
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null)
  const [historyModalVisible, setHistoryModalVisible] = useState(false)
  const [currentInstance, setCurrentInstance] = useState<ProcessInstance | null>(null)
  const [taskHistory, setTaskHistory] = useState<any[]>([])
  const [historyLoading, setHistoryLoading] = useState(false)

  // 加载历史流程实例
  const loadHistoricInstances = async () => {
    setLoading(true)
    try {
      // 假设有历史流程实例的API，如果没有则使用已完成的流程实例
      const response = await listHistoricProcessInstances({})
      if (response.success) {
        const instanceList = response.data?.list || []
        setInstances(instanceList)
        setFilteredInstances(instanceList)
      } else {
        message.error(response.message || '加载历史流程失败')
      }
    } catch (error) {
      message.error('加载历史流程失败')
      console.error(error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadHistoricInstances()
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
          instance.id?.toLowerCase().includes(searchText.toLowerCase()) ||
          instance.startUserId?.toLowerCase().includes(searchText.toLowerCase())
      )
    }

    // 状态过滤
    if (statusFilter !== 'all') {
      if (statusFilter === 'completed') {
        filtered = filtered.filter((instance) => instance.ended)
      } else if (statusFilter === 'running') {
        filtered = filtered.filter((instance) => !instance.ended)
      } else if (statusFilter === 'terminated') {
        filtered = filtered.filter((instance) => instance.deleteReason)
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

  // 查看详细历史
  const handleViewHistory = async (instance: ProcessInstance) => {
    setCurrentInstance(instance)
    setHistoryModalVisible(true)
    setHistoryLoading(true)

    try {
      const response = await listHistoricTasksByProcessInstanceId(instance.id)
      if (response.success) {
        setTaskHistory(response.data?.list || [])
      } else {
        message.error(response.message || '加载任务历史失败')
      }
    } catch (error) {
      message.error('加载任务历史失败')
      console.error(error)
    } finally {
      setHistoryLoading(false)
    }
  }

  // 获取流程状态
  const getStatusTag = (instance: ProcessInstance) => {
    if (instance.deleteReason) {
      return (
        <Tag icon={<CloseCircleOutlined />} color="error">
          已终止
        </Tag>
      )
    } else if (instance.ended) {
      return (
        <Tag icon={<CheckCircleOutlined />} color="success">
          已完成
        </Tag>
      )
    } else {
      return (
        <Tag icon={<ClockCircleOutlined />} color="processing">
          进行中
        </Tag>
      )
    }
  }

  // 计算持续时间
  const calculateDuration = (instance: ProcessInstance) => {
    const endTime = instance.ended && instance.endTime ? dayjs(instance.endTime) : dayjs()
    const duration = endTime.diff(dayjs(instance.startTime), 'minute')

    if (duration < 60) {
      return `${duration}分钟`
    } else if (duration < 1440) {
      const hours = Math.floor(duration / 60)
      const minutes = duration % 60
      return `${hours}小时${minutes}分钟`
    } else {
      const days = Math.floor(duration / 1440)
      const hours = Math.floor((duration % 1440) / 60)
      return `${days}天${hours}小时`
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
      defaultSortOrder: 'descend',
      render: (text) => dayjs(text).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: '结束时间',
      dataIndex: 'endTime',
      key: 'endTime',
      width: 180,
      render: (text) => (text ? dayjs(text).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: '持续时间',
      key: 'duration',
      width: 150,
      render: (_, record) => calculateDuration(record),
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => navigate(`/workflow/instance/${record.id}`)}
          >
            详情
          </Button>
          <Button
            type="link"
            size="small"
            icon={<HistoryOutlined />}
            onClick={() => handleViewHistory(record)}
          >
            历史
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <>
      <Card
        title="流程历史追踪"
        extra={<Button onClick={loadHistoricInstances}>刷新</Button>}
      >
        {/* 筛选区域 */}
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={8}>
            <Search
              placeholder="搜索流程名称、业务键、发起人或实例ID"
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
              <Option value="completed">已完成</Option>
              <Option value="running">进行中</Option>
              <Option value="terminated">已终止</Option>
            </Select>
          </Col>
          <Col span={11}>
            <RangePicker
              style={{ width: '100%' }}
              placeholder={['开始日期', '结束日期']}
              value={dateRange}
              onChange={(dates) => setDateRange(dates as [dayjs.Dayjs, dayjs.Dayjs] | null)}
              showTime
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
                  {instances.filter((i) => i.ended && !i.deleteReason).length}
                </div>
                <div style={{ color: '#999' }}>已完成</div>
              </div>
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small">
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: 24, fontWeight: 'bold', color: '#13c2c2' }}>
                  {instances.filter((i) => !i.ended).length}
                </div>
                <div style={{ color: '#999' }}>进行中</div>
              </div>
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small">
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: 24, fontWeight: 'bold', color: '#f5222d' }}>
                  {instances.filter((i) => i.deleteReason).length}
                </div>
                <div style={{ color: '#999' }}>已终止</div>
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

      {/* 详细历史模态框 */}
      <Modal
        title="流程历史详情"
        open={historyModalVisible}
        onCancel={() => setHistoryModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setHistoryModalVisible(false)}>
            关闭
          </Button>,
        ]}
        width={800}
      >
        {currentInstance && (
          <>
            <Descriptions column={2} bordered style={{ marginBottom: 16 }}>
              <Descriptions.Item label="流程名称" span={2}>
                {currentInstance.processDefinitionName}
              </Descriptions.Item>
              <Descriptions.Item label="业务键">
                {currentInstance.businessKey}
              </Descriptions.Item>
              <Descriptions.Item label="发起人">
                {currentInstance.startUserId}
              </Descriptions.Item>
              <Descriptions.Item label="开始时间">
                {dayjs(currentInstance.startTime).format('YYYY-MM-DD HH:mm:ss')}
              </Descriptions.Item>
              <Descriptions.Item label="结束时间">
                {currentInstance.endTime
                  ? dayjs(currentInstance.endTime).format('YYYY-MM-DD HH:mm:ss')
                  : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="状态" span={2}>
                {getStatusTag(currentInstance)}
              </Descriptions.Item>
              <Descriptions.Item label="持续时间" span={2}>
                {calculateDuration(currentInstance)}
              </Descriptions.Item>
              {currentInstance.deleteReason && (
                <Descriptions.Item label="终止原因" span={2}>
                  <span style={{ color: '#f5222d' }}>
                    {currentInstance.deleteReason}
                  </span>
                </Descriptions.Item>
              )}
            </Descriptions>

            <Card title="任务执行历史" size="small" loading={historyLoading}>
              {taskHistory.length > 0 ? (
                <Timeline>
                  {taskHistory.map((task, index) => {
                    const isCompleted = !!task.endTime

                    return (
                      <Timeline.Item
                        key={task.id || index}
                        color={isCompleted ? 'green' : 'blue'}
                        dot={
                          isCompleted ? (
                            <CheckCircleOutlined style={{ fontSize: 16 }} />
                          ) : (
                            <ClockCircleOutlined style={{ fontSize: 16 }} />
                          )
                        }
                      >
                        <div>
                          <div style={{ fontWeight: 'bold', marginBottom: 4 }}>
                            {task.name}
                          </div>
                          <div style={{ color: '#666', fontSize: 12, marginBottom: 4 }}>
                            办理人：{task.assignee || '待认领'}
                          </div>
                          <div style={{ color: '#666', fontSize: 12, marginBottom: 4 }}>
                            开始：{dayjs(task.startTime).format('YYYY-MM-DD HH:mm:ss')}
                          </div>
                          {task.endTime && (
                            <>
                              <div style={{ color: '#666', fontSize: 12, marginBottom: 4 }}>
                                完成：{dayjs(task.endTime).format('YYYY-MM-DD HH:mm:ss')}
                              </div>
                              <div style={{ color: '#666', fontSize: 12 }}>
                                耗时：
                                {(() => {
                                  const duration = dayjs(task.endTime).diff(
                                    dayjs(task.startTime),
                                    'minute'
                                  )
                                  if (duration < 60) {
                                    return `${duration}分钟`
                                  } else if (duration < 1440) {
                                    return `${Math.floor(duration / 60)}小时`
                                  } else {
                                    return `${Math.floor(duration / 1440)}天`
                                  }
                                })()}
                              </div>
                            </>
                          )}
                          {!task.endTime && (
                            <Tag color="processing" style={{ marginTop: 4 }}>
                              进行中
                            </Tag>
                          )}
                          {task.deleteReason && (
                            <div style={{ color: '#f5222d', fontSize: 12, marginTop: 4 }}>
                              终止原因：{task.deleteReason}
                            </div>
                          )}
                        </div>
                      </Timeline.Item>
                    )
                  })}
                </Timeline>
              ) : (
                <div style={{ textAlign: 'center', padding: '20px', color: '#999' }}>
                  暂无任务历史
                </div>
              )}
            </Card>
          </>
        )}
      </Modal>
    </>
  )
}

export default ProcessHistory
