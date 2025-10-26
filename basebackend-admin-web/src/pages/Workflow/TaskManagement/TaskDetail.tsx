import React, { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Card,
  Descriptions,
  Form,
  Input,
  Radio,
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
} from 'antd'
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  RollbackOutlined,
  ArrowLeftOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'

import {
  getTaskById,
  completeTask,
  getTaskVariables,
  listHistoricTasksByProcessInstanceId,
} from '@/api/workflow/task'
import { getProcessInstanceById } from '@/api/workflow/processInstance'
import { useAuthStore } from '@/stores/auth'
import type { Task, ProcessInstance } from '@/types/workflow'

const { TextArea } = Input

const TaskDetail: React.FC = () => {
  const { taskId } = useParams<{ taskId: string }>()
  const navigate = useNavigate()
  const [form] = Form.useForm()
  const { user } = useAuthStore()

  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [task, setTask] = useState<Task | null>(null)
  const [processInstance, setProcessInstance] = useState<ProcessInstance | null>(null)
  const [variables, setVariables] = useState<Record<string, any>>({})
  const [approvalHistory, setApprovalHistory] = useState<any[]>([])

  // 加载任务详情
  const loadTaskDetail = async () => {
    if (!taskId) return

    setLoading(true)
    try {
      // 加载任务信息
      const taskResponse = await getTaskById(taskId)
      if (taskResponse.success && taskResponse.data) {
        const taskData = taskResponse.data
        setTask(taskData)

        // 加载任务变量
        const variablesResponse = await getTaskVariables(taskId)
        if (variablesResponse.success) {
          setVariables(variablesResponse.data || {})
        }

        // 加载流程实例信息
        if (taskData.processInstanceId) {
          const instanceResponse = await getProcessInstanceById(taskData.processInstanceId)
          if (instanceResponse.success) {
            setProcessInstance(instanceResponse.data)
          }

          // 加载审批历史
          const historyResponse = await listHistoricTasksByProcessInstanceId(
            taskData.processInstanceId
          )
          if (historyResponse.success) {
            setApprovalHistory(historyResponse.data?.list || [])
          }
        }
      } else {
        message.error(taskResponse.message || '加载任务失败')
      }
    } catch (error) {
      message.error('加载任务失败')
      console.error(error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadTaskDetail()
  }, [taskId])

  // 提交审批
  const handleSubmit = async (values: any) => {
    if (!taskId) return

    setSubmitting(true)
    try {
      const approvalVariables = {
        approved: values.decision === 'approve',
        approver: user?.username,
        approverName: user?.realName || user?.username,
        comment: values.comment,
        approvalTime: new Date().toISOString(),
      }

      const response = await completeTask(taskId, { variables: approvalVariables })
      if (response.success) {
        message.success('审批成功')
        navigate('/workflow/todo')
      } else {
        message.error(response.message || '审批失败')
      }
    } catch (error) {
      message.error('审批失败')
      console.error(error)
    } finally {
      setSubmitting(false)
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

  if (!task) {
    return (
      <Card>
        <Alert
          message="任务不存在"
          description="该任务可能已被处理或删除"
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
          <Button
            icon={<ArrowLeftOutlined />}
            onClick={() => navigate('/workflow/todo')}
          >
            返回
          </Button>
          <Divider type="vertical" />
          <h2 style={{ margin: 0 }}>{task.name}</h2>
        </Space>
      </Card>

      <Row gutter={16} style={{ marginTop: 16 }}>
        {/* 左侧：任务信息和审批表单 */}
        <Col span={16}>
          {/* 任务基本信息 */}
          <Card title="任务信息" style={{ marginBottom: 16 }}>
            <Descriptions column={2} bordered>
              <Descriptions.Item label="任务名称">{task.name}</Descriptions.Item>
              <Descriptions.Item label="流程实例ID">
                {task.processInstanceId}
              </Descriptions.Item>
              <Descriptions.Item label="办理人">
                {task.assignee || <Tag>待认领</Tag>}
              </Descriptions.Item>
              <Descriptions.Item label="创建时间">
                {dayjs(task.createTime).format('YYYY-MM-DD HH:mm:ss')}
              </Descriptions.Item>
              <Descriptions.Item label="到期时间">
                {task.dueDate
                  ? dayjs(task.dueDate).format('YYYY-MM-DD HH:mm:ss')
                  : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="优先级">
                <Tag color={task.priority >= 80 ? 'red' : task.priority >= 50 ? 'orange' : 'blue'}>
                  {task.priority >= 80 ? '紧急' : task.priority >= 50 ? '重要' : '普通'}
                </Tag>
              </Descriptions.Item>
            </Descriptions>
          </Card>

          {/* 流程信息 */}
          {processInstance && (
            <Card title="流程信息" style={{ marginBottom: 16 }}>
              <Descriptions column={2} bordered>
                <Descriptions.Item label="流程名称">
                  {processInstance.processDefinitionName}
                </Descriptions.Item>
                <Descriptions.Item label="业务键">
                  {processInstance.businessKey}
                </Descriptions.Item>
                <Descriptions.Item label="开始时间">
                  {dayjs(processInstance.startTime).format('YYYY-MM-DD HH:mm:ss')}
                </Descriptions.Item>
                <Descriptions.Item label="流程状态">
                  <Tag color={processInstance.ended ? 'default' : 'processing'}>
                    {processInstance.ended ? '已结束' : '进行中'}
                  </Tag>
                </Descriptions.Item>
              </Descriptions>
            </Card>
          )}

          {/* 申请表单数据 */}
          <Card title="申请信息" style={{ marginBottom: 16 }}>
            <Descriptions column={1} bordered>
              {Object.entries(variables).map(([key, value]) => {
                // 过滤掉系统字段
                if (key.startsWith('_') || key === 'approved' || key === 'comment') {
                  return null
                }
                return (
                  <Descriptions.Item key={key} label={key}>
                    {typeof value === 'object' ? JSON.stringify(value) : String(value)}
                  </Descriptions.Item>
                )
              })}
            </Descriptions>
          </Card>

          {/* 审批表单 */}
          {task.assignee === user?.username && (
            <Card title="审批操作">
              <Form
                form={form}
                layout="vertical"
                onFinish={handleSubmit}
                initialValues={{ decision: 'approve' }}
              >
                <Form.Item
                  label="审批决定"
                  name="decision"
                  rules={[{ required: true, message: '请选择审批决定' }]}
                >
                  <Radio.Group>
                    <Radio.Button value="approve">
                      <CheckCircleOutlined /> 通过
                    </Radio.Button>
                    <Radio.Button value="reject">
                      <CloseCircleOutlined /> 驳回
                    </Radio.Button>
                    <Radio.Button value="return">
                      <RollbackOutlined /> 退回
                    </Radio.Button>
                  </Radio.Group>
                </Form.Item>

                <Form.Item
                  label="审批意见"
                  name="comment"
                  rules={[
                    { required: true, message: '请输入审批意见' },
                    { min: 5, message: '审批意见至少5个字符' },
                  ]}
                >
                  <TextArea
                    rows={4}
                    placeholder="请输入您的审批意见..."
                    maxLength={500}
                    showCount
                  />
                </Form.Item>

                <Form.Item>
                  <Space>
                    <Button type="primary" htmlType="submit" loading={submitting}>
                      提交审批
                    </Button>
                    <Button onClick={() => form.resetFields()}>重置</Button>
                    <Button onClick={() => navigate('/workflow/todo')}>取消</Button>
                  </Space>
                </Form.Item>
              </Form>
            </Card>
          )}

          {task.assignee && task.assignee !== user?.username && (
            <Card>
              <Alert
                message="无权操作"
                description={`该任务已被 ${task.assignee} 认领，您无权处理`}
                type="warning"
                showIcon
              />
            </Card>
          )}
        </Col>

        {/* 右侧：审批历史 */}
        <Col span={8}>
          <Card title="审批历史">
            {approvalHistory.length > 0 ? (
              <Timeline>
                {approvalHistory.map((item, index) => (
                  <Timeline.Item
                    key={index}
                    color={item.endTime ? 'green' : 'blue'}
                  >
                    <div>
                      <strong>{item.name}</strong>
                      <div style={{ color: '#666', fontSize: '12px' }}>
                        办理人：{item.assignee || '待认领'}
                      </div>
                      <div style={{ color: '#666', fontSize: '12px' }}>
                        开始：{dayjs(item.startTime).format('MM-DD HH:mm')}
                      </div>
                      {item.endTime && (
                        <div style={{ color: '#666', fontSize: '12px' }}>
                          完成：{dayjs(item.endTime).format('MM-DD HH:mm')}
                        </div>
                      )}
                      {!item.endTime && <Tag color="processing">进行中</Tag>}
                    </div>
                  </Timeline.Item>
                ))}
              </Timeline>
            ) : (
              <div style={{ textAlign: 'center', padding: '20px', color: '#999' }}>
                暂无审批历史
              </div>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default TaskDetail
