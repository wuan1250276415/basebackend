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
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons'
import type { UploadFile } from 'antd/es/upload/interface'
import dayjs from 'dayjs'

import { startProcessInstance } from '@/api/workflow/processInstance'
import { useAuthStore } from '@/stores/auth'

const { TextArea } = Input
const { Option } = Select

interface PurchaseItem {
  key: string
  name: string
  specification: string
  quantity: number
  unit: string
  unitPrice: number
  totalPrice: number
  supplier: string
}

const PurchaseApproval: React.FC = () => {
  const navigate = useNavigate()
  const [form] = Form.useForm()
  const { user } = useAuthStore()

  const [loading, setLoading] = useState(false)
  const [fileList, setFileList] = useState<UploadFile[]>([])
  const [purchaseItems, setPurchaseItems] = useState<PurchaseItem[]>([
    {
      key: '1',
      name: '',
      specification: '',
      quantity: 1,
      unit: '个',
      unitPrice: 0,
      totalPrice: 0,
      supplier: '',
    },
  ])

  // 计算总金额
  const calculateTotalAmount = () => {
    return purchaseItems.reduce((sum, item) => sum + (item.totalPrice || 0), 0)
  }

  // 添加采购项
  const handleAddPurchaseItem = () => {
    const newKey = String(Date.now())
    setPurchaseItems([
      ...purchaseItems,
      {
        key: newKey,
        name: '',
        specification: '',
        quantity: 1,
        unit: '个',
        unitPrice: 0,
        totalPrice: 0,
        supplier: '',
      },
    ])
  }

  // 删除采购项
  const handleDeletePurchaseItem = (key: string) => {
    setPurchaseItems(purchaseItems.filter((item) => item.key !== key))
  }

  // 更新采购项
  const handleUpdatePurchaseItem = (
    key: string,
    field: keyof PurchaseItem,
    value: any
  ) => {
    setPurchaseItems(
      purchaseItems.map((item) => {
        if (item.key === key) {
          const updatedItem = { ...item, [field]: value }
          // 自动计算总价
          if (field === 'quantity' || field === 'unitPrice') {
            updatedItem.totalPrice = updatedItem.quantity * updatedItem.unitPrice
          }
          return updatedItem
        }
        return item
      })
    )
  }

  // 提交表单
  const handleSubmit = async (values: any) => {
    if (!user) {
      message.error('用户未登录')
      return
    }

    if (purchaseItems.length === 0) {
      message.error('请至少添加一条采购明细')
      return
    }

    // 验证采购项
    for (const item of purchaseItems) {
      if (!item.name) {
        message.error('请填写采购物品名称')
        return
      }
      if (item.quantity <= 0 || item.unitPrice <= 0) {
        message.error('数量和单价必须大于0')
        return
      }
    }

    const totalAmount = calculateTotalAmount()
    if (totalAmount === 0) {
      message.error('采购总金额不能为0')
      return
    }

    setLoading(true)
    try {
      const businessKey = `PURCHASE-${Date.now()}`

      // 构建流程变量
      const variables = {
        applicant: user.username,
        applicantName: user.realName || user.username,
        department: values.department,
        purchaseType: values.purchaseType,
        expectedDate: values.expectedDate.format('YYYY-MM-DD'),
        totalAmount: totalAmount,
        purchaseItems: purchaseItems,
        reason: values.reason,
        approver: values.approver,
        email: user.email,
        emailSubject: '采购审批通知',
        emailContent: `${user.realName || user.username} 提交了采购申请，总金额：${totalAmount}元`,
        attachments: fileList.map((file) => file.url || file.name),
      }

      // 启动流程
      const response = await startProcessInstance({
        processDefinitionKey: 'purchase-approval-process',
        businessKey,
        variables,
      })

      if (response.success) {
        message.success('采购申请提交成功')
        form.resetFields()
        setPurchaseItems([])
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

  const purchaseColumns = [
    {
      title: '物品名称',
      dataIndex: 'name',
      width: 150,
      render: (text: string, record: PurchaseItem) => (
        <Input
          value={text}
          onChange={(e) => handleUpdatePurchaseItem(record.key, 'name', e.target.value)}
          placeholder="物品名称"
        />
      ),
    },
    {
      title: '规格型号',
      dataIndex: 'specification',
      width: 150,
      render: (text: string, record: PurchaseItem) => (
        <Input
          value={text}
          onChange={(e) =>
            handleUpdatePurchaseItem(record.key, 'specification', e.target.value)
          }
          placeholder="规格型号"
        />
      ),
    },
    {
      title: '数量',
      dataIndex: 'quantity',
      width: 100,
      render: (text: number, record: PurchaseItem) => (
        <InputNumber
          value={text}
          onChange={(value) => handleUpdatePurchaseItem(record.key, 'quantity', value || 1)}
          min={1}
          style={{ width: '100%' }}
        />
      ),
    },
    {
      title: '单位',
      dataIndex: 'unit',
      width: 80,
      render: (text: string, record: PurchaseItem) => (
        <Select
          value={text}
          onChange={(value) => handleUpdatePurchaseItem(record.key, 'unit', value)}
          style={{ width: '100%' }}
        >
          <Option value="个">个</Option>
          <Option value="台">台</Option>
          <Option value="套">套</Option>
          <Option value="件">件</Option>
          <Option value="箱">箱</Option>
          <Option value="批">批</Option>
        </Select>
      ),
    },
    {
      title: '单价（元）',
      dataIndex: 'unitPrice',
      width: 120,
      render: (text: number, record: PurchaseItem) => (
        <InputNumber
          value={text}
          onChange={(value) => handleUpdatePurchaseItem(record.key, 'unitPrice', value || 0)}
          min={0}
          precision={2}
          style={{ width: '100%' }}
        />
      ),
    },
    {
      title: '总价（元）',
      dataIndex: 'totalPrice',
      width: 120,
      render: (text: number) => <span style={{ color: '#f5222d' }}>¥ {text.toFixed(2)}</span>,
    },
    {
      title: '供应商',
      dataIndex: 'supplier',
      width: 150,
      render: (text: string, record: PurchaseItem) => (
        <Input
          value={text}
          onChange={(e) => handleUpdatePurchaseItem(record.key, 'supplier', e.target.value)}
          placeholder="供应商"
        />
      ),
    },
    {
      title: '操作',
      width: 80,
      fixed: 'right' as const,
      render: (_: any, record: PurchaseItem) => (
        <Button
          type="link"
          danger
          icon={<DeleteOutlined />}
          onClick={() => handleDeletePurchaseItem(record.key)}
          disabled={purchaseItems.length === 1}
        >
          删除
        </Button>
      ),
    },
  ]

  return (
    <Card title="采购申请" extra={<Button onClick={() => navigate(-1)}>返回</Button>}>
      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        initialValues={{
          applicant: user?.realName || user?.username,
          expectedDate: dayjs().add(7, 'day'),
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
              label="采购类型"
              name="purchaseType"
              rules={[{ required: true, message: '请选择采购类型' }]}
            >
              <Select placeholder="请选择">
                <Option value="equipment">设备采购</Option>
                <Option value="office">办公用品</Option>
                <Option value="software">软件采购</Option>
                <Option value="service">服务采购</Option>
                <Option value="other">其他</Option>
              </Select>
            </Form.Item>
          </Col>

          <Col span={8}>
            <Form.Item
              label="期望到货日期"
              name="expectedDate"
              rules={[{ required: true, message: '请选择期望到货日期' }]}
            >
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>
          </Col>

          <Col span={24}>
            <Form.Item label="采购清单">
              <Table
                columns={purchaseColumns}
                dataSource={purchaseItems}
                pagination={false}
                scroll={{ x: 1200 }}
                footer={() => (
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Button
                      type="dashed"
                      onClick={handleAddPurchaseItem}
                      icon={<PlusOutlined />}
                    >
                      添加采购项
                    </Button>
                    <div style={{ fontSize: '16px', fontWeight: 'bold' }}>
                      合计：
                      <span style={{ color: '#f5222d', marginLeft: 8 }}>
                        ¥ {calculateTotalAmount().toFixed(2)}
                      </span>
                    </div>
                  </div>
                )}
              />
            </Form.Item>
          </Col>

          <Col span={24}>
            <Form.Item
              label="采购理由"
              name="reason"
              rules={[
                { required: true, message: '请输入采购理由' },
                { min: 10, message: '采购理由至少10个字符' },
              ]}
            >
              <TextArea
                rows={4}
                placeholder="请详细说明采购理由和用途"
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
                <Option value="finance">财务主管</Option>
              </Select>
            </Form.Item>
          </Col>

          <Col span={24}>
            <Form.Item label="报价单附件" name="attachments" extra="请上传供应商报价单等相关文件">
              <Upload
                fileList={fileList}
                onChange={({ fileList }) => setFileList(fileList)}
                beforeUpload={() => false}
                accept=".pdf,.doc,.docx,.xls,.xlsx,image/*"
              >
                <Button icon={<PlusOutlined />}>上传附件</Button>
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

export default PurchaseApproval
