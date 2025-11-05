import { Card, Descriptions, Tag, Space, Button, Empty } from 'antd'
import { EditOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import { Dept } from '@/types'

interface DeptDetailPanelProps {
  dept: Dept | null
  onEdit: (dept: Dept) => void
  onDelete: (id: string) => void
  onAddChild: (parentDept: Dept) => void
}

const DeptDetailPanel = ({ dept, onEdit, onDelete, onAddChild }: DeptDetailPanelProps) => {
  if (!dept) {
    return (
      <Card title="部门详情" style={{ height: '100%', minHeight: 500 }}>
        <Empty
          description="请在左侧选择一个部门"
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          style={{ marginTop: 100 }}
        />
      </Card>
    )
  }

  return (
    <Card
      title={
        <Space>
          <span>部门详情</span>
          <Tag color="blue">{dept.deptName}</Tag>
        </Space>
      }
      extra={
        <Space>
          <Button
            icon={<PlusOutlined />}
            onClick={() => onAddChild(dept)}
            size="small"
          >
            新增子部门
          </Button>
          <Button
            icon={<EditOutlined />}
            onClick={() => onEdit(dept)}
            type="primary"
            size="small"
          >
            编辑
          </Button>
          <Button
            icon={<DeleteOutlined />}
            onClick={() => onDelete(dept.id!)}
            danger
            size="small"
          >
            删除
          </Button>
        </Space>
      }
      style={{ height: '100%' }}
    >
      <Descriptions column={1} bordered size="small">
        <Descriptions.Item label="部门ID">
          <code
            style={{
              background: '#f5f5f5',
              padding: '2px 6px',
              borderRadius: 4,
              fontSize: 12,
              color: '#8c8c8c',
            }}
          >
            {dept.id}
          </code>
        </Descriptions.Item>

        <Descriptions.Item label="部门名称">
          <strong>{dept.deptName}</strong>
        </Descriptions.Item>

        <Descriptions.Item label="排序号">{dept.orderNum ?? '-'}</Descriptions.Item>

        <Descriptions.Item label="负责人">
          {dept.leader ? <Tag color="blue">{dept.leader}</Tag> : '-'}
        </Descriptions.Item>

        <Descriptions.Item label="联系电话">
          {dept.phone ? (
            <code
              style={{
                background: '#f5f5f5',
                padding: '2px 6px',
                borderRadius: 4,
                fontSize: 12,
              }}
            >
              {dept.phone}
            </code>
          ) : (
            '-'
          )}
        </Descriptions.Item>

        <Descriptions.Item label="邮箱">
          {dept.email ? (
            <code
              style={{
                background: '#f5f5f5',
                padding: '2px 6px',
                borderRadius: 4,
                fontSize: 12,
              }}
            >
              {dept.email}
            </code>
          ) : (
            '-'
          )}
        </Descriptions.Item>

        <Descriptions.Item label="部门状态">
          {dept.status === 1 ? (
            <Tag color="success">启用</Tag>
          ) : (
            <Tag color="error">禁用</Tag>
          )}
        </Descriptions.Item>

        {dept.remark && <Descriptions.Item label="备注">{dept.remark}</Descriptions.Item>}

        <Descriptions.Item label="创建时间">{dept.createTime || '-'}</Descriptions.Item>

        <Descriptions.Item label="更新时间">{dept.updateTime || '-'}</Descriptions.Item>

        {dept.children && dept.children.length > 0 && (
          <Descriptions.Item label="子部门数量">
            <Tag color="cyan">{dept.children.length}</Tag>
          </Descriptions.Item>
        )}
      </Descriptions>
    </Card>
  )
}

export default DeptDetailPanel
