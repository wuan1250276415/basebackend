import React, { useState, useEffect } from 'react'
import {
    Card,
    Table,
    Button,
    Space,
    Modal,
    Form,
    Input,
    Select,
    message,
    Popconfirm,
    Tag,
} from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'

import {
    listFormTemplates,
    createFormTemplate,
    updateFormTemplate,
    deleteFormTemplate,
} from '@/api/workflow/formTemplate'
import type { FormTemplate, FormTemplateCreateParams, FormTemplateUpdateParams } from '@/types/workflow'

const { TextArea } = Input
const { Option } = Select

const FormTemplateList: React.FC = () => {
    const [loading, setLoading] = useState(false)
    const [data, setData] = useState<FormTemplate[]>([])
    const [total, setTotal] = useState(0)
    const [current, setCurrent] = useState(1)
    const [size, setSize] = useState(10)
    const [searchText, setSearchText] = useState('')

    const [modalVisible, setModalVisible] = useState(false)
    const [currentTemplate, setCurrentTemplate] = useState<FormTemplate | null>(null)
    const [form] = Form.useForm()

    const loadData = async (page = current, pageSize = size) => {
        setLoading(true)
        try {
            const response = await listFormTemplates({
                current: page,
                size: pageSize,
                keyword: searchText,
            })
            if (response.code === 200) {
                setData(response.data.records)
                setTotal(response.data.total)
                setCurrent(response.data.current)
                setSize(response.data.size)
            } else {
                message.error(response.message || '加载表单模板失败')
            }
        } catch (error) {
            message.error('加载表单模板失败')
            console.error(error)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        loadData()
    }, [])

    const handleSearch = (value: string) => {
        setSearchText(value)
        loadData(1, size)
    }

    const handleCreate = () => {
        setCurrentTemplate(null)
        form.resetFields()
        setModalVisible(true)
    }

    const handleEdit = (record: FormTemplate) => {
        setCurrentTemplate(record)
        form.setFieldsValue({
            name: record.name,
            description: record.description,
            type: record.type,
            content: record.content,
        })
        setModalVisible(true)
    }

    const handleDelete = async (id: string) => {
        try {
            const response = await deleteFormTemplate(id)
            if (response.code === 200) {
                message.success('删除成功')
                loadData()
            } else {
                message.error(response.message || '删除失败')
            }
        } catch (error) {
            message.error('删除失败')
            console.error(error)
        }
    }

    const handleModalOk = async () => {
        try {
            const values = await form.validateFields()

            let response
            if (currentTemplate) {
                // Update
                const params: FormTemplateUpdateParams = {
                    name: values.name,
                    description: values.description,
                    content: values.content
                }
                response = await updateFormTemplate(currentTemplate.id, params)
            } else {
                // Create
                const params: FormTemplateCreateParams = {
                    name: values.name,
                    description: values.description,
                    type: values.type,
                    content: values.content
                }
                response = await createFormTemplate(params)
            }

            if (response.code === 200) {
                message.success(currentTemplate ? '更新成功' : '创建成功')
                setModalVisible(false)
                loadData()
            } else {
                message.error(response.message || '操作失败')
            }
        } catch (error) {
            console.error(error)
        }
    }

    const columns: ColumnsType<FormTemplate> = [
        {
            title: '模板名称',
            dataIndex: 'name',
            key: 'name',
        },
        {
            title: '类型',
            dataIndex: 'type',
            key: 'type',
            render: (text) => <Tag color="blue">{text}</Tag>
        },
        {
            title: '版本',
            dataIndex: 'version',
            key: 'version',
            render: (text) => <Tag>v{text}</Tag>
        },
        {
            title: '描述',
            dataIndex: 'description',
            key: 'description',
            ellipsis: true,
        },
        {
            title: '创建时间',
            dataIndex: 'createTime',
            key: 'createTime',
            render: (text) => dayjs(text).format('YYYY-MM-DD HH:mm:ss'),
        },
        {
            title: '操作',
            key: 'action',
            render: (_, record) => (
                <Space>
                    <Button
                        type="primary"
                        size="small"
                        icon={<EditOutlined />}
                        onClick={() => handleEdit(record)}
                    >
                        编辑
                    </Button>
                    <Popconfirm title="确定要删除吗？" onConfirm={() => handleDelete(record.id)}>
                        <Button type="primary" size="small" danger icon={<DeleteOutlined />}>
                            删除
                        </Button>
                    </Popconfirm>
                </Space>
            ),
        },
    ]

    return (
        <Card
            title="表单模板管理"
            extra={
                <Space>
                    <Input.Search
                        placeholder="搜索模板名称"
                        onSearch={handleSearch}
                        style={{ width: 200 }}
                    />
                    <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
                        新建模板
                    </Button>
                </Space>
            }
        >
            <Table
                columns={columns}
                dataSource={data}
                rowKey="id"
                loading={loading}
                pagination={{
                    current,
                    pageSize: size,
                    total,
                    showSizeChanger: true,
                    onChange: (page, pageSize) => loadData(page, pageSize),
                }}
            />

            <Modal
                title={currentTemplate ? '编辑模板' : '新建模板'}
                open={modalVisible}
                onOk={handleModalOk}
                onCancel={() => setModalVisible(false)}
                width={800}
            >
                <Form form={form} layout="vertical">
                    <Form.Item
                        name="name"
                        label="模板名称"
                        rules={[{ required: true, message: '请输入模板名称' }]}
                    >
                        <Input placeholder="请输入模板名称" />
                    </Form.Item>
                    <Form.Item
                        name="type"
                        label="模板类型"
                        rules={[{ required: true, message: '请选择模板类型' }]}
                    >
                        <Select placeholder="请选择模板类型" disabled={!!currentTemplate}>
                            <Option value="form">普通表单</Option>
                            <Option value="process">流程表单</Option>
                        </Select>
                    </Form.Item>
                    <Form.Item name="description" label="描述">
                        <TextArea rows={2} placeholder="请输入描述" />
                    </Form.Item>
                    <Form.Item
                        name="content"
                        label="表单内容 (JSON)"
                        rules={[{ required: true, message: '请输入表单内容' }]}
                    >
                        <TextArea rows={10} placeholder="请输入JSON格式的表单内容" style={{ fontFamily: 'monospace' }} />
                    </Form.Item>
                </Form>
            </Modal>
        </Card>
    )
}

export default FormTemplateList
