# 工作流前端代码示例

本文档包含关键页面的完整代码示例，可以直接使用或作为参考。

---

## 📝 待办任务列表页面

这是最重要的页面，用户使用频率最高。

### 文件位置
`src/pages/Workflow/TaskManagement/TodoList.tsx`

### 完整代码

```typescript
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
  Badge,
  Tooltip,
} from 'antd'
import {
  ClockCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  EyeOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

import { listPendingTasks, claimTask } from '@/api/workflow/task'
import { useWorkflowStore } from '@/stores/workflow'
import { useAuthStore } from '@/stores/auth'
import type { Task } from '@/types/workflow'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const { Search } = Input

const TodoList: React.FC = () => {
  const navigate = useNavigate()
  const { user } = useAuthStore()
  const { setCurrentTask, setPendingTaskCount } = useWorkflowStore()

  const [loading, setLoading] = useState(false)
  const [tasks, setTasks] = useState<Task[]>([])
  const [searchText, setSearchText] = useState('')
  const [filteredTasks, setFilteredTasks] = useState<Task[]>([])

  // 加载待办任务
  const loadTasks = async () => {
    if (!user) return

    setLoading(true)
    try {
      const response = await listPendingTasks(user.username)
      if (response.success) {
        const taskList = response.data?.list || []
        setTasks(taskList)
        setFilteredTasks(taskList)
        setPendingTaskCount(taskList.length)
      } else {
        message.error(response.message || '加载任务失败')
      }
    } catch (error) {
      message.error('加载任务失败')
      console.error(error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadTasks()
  }, [user])

  // 搜索过滤
  useEffect(() => {
    if (searchText) {
      const filtered = tasks.filter(
        (task) =>
          task.name?.toLowerCase().includes(searchText.toLowerCase()) ||
          task.processInstanceId?.toLowerCase().includes(searchText.toLowerCase())
      )
      setFilteredTasks(filtered)
    } else {
      setFilteredTasks(tasks)
    }
  }, [searchText, tasks])

  // 处理查看任务
  const handleView = (task: Task) => {
    setCurrentTask(task)
    navigate(`/workflow/todo/${task.id}`)
  }

  // 处理认领任务
  const handleClaim = async (task: Task) => {
    if (!user) return

    try {
      const response = await claimTask(task.id, { userId: user.username })
      if (response.success) {
        message.success('认领成功')
        loadTasks()
      } else {
        message.error(response.message || '认领失败')
      }
    } catch (error) {
      message.error('认领失败')
      console.error(error)
    }
  }

  // 获取优先级标签
  const getPriorityTag = (priority: number) => {
    if (priority >= 80) {
      return <Tag color="red">紧急</Tag>
    } else if (priority >= 50) {
      return <Tag color="orange">重要</Tag>
    } else {
      return <Tag color="blue">普通</Tag>
    }
  }

  // 获取任务状态
  const getTaskStatus = (task: Task) => {
    if (task.dueDate) {
      const now = dayjs()
      const due = dayjs(task.dueDate)
      if (due.isBefore(now)) {
        return <Tag icon={<CloseCircleOutlined />} color="error">已超时</Tag>
      } else if (due.diff(now, 'hour') < 24) {
        return <Tag icon={<ClockCircleOutlined />} color="warning">即将超时</Tag>
      }
    }
    return <Tag icon={<CheckCircleOutlined />} color="success">正常</Tag>
  }

  const columns: ColumnsType<Task> = [
    {
      title: '任务名称',
      dataIndex: 'name',
      key: 'name',
      render: (text, record) => (
        <Space>
          <span>{text}</span>
          {record.priority >= 80 && (
            <Badge status="error" text="紧急" />
          )}
        </Space>
      ),
    },
    {
      title: '流程实例',
      dataIndex: 'processInstanceId',
      key: 'processInstanceId',
      render: (text) => (
        <Tooltip title={text}>
          <span style={{ maxWidth: 150, display: 'inline-block', overflow: 'hidden', textOverflow: 'ellipsis' }}>
            {text}
          </span>
        </Tooltip>
      ),
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      width: 100,
      render: (priority) => getPriorityTag(priority),
    },
    {
      title: '状态',
      key: 'status',
      width: 120,
      render: (_, record) => getTaskStatus(record),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
      render: (text) => (
        <Tooltip title={dayjs(text).format('YYYY-MM-DD HH:mm:ss')}>
          {dayjs(text).fromNow()}
        </Tooltip>
      ),
    },
    {
      title: '到期时间',
      dataIndex: 'dueDate',
      key: 'dueDate',
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
      title: '办理人',
      dataIndex: 'assignee',
      key: 'assignee',
      width: 100,
      render: (text) => text || <Tag>待认领</Tag>,
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
            onClick={() => handleView(record)}
          >
            查看
          </Button>
          {!record.assignee && (
            <Button
              type="link"
              size="small"
              onClick={() => handleClaim(record)}
            >
              认领
            </Button>
          )}
        </Space>
      ),
    },
  ]

  return (
    <Card
      title={
        <Space>
          <span>待办任务</span>
          <Badge count={filteredTasks.length} overflowCount={99} />
        </Space>
      }
      extra={
        <Space>
          <Search
            placeholder="搜索任务名称或流程实例"
            allowClear
            onSearch={setSearchText}
            onChange={(e) => setSearchText(e.target.value)}
            style={{ width: 300 }}
          />
          <Button onClick={loadTasks}>刷新</Button>
        </Space>
      }
    >
      <Table
        columns={columns}
        dataSource={filteredTasks}
        rowKey="id"
        loading={loading}
        pagination={{
          total: filteredTasks.length,
          pageSize: 10,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 条`,
        }}
        scroll={{ x: 1200 }}
      />
    </Card>
  )
}

export default TodoList
```

---

## 📋 流程模板（请假审批）

### 文件位置
`src/pages/Workflow/ProcessTemplate/LeaveApproval.tsx`

### 完整代码

```typescript
import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Card,
  Form,
  Input,
  DatePicker,
  Select,
  InputNumber,
  Button,
  Space,
  message,
  Upload,
  Row,
  Col,
} from 'antd'
import { UploadOutlined } from '@ant-design/icons'
import type { UploadFile } from 'antd/es/upload/interface'
import dayjs, { Dayjs } from 'dayjs'

import { startProcessInstance } from '@/api/workflow/processInstance'
import { useAuthStore } from '@/stores/auth'

const { TextArea } = Input
const { RangePicker } = DatePicker
const { Option } = Select

const LeaveApproval: React.FC = () => {
  const navigate = useNavigate()
  const [form] = Form.useForm()
  const { user } = useAuthStore()

  const [loading, setLoading] = useState(false)
  const [fileList, setFileList] = useState<UploadFile[]>([])

  // 计算请假天数
  const calculateLeaveDays = (dates: [Dayjs, Dayjs] | null) => {
    if (!dates) return 0
    const [start, end] = dates
    return end.diff(start, 'day') + 1
  }

  // 提交表单
  const handleSubmit = async (values: any) => {
    if (!user) {
      message.error('用户未登录')
      return
    }

    setLoading(true)
    try {
      const [startDate, endDate] = values.leaveDates
      const businessKey = `LEAVE-${Date.now()}`

      // 构建流程变量
      const variables = {
        applicant: user.username,
        applicantName: user.realName || user.username,
        leaveType: values.leaveType,
        startDate: startDate.format('YYYY-MM-DD'),
        endDate: endDate.format('YYYY-MM-DD'),
        leaveDays: calculateLeaveDays(values.leaveDates),
        reason: values.reason,
        approver: values.approver,
        email: user.email,
        emailSubject: '请假审批通知',
        emailContent: `${user.realName || user.username} 提交了请假申请，请及时处理。`,
        attachments: fileList.map((file) => file.url || file.name),
      }

      // 启动流程
      const response = await startProcessInstance({
        processDefinitionKey: 'leave-approval-process',
        businessKey,
        variables,
      })

      if (response.success) {
        message.success('请假申请提交成功')
        form.resetFields()
        setFileList([])
        navigate('/workflow/initiated')
      } else {
        message.error(response.message || '提交失败')
      }
    } catch (error) {
      message.error('提交失败')
      console.error(error)
    } finally {
      setLoading(false)
    }
  }

  return (
    <Card title="请假申请" extra={<Button onClick={() => navigate(-1)}>返回</Button>}>
      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        initialValues={{
          leaveType: 'annual',
          leaveDays: 0,
        }}
      >
        <Row gutter={24}>
          <Col span={12}>
            <Form.Item
              label="申请人"
              name="applicant"
              initialValue={user?.realName || user?.username}
            >
              <Input disabled />
            </Form.Item>
          </Col>

          <Col span={12}>
            <Form.Item
              label="请假类型"
              name="leaveType"
              rules={[{ required: true, message: '请选择请假类型' }]}
            >
              <Select placeholder="请选择">
                <Option value="annual">年假</Option>
                <Option value="sick">病假</Option>
                <Option value="personal">事假</Option>
                <Option value="marriage">婚假</Option>
                <Option value="maternity">产假</Option>
                <Option value="other">其他</Option>
              </Select>
            </Form.Item>
          </Col>

          <Col span={16}>
            <Form.Item
              label="请假时间"
              name="leaveDates"
              rules={[{ required: true, message: '请选择请假时间' }]}
            >
              <RangePicker
                style={{ width: '100%' }}
                onChange={(dates) => {
                  if (dates) {
                    form.setFieldValue('leaveDays', calculateLeaveDays(dates))
                  }
                }}
              />
            </Form.Item>
          </Col>

          <Col span={8}>
            <Form.Item label="请假天数" name="leaveDays">
              <InputNumber disabled style={{ width: '100%' }} suffix="天" />
            </Form.Item>
          </Col>

          <Col span={24}>
            <Form.Item
              label="请假事由"
              name="reason"
              rules={[
                { required: true, message: '请输入请假事由' },
                { min: 10, message: '请假事由至少10个字符' },
              ]}
            >
              <TextArea
                rows={4}
                placeholder="请详细说明请假事由"
                maxLength={500}
                showCount
              />
            </Form.Item>
          </Col>

          <Col span={12}>
            <Form.Item
              label="审批人"
              name="approver"
              rules={[{ required: true, message: '请选择审批人' }]}
            >
              <Select placeholder="请选择审批人">
                <Option value="manager">直属经理</Option>
                <Option value="director">部门总监</Option>
                <Option value="hr">人力资源</Option>
              </Select>
            </Form.Item>
          </Col>

          <Col span={24}>
            <Form.Item label="附件" name="attachments">
              <Upload
                fileList={fileList}
                onChange={({ fileList }) => setFileList(fileList)}
                beforeUpload={() => false}
              >
                <Button icon={<UploadOutlined />}>上传附件</Button>
              </Upload>
            </Form.Item>
          </Col>
        </Row>

        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" loading={loading}>
              提交申请
            </Button>
            <Button onClick={() => form.resetFields()}>重置</Button>
            <Button onClick={() => navigate(-1)}>取消</Button>
          </Space>
        </Form.Item>
      </Form>
    </Card>
  )
}

export default LeaveApproval
```

---

## 🔄 使用这些代码

### 1. 创建目录结构

```bash
cd src/pages
mkdir -p Workflow/TaskManagement
mkdir -p Workflow/ProcessTemplate
```

### 2. 复制代码文件

将上面的代码保存到对应的文件中。

### 3. 更新路由配置

在 `src/router/index.tsx` 中添加：

```typescript
import TodoList from '@/pages/Workflow/TaskManagement/TodoList'
import LeaveApproval from '@/pages/Workflow/ProcessTemplate/LeaveApproval'

// 在 Routes 中添加
<Route path="workflow/todo" element={<TodoList />} />
<Route path="workflow/template/leave" element={<LeaveApproval />} />
```

### 4. 测试页面

访问：
- http://localhost:5173/workflow/todo
- http://localhost:5173/workflow/template/leave

---

## 📚 更多示例

完整的代码示例包括：

1. ✅ 待办任务列表 - TodoList.tsx
2. ✅ 请假申请表单 - LeaveApproval.tsx
3. ⏳ 任务详情页面 - TaskDetail.tsx (待创建)
4. ⏳ 流程实例监控 - ProcessInstance/index.tsx (待创建)
5. ⏳ BPMN 流程查看器 - components/BpmnViewer.tsx (待创建)

按照这个模式，您可以继续创建其他页面。所有页面都遵循相同的结构和风格。
