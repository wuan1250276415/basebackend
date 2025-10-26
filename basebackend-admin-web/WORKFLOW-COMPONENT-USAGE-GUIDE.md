# 工作流组件使用指南

本文档介绍如何使用工作流模块提供的通用组件、工具函数和常量配置。

---

## 📦 通用组件库

位置：`src/components/Workflow/`

### 1. 状态标签组件

#### ProcessStatusTag - 流程状态标签

```tsx
import { ProcessStatusTag } from '@/components/Workflow'

// 使用示例
<ProcessStatusTag ended={false} suspended={false} />
// 显示：进行中（蓝色旋转图标）

<ProcessStatusTag ended={true} />
// 显示：已完成（绿色勾选图标）

<ProcessStatusTag suspended={true} />
// 显示：已挂起（橙色暂停图标）

<ProcessStatusTag deleteReason="用户取消" />
// 显示：已终止（红色叉号图标）
```

#### TaskStatusTag - 任务状态标签

```tsx
import { TaskStatusTag } from '@/components/Workflow'

// 已完成任务
<TaskStatusTag endTime="2025-01-15 10:00:00" />

// 即将超时任务（24小时内到期）
<TaskStatusTag dueDate="2025-01-16 12:00:00" />

// 已超时任务
<TaskStatusTag dueDate="2025-01-14 12:00:00" />

// 正常任务
<TaskStatusTag />
```

#### PriorityTag - 优先级标签

```tsx
import { PriorityTag } from '@/components/Workflow'

<PriorityTag priority={90} />  // 紧急（红色）
<PriorityTag priority={70} />  // 重要（橙色）
<PriorityTag priority={50} />  // 普通（蓝色）
```

#### ProcessTypeTag - 流程类型标签

```tsx
import { ProcessTypeTag } from '@/components/Workflow'

<ProcessTypeTag processName="请假审批流程" />  // 蓝色
<ProcessTypeTag processName="报销审批流程" />  // 绿色
<ProcessTypeTag processName="采购审批流程" />  // 橙色
```

### 2. 统计卡片组件

#### WorkflowStatistics - 工作流统计卡片

```tsx
import { WorkflowStatistics } from '@/components/Workflow'

<WorkflowStatistics
  total={100}
  active={30}
  completed={60}
  suspended={10}
  loading={false}
/>
```

显示效果：四个统计卡片，展示总数、进行中、已完成、已挂起

#### SimpleStatistics - 简单统计卡片

```tsx
import { SimpleStatistics } from '@/components/Workflow'
import { ClockCircleOutlined, CheckCircleOutlined } from '@ant-design/icons'

<SimpleStatistics
  items={[
    {
      title: '待处理',
      value: 15,
      color: '#faad14',
      icon: <ClockCircleOutlined />,
    },
    {
      title: '已完成',
      value: 85,
      color: '#52c41a',
      icon: <CheckCircleOutlined />,
    },
  ]}
/>
```

### 3. 空状态组件

#### EmptyTodoTasks - 无待办任务

```tsx
import { EmptyTodoTasks } from '@/components/Workflow'

<EmptyTodoTasks onRefresh={() => loadTasks()} />
```

#### EmptyProcessInstances - 无流程实例

```tsx
import { EmptyProcessInstances } from '@/components/Workflow'

<EmptyProcessInstances onCreate={() => navigate('/workflow/template')} />
```

#### EmptySearchResult - 无搜索结果

```tsx
import { EmptySearchResult } from '@/components/Workflow'

<EmptySearchResult onClear={() => setSearchText('')} />
```

#### EmptyState - 通用空状态

```tsx
import { EmptyState } from '@/components/Workflow'
import { FileTextOutlined } from '@ant-design/icons'

<EmptyState
  icon={<FileTextOutlined style={{ fontSize: 64, color: '#d9d9d9' }} />}
  title="暂无数据"
  description="请先添加数据"
  action={{
    text: '立即添加',
    onClick: handleAdd,
    type: 'primary',
  }}
/>
```

### 4. 时间轴组件

#### TaskHistoryTimeline - 任务历史时间轴

```tsx
import { TaskHistoryTimeline } from '@/components/Workflow'
import type { HistoryItem } from '@/components/Workflow'

const history: HistoryItem[] = [
  {
    id: '1',
    name: '提交申请',
    assignee: '张三',
    startTime: '2025-01-15 09:00:00',
    endTime: '2025-01-15 09:05:00',
    comment: '同意',
  },
  {
    id: '2',
    name: '部门经理审批',
    assignee: '李四',
    startTime: '2025-01-15 10:00:00',
    endTime: '2025-01-15 10:30:00',
    comment: '批准通过',
  },
]

<TaskHistoryTimeline history={history} loading={false} />
```

#### SimpleTimeline - 简化版时间轴

```tsx
import { SimpleTimeline } from '@/components/Workflow'

<SimpleTimeline history={history} />
```

---

## 🛠️ 工具函数库

位置：`src/utils/workflow/`

### 1. 日期时间工具（dateUtils）

```tsx
import {
  calculateDaysBetween,
  calculateDuration,
  formatDateTime,
  formatRelativeTime,
  isApproachingDue,
  isOverdue,
} from '@/utils/workflow'

// 计算两个日期之间的天数
const days = calculateDaysBetween('2025-01-15', '2025-01-20')
// 结果：6天

// 计算持续时间（友好格式）
const duration = calculateDuration('2025-01-15 09:00:00', '2025-01-15 11:30:00')
// 结果："2小时30分钟"

// 格式化日期时间
const formatted = formatDateTime(new Date(), 'YYYY-MM-DD HH:mm:ss')
// 结果："2025-01-15 14:30:00"

// 格式化相对时间
const relative = formatRelativeTime('2025-01-15 10:00:00')
// 结果："2小时前"

// 检查是否即将到期（24小时内）
const approaching = isApproachingDue('2025-01-16 10:00:00')
// 结果：true/false

// 检查是否已超时
const overdue = isOverdue('2025-01-14 10:00:00')
// 结果：true
```

### 2. 键值生成工具（keyGenerator）

```tsx
import {
  generateBusinessKey,
  generateProcessBusinessKey,
  parseBusinessKey,
  generateUUID,
} from '@/utils/workflow'

// 生成业务键
const key1 = generateBusinessKey('LEAVE')
// 结果："LEAVE-1705302000000-1234"

// 生成流程业务键
const key2 = generateProcessBusinessKey('leave')
// 结果："LEAVE-1705302000000-5678"

// 解析业务键
const parsed = parseBusinessKey('LEAVE-1705302000000-1234')
// 结果：{ type: 'LEAVE', timestamp: 1705302000000, id: '1234' }

// 生成UUID
const uuid = generateUUID()
// 结果："550e8400-e29b-41d4-a716-446655440000"
```

### 3. 状态工具（statusUtils）

```tsx
import {
  getPriorityColor,
  getPriorityText,
  getProcessStatusColor,
  getProcessStatusText,
  getTaskStatusColor,
  getTaskStatusText,
} from '@/utils/workflow'

// 获取优先级颜色
const color = getPriorityColor(80)
// 结果："#f5222d"（红色）

// 获取优先级文本
const text = getPriorityText(80)
// 结果："紧急"

// 获取流程状态颜色
const processColor = getProcessStatusColor(processInstance)
// 根据流程状态返回对应颜色

// 获取流程状态文本
const processText = getProcessStatusText(processInstance)
// 结果："进行中" / "已完成" / "已挂起" / "已终止"
```

### 4. 金额工具（amountUtils）

```tsx
import {
  formatCurrency,
  formatCurrencyWithSymbol,
  calculateTotalAmount,
  isValidAmount,
  roundAmount,
} from '@/utils/workflow'

// 格式化金额（千分位）
const formatted = formatCurrency(12345.67)
// 结果："12,345.67"

// 格式化金额（带货币符号）
const withSymbol = formatCurrencyWithSymbol(12345.67)
// 结果："¥ 12,345.67"

// 计算总金额
const items = [
  { amount: 100.50 },
  { amount: 200.30 },
  { amount: 300.20 },
]
const total = calculateTotalAmount(items)
// 结果：601.00

// 验证金额是否有效
const valid = isValidAmount(12345.67)
// 结果：true

// 四舍五入
const rounded = roundAmount(12.345, 2)
// 结果：12.35
```

---

## 🔧 常量配置

位置：`src/constants/workflow/`

### 1. 流程状态常量

```tsx
import { PROCESS_STATUS } from '@/constants/workflow'

PROCESS_STATUS.ACTIVE      // 'active'
PROCESS_STATUS.SUSPENDED   // 'suspended'
PROCESS_STATUS.COMPLETED   // 'completed'
PROCESS_STATUS.TERMINATED  // 'terminated'
```

### 2. 审批决定常量

```tsx
import { APPROVAL_DECISION, APPROVAL_DECISION_TEXT } from '@/constants/workflow'

APPROVAL_DECISION.APPROVE  // 'approve'
APPROVAL_DECISION.REJECT   // 'reject'
APPROVAL_DECISION.RETURN   // 'return'

APPROVAL_DECISION_TEXT[APPROVAL_DECISION.APPROVE]  // '通过'
APPROVAL_DECISION_TEXT[APPROVAL_DECISION.REJECT]   // '驳回'
APPROVAL_DECISION_TEXT[APPROVAL_DECISION.RETURN]   // '退回'
```

### 3. 优先级常量

```tsx
import { PRIORITY, PRIORITY_TEXT, PRIORITY_COLOR } from '@/constants/workflow'

PRIORITY.LOW      // 30
PRIORITY.NORMAL   // 50
PRIORITY.HIGH     // 70
PRIORITY.URGENT   // 90

PRIORITY_TEXT[PRIORITY.URGENT]   // '紧急'
PRIORITY_COLOR[PRIORITY.URGENT]  // '#f5222d'
```

### 4. 请假类型常量

```tsx
import { LEAVE_TYPE, LEAVE_TYPE_TEXT } from '@/constants/workflow'

LEAVE_TYPE.ANNUAL      // 'annual'
LEAVE_TYPE.SICK        // 'sick'
LEAVE_TYPE.PERSONAL    // 'personal'
LEAVE_TYPE.MARRIAGE    // 'marriage'
LEAVE_TYPE.MATERNITY   // 'maternity'

LEAVE_TYPE_TEXT[LEAVE_TYPE.ANNUAL]  // '年假'
```

### 5. 报销类型常量

```tsx
import { EXPENSE_TYPE, EXPENSE_TYPE_TEXT } from '@/constants/workflow'

EXPENSE_TYPE.TRANSPORTATION  // 'transportation'
EXPENSE_TYPE.ACCOMMODATION   // 'accommodation'
EXPENSE_TYPE.MEAL            // 'meal'

EXPENSE_TYPE_TEXT[EXPENSE_TYPE.TRANSPORTATION]  // '交通费'
```

### 6. 采购类型常量

```tsx
import { PURCHASE_TYPE, PURCHASE_TYPE_TEXT } from '@/constants/workflow'

PURCHASE_TYPE.EQUIPMENT  // 'equipment'
PURCHASE_TYPE.OFFICE     // 'office'
PURCHASE_TYPE.SOFTWARE   // 'software'

PURCHASE_TYPE_TEXT[PURCHASE_TYPE.EQUIPMENT]  // '设备采购'
```

### 7. 分页配置

```tsx
import { PAGINATION } from '@/constants/workflow'

const paginationConfig = {
  pageSize: PAGINATION.DEFAULT_PAGE_SIZE,           // 10
  pageSizeOptions: PAGINATION.PAGE_SIZE_OPTIONS,    // [10, 20, 50, 100]
  showSizeChanger: PAGINATION.SHOW_SIZE_CHANGER,    // true
  showQuickJumper: PAGINATION.SHOW_QUICK_JUMPER,    // true
}
```

### 8. 文件上传配置

```tsx
import { UPLOAD_CONFIG } from '@/constants/workflow'

<Upload
  maxCount={UPLOAD_CONFIG.MAX_FILE_COUNT}
  accept={UPLOAD_CONFIG.ACCEPT_IMAGE}
  beforeUpload={(file) => {
    const isValid = file.size <= UPLOAD_CONFIG.MAX_FILE_SIZE
    if (!isValid) {
      message.error('文件大小不能超过10MB')
    }
    return isValid || Upload.LIST_IGNORE
  }}
>
  <Button icon={<UploadOutlined />}>上传图片</Button>
</Upload>
```

### 9. 权限常量

```tsx
import { PERMISSIONS } from '@/constants/workflow'

// 检查权限示例
const hasPermission = (permission: string) => {
  const userPermissions = user?.permissions || []
  return userPermissions.includes(permission)
}

// 使用
if (hasPermission(PERMISSIONS.TASK_COMPLETE)) {
  // 显示完成任务按钮
}

if (hasPermission(PERMISSIONS.DEFINITION_DEPLOY)) {
  // 显示部署流程按钮
}
```

### 10. 路由常量

```tsx
import { ROUTES } from '@/constants/workflow'

// 使用路由常量
navigate(ROUTES.TODO)                    // 跳转到待办任务
navigate(ROUTES.TEMPLATE_LEAVE)          // 跳转到请假申请
navigate(`/workflow/todo/${taskId}`)     // 跳转到任务详情
```

---

## 💡 完整使用示例

### 示例1：优化后的待办任务列表

```tsx
import React, { useState, useEffect } from 'react'
import { Card, Table, Button, Space, message } from 'antd'
import {
  ProcessTypeTag,
  PriorityTag,
  TaskStatusTag,
  EmptyTodoTasks,
} from '@/components/Workflow'
import { formatRelativeTime, calculateDuration } from '@/utils/workflow'
import { PAGINATION, ROUTES } from '@/constants/workflow'
import { listPendingTasks } from '@/api/workflow/task'
import { useNavigate } from 'react-router-dom'

const TodoListOptimized: React.FC = () => {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [tasks, setTasks] = useState([])

  const loadTasks = async () => {
    setLoading(true)
    try {
      const response = await listPendingTasks()
      if (response.success) {
        setTasks(response.data?.list || [])
      }
    } catch (error) {
      message.error('加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadTasks()
  }, [])

  const columns = [
    {
      title: '任务名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '流程类型',
      dataIndex: 'processDefinitionName',
      key: 'type',
      render: (text: string) => <ProcessTypeTag processName={text} />,
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      render: (priority: number) => <PriorityTag priority={priority} />,
    },
    {
      title: '状态',
      key: 'status',
      render: (_: any, record: any) => (
        <TaskStatusTag dueDate={record.dueDate} endTime={record.endTime} />
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      render: (time: string) => formatRelativeTime(time),
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: any) => (
        <Space>
          <Button
            type="link"
            onClick={() => navigate(`${ROUTES.TODO}/${record.id}`)}
          >
            处理
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <Card title="待办任务" extra={<Button onClick={loadTasks}>刷新</Button>}>
      <Table
        columns={columns}
        dataSource={tasks}
        rowKey="id"
        loading={loading}
        locale={{
          emptyText: <EmptyTodoTasks onRefresh={loadTasks} />,
        }}
        pagination={{
          pageSize: PAGINATION.DEFAULT_PAGE_SIZE,
          showSizeChanger: PAGINATION.SHOW_SIZE_CHANGER,
          showQuickJumper: PAGINATION.SHOW_QUICK_JUMPER,
        }}
      />
    </Card>
  )
}

export default TodoListOptimized
```

### 示例2：优化后的报销申请表单

```tsx
import React, { useState } from 'react'
import { Form, Input, Button, Select, DatePicker, InputNumber, message } from 'antd'
import { formatCurrencyWithSymbol, calculateTotalAmount, generateProcessBusinessKey } from '@/utils/workflow'
import { EXPENSE_TYPE, EXPENSE_TYPE_TEXT } from '@/constants/workflow'
import { startProcessInstance } from '@/api/workflow/processInstance'
import { useNavigate } from 'react-router-dom'

const ExpenseApprovalOptimized: React.FC = () => {
  const [form] = Form.useForm()
  const navigate = useNavigate()
  const [expenseItems, setExpenseItems] = useState([])

  const handleSubmit = async (values: any) => {
    const businessKey = generateProcessBusinessKey('expense')
    const totalAmount = calculateTotalAmount(expenseItems)

    try {
      const response = await startProcessInstance({
        processDefinitionKey: 'expense-approval-process',
        businessKey,
        variables: {
          ...values,
          totalAmount,
          items: expenseItems,
        },
      })

      if (response.success) {
        message.success('提交成功')
        navigate('/workflow/initiated')
      }
    } catch (error) {
      message.error('提交失败')
    }
  }

  return (
    <Form form={form} layout="vertical" onFinish={handleSubmit}>
      <Form.Item label="报销类型" name="expenseType" rules={[{ required: true }]}>
        <Select>
          {Object.entries(EXPENSE_TYPE_TEXT).map(([key, value]) => (
            <Select.Option key={key} value={key}>
              {value}
            </Select.Option>
          ))}
        </Select>
      </Form.Item>

      {/* 其他表单项... */}

      <div style={{ marginTop: 16 }}>
        总金额：<strong style={{ fontSize: 18, color: '#f5222d' }}>
          {formatCurrencyWithSymbol(calculateTotalAmount(expenseItems))}
        </strong>
      </div>

      <Form.Item style={{ marginTop: 24 }}>
        <Button type="primary" htmlType="submit">
          提交申请
        </Button>
      </Form.Item>
    </Form>
  )
}

export default ExpenseApprovalOptimized
```

---

## 🎯 最佳实践

### 1. 使用常量而非硬编码

❌ **不推荐：**
```tsx
if (status === 'active') {
  // ...
}
```

✅ **推荐：**
```tsx
import { PROCESS_STATUS } from '@/constants/workflow'

if (status === PROCESS_STATUS.ACTIVE) {
  // ...
}
```

### 2. 使用工具函数统一格式化

❌ **不推荐：**
```tsx
const formatted = new Date(dateString).toLocaleString()
```

✅ **推荐：**
```tsx
import { formatDateTime } from '@/utils/workflow'

const formatted = formatDateTime(dateString)
```

### 3. 使用组件而非重复代码

❌ **不推荐：**
```tsx
{instance.ended ? (
  <Tag color="success">已完成</Tag>
) : instance.suspended ? (
  <Tag color="warning">已挂起</Tag>
) : (
  <Tag color="processing">进行中</Tag>
)}
```

✅ **推荐：**
```tsx
import { ProcessStatusTag } from '@/components/Workflow'

<ProcessStatusTag ended={instance.ended} suspended={instance.suspended} />
```

### 4. 使用空状态组件提升体验

❌ **不推荐：**
```tsx
{tasks.length === 0 && <div>暂无数据</div>}
```

✅ **推荐：**
```tsx
import { EmptyTodoTasks } from '@/components/Workflow'

{tasks.length === 0 && <EmptyTodoTasks onRefresh={loadTasks} />}
```

---

## 📚 相关文档

- [工作流前端完成报告](./WORKFLOW-FRONTEND-FINAL-REPORT.md)
- [工作流实施指南](./WORKFLOW-IMPLEMENTATION.md)
- [菜单配置指南](./WORKFLOW-MENU-CONFIG-GUIDE.md)

---

希望这份指南能帮助你更好地使用工作流模块！🎉
