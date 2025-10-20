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
  Modal,
  Upload,
  Form,
  Select,
  Descriptions,
  Drawer,
} from 'antd'
import {
  UploadOutlined,
  DeleteOutlined,
  EyeOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  CloudDownloadOutlined,
  FileTextOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { UploadFile } from 'antd/es/upload/interface'
import dayjs from 'dayjs'

import {
  listProcessDefinitions,
  deployProcessDefinition,
  deleteDeployment,
  suspendProcessDefinition,
  activateProcessDefinition,
  getProcessDefinitionXml,
} from '@/api/workflow/processDefinition'
import type { ProcessDefinition } from '@/types/workflow'

const { Search } = Input
const { Option } = Select
const { confirm } = Modal

const ProcessDefinitionList: React.FC = () => {
  const navigate = useNavigate()
  const [form] = Form.useForm()

  const [loading, setLoading] = useState(false)
  const [definitions, setDefinitions] = useState<ProcessDefinition[]>([])
  const [filteredDefinitions, setFilteredDefinitions] = useState<ProcessDefinition[]>([])
  const [searchText, setSearchText] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('all')
  const [deployModalVisible, setDeployModalVisible] = useState(false)
  const [deployLoading, setDeployLoading] = useState(false)
  const [fileList, setFileList] = useState<UploadFile[]>([])
  const [xmlDrawerVisible, setXmlDrawerVisible] = useState(false)
  const [currentXml, setCurrentXml] = useState<string>('')
  const [currentDefinition, setCurrentDefinition] = useState<ProcessDefinition | null>(null)

  // 加载流程定义
  const loadDefinitions = async () => {
    setLoading(true)
    try {
      const response = await listProcessDefinitions()
      if (response.success) {
        const definitionList = response.data?.list || []
        setDefinitions(definitionList)
        setFilteredDefinitions(definitionList)
      } else {
        message.error(response.message || '加载流程定义失败')
      }
    } catch (error) {
      message.error('加载流程定义失败')
      console.error(error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadDefinitions()
  }, [])

  // 过滤逻辑
  useEffect(() => {
    let filtered = definitions

    // 搜索过滤
    if (searchText) {
      filtered = filtered.filter(
        (def) =>
          def.name?.toLowerCase().includes(searchText.toLowerCase()) ||
          def.key?.toLowerCase().includes(searchText.toLowerCase()) ||
          def.id?.toLowerCase().includes(searchText.toLowerCase())
      )
    }

    // 状态过滤
    if (statusFilter !== 'all') {
      if (statusFilter === 'active') {
        filtered = filtered.filter((def) => !def.suspended)
      } else if (statusFilter === 'suspended') {
        filtered = filtered.filter((def) => def.suspended)
      }
    }

    setFilteredDefinitions(filtered)
  }, [searchText, statusFilter, definitions])

  // 部署流程定义
  const handleDeploy = async (values: any) => {
    if (fileList.length === 0) {
      message.error('请上传BPMN文件')
      return
    }

    setDeployLoading(true)
    try {
      const file = fileList[0].originFileObj as File
      const response = await deployProcessDefinition({
        name: values.name,
        file: file,
      })

      if (response.success) {
        message.success('部署成功')
        setDeployModalVisible(false)
        form.resetFields()
        setFileList([])
        loadDefinitions()
      } else {
        message.error(response.message || '部署失败')
      }
    } catch (error) {
      message.error('部署失败')
      console.error(error)
    } finally {
      setDeployLoading(false)
    }
  }

  // 删除流程定义
  const handleDelete = (definition: ProcessDefinition) => {
    confirm({
      title: '确认删除',
      content: `确定要删除流程定义 "${definition.name}" 吗？此操作将删除所有版本！`,
      okType: 'danger',
      onOk: async () => {
        try {
          const response = await deleteDeployment(definition.deploymentId, true)
          if (response.success) {
            message.success('删除成功')
            loadDefinitions()
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

  // 挂起流程定义
  const handleSuspend = (definition: ProcessDefinition) => {
    confirm({
      title: '确认挂起',
      content: `确定要挂起流程定义 "${definition.name}" 吗？挂起后无法发起新的流程实例。`,
      onOk: async () => {
        try {
          const response = await suspendProcessDefinition(definition.id)
          if (response.success) {
            message.success('挂起成功')
            loadDefinitions()
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

  // 激活流程定义
  const handleActivate = (definition: ProcessDefinition) => {
    confirm({
      title: '确认激活',
      content: `确定要激活流程定义 "${definition.name}" 吗？`,
      onOk: async () => {
        try {
          const response = await activateProcessDefinition(definition.id)
          if (response.success) {
            message.success('激活成功')
            loadDefinitions()
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

  // 查看BPMN XML
  const handleViewXml = async (definition: ProcessDefinition) => {
    try {
      const response = await getProcessDefinitionXml(definition.id)
      if (response.success && response.data) {
        setCurrentXml(response.data.xml)
        setCurrentDefinition(definition)
        setXmlDrawerVisible(true)
      } else {
        message.error(response.message || '获取XML失败')
      }
    } catch (error) {
      message.error('获取XML失败')
      console.error(error)
    }
  }

  // 下载BPMN XML
  const handleDownloadXml = () => {
    if (!currentXml || !currentDefinition) return

    const blob = new Blob([currentXml], { type: 'application/xml' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${currentDefinition.key}_v${currentDefinition.version}.bpmn`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  }

  const columns: ColumnsType<ProcessDefinition> = [
    {
      title: '流程名称',
      dataIndex: 'name',
      key: 'name',
      width: 200,
      render: (text, record) => (
        <Space>
          <FileTextOutlined style={{ color: '#1890ff' }} />
          <span>{text}</span>
          {record.version > 1 && <Tag color="blue">v{record.version}</Tag>}
        </Space>
      ),
    },
    {
      title: '流程标识',
      dataIndex: 'key',
      key: 'key',
      width: 180,
      render: (text) => <Tag color="purple">{text}</Tag>,
    },
    {
      title: '版本号',
      dataIndex: 'version',
      key: 'version',
      width: 100,
      sorter: (a, b) => a.version - b.version,
      render: (text) => <Tag>v{text}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'suspended',
      key: 'suspended',
      width: 100,
      render: (suspended) =>
        suspended ? (
          <Tag color="warning" icon={<PauseCircleOutlined />}>
            已挂起
          </Tag>
        ) : (
          <Tag color="success" icon={<PlayCircleOutlined />}>
            激活
          </Tag>
        ),
    },
    {
      title: '部署ID',
      dataIndex: 'deploymentId',
      key: 'deploymentId',
      width: 180,
      render: (text) => (
        <Tooltip title={text}>
          <span
            style={{
              maxWidth: 150,
              display: 'inline-block',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              fontFamily: 'monospace',
              fontSize: 12,
            }}
          >
            {text}
          </span>
        </Tooltip>
      ),
    },
    {
      title: '资源名称',
      dataIndex: 'resourceName',
      key: 'resourceName',
      width: 200,
      render: (text) => (
        <Tooltip title={text}>
          <span
            style={{
              maxWidth: 180,
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
      title: '部署时间',
      dataIndex: 'deploymentTime',
      key: 'deploymentTime',
      width: 180,
      sorter: (a, b) => dayjs(a.deploymentTime).unix() - dayjs(b.deploymentTime).unix(),
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
      title: '操作',
      key: 'action',
      width: 280,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handleViewXml(record)}
          >
            查看
          </Button>
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
          <Button
            type="link"
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record)}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <>
      <Card
        title="流程定义管理"
        extra={
          <Space>
            <Button type="primary" icon={<UploadOutlined />} onClick={() => setDeployModalVisible(true)}>
              部署流程
            </Button>
            <Button onClick={loadDefinitions}>刷新</Button>
          </Space>
        }
      >
        {/* 筛选区域 */}
        <Space style={{ marginBottom: 16 }}>
          <Search
            placeholder="搜索流程名称、标识或ID"
            allowClear
            style={{ width: 300 }}
            onSearch={setSearchText}
            onChange={(e) => setSearchText(e.target.value)}
          />
          <Select
            placeholder="流程状态"
            style={{ width: 150 }}
            value={statusFilter}
            onChange={setStatusFilter}
          >
            <Option value="all">全部状态</Option>
            <Option value="active">激活</Option>
            <Option value="suspended">已挂起</Option>
          </Select>
        </Space>

        {/* 统计信息 */}
        <div style={{ marginBottom: 16 }}>
          <Space size="large">
            <span>
              总流程定义：<strong style={{ color: '#1890ff', fontSize: 18 }}>{definitions.length}</strong>
            </span>
            <span>
              激活：
              <strong style={{ color: '#52c41a', fontSize: 18 }}>
                {definitions.filter((d) => !d.suspended).length}
              </strong>
            </span>
            <span>
              已挂起：
              <strong style={{ color: '#faad14', fontSize: 18 }}>
                {definitions.filter((d) => d.suspended).length}
              </strong>
            </span>
          </Space>
        </div>

        {/* 表格 */}
        <Table
          columns={columns}
          dataSource={filteredDefinitions}
          rowKey="id"
          loading={loading}
          pagination={{
            total: filteredDefinitions.length,
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
          }}
          scroll={{ x: 1500 }}
        />
      </Card>

      {/* 部署流程模态框 */}
      <Modal
        title="部署流程定义"
        open={deployModalVisible}
        onCancel={() => {
          setDeployModalVisible(false)
          form.resetFields()
          setFileList([])
        }}
        footer={null}
        width={600}
      >
        <Form form={form} layout="vertical" onFinish={handleDeploy}>
          <Form.Item
            label="流程名称"
            name="name"
            rules={[{ required: true, message: '请输入流程名称' }]}
          >
            <Input placeholder="例如：请假审批流程" />
          </Form.Item>

          <Form.Item
            label="BPMN 文件"
            required
            help="请上传符合 BPMN 2.0 规范的 .bpmn 或 .xml 文件"
          >
            <Upload
              fileList={fileList}
              onChange={({ fileList }) => setFileList(fileList)}
              beforeUpload={() => false}
              accept=".bpmn,.xml"
              maxCount={1}
            >
              <Button icon={<UploadOutlined />}>选择文件</Button>
            </Upload>
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" loading={deployLoading}>
                部署
              </Button>
              <Button onClick={() => setDeployModalVisible(false)}>取消</Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* BPMN XML 查看抽屉 */}
      <Drawer
        title="BPMN 流程定义"
        placement="right"
        width={800}
        open={xmlDrawerVisible}
        onClose={() => setXmlDrawerVisible(false)}
        extra={
          <Button icon={<CloudDownloadOutlined />} onClick={handleDownloadXml}>
            下载
          </Button>
        }
      >
        {currentDefinition && (
          <Descriptions column={2} bordered style={{ marginBottom: 16 }}>
            <Descriptions.Item label="流程名称">{currentDefinition.name}</Descriptions.Item>
            <Descriptions.Item label="流程标识">{currentDefinition.key}</Descriptions.Item>
            <Descriptions.Item label="版本号">v{currentDefinition.version}</Descriptions.Item>
            <Descriptions.Item label="状态">
              {currentDefinition.suspended ? (
                <Tag color="warning">已挂起</Tag>
              ) : (
                <Tag color="success">激活</Tag>
              )}
            </Descriptions.Item>
          </Descriptions>
        )}

        <Card title="BPMN XML" size="small">
          <pre
            style={{
              maxHeight: 600,
              overflow: 'auto',
              backgroundColor: '#f5f5f5',
              padding: 16,
              borderRadius: 4,
              fontSize: 12,
              lineHeight: 1.5,
            }}
          >
            {currentXml}
          </pre>
        </Card>
      </Drawer>
    </>
  )
}

export default ProcessDefinitionList
