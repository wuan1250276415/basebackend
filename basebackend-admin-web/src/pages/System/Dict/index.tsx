import { useState, useEffect } from 'react'
import {
  Row,
  Col,
  Card,
  Table,
  Button,
  Space,
  Input,
  Form,
  Modal,
  message,
  Tag,
  Popconfirm,
  Select,
  InputNumber,
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SearchOutlined,
  ReloadOutlined,
  SyncOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import {
  getDictPage,
  createDict,
  updateDict,
  deleteDict,
  getDictDataPage,
  createDictData,
  updateDictData,
  deleteDictData,
  refreshDictCache,
} from '@/api/dict'
import { getEnabledApplications } from '@/api/application'
import { Dict, DictData, Application } from '@/types'

const DictList = () => {
  // 字典表单
  const [dictForm] = Form.useForm()
  const [dictSearchForm] = Form.useForm()
  const [dictDataForm] = Form.useForm()
  const [dictDataSearchForm] = Form.useForm()

  // 应用列表
  const [applications, setApplications] = useState<Application[]>([])

  // 字典状态
  const [dictLoading, setDictLoading] = useState(false)
  const [dictDataSource, setDictDataSource] = useState<Dict[]>([])
  const [dictTotal, setDictTotal] = useState(0)
  const [dictCurrent, setDictCurrent] = useState(1)
  const [dictPageSize, setDictPageSize] = useState(10)
  const [dictModalVisible, setDictModalVisible] = useState(false)
  const [dictModalTitle, setDictModalTitle] = useState('新增字典')
  const [editingDictId, setEditingDictId] = useState<string | null>(null)

  // 字典数据状态
  const [dictDataLoading, setDictDataLoading] = useState(false)
  const [dictDataDataSource, setDictDataDataSource] = useState<DictData[]>([])
  const [dictDataTotal, setDictDataTotal] = useState(0)
  const [dictDataCurrent, setDictDataCurrent] = useState(1)
  const [dictDataPageSize, setDictDataPageSize] = useState(10)
  const [dictDataModalVisible, setDictDataModalVisible] = useState(false)
  const [dictDataModalTitle, setDictDataModalTitle] = useState('新增字典数据')
  const [editingDictDataId, setEditingDictDataId] = useState<string | null>(null)
  const [selectedDictType, setSelectedDictType] = useState<string>('')
  const [selectedDictAppId, setSelectedDictAppId] = useState<string | undefined>(undefined)

  // 加载应用列表
  const loadApplications = async () => {
    try {
      const response = await getEnabledApplications()
      setApplications(response.data)
    } catch (error) {
      console.error('加载应用列表失败', error)
    }
  }

  // 加载字典列表
  const loadDictData = async (page = dictCurrent, size = dictPageSize) => {
    setDictLoading(true)
    try {
      const searchValues = dictSearchForm.getFieldsValue()
      const response = await getDictPage({
        current: page,
        size,
        ...searchValues,
      })
      setDictDataSource(response.data.records)
      setDictTotal(response.data.total)
      setDictCurrent(response.data.current)
      setDictPageSize(response.data.size)
    } catch (error) {
      message.error('加载字典列表失败')
    } finally {
      setDictLoading(false)
    }
  }

  // 加载字典数据列表
  const loadDictDataData = async (page = dictDataCurrent, size = dictDataPageSize) => {
    if (!selectedDictType) {
      return
    }
    setDictDataLoading(true)
    try {
      const searchValues = dictDataSearchForm.getFieldsValue()
      const response = await getDictDataPage({
        current: page,
        size,
        dictType: selectedDictType,
        ...searchValues,
      })
      setDictDataDataSource(response.data.records)
      setDictDataTotal(response.data.total)
      setDictDataCurrent(response.data.current)
      setDictDataPageSize(response.data.size)
    } catch (error) {
      message.error('加载字典数据列表失败')
    } finally {
      setDictDataLoading(false)
    }
  }

  useEffect(() => {
    loadDictData()
    loadApplications()
  }, [])

  useEffect(() => {
    if (selectedDictType) {
      loadDictDataData(1)
    } else {
      setDictDataDataSource([])
    }
  }, [selectedDictType])

  // 获取应用名称
  const getAppName = (appId?: string) => {
    if (!appId) return <Tag>系统字典</Tag>
    const app = applications.find((a) => a.id === appId)
    return app ? <Tag color="blue">{app.appName}</Tag> : '-'
  }

  // 打开字典弹窗
  const handleOpenDictModal = (record?: Dict) => {
    if (record) {
      setDictModalTitle('编辑字典')
      setEditingDictId(record.id!)
      dictForm.setFieldsValue(record)
    } else {
      setDictModalTitle('新增字典')
      setEditingDictId(null)
      dictForm.resetFields()
      dictForm.setFieldsValue({ status: 1 })
    }
    setDictModalVisible(true)
  }

  // 提交字典表单
  const handleDictSubmit = async () => {
    try {
      const values = await dictForm.validateFields()
      if (editingDictId) {
        await updateDict(editingDictId, values)
        message.success('更新成功')
      } else {
        await createDict(values)
        message.success('创建成功')
      }
      setDictModalVisible(false)
      loadDictData()
    } catch (error: any) {
      if (error.errorFields) {
        message.error('请填写必填项')
      }
    }
  }

  // 删除字典
  const handleDeleteDict = async (id: string) => {
    try {
      await deleteDict(id)
      message.success('删除成功')
      loadDictData()
      if (selectedDictType) {
        setSelectedDictType('')
      }
    } catch (error) {
      message.error('删除失败')
    }
  }

  // 打开字典数据弹窗
  const handleOpenDictDataModal = (record?: DictData) => {
    if (record) {
      setDictDataModalTitle('编辑字典数据')
      setEditingDictDataId(record.id!)
      dictDataForm.setFieldsValue(record)
    } else {
      setDictDataModalTitle('新增字典数据')
      setEditingDictDataId(null)
      dictDataForm.resetFields()
      dictDataForm.setFieldsValue({
        dictType: selectedDictType,
        appId: selectedDictAppId,
        status: 1,
        isDefault: 0,
      })
    }
    setDictDataModalVisible(true)
  }

  // 提交字典数据表单
  const handleDictDataSubmit = async () => {
    try {
      const values = await dictDataForm.validateFields()
      if (editingDictDataId) {
        await updateDictData(editingDictDataId, values)
        message.success('更新成功')
      } else {
        await createDictData(values)
        message.success('创建成功')
      }
      setDictDataModalVisible(false)
      loadDictDataData()
    } catch (error: any) {
      if (error.errorFields) {
        message.error('请填写必填项')
      }
    }
  }

  // 删除字典数据
  const handleDeleteDictData = async (id: string) => {
    try {
      await deleteDictData(id)
      message.success('删除成功')
      loadDictDataData()
    } catch (error) {
      message.error('删除失败')
    }
  }

  // 刷新缓存
  const handleRefreshCache = async () => {
    try {
      await refreshDictCache()
      message.success('缓存刷新成功')
    } catch (error) {
      message.error('缓存刷新失败')
    }
  }

  // 查看字典数据列表
  const handleViewDictData = (record: Dict) => {
    setSelectedDictType(record.dictType)
    setSelectedDictAppId(record.appId)
  }

  const dictColumns: ColumnsType<Dict> = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
    { title: '字典名称', dataIndex: 'dictName', key: 'dictName', width: 150 },
    { title: '字典类型', dataIndex: 'dictType', key: 'dictType', width: 150 },
    {
      title: '所属应用',
      dataIndex: 'appId',
      key: 'appId',
      width: 150,
      render: (appId: string) => getAppName(appId),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: number) =>
        status === 1 ? <Tag color="success">启用</Tag> : <Tag color="error">禁用</Tag>,
    },
    { title: '备注', dataIndex: 'remark', key: 'remark' },
    { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 220,
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            onClick={() => handleViewDictData(record)}
          >
            数据列表
          </Button>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleOpenDictModal(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定要删除吗?"
            onConfirm={() => handleDeleteDict(record.id!)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const dictDataColumns: ColumnsType<DictData> = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
    { title: '字典标签', dataIndex: 'dictLabel', key: 'dictLabel' },
    { title: '字典值', dataIndex: 'dictValue', key: 'dictValue' },
    { title: '排序', dataIndex: 'dictSort', key: 'dictSort', width: 80 },
    { title: '样式类', dataIndex: 'cssClass', key: 'cssClass' },
    {
      title: '默认',
      dataIndex: 'isDefault',
      key: 'isDefault',
      render: (isDefault: number) =>
        isDefault === 1 ? <Tag color="blue">是</Tag> : <Tag>否</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: number) =>
        status === 1 ? <Tag color="success">启用</Tag> : <Tag color="error">禁用</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 180,
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleOpenDictDataModal(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定要删除吗?"
            onConfirm={() => handleDeleteDictData(record.id!)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Row gutter={16}>
        {/* 左侧：字典列表 */}
        <Col span={selectedDictType ? 10 : 24}>
          <Card style={{ marginBottom: 16 }}>
            <Form form={dictSearchForm} layout="inline">
              <Form.Item name="appId" label="所属应用">
                <Select
                  placeholder="请选择应用"
                  allowClear
                  style={{ width: 200 }}
                  options={[
                    { label: '全部', value: undefined },
                    { label: '系统字典', value: '' },
                    ...applications.map((app) => ({
                      label: app.appName,
                      value: app.id,
                    })),
                  ]}
                />
              </Form.Item>
              <Form.Item name="dictName" label="字典名称">
                <Input placeholder="请输入字典名称" allowClear />
              </Form.Item>
              <Form.Item name="dictType" label="字典类型">
                <Input placeholder="请输入字典类型" allowClear />
              </Form.Item>
              <Form.Item name="status" label="状态">
                <Select placeholder="请选择状态" allowClear style={{ width: 120 }}>
                  <Select.Option value={1}>启用</Select.Option>
                  <Select.Option value={0}>禁用</Select.Option>
                </Select>
              </Form.Item>
              <Form.Item>
                <Space>
                  <Button type="primary" icon={<SearchOutlined />} onClick={() => loadDictData(1)}>
                    查询
                  </Button>
                  <Button
                    icon={<ReloadOutlined />}
                    onClick={() => {
                      dictSearchForm.resetFields()
                      loadDictData(1)
                    }}
                  >
                    重置
                  </Button>
                </Space>
              </Form.Item>
            </Form>
          </Card>

          <Card
            title="字典列表"
            extra={
              <Space>
                <Button icon={<SyncOutlined />} onClick={handleRefreshCache}>
                  刷新缓存
                </Button>
                <Button type="primary" icon={<PlusOutlined />} onClick={() => handleOpenDictModal()}>
                  新增字典
                </Button>
              </Space>
            }
          >
            <Table
              loading={dictLoading}
              dataSource={dictDataSource}
              columns={dictColumns}
              rowKey="id"
              scroll={{ x: 1200 }}
              pagination={{
                current: dictCurrent,
                pageSize: dictPageSize,
                total: dictTotal,
                showSizeChanger: true,
                showQuickJumper: true,
                showTotal: (total) => `共 ${total} 条`,
                onChange: (page, size) => {
                  setDictCurrent(page)
                  setDictPageSize(size)
                  loadDictData(page, size)
                },
              }}
            />
          </Card>
        </Col>

        {/* 右侧：字典数据列表 */}
        {selectedDictType && (
          <Col span={14}>
            <Card style={{ marginBottom: 16 }}>
              <Form form={dictDataSearchForm} layout="inline">
                <Form.Item name="dictLabel" label="字典标签">
                  <Input placeholder="请输入字典标签" allowClear />
                </Form.Item>
                <Form.Item name="status" label="状态">
                  <Select placeholder="请选择状态" allowClear style={{ width: 120 }}>
                    <Select.Option value={1}>启用</Select.Option>
                    <Select.Option value={0}>禁用</Select.Option>
                  </Select>
                </Form.Item>
                <Form.Item>
                  <Space>
                    <Button type="primary" icon={<SearchOutlined />} onClick={() => loadDictDataData(1)}>
                      查询
                    </Button>
                    <Button
                      icon={<ReloadOutlined />}
                      onClick={() => {
                        dictDataSearchForm.resetFields()
                        loadDictDataData(1)
                      }}
                    >
                      重置
                    </Button>
                  </Space>
                </Form.Item>
              </Form>
            </Card>

            <Card
              title={`字典数据列表 (${selectedDictType}) - ${getAppName(selectedDictAppId)}`}
              extra={
                <Space>
                  <Button onClick={() => setSelectedDictType('')}>关闭</Button>
                  <Button
                    type="primary"
                    icon={<PlusOutlined />}
                    onClick={() => handleOpenDictDataModal()}
                  >
                    新增数据
                  </Button>
                </Space>
              }
            >
              <Table
                loading={dictDataLoading}
                dataSource={dictDataDataSource}
                columns={dictDataColumns}
                rowKey="id"
                pagination={{
                  current: dictDataCurrent,
                  pageSize: dictDataPageSize,
                  total: dictDataTotal,
                  showSizeChanger: true,
                  showQuickJumper: true,
                  showTotal: (total) => `共 ${total} 条`,
                  onChange: (page, size) => {
                    setDictDataCurrent(page)
                    setDictDataPageSize(size)
                    loadDictDataData(page, size)
                  },
                }}
              />
            </Card>
          </Col>
        )}
      </Row>

      {/* 字典弹窗 */}
      <Modal
        title={dictModalTitle}
        open={dictModalVisible}
        onOk={handleDictSubmit}
        onCancel={() => setDictModalVisible(false)}
        width={600}
        destroyOnClose
      >
        <Form form={dictForm} labelCol={{ span: 6 }} wrapperCol={{ span: 16 }}>
          <Form.Item name="appId" label="所属应用">
            <Select placeholder="请选择所属应用（系统字典请不选）" allowClear>
              {applications.map((app) => (
                <Select.Option key={app.id} value={app.id}>
                  {app.appName}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            name="dictName"
            label="字典名称"
            rules={[{ required: true, message: '请输入字典名称' }]}
          >
            <Input placeholder="请输入字典名称" />
          </Form.Item>

          <Form.Item
            name="dictType"
            label="字典类型"
            rules={[{ required: true, message: '请输入字典类型' }]}
          >
            <Input placeholder="请输入字典类型" disabled={!!editingDictId} />
          </Form.Item>

          <Form.Item name="status" label="状态" initialValue={1}>
            <Select>
              <Select.Option value={1}>启用</Select.Option>
              <Select.Option value={0}>禁用</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={4} placeholder="请输入备注" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 字典数据弹窗 */}
      <Modal
        title={dictDataModalTitle}
        open={dictDataModalVisible}
        onOk={handleDictDataSubmit}
        onCancel={() => setDictDataModalVisible(false)}
        width={600}
        destroyOnClose
      >
        <Form form={dictDataForm} labelCol={{ span: 6 }} wrapperCol={{ span: 16 }}>
          <Form.Item name="dictType" label="字典类型">
            <Input disabled />
          </Form.Item>

          <Form.Item name="appId" label="所属应用">
            <Select placeholder="请选择所属应用（系统字典请不选）" allowClear disabled>
              {applications.map((app) => (
                <Select.Option key={app.id} value={app.id}>
                  {app.appName}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            name="dictLabel"
            label="字典标签"
            rules={[{ required: true, message: '请输入字典标签' }]}
          >
            <Input placeholder="请输入字典标签" />
          </Form.Item>

          <Form.Item
            name="dictValue"
            label="字典值"
            rules={[{ required: true, message: '请输入字典值' }]}
          >
            <Input placeholder="请输入字典值" />
          </Form.Item>

          <Form.Item name="dictSort" label="排序" initialValue={0}>
            <InputNumber min={0} style={{ width: '100%' }} placeholder="请输入排序" />
          </Form.Item>

          <Form.Item name="cssClass" label="样式类">
            <Input placeholder="请输入样式类" />
          </Form.Item>

          <Form.Item name="listClass" label="列表类">
            <Select placeholder="请选择列表类">
              <Select.Option value="default">默认</Select.Option>
              <Select.Option value="primary">主要</Select.Option>
              <Select.Option value="success">成功</Select.Option>
              <Select.Option value="info">信息</Select.Option>
              <Select.Option value="warning">警告</Select.Option>
              <Select.Option value="danger">危险</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item name="isDefault" label="是否默认" initialValue={0}>
            <Select>
              <Select.Option value={1}>是</Select.Option>
              <Select.Option value={0}>否</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item name="status" label="状态" initialValue={1}>
            <Select>
              <Select.Option value={1}>启用</Select.Option>
              <Select.Option value={0}>禁用</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} placeholder="请输入备注" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default DictList
