import React from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, Row, Col, Button, Descriptions, Tag } from 'antd'
import {
  FileTextOutlined,
  DollarOutlined,
  ShoppingCartOutlined,
  FormOutlined,
} from '@ant-design/icons'

interface TemplateCardProps {
  title: string
  description: string
  icon: React.ReactNode
  route: string
  color: string
  features: string[]
}

const TemplateCard: React.FC<TemplateCardProps> = ({
  title,
  description,
  icon,
  route,
  color,
  features,
}) => {
  const navigate = useNavigate()

  return (
    <Card
      hoverable
      style={{ height: '100%' }}
      actions={[
        <Button
          type="primary"
          icon={<FormOutlined />}
          onClick={() => navigate(route)}
          style={{ backgroundColor: color, borderColor: color }}
        >
          发起申请
        </Button>,
      ]}
    >
      <Card.Meta
        avatar={
          <div
            style={{
              fontSize: 48,
              color: color,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            {icon}
          </div>
        }
        title={<h3 style={{ margin: 0 }}>{title}</h3>}
        description={
          <div style={{ marginTop: 16 }}>
            <p style={{ color: '#666', marginBottom: 16 }}>{description}</p>
            <div>
              <strong style={{ fontSize: 12, color: '#999' }}>主要功能：</strong>
              <div style={{ marginTop: 8 }}>
                {features.map((feature, index) => (
                  <Tag key={index} style={{ marginTop: 4 }}>
                    {feature}
                  </Tag>
                ))}
              </div>
            </div>
          </div>
        }
      />
    </Card>
  )
}

const ProcessTemplateIndex: React.FC = () => {
  const templates: TemplateCardProps[] = [
    {
      title: '请假申请',
      description: '提交各类请假申请，包括年假、病假、事假、婚假、产假等',
      icon: <FileTextOutlined />,
      route: '/workflow/template/leave',
      color: '#1890ff',
      features: ['请假类型选择', '日期范围选择', '自动计算天数', '附件上传', '审批人指定'],
    },
    {
      title: '报销申请',
      description: '提交各类费用报销申请，支持多条费用明细和发票上传',
      icon: <DollarOutlined />,
      route: '/workflow/template/expense',
      color: '#52c41a',
      features: ['费用分类', '多条明细', '自动汇总', '发票上传', '必填附件验证'],
    },
    {
      title: '采购申请',
      description: '提交采购需求申请，支持多物品采购和供应商报价',
      icon: <ShoppingCartOutlined />,
      route: '/workflow/template/purchase',
      color: '#faad14',
      features: ['采购清单', '规格型号', '自动计价', '供应商管理', '报价单上传'],
    },
  ]

  return (
    <div>
      <Card
        title="流程申请模板"
        extra={
          <span style={{ fontSize: 14, color: '#999', fontWeight: 'normal' }}>
            请选择要发起的流程类型
          </span>
        }
      >
        <Row gutter={[24, 24]}>
          {templates.map((template, index) => (
            <Col xs={24} sm={24} md={12} lg={8} key={index}>
              <TemplateCard {...template} />
            </Col>
          ))}
        </Row>

        <Card
          title="使用说明"
          type="inner"
          style={{ marginTop: 24, backgroundColor: '#fafafa' }}
        >
          <Descriptions column={1} size="small">
            <Descriptions.Item label="步骤 1">
              选择对应的流程模板，点击"发起申请"按钮
            </Descriptions.Item>
            <Descriptions.Item label="步骤 2">
              填写表单信息，确保所有必填项都已填写
            </Descriptions.Item>
            <Descriptions.Item label="步骤 3">
              选择审批人，上传相关附件（如有需要）
            </Descriptions.Item>
            <Descriptions.Item label="步骤 4">
              提交申请后，可在"我发起的"页面查看审批进度
            </Descriptions.Item>
            <Descriptions.Item label="提示">
              <Tag color="blue">审批通知会通过邮件或站内消息发送</Tag>
              <Tag color="green">可以在待办任务中查看需要您审批的流程</Tag>
            </Descriptions.Item>
          </Descriptions>
        </Card>
      </Card>
    </div>
  )
}

export default ProcessTemplateIndex
