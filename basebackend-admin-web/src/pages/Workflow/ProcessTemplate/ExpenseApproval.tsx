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
  Table,
  Row,
  Col,
} from 'antd'
import { PlusOutlined, DeleteOutlined, UploadOutlined } from '@ant-design/icons'
import type { UploadFile } from 'antd/es/upload/interface'
import dayjs from 'dayjs'

import { startProcessInstance } from '@/api/workflow/processInstance'
import { useAuthStore } from '@/stores/auth'

const { TextArea } = Input
const { Option } = Select

interface ExpenseItem {
  key: string
  type: string
  amount: number
  date: string
  description: string
}

const ExpenseApproval: React.FC = () => {
  const navigate = useNavigate()
  const [form] = Form.useForm()
  const { user } = useAuthStore()

  const [loading, setLoading] = useState(false)
  const [fileList, setFileList] = useState<UploadFile[]>([])
  const [expenseItems, setExpenseItems] = useState<ExpenseItem[]>([
    {
      key: '1',
      type: 'transportation',
      amount: 0,
      date: dayjs().format('YYYY-MM-DD'),
      description: '',
    },
  ])

  // 计算总金额
  const calculateTotalAmount = () => {
    return expenseItems.reduce((sum, item) => sum + (item.amount || 0), 0)
  }

  // 添加费用项
  const handleAddExpenseItem = () => {
    const newKey = String(Date.now())
    setExpenseItems([
      ...expenseItems,
      {
        key: newKey,
        type: 'transportation',
        amount: 0,
        date: dayjs().format('YYYY-MM-DD'),
        description: '',
      },
    ])
  }

  // 删除费用项
  const handleDeleteExpenseItem = (key: string) => {
    setExpenseItems(expenseItems.filter((item) => item.key !== key))
  }

  // 更新费用项
  const handleUpdateExpenseItem = (key: string, field: keyof ExpenseItem, value: any) => {
    setExpenseItems(
      expenseItems.map((item) =>
        item.key === key ? { ...item, [field]: value } : item
      )
    )
  }

  // 提交表单
  const handleSubmit = async (values: any) => {
    if (!user) {
      message.error('用户未登录')
      return
    }

    if (expenseItems.length === 0) {
      message.error('请至少添加一条费用明细')
      return
    }

    const totalAmount = calculateTotalAmount()
    if (totalAmount === 0) {
      message.error('报销总金额不能为0')
      return
    }

    setLoading(true)
    try {
      const businessKey = `EXPENSE-${Date.now()}`

      // 构建流程变量
      const variables = {
        applicant: user.username,
        applicantName: user.realName || user.username,
        department: values.department,
        expenseDate: values.expenseDate.format('YYYY-MM-DD'),
        totalAmount: totalAmount,
        expenseItems: expenseItems,
        purpose: values.purpose,
        approver: values.approver,
        email: user.email,
        emailSubject: '报销审批通知',
        emailContent: `${user.realName || user.username} 提交了报销申请，总金额：${totalAmount}元`,
        attachments: fileList.map((file) => file.url || file.name),
      }

      // 启动流程
      const response = await startProcessInstance({
        processDefinitionKey: 'expense-approval-process',
        businessKey,
        variables,
      })

      if (response.success) {
        message.success('报销申请提交成功')
        form.resetFields()
        setExpenseItems([])
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

  const expenseColumns = [
    {
      title: '费用类型',
      dataIndex: 'type',
      width: 150,
      render: (text: string, record: ExpenseItem) => (
        <Select
          value={text}
          onChange={(value) => handleUpdateExpenseItem(record.key, 'type', value)}
          style={{ width: '100%' }}
        >
          <Option value="transportation">交通费</Option>
          <Option value="accommodation">住宿费</Option>
          <Option value="meal">餐饮费</Option>
          <Option value="communication">通讯费</Option>
          <Option value="entertainment">招待费</Option>
          <Option value="office">办公费</Option>
          <Option value="other">其他</Option>
        </Select>
      ),
    },
    {
      title: '金额（元）',
      dataIndex: 'amount',
      width: 120,
      render: (text: number, record: ExpenseItem) => (
        <InputNumber
          value={text}
          onChange={(value) => handleUpdateExpenseItem(record.key, 'amount', value || 0)}
          min={0}
          precision={2}
          style={{ width: '100%' }}
        />
      ),
    },
    {
      title: '日期',
      dataIndex: 'date',
      width: 150,
      render: (text: string, record: ExpenseItem) => (
        <DatePicker
          value={dayjs(text)}
          onChange={(date) =>
            handleUpdateExpenseItem(record.key, 'date', date?.format('YYYY-MM-DD'))
          }
          style={{ width: '100%' }}
        />
      ),
    },
    {
      title: '说明',
      dataIndex: 'description',
      render: (text: string, record: ExpenseItem) => (
        <Input
          value={text}
          onChange={(e) => handleUpdateExpenseItem(record.key, 'description', e.target.value)}
          placeholder="费用说明"
        />
      ),
    },
    {
      title: '操作',
      width: 80,
      render: (_: any, record: ExpenseItem) => (
        <Button
          type="link"
          danger
          icon={<DeleteOutlined />}
          onClick={() => handleDeleteExpenseItem(record.key)}
          disabled={expenseItems.length === 1}
        >
          删除
        </Button>
      ),
    },
  ]

  return (
    <Card
      title="报销申请"
      extra={<Button onClick={() => navigate(-1)}>返回</Button>}
    >
      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        initialValues={{
          applicant: user?.realName || user?.username,
          expenseDate: dayjs(),
        }}
      >
        <Row gutter={24}>
          <Col span={8}>
            <Form.Item label="申请人" name="applicant">
              <Input disabled />
            </Form.Item>
          </Col>

          <Col span={8}>
            <Form.Item
              label="所属部门"
              name="department"
              rules={[{ required: true, message: '请选择所属部门' }]}
            >
              <Select placeholder="请选择">
                <Option value="研发部">研发部</Option>
                <Option value="市场部">市场部</Option>
                <Option value="销售部">销售部</Option>
                <Option value="财务部">财务部</Option>
                <Option value="人力资源部">人力资源部</Option>
                <Option value="行政部">行政部</Option>
              </Select>
            </Form.Item>
          </Col>

          <Col span={8}>
            <Form.Item
              label="报销日期"
              name="expenseDate"
              rules={[{ required: true, message: '请选择报销日期' }]}
            >
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>
          </Col>

          <Col span={24}>
            <Form.Item label="费用明细">
              <Table
                columns={expenseColumns}
                dataSource={expenseItems}
                pagination={false}
                footer={() => (
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Button
                      type="dashed"
                      onClick={handleAddExpenseItem}
                      icon={<PlusOutlined />}
                    >
                      添加费用项
                    </Button>
                    <div style={{ fontSize: '16px', fontWeight: 'bold' }}>
                      合计：<span style={{ color: '#f5222d' }}>¥ {calculateTotalAmount().toFixed(2)}</span>
                    </div>
                  </div>
                )}
              />
            </Form.Item>
          </Col>

          <Col span={24}>
            <Form.Item
              label="报销事由"
              name="purpose"
              rules={[
                { required: true, message: '请输入报销事由' },
                { min: 10, message: '报销事由至少10个字符' },
              ]}
            >
              <TextArea
                rows={4}
                placeholder="请详细说明报销事由"
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
                <Option value="finance">财务主管</Option>
                <Option value="director">部门总监</Option>
              </Select>
            </Form.Item>
          </Col>

          <Col span={24}>
            <Form.Item
              label="发票附件"
              name="attachments"
              extra="请上传发票、收据等凭证（必须）"
              rules={[
                {
                  validator: () => {
                    if (fileList.length === 0) {
                      return Promise.reject('请至少上传一个发票附件')
                    }
                    return Promise.resolve()
                  },
                },
              ]}
            >
              <Upload
                fileList={fileList}
                onChange={({ fileList }) => setFileList(fileList)}
                beforeUpload={() => false}
                accept="image/*,.pdf"
                listType="picture-card"
              >
                {fileList.length < 10 && (
                  <div>
                    <PlusOutlined />
                    <div style={{ marginTop: 8 }}>上传</div>
                  </div>
                )}
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

export default ExpenseApproval
