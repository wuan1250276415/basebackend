import React, { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Card,
  Descriptions,
  Button,
  Space,
  message,
  Spin,
  Alert,
  Divider,
  Timeline,
  Tag,
  Row,
  Col,
  Tabs,
} from 'antd'
import {
  ArrowLeftOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  SyncOutlined,
  PauseCircleOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'

import {
  getProcessInstanceById,
  getProcessInstanceVariables,
} from '@/api/workflow/processInstance'
import { listHistoricActivities } from '@/api/workflow/history'
import { getProcessDefinitionXml } from '@/api/workflow/processDefinition'
import type { ProcessInstance } from '@/types/workflow'

const { TabPane } = Tabs

const ProcessInstanceDetail: React.FC = () => {
  const { instanceId } = useParams<{ instanceId: string }>()
  const navigate = useNavigate()

  const [loading, setLoading] = useState(false)
  const [instance, setInstance] = useState<ProcessInstance | null>(null)
  const [variables, setVariables] = useState<Record<string, any>>({})
  const [taskHistory, setTaskHistory] = useState<any[]>([])
  const [bpmnXml, setBpmnXml] = useState<string>('')

  // 加载流程实例详情
  const loadInstanceDetail = async () => {
    if (!instanceId) return

    setLoading(true)
    try {
      // 加载流程实例信息
      const instanceResponse = await getProcessInstanceById(instanceId)
      if (instanceResponse.success && instanceResponse.data) {
        const instanceData = instanceResponse.data
        setInstance(instanceData)

        // 加载流程变量
        const variablesResponse = await getProcessInstanceVariables(instanceId)
        if (variablesResponse.success) {
          setVariables(variablesResponse.data || {})
        }

        // 加载任务历史 (通过活动历史获取用户任务)
        const historyResponse = await listHistoricActivities(instanceId, { size: 100 })
        if (historyResponse.success) {
          const activities = historyResponse.data?.list || []
          // 过滤出用户任务
          const userTasks = activities
            .filter((activity) => activity.activityType === 'userTask')
            .map((activity) => ({
              id: activity.taskId || activity.id,
              name: activity.activityName,
              assignee: activity.assignee,
              startTime: activity.startTime,
              endTime: activity.endTime,
              deleteReason: activity.canceled ? '已取消' : undefined,
            }))
          setTaskHistory(userTasks)
        }

        // 加载BPMN XML
        if (instanceData.processDefinitionId) {
          const xmlResponse = await getProcessDefinitionXml(instanceData.processDefinitionId)
          if (xmlResponse.success && xmlResponse.data) {
            setBpmnXml(xmlResponse.data.xml)
          }
        }
      } else {
        message.error(instanceResponse.message || '加载流程实例失败')
      }
    } catch (error) {
      message.error('加载流程实例失败')
      console.error(error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadInstanceDetail()
  }, [instanceId])

  // 获取状态标签
  const getStatusTag = () => {
    if (!instance) return null
    if (instance.ended) {
      return (
        <Tag icon={<CheckCircleOutlined />} color="success" style={{ fontSize: 14 }}>
          已完成
        </Tag>
      )
    } else if (instance.suspended) {
      return (
        <Tag icon={<PauseCircleOutlined />} color="warning" style={{ fontSize: 14 }}>
          已挂起
        </Tag>
      )
    } else {
      return (
        <Tag icon={<SyncOutlined spin />} color="processing" style={{ fontSize: 14 }}>
          进行中
        </Tag>
      )
    }
  }

  if (loading) {
    return (
      <Card>
        <div style={{ textAlign: 'center', padding: '50px 0' }}>
          <Spin size="large" />
          <div style={{ marginTop: 16 }}>加载中...</div>
        </div>
      </Card>
    )
  }

  if (!instance) {
    return (
      <Card>
        <Alert
          message="流程实例不存在"
          description="该流程实例可能已被删除"
          type="warning"
          showIcon
        />
      </Card>
    )
  }

  return (
    <div>
      {/* 页面头部 */}
      <Card>
        <Space>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)}>
            返回
          </Button>
          <Divider type="vertical" />
          <h2 style={{ margin: 0 }}>{instance.processDefinitionName}</h2>
          {getStatusTag()}
        </Space>
      </Card>

      <Row gutter={16} style={{ marginTop: 16 }}>
        {/* 左侧：详细信息 */}
        <Col span={16}>
          <Card>
            <Tabs defaultActiveKey="basic">
              {/* 基本信息 */}
              <TabPane tab="基本信息" key="basic">
                <Descriptions column={2} bordered>
                  <Descriptions.Item label="流程实例ID" span={2}>
                    {instance.id}
                  </Descriptions.Item>
                  <Descriptions.Item label="流程定义名称">
                    {instance.processDefinitionName}
                  </Descriptions.Item>
                  <Descriptions.Item label="业务键">{instance.businessKey}</Descriptions.Item>
                  <Descriptions.Item label="发起人">{instance.startUserId}</Descriptions.Item>
                  <Descriptions.Item label="流程状态">{getStatusTag()}</Descriptions.Item>
                  <Descriptions.Item label="开始时间">
                    {dayjs(instance.startTime).format('YYYY-MM-DD HH:mm:ss')}
                  </Descriptions.Item>
                  <Descriptions.Item label="结束时间">
                    {instance.endTime
                      ? dayjs(instance.endTime).format('YYYY-MM-DD HH:mm:ss')
                      : '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="持续时间" span={2}>
                    {(() => {
                      const endTime =
                        instance.ended && instance.endTime ? dayjs(instance.endTime) : dayjs()
                      const duration = endTime.diff(dayjs(instance.startTime), 'minute')
                      if (duration < 60) {
                        return `${duration}分钟`
                      } else if (duration < 1440) {
                        return `${Math.floor(duration / 60)}小时 ${duration % 60}分钟`
                      } else {
                        const days = Math.floor(duration / 1440)
                        const hours = Math.floor((duration % 1440) / 60)
                        return `${days}天 ${hours}小时`
                      }
                    })()}
                  </Descriptions.Item>
                </Descriptions>
              </TabPane>

              {/* 流程变量 */}
              <TabPane tab="流程变量" key="variables">
                <Descriptions column={1} bordered>
                  {Object.keys(variables).length > 0 ? (
                    Object.entries(variables).map(([key, value]) => (
                      <Descriptions.Item key={key} label={key}>
                        {typeof value === 'object' ? (
                          <pre style={{ margin: 0, maxHeight: 200, overflow: 'auto' }}>
                            {JSON.stringify(value, null, 2)}
                          </pre>
                        ) : (
                          String(value)
                        )}
                      </Descriptions.Item>
                    ))
                  ) : (
                    <Descriptions.Item label="提示">暂无流程变量</Descriptions.Item>
                  )}
                </Descriptions>
              </TabPane>

              {/* 流程图 */}
              <TabPane tab="流程图" key="diagram">
                {bpmnXml ? (
                  <div
                    style={{
                      border: '1px solid #d9d9d9',
                      borderRadius: 4,
                      padding: 16,
                      backgroundColor: '#fafafa',
                      minHeight: 400,
                    }}
                  >
                    <Alert
                      message="BPMN流程图查看器"
                      description="完整的BPMN流程图查看器需要集成 bpmn-js 库，这里显示XML源码。您可以将XML导入到Camunda Modeler中查看可视化流程图。"
                      type="info"
                      showIcon
                      style={{ marginBottom: 16 }}
                    />
                    <pre
                      style={{
                        maxHeight: 500,
                        overflow: 'auto',
                        backgroundColor: '#fff',
                        padding: 16,
                        borderRadius: 4,
                        fontSize: 12,
                      }}
                    >
                      {bpmnXml}
                    </pre>
                  </div>
                ) : (
                  <Alert
                    message="暂无流程图数据"
                    description="无法加载BPMN流程图"
                    type="warning"
                    showIcon
                  />
                )}
              </TabPane>
            </Tabs>
          </Card>
        </Col>

        {/* 右侧：任务历史 */}
        <Col span={8}>
          <Card title="任务历史" style={{ height: '100%' }}>
            {taskHistory.length > 0 ? (
              <Timeline>
                {taskHistory.map((task, index) => {
                  const isCompleted = !!task.endTime
                  const isCurrent = !task.endTime && index === taskHistory.length - 1

                  return (
                    <Timeline.Item
                      key={task.id || index}
                      color={isCompleted ? 'green' : isCurrent ? 'blue' : 'gray'}
                      dot={
                        isCompleted ? (
                          <CheckCircleOutlined style={{ fontSize: 16 }} />
                        ) : isCurrent ? (
                          <SyncOutlined spin style={{ fontSize: 16 }} />
                        ) : (
                          <ClockCircleOutlined style={{ fontSize: 16 }} />
                        )
                      }
                    >
                      <div>
                        <div style={{ fontWeight: 'bold', marginBottom: 4 }}>{task.name}</div>
                        <div style={{ color: '#666', fontSize: 12, marginBottom: 4 }}>
                          办理人：{task.assignee || '待认领'}
                        </div>
                        <div style={{ color: '#666', fontSize: 12, marginBottom: 4 }}>
                          开始时间：{dayjs(task.startTime).format('MM-DD HH:mm')}
                        </div>
                        {task.endTime && (
                          <div style={{ color: '#666', fontSize: 12, marginBottom: 4 }}>
                            完成时间：{dayjs(task.endTime).format('MM-DD HH:mm')}
                          </div>
                        )}
                        {task.endTime && (
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
                        )}
                        {!task.endTime && isCurrent && (
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
        </Col>
      </Row>
    </div>
  )
}

export default ProcessInstanceDetail
